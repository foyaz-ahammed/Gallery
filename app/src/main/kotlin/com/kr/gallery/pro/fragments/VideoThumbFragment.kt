package com.kr.gallery.pro.fragments

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kr.commons.extensions.*
import com.kr.gallery.pro.R
import com.kr.gallery.pro.activities.ViewPagerActivity
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Medium
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.pager_video_thumb_item.gestures_view
import kotlinx.android.synthetic.main.pager_video_thumb_item.view.*
import kotlinx.android.synthetic.main.pager_video_thumb_item.view.gestures_view
import kotlin.math.abs

class VideoThumbFragment(private var startTransitionKey: String?) : ViewPagerFragment(){

    constructor(): this("")

    private val PROGRESS = "progress"

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mCurrentGestureViewZoom = 1f

    private lateinit var mView: View
    private lateinit var mMedium: Medium
    private lateinit var mConfig: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(savedInstanceState != null) {
            startTransitionKey = savedInstanceState.getString(TRANSITION_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mMedium = requireArguments().getSerializable(MEDIUM) as Medium
        mConfig = requireContext().config
        mView = inflater.inflate(R.layout.pager_video_thumb_item, container, false).apply {
            gestures_view.setOnClickListener{ toggleFullscreen() }
            gestures_view.setViewPagerActivity(activity as ViewPagerActivity)
            gestures_view.setPlayIcon(video_play_button)

            //  MediaActivity의 itemClicked에서 sharedImageTransitionName과 같아야 한다
            gestures_view.transitionName = mMedium.path.hashCode().toString()

            video_play_button.setOnClickListener {
                launchVideoPlayer()
            }

            gestures_view.controller.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
                override fun onStateChanged(state: State) {
                    // pagerTransformer의 scale을 설정하여 viewpager를 확대축소 진행한다

                    var pagerTransformZoom = 1f
                    if (activity != null) {
                        pagerTransformZoom = (activity as ViewPagerActivity).getPagerTransformerScale()
                    }

                    // 일반 확대축소이다
                    // 1f를 포함시키면 페지를 넘길때 reset이 되면서 viewpager가 다시 커지는 현상이 있다
                    if (state.zoom < 1f && state.zoom > 0f) {
                        pagerTransformZoom = state.zoom
                        val delta1f = abs(1f - state.zoom)
                        if ( delta1f > 0 && delta1f < 0.01) pagerTransformZoom = 1f
                    }

                    if (activity != null) {
                        (activity as ViewPagerActivity).setPagerTransformerScale(pagerTransformZoom)
                    }

                    mCurrentGestureViewZoom = state.zoom
                }

                override fun onDelete() {
                    if (activity != null) {
                        (activity as ViewPagerActivity).runRemoveAnimation()
                    }
                }
            })

            gestures_view.setOnTouchListener { v, event ->
                if (mCurrentGestureViewZoom == 1f) {
                    handleEvent(event)
                }
                false
            }
        }

        if (!requireArguments().getBoolean(SHOULD_INIT_FRAGMENT, true)) {
            return mView
        }

        checkScreenDimensions()
        loadBitmap(true)

        return mView
    }

    override fun updateStateZoom(scale: Float) {
        if (gestures_view != null) {
            val currentStateZoom = gestures_view.controller.state.zoom
            if (currentStateZoom <= 1f) {
                gestures_view.controller.state.zoom = scale
                gestures_view.controller.resetPrevState()
            }
        }
    }
    override fun setIsCurrentFrag(center: Boolean) {
        mIsCurrentFrag = center
        if(gestures_view != null) gestures_view.setIsCenterFrag(center)
    }
    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()

        if (gestures_view.transitionName == startTransitionKey) {
            gestures_view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    gestures_view.viewTreeObserver.removeOnPreDrawListener(this)
                    Handler().postDelayed(Runnable {
                        // start background transparent to make MediaActivity fade-out
                        activity!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                        activity!!.startPostponedEnterTransition()
                    }, 70)
                    return true
                }
            })
        }
    }

    private fun getFilePathToShow() = getPathToLoad(mMedium)

    private fun loadBitmap(addZoomableView: Boolean = true) {
        val priority = Priority.IMMEDIATE
        val options = RequestOptions()
                .signature(getFilePathToShow().getFileSignature())
                .format(DecodeFormat.PREFER_ARGB_8888)
                .priority(priority)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .fitCenter()

        if (context == null) {
            return
        }

        Glide.with(requireContext())
                .load(getFilePathToShow())
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        if (activity != null && !activity!!.isDestroyed && !activity!!.isFinishing) {
                            tryLoadingWithPicasso(addZoomableView)
                        }
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        mView.gestures_view.controller.settings.isZoomEnabled = true

                        startPostponedEnterTransition()

                        return false
                    }
                }).into(mView.gestures_view)
    }

    private fun tryLoadingWithPicasso(addZoomableView: Boolean) {
        var pathToLoad = if (getFilePathToShow().startsWith("content://")) getFilePathToShow() else "file://${getFilePathToShow()}"
        pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")

        try {
            val picasso = Picasso.get()
                    .load(pathToLoad)
                    .centerInside()
                    .stableKey(mMedium.path.getFileKey())
                    .resize(mScreenWidth, mScreenHeight)

            picasso.into(mView.gestures_view, object : Callback {
                override fun onSuccess() {
                    mView.gestures_view.controller.settings.isZoomEnabled = true

                    startPostponedEnterTransition()
                }

                override fun onError(e: Exception?) {
                }
            })
        } catch (ignored: Exception) {
        }
    }

    private fun checkScreenDimensions() {
        if (mScreenWidth == 0 || mScreenHeight == 0) {
            measureScreen()
        }
    }

    private fun measureScreen() {
        val metrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getRealMetrics(metrics)
        mScreenWidth = metrics.widthPixels
        mScreenHeight = metrics.heightPixels
    }
    /**
     * Returns the shared element that should be transitioned back to the previous Activity,
     * or null if the view is not visible on the screen.
     */
    @Nullable
    override fun getGesturesView(): ImageView? {
        return if (isViewInBounds(requireActivity().window.decorView, gestures_view)) {
            gestures_view
        } else null
    }

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    private fun isViewInBounds(@NonNull container: View, @NonNull view: View): Boolean {
        val containerBounds = Rect()
        container.getHitRect(containerBounds)
        return view.getLocalVisibleRect(containerBounds)
    }

    override fun onResume() {
        super.onResume()
        mConfig = requireContext().config      // make sure we get a new config, in case the user changed something in the app settings
        requireActivity().updateTextColors(mView.video_holder)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(TRANSITION_KEY, startTransitionKey)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (activity?.isDestroyed == false) {
            try {
                if (context != null) {
                    Glide.with(requireContext()).clear(mView.gestures_view)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun launchVideoPlayer() {
        (activity as ViewPagerActivity).launchViewVideoIntent(mMedium.path)
    }

    private fun toggleFullscreen() {
        (activity as ViewPagerActivity).fragmentClicked()
    }
    override fun showPanoIcon() {

    }
}
