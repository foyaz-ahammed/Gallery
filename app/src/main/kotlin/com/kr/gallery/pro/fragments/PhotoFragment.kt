package com.kr.gallery.pro.fragments

import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.media.ExifInterface.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.alexvasilkov.gestures.GestureController
import com.alexvasilkov.gestures.State
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.Rotate
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.DecoderFactory
import com.davemorrissey.labs.subscaleview.ImageDecoder
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.*
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.gallery.pro.R
import com.kr.gallery.pro.activities.PanoramaPhotoActivity
import com.kr.gallery.pro.activities.PhotoActivity
import com.kr.gallery.pro.activities.ViewPagerActivity
import com.kr.gallery.pro.adapters.PortraitPhotosAdapter
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.extensions.saveRotatedImageToFile
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.svg.SvgSoftwareLayerSetter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import it.sephiroth.android.library.exif2.ExifInterface
import kotlinx.android.synthetic.main.pager_photo_item.*
import kotlinx.android.synthetic.main.pager_photo_item.view.*
import org.apache.sanselan.common.byteSources.ByteSourceInputStream
import org.apache.sanselan.formats.jpeg.JpegImageParser
import pl.droidsonroids.gif.InputSource
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil

class PhotoFragment(private var startTransitionKey: String?) : ViewPagerFragment() {
    constructor(): this("")

    private val DEFAULT_DOUBLE_TAP_ZOOM = 2f
    private val ZOOMABLE_VIEW_LOAD_DELAY = 100L
    private val SAME_ASPECT_RATIO_THRESHOLD = 0.01

    // devices with good displays, but the rest of the hardware not good enough for them
    private val WEIRD_DEVICES = arrayListOf(
            "motorola xt1685",
            "google nexus 5x"
    )

    var mCurrentRotationDegrees = 0
    private var mIsFragmentVisible = false
    private var mWasInit = false
    private var mIsPanorama = false
    private var mIsSubsamplingVisible = false    // checking view.visibility is unreliable, use an extra variable for it
    private var mCurrentPortraitPhotoPath = ""
    private var mOriginalPath = ""
    private var mImageOrientation = -1
    private var mLoadZoomableViewHandler = Handler()
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mCurrentGestureViewZoom = 1f

