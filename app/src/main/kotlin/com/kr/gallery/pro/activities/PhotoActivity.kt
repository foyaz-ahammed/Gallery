package com.kr.gallery.pro.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kr.commons.extensions.*
import com.kr.commons.helpers.*
import com.kr.gallery.pro.BuildConfig
import com.kr.gallery.pro.R
import com.kr.gallery.pro.dialogs.PropertiesDialogFragment
import com.kr.gallery.pro.dialogs.TransferMethodSelectionDialog
import com.kr.gallery.pro.extensions.*
import com.kr.gallery.pro.fragments.ViewPagerFragment
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.views.KrBottomSheet
import kotlinx.android.synthetic.main.bottom_sheet_behavior.*
import kotlinx.android.synthetic.main.fragment_holder.*
import java.io.File

/**
 * 화상열람화면 activity
 * 실례: File Manager App에서 화상을 열때 우리 Gallery App을 선택하였을때 기동된다.
 * @see Intent.ACTION_VIEW
 * @see Intent.CATEGORY_BROWSABLE
 */
open class PhotoActivity : SimpleActivity(), ViewPagerFragment.FragmentListener, TransferMethodSelectionDialog.TransferDialogListener {
    private var mMedium: Medium? = null
    private var mIsFullScreen = false
    private var mUri: Uri? = null
    private var mPath = ""

    private var mTimeOutHandler = Handler()
    private lateinit var mTimeOUtRunnable: Runnable
    private var mTimeOutDelay = 5000L

    //Bottom sheet 관리 변수
    lateinit var bottomSheetBehavior: BottomSheetBehavior<KrBottomSheet>
    //Bottom sheet 상태
    private var mSavedBottomState: Int = BottomSheetBehavior.STATE_COLLAPSED

    /* Activity파괴시 저장할 상태변수들 */
    private var dialogOpened = false
    private var dialogType = -1

    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_holder)

        if (checkAppSideloading()) {
            return
        }

        handlePermission(PERMISSION_WRITE_STORAGE) {
            if (it) {
                checkIntent()
            } else {
                toast(R.string.no_storage_permissions)
                finish()
            }
        }

        //상태들 복귀
        if (savedInstanceState != null) {
            mIsFullScreen = savedInstanceState.getBoolean(IS_FULLSCREEN)
            mSavedBottomState = savedInstanceState.getInt(LAST_BOTTOM_BAR_STATE)
            dialogOpened = savedInstanceState.getBoolean(DIALOG_OPENED, false)
            dialogType = savedInstanceState.getInt(DIALOG_TYPE)
        }

        gestures_view.setOnClickListener{fragmentClicked()}
        gestures_view.enableZoomDown(true)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        buildSheet()
        bottomSheetBehavior.state = mSavedBottomState
        bottomSheet.expand(mSavedBottomState == BottomSheetBehavior.STATE_EXPANDED)
        bottomSheetBehavior.disableDragging(mSavedBottomState)

        mTimeOUtRunnable = Runnable {
            hideSystemUI(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        hideSystemUI(true)
    }

    private fun checkIntent() {
        //intent에 들어온 자료가 없으면 activity를 닫는다
        if (intent.data == null && intent.action == Intent.ACTION_VIEW) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        mUri = intent.data ?: return

        //uri로부터 화일경로를 얻어낸다.
        val filePath = FileUtils.getPath(this, mUri!!)?: ""
        mPath = filePath
        mMedium = getImage(filePath)
                ?: Medium(null, getFilenameFromUri(mUri!!), mUri.toString(), mUri!!.path!!.getParentPath(), 0, 0, "", 0, TYPE_IMAGES, 0, 0)

        checkNotchSupport()
        showSystemUI(true)

        supportActionBar?.title = mMedium!!.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadBitmap()
        video_play_button.beGone()

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            mIsFullScreen = if (visibility and View.SYSTEM_UI_FLAG_LOW_PROFILE == 0) {
                false
            } else {
                visibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
            }

            checkSystemUI()
            fullscreenToggled()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photo_video_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mMedium == null || mUri == null)
            return true

        when (item.itemId) {
            R.id.menu_properties -> showProperties()
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showProperties() {
        if (mMedium != null &&
                supportFragmentManager.findFragmentByTag(PROPERTY_DIALOG_TAG) == null) {
            val propertiesDialogFragment: DialogFragment? = PropertiesDialogFragment.newInstance(mPath, false)
            propertiesDialogFragment?.show(supportFragmentManager, PROPERTY_DIALOG_TAG)
        }
    }

    private fun isFileTypeVisible(path: String): Boolean {
        return path.isImageFast() || path.isVideoFast() || path.isGif() || path.isRawFast() || path.isSvg()
    }

    override fun fragmentClicked() {
        mIsFullScreen = !mIsFullScreen
        checkSystemUI()                                 // setOnSystemUiVisibilityChangeListener call fullscreenToggled()
        fullscreenToggled()
    }

    private fun checkSystemUI() {
        if (mIsFullScreen) {
            hideSystemUI(true)
        } else {
            showSystemUI(true)
        }
    }

    private fun fullscreenToggled() {
        val newAlpha = if (mIsFullScreen) 0f else 1f
        top_shadow.animate().alpha(newAlpha).start()

        if(mIsFullScreen) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheet.expand(false)

            mSavedBottomState = BottomSheetBehavior.STATE_COLLAPSED
        }
        else {
            if(!dialogOpened)
                bottomSheetBehavior.state = mSavedBottomState
            bottomSheet.expand(mSavedBottomState == BottomSheetBehavior.STATE_EXPANDED)
        }

        if (!mIsFullScreen) restartTimeOut()
    }

    private fun loadBitmap() {

        val options = RequestOptions()
                .signature(mPath.getFileSignature())
                .format(DecodeFormat.PREFER_ARGB_8888)
                .priority(Priority.IMMEDIATE)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .fitCenter()

        Glide.with(this)
                .load(mPath)
                .apply(options)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                }).into(gestures_view)
    }

    private fun buildSheet() {
        //Bottom bar의 자식 view들 생성
        bottomSheet.addItem(
                applicationContext,
                KEY_SHARE,
                R.drawable.ic_outline_share_24,
                R.string.share,
                false
        )
        //Bottom bar단추들의 눌림동작
        bottomSheet.itemMap[KEY_SHARE]!!.setOnClickListener {
            stopTimeOut()
            onClickShare()
        }

        bottomSheet.addItem(
                applicationContext,
                KEY_EDIT,
                R.drawable.ic_edit_vector,
                R.string.edit,
                false
        )
        bottomSheet.itemMap[KEY_EDIT]!!.setOnClickListener {
            stopTimeOut()
            onClickEdit()
        }
    }

    //전송단추를 눌렀을때
    private fun onClickShare(){
        //Bottom bar를 숨긴다
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheet.expand(false)

        //전송대화창을 연다
        val dialog = TransferMethodSelectionDialog(this)
        dialog.show(supportFragmentManager, MainActivity.TRANSFER_DIALOG_TAG)

        //상태보관
        dialogOpened = true
        dialogType = TRANSFER_DIALOG
    }

    //편집단추를 눌렀을때
    private fun onClickEdit(){
        openEditor(mPath, BuildConfig.APPLICATION_ID)
    }

    private fun openEditor(path: String, applicationId: String) {
        ensureBackgroundThread {
            val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
            Intent(this, EditActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = Intent.ACTION_EDIT
                setDataAndType(newUri, getUriMimeType(path, newUri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                val parent = path.getParentPath()
                val newFilename = "${path.getFilenameFromPath().substringBeforeLast('.')}_1"
                val extension = path.getFilenameExtension()
                val newFilePath = File(parent, "$newFilename.$extension")

                val outputUri = if (isPathOnOTG(path)) newUri else getFinalUriFromPath("$newFilePath", applicationId)
                val resInfoList = packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                putExtra(REAL_FILE_PATH, path)

                if (resolveActivity(packageManager) != null) {
                    try {
                        startActivityForResult(this, REQUEST_EDIT_IMAGE)
                    } catch (e: SecurityException) {
                        showErrorToast(e)
                    }
                } else {
                    toast(com.kr.commons.R.string.no_app_found)
                }
            }

            runOnUiThread {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                bottomSheet.expand(false)
            }
        }
    }

    private fun restartTimeOut() {
        stopTimeOut()
        mTimeOutHandler.postDelayed(mTimeOUtRunnable, mTimeOutDelay)
    }

    private fun stopTimeOut() {
        mTimeOutHandler.removeCallbacksAndMessages(null)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        restartTimeOut()
    }

    override fun videoEnded() = false

    override fun launchViewVideoIntent(path: String) {}

    override fun isSlideShowActive() = false

    //전송-통보문단추를 눌렀을때
    override fun onMessage() {
        dialogOpened = false

        runAfterDelay {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            shareMediumPath(mPath, SHARE_BY_MESSAGE)
            restartTimeOut()
        }
    }

    //전송-기록장단추를 눌렀을때
    override fun onNote() {
        dialogOpened = false

        runAfterDelay {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            shareMediumPath(mPath, SHARE_BY_NOTE)
            restartTimeOut()
        }
    }

    //전송-블루투스단추를 눌렀을때
    override fun onBluetooth() {
        dialogOpened = false

        runAfterDelay {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            shareMediumPath(mPath, SHARE_BY_BLUETOOTH)
            restartTimeOut()
        }
    }

    //전송대화창 혹은 삭제대화창에서 cancel동작
    override fun onCancel() {
        dialogOpened = false

        runAfterDelay {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //상태들 저장
        outState.putBoolean(IS_FULLSCREEN, mIsFullScreen)
        outState.putInt(LAST_BOTTOM_BAR_STATE, bottomSheetBehavior.state)
        outState.putBoolean(DIALOG_OPENED, dialogOpened)
        outState.putInt(DIALOG_TYPE, dialogType)
    }
}