    private lateinit var mView: ViewGroup
    private lateinit var mMedium: Medium

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(savedInstanceState != null) {
            startTransitionKey = savedInstanceState.getString(TRANSITION_KEY)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mView = (inflater.inflate(R.layout.pager_photo_item, container, false) as ViewGroup)
        if (!arguments!!.getBoolean(SHOULD_INIT_FRAGMENT, true)) {
            return mView
        }

        mMedium = arguments!!.getSerializable(MEDIUM) as Medium
        mOriginalPath = mMedium.path

        mView.apply {
            subsampling_view.setOnClickListener { photoClicked() }
            gestures_view.setOnClickListener { photoClicked() }

            gestures_view.setViewPagerActivity(activity as ViewPagerActivity)

            //  MediaActivity의 itemClicked에서 sharedImageTransitionName과 같아야 한다
            gestures_view.transitionName = mOriginalPath.hashCode().toString()

            panorama_outline.setOnClickListener { openPanorama() }

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

            subsampling_view.setOnTouchListener { v, event ->
                if (subsampling_view.isZoomedOut()) {
                    handleEvent(event)
                }
                false
            }
        }

        checkScreenDimensions()
        if (!mIsFragmentVisible && activity is PhotoActivity) {
            mIsFragmentVisible = true
        }

        if (mMedium.path.startsWith("content://") && !mMedium.path.startsWith("content://mms/")) {
            mMedium.path = context!!.getRealPathFromURI(Uri.parse(mOriginalPath)) ?: mMedium.path

            if (mMedium.path.isEmpty()) {
                var out: FileOutputStream? = null
                try {
                    var inputStream = context!!.contentResolver.openInputStream(Uri.parse(mOriginalPath))
                    val exif = ExifInterface()
                    exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                    val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                    val orientation = tag?.getValueAsInt(-1) ?: -1
                    inputStream = context!!.contentResolver.openInputStream(Uri.parse(mOriginalPath))
                    val original = BitmapFactory.decodeStream(inputStream)
                    val rotated = rotateViaMatrix(original, orientation)
                    exif.setTagValue(ExifInterface.TAG_ORIENTATION, 1)
                    exif.removeCompressedThumbnail()

                    val file = File(context!!.externalCacheDir, Uri.parse(mOriginalPath).lastPathSegment)
                    out = FileOutputStream(file)
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    mMedium.path = file.absolutePath
                } catch (e: Exception) {
                    activity!!.toast(R.string.unknown_error_occurred)
                    return mView
                } finally {
                    out?.close()
                }
            }
        }

        mIsFullscreen = activity!!.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == View.SYSTEM_UI_FLAG_FULLSCREEN
        loadImage()
        mWasInit = true
        checkIfPanorama()

        updateStatusbarColor()
        updateNavigationBarColor()

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

    override fun setIsCurrentFrag(is_center: Boolean) {
        mIsCurrentFrag = is_center
        if (gestures_view != null) gestures_view.setIsCenterFrag(is_center)
    }
    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()

        if (gestures_view != null && gestures_view.transitionName == startTransitionKey) {
            gestures_view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    gestures_view.viewTreeObserver.removeOnPreDrawListener(this)
                    Handler().postDelayed(Runnable {
                        // start background transparent to make MediaActivity fade-out
                        if (activity != null) {
                            activity!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
                            activity!!.startPostponedEnterTransition()
                        }
                    }, 70)
                    return true
                }
            })
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(TRANSITION_KEY, startTransitionKey)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStatusbarColor() {
        val view = activity?.window?.decorView
        view?.systemUiVisibility = view?.systemUiVisibility!! and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        activity?.window?.statusBarColor = Color.BLACK
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNavigationBarColor() {
        val view = activity?.window?.decorView
        view?.systemUiVisibility = view?.systemUiVisibility!! and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        activity?.window?.statusBarColor = Color.BLACK
    }

    override fun onDestroyView() {
        super.onDestroyView()

        gestures_view.setOnClickListener(null)
        gestures_view.setOnTouchListener(null)

        if (activity?.isDestroyed == false) {
            mView.subsampling_view.recycle()

            try {
                if (context != null) {
                    Glide.with(context!!).clear(mView.gestures_view)
                }
            } catch (ignored: Exception) {
            }
        }

        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        if (mCurrentRotationDegrees != 0) {
            ensureBackgroundThread {
                val path = mMedium.path
                (activity as? BaseSimpleActivity)?.saveRotatedImageToFile(path, path, mCurrentRotationDegrees, false) {}
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!mWasInit) {
            return
        }

        // avoid GIFs being skewed, played in wrong aspect ratio
        hideZoomableView()
        loadImage()

        measureScreen()
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        mIsFragmentVisible = menuVisible
        if (mWasInit) {
            photoFragmentVisibilityChanged(menuVisible)
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

    private fun photoFragmentVisibilityChanged(isVisible: Boolean) {
        if (isVisible) {
            scheduleZoomableView()
        } else {
            hideZoomableView()
        }
    }

    private fun degreesForRotation(orientation: Int) = when (orientation) {
        ORIENTATION_ROTATE_270 -> 270
        ORIENTATION_ROTATE_180 -> 180
        ORIENTATION_ROTATE_90 -> 90
        else -> 0
    }

    private fun rotateViaMatrix(original: Bitmap, orientation: Int): Bitmap {
        val degrees = degreesForRotation(orientation).toFloat()
        return if (degrees == 0f) {
            original
        } else {
            val matrix = Matrix()
            matrix.setRotate(degrees)
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        }
    }

    private fun loadImage() {
        checkScreenDimensions()

        mImageOrientation = getImageOrientation()
        loadBitmap()
    }

    private fun loadSVG() {
        Glide.with(context!!)
                .`as`(PictureDrawable::class.java)
                .listener(SvgSoftwareLayerSetter())
                .load(mMedium.path)
                .listener(object : RequestListener<PictureDrawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<PictureDrawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                    override fun onResourceReady(resource: PictureDrawable?, model: Any?, target: Target<PictureDrawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        startPostponedEnterTransition()
                        return false
                    }
                })
                .into(mView.gestures_view)
    }

    private fun loadBitmap(addZoomableView: Boolean = true) {
        val priority = if (mIsFragmentVisible) Priority.IMMEDIATE else Priority.NORMAL
        val options = RequestOptions()
                .signature(getFilePathToShow().getFileSignature())
                .format(DecodeFormat.PREFER_ARGB_8888)
                .priority(priority)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .fitCenter()

        if (mCurrentRotationDegrees != 0) {
            options.transform(Rotate(mCurrentRotationDegrees))
            options.diskCacheStrategy(DiskCacheStrategy.NONE)
        }

        if (context == null) {
            return
        }

        Glide.with(context!!)
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
                        mView.gestures_view.controller.settings.isZoomEnabled = mCurrentRotationDegrees != 0
                        if (mIsFragmentVisible && addZoomableView) {
                            scheduleZoomableView()
                        }

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

            if (mCurrentRotationDegrees != 0) {
                picasso.rotate(mCurrentRotationDegrees.toFloat())
            } else {
                degreesForRotation(mImageOrientation).toFloat()
            }

            picasso.into(mView.gestures_view, object : Callback {
                override fun onSuccess() {
                    mView.gestures_view.controller.settings.isZoomEnabled = mCurrentRotationDegrees != 0
                    if (mIsFragmentVisible && addZoomableView) {
                        scheduleZoomableView()
                    }

                    startPostponedEnterTransition()
                }

                override fun onError(e: Exception?) {
                    if (mMedium.path != mOriginalPath) {
                        mMedium.path = mOriginalPath
                        loadImage()
                        checkIfPanorama()
                    }
                }
            })
        } catch (ignored: Exception) {
        }
    }

    private fun showPortraitStripe() {
        val files = File(mMedium.parentPath).listFiles()?.toMutableList() as? ArrayList<File>
        if (files != null) {
            val screenWidth = context!!.realScreenSize.x
            val itemWidth = context!!.resources.getDimension(R.dimen.portrait_photos_stripe_height).toInt() + context!!.resources.getDimension(R.dimen.one_dp).toInt()
            val sideWidth = screenWidth / 2 - itemWidth / 2
            val fakeItemsCnt = ceil(sideWidth / itemWidth.toDouble()).toInt()

            val paths = fillPhotoPaths(files, fakeItemsCnt)
            var curWidth = itemWidth
            while (curWidth < screenWidth) {
                curWidth += itemWidth
            }

            val sideElementWidth = curWidth - screenWidth
            val adapter = PortraitPhotosAdapter(context!!, paths, sideElementWidth) { position, x ->
                if (mIsFullscreen) {
                    return@PortraitPhotosAdapter
                }

                mView.photo_portrait_stripe.smoothScrollBy((x + itemWidth / 2) - screenWidth / 2, 0)
                if (paths[position] != mCurrentPortraitPhotoPath) {
                    mCurrentPortraitPhotoPath = paths[position]
                    hideZoomableView()
                    loadBitmap()
                }
            }

            mView.photo_portrait_stripe.adapter = adapter
            setupStripeBottomMargin()

            val coverIndex = getCoverImageIndex(paths)
            if (coverIndex != -1) {
                mCurrentPortraitPhotoPath = paths[coverIndex]
                setupStripeUpListener(adapter, screenWidth, itemWidth)

                mView.photo_portrait_stripe.onGlobalLayout {
                    mView.photo_portrait_stripe.scrollBy((coverIndex - fakeItemsCnt) * itemWidth, 0)
                    adapter.setCurrentPhoto(coverIndex)
                    mView.photo_portrait_stripe_wrapper.beVisible()
                    if (mIsFullscreen) {
                        mView.photo_portrait_stripe_wrapper.alpha = 0f
                    }
                }
            }
        }
    }

    private fun fillPhotoPaths(files: ArrayList<File>, fakeItemsCnt: Int): ArrayList<String> {
        val paths = ArrayList<String>()
        for (i in 0 until fakeItemsCnt) {
            paths.add("")
        }

        files.forEach {
            paths.add(it.absolutePath)
        }

        for (i in 0 until fakeItemsCnt) {
            paths.add("")
        }
        return paths
    }

    private fun setupStripeBottomMargin() {
        var bottomMargin = context!!.navigationBarHeight + context!!.resources.getDimension(R.dimen.normal_margin).toInt()
        bottomMargin += context!!.resources.getDimension(R.dimen.bottom_actions_height).toInt()
        (mView.photo_portrait_stripe_wrapper.layoutParams as RelativeLayout.LayoutParams).bottomMargin = bottomMargin
    }

    private fun getCoverImageIndex(paths: ArrayList<String>): Int {
        var coverIndex = -1
        paths.forEachIndexed { index, path ->
            if (path.contains("cover", true)) {
                coverIndex = index
            }
        }

        if (coverIndex == -1) {
            paths.forEachIndexed { index, path ->
                if (path.isNotEmpty()) {
                    coverIndex = index
                }
            }
        }
        return coverIndex
    }

    private fun setupStripeUpListener(adapter: PortraitPhotosAdapter, screenWidth: Int, itemWidth: Int) {
        mView.photo_portrait_stripe.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                var closestIndex = -1
                var closestDistance = Integer.MAX_VALUE
                val center = screenWidth / 2
                for ((key, value) in adapter.views) {
                    val distance = Math.abs(value.x.toInt() + itemWidth / 2 - center)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestIndex = key
                    }
                }

                Handler().postDelayed({
                    adapter.performClickOn(closestIndex)
                }, 100)
            }
            false
        }
    }

    private fun getFilePathToShow() = getPathToLoad(mMedium)

    private fun openPanorama() {
        Intent(context, PanoramaPhotoActivity::class.java).apply {
            putExtra(PATH, mMedium.path)
            startActivity(this)
        }
    }

    private fun scheduleZoomableView() {
        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
        mLoadZoomableViewHandler.postDelayed({
            if (mIsFragmentVisible && mMedium.isImage() && !mIsSubsamplingVisible) {
                addZoomableView()
            }
        }, ZOOMABLE_VIEW_LOAD_DELAY)
    }

    private fun addZoomableView() {
        val rotation = degreesForRotation(mImageOrientation)
        mIsSubsamplingVisible = true
        val config = context!!.config
        val minTileDpi = getMinTileDpi()

        val bitmapDecoder = object : DecoderFactory<ImageDecoder> {
            override fun make() = MyGlideImageDecoder(rotation, mMedium.getSignature())
        }

        val regionDecoder = object : DecoderFactory<ImageRegionDecoder> {
            override fun make() = PicassoRegionDecoder(mScreenWidth, mScreenHeight, minTileDpi)
        }

        var newOrientation = (rotation + mCurrentRotationDegrees) % 360
        if (newOrientation < 0) {
            newOrientation += 360
        }

        mView.subsampling_view.apply {
            setMaxTileSize(4096)
            setMinimumTileDpi(minTileDpi)
            background = ColorDrawable(Color.TRANSPARENT)
            bitmapDecoderFactory = bitmapDecoder
            regionDecoderFactory = regionDecoder
            maxScale = 10f
            beVisible()
            rotationEnabled = true
            isOneToOneZoomEnabled = false
            orientation = newOrientation
            setImage(getFilePathToShow())

            onImageEventListener = object : SubsamplingScaleImageView.OnImageEventListener {
                override fun onReady() {
                    background = ColorDrawable(config.backgroundColor)
                    val useWidth = if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sHeight else sWidth
                    val useHeight = if (mImageOrientation == ORIENTATION_ROTATE_90 || mImageOrientation == ORIENTATION_ROTATE_270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                }

                override fun onImageLoadError(e: Exception) {
                    mView.gestures_view.controller.settings.isZoomEnabled = true
                    background = ColorDrawable(Color.TRANSPARENT)
                    mIsSubsamplingVisible = false
                    beGone()
                }

                override fun onImageRotation(degrees: Int) {
                    val fullRotation = (rotation + degrees) % 360
                    val useWidth = if (fullRotation == 90 || fullRotation == 270) sHeight else sWidth
                    val useHeight = if (fullRotation == 90 || fullRotation == 270) sWidth else sHeight
                    doubleTapZoomScale = getDoubleTapZoomScale(useWidth, useHeight)
                    mCurrentRotationDegrees = (mCurrentRotationDegrees + degrees) % 360
                    loadBitmap(false)
                    activity?.invalidateOptionsMenu()
                }
            }
        }
    }

    private fun getMinTileDpi(): Int {
        val metrics = resources.displayMetrics
        val averageDpi = (metrics.xdpi + metrics.ydpi) / 2
        val device = "${Build.BRAND} ${Build.MODEL}".toLowerCase()
        return when {
            WEIRD_DEVICES.contains(device) -> WEIRD_TILE_DPI
            averageDpi > 400 -> HIGH_TILE_DPI
            averageDpi > 300 -> NORMAL_TILE_DPI
            else -> LOW_TILE_DPI
        }
    }

    private fun checkIfPanorama() {
        mIsPanorama = try {
            val inputStream = if (mMedium.path.startsWith("content:/")) context!!.contentResolver.openInputStream(Uri.parse(mMedium.path)) else File(mMedium.path).inputStream()
            val imageParser = JpegImageParser().getXmpXml(ByteSourceInputStream(inputStream, mMedium.name), HashMap<String, Any>())
            imageParser.contains("GPano:UsePanoramaViewer=\"True\"", true) ||
                    imageParser.contains("<GPano:UsePanoramaViewer>True</GPano:UsePanoramaViewer>", true) ||
                    imageParser.contains("GPano:FullPanoWidthPixels=") ||
                    imageParser.contains("GPano:ProjectionType>Equirectangular")
        } catch (e: Exception) {
            false
        } catch (e: OutOfMemoryError) {
            false
        }

        mView.panorama_outline.beVisibleIf(mIsPanorama)
        if (mIsFullscreen) {
            mView.panorama_outline.alpha = 0f
        }
    }

    private fun getImageOrientation(): Int {
        val defaultOrientation = -1
        var orient = defaultOrientation

        try {
            val path = getFilePathToShow()
            orient = if (path.startsWith("content:/")) {
                val inputStream = context!!.contentResolver.openInputStream(Uri.parse(path))
                val exif = ExifInterface()
                exif.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                val tag = exif.getTag(ExifInterface.TAG_ORIENTATION)
                tag?.getValueAsInt(defaultOrientation) ?: defaultOrientation
            } else {
                val exif = android.media.ExifInterface(path)
                exif.getAttributeInt(TAG_ORIENTATION, defaultOrientation)
            }

            if (orient == defaultOrientation || context!!.isPathOnOTG(getFilePathToShow())) {
                val uri = if (path.startsWith("content:/")) Uri.parse(path) else Uri.fromFile(File(path))
                val inputStream = context!!.contentResolver.openInputStream(uri)
                val exif2 = ExifInterface()
                exif2.readExif(inputStream, ExifInterface.Options.OPTION_ALL)
                orient = exif2.getTag(ExifInterface.TAG_ORIENTATION)?.getValueAsInt(defaultOrientation) ?: defaultOrientation
            }
        } catch (ignored: Exception) {
        } catch (ignored: OutOfMemoryError) {
        }
        return orient
    }

    private fun getDoubleTapZoomScale(width: Int, height: Int): Float {
        val bitmapAspectRatio = height / width.toFloat()
        val screenAspectRatio = mScreenHeight / mScreenWidth.toFloat()

        return if (context == null || Math.abs(bitmapAspectRatio - screenAspectRatio) < SAME_ASPECT_RATIO_THRESHOLD) {
            DEFAULT_DOUBLE_TAP_ZOOM
        } else if (context!!.portrait && bitmapAspectRatio <= screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else if (context!!.portrait && bitmapAspectRatio > screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio >= screenAspectRatio) {
            mScreenWidth / width.toFloat()
        } else if (!context!!.portrait && bitmapAspectRatio < screenAspectRatio) {
            mScreenHeight / height.toFloat()
        } else {
            DEFAULT_DOUBLE_TAP_ZOOM
        }
    }

    fun rotateImageViewBy(degrees: Int) {
        if (mIsSubsamplingVisible) {
            mView.subsampling_view.rotateBy(degrees)
        } else {
            mCurrentRotationDegrees = (mCurrentRotationDegrees + degrees) % 360
            mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
            mIsSubsamplingVisible = false
            loadBitmap()
        }
    }

    private fun hideZoomableView() {
        mIsSubsamplingVisible = false
        mView.subsampling_view.recycle()
        mView.subsampling_view.beGone()
        mLoadZoomableViewHandler.removeCallbacksAndMessages(null)
    }

    private fun photoClicked() {
        (activity as ViewPagerActivity).fragmentClicked()
    }

    override fun showPanoIcon() {
        mView.apply {
            if (mIsPanorama) {
                panorama_outline.animate().alpha(if (mIsFullscreen) 0f else 1f).start()
                panorama_outline.isClickable = !mIsFullscreen
            }
        }
    }
}
