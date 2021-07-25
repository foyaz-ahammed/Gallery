package com.kr.gallery.pro.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.kk.taurus.playerbase.assist.AssistPlay
import com.kk.taurus.playerbase.assist.OnAssistPlayEventHandler
import com.kk.taurus.playerbase.assist.RelationAssist
import com.kk.taurus.playerbase.config.PlayerLibrary
import com.kk.taurus.playerbase.entity.DataSource
import com.kk.taurus.playerbase.event.EventKey
import com.kk.taurus.playerbase.event.OnPlayerEventListener
import com.kk.taurus.playerbase.player.IPlayer
import com.kk.taurus.playerbase.receiver.ReceiverGroup
import com.kk.taurus.playerbase.window.FloatWindow
import com.kk.taurus.playerbase.window.FloatWindowParams
import com.kk.taurus.playerbase.window.WindowPermissionCheck
import com.kr.commons.extensions.getDoesFilePathExist
import com.kr.gallery.pro.R
import com.kr.gallery.pro.databases.GalleryDatabase.Companion.getInstance
import com.kr.gallery.pro.extensions.parseFileChannel
import com.kr.gallery.pro.helpers.PATH
import com.kr.gallery.pro.helpers.POSITION
import com.kr.gallery.pro.helpers.PUtil
import com.kr.gallery.pro.helpers.ReceiverGroupManager
import com.kr.gallery.pro.interfaces.DataInter
import com.kr.gallery.pro.models.CloseCover
import com.kr.gallery.pro.models.ControllerCover
import com.kr.gallery.pro.models.GestureCover
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 동영상열람화면의 activity
 */
class VideoPlayerActivity: AppCompatActivity(), OnPlayerEventListener {
    private lateinit var mVideoView: FrameLayout
    private lateinit var mReceiverGroup: ReceiverGroup

    private lateinit var mAssist: RelationAssist
    private lateinit var mFloatWindow: FloatWindow
    private lateinit var mFloatVideoContainer: FrameLayout

    private var isLandscape = false
    private var margin = 0

    private var hasStart = false
    private var mScreenshotAnimRunning = false

    private var screenW = 0
    private var screenH = 0

    private lateinit var mUri: Uri
    private var mPath: String? = null
    private var mPos = 0

    private var mWhoIntentFullScreen = 0

    private val VIEW_INTENT_FULL_SCREEN = 1
    private val WINDOW_INTENT_FULL_SCREEN = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //intent로부터 정보들 얻기
        if (!checkIntent(intent)) return

        PlayerLibrary.init(this)

        setContentView(R.layout.activity_base_video_view)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mVideoView = findViewById(R.id.video_container)

        margin = PUtil.dip2px(this, 2f)

        updateLayout(false)

        mReceiverGroup = ReceiverGroupManager.get().getLiteReceiverGroup(this)
        mReceiverGroup.getGroupValue().putBoolean(DataInter.Key.KEY_CONTROLLER_TOP_ENABLE, true)

        screenW = PUtil.getScreenW(this)
        screenH = PUtil.getScreenH(this)

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val width = resources.displayMetrics.widthPixels
        mFloatVideoContainer = FrameLayout(this)
        mFloatWindow = FloatWindow(this, mFloatVideoContainer,
                FloatWindowParams()
                        .setWindowType(type)
                        .setX(100)
                        .setY(400)
                        .setWidth(width)
                        .setHeight(width * 9 / 16))
        mFloatWindow.setBackgroundColor(Color.BLACK)

        mAssist = RelationAssist(this)
        mAssist.superContainer.setBackgroundColor(Color.BLACK)
        mAssist.setEventAssistHandler(eventHandler)
        mAssist.setOnPlayerEventListener(this)
        mAssist.receiverGroup = mReceiverGroup
        changeMode(false)

        mAssist.attachContainer(mVideoView)

        val orientation = resources.configuration.orientation
        isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private val eventHandler: OnAssistPlayEventHandler = object : OnAssistPlayEventHandler() {
        override fun onAssistHandle(assist: AssistPlay, eventCode: Int, bundle: Bundle?) {
            super.onAssistHandle(assist, eventCode, bundle)
            when (eventCode) {
                DataInter.Event.EVENT_CODE_REQUEST_BACK -> onBackPressed()
                DataInter.Event.EVENT_CODE_ERROR_SHOW -> mAssist.stop()
                DataInter.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN -> if (isLandscape) {
                    quitFullScreen()
                } else {
                    mWhoIntentFullScreen = if (mFloatWindow.isWindowShow) WINDOW_INTENT_FULL_SCREEN else VIEW_INTENT_FULL_SCREEN
                    enterFullScreen()
                }
                DataInter.Event.EVENT_CODE_REQUEST_CLOSE -> normalPlay()
                DataInter.Event.EVENT_CODE_REQUEST_SCREEN_SHOT -> saveScreenShot()
                DataInter.Event.EVENT_CODE_REQUEST_CONTROLLER_LOCK -> switchWindowPlay()
                DataInter.Event.EVENT_CODE_REQUEST_CANCEL_FLOAT_WINDOW -> normalPlay()
                DataInter.Event.EVENT_CODE_REQUEST_SET_PLAY_SPEED -> {
                    val playSpeed = bundle!!.getFloat(EventKey.INT_ARG1)
                    mAssist.setSpeed(playSpeed)
                }
                DataInter.Event.EVENT_CODE_REQUEST_SET_PLAY_LOOP -> {
                    val playLoop = bundle!!.getString(EventKey.INT_ARG1)
                    mAssist.setLooping(playLoop == ControllerCover.LOOP_ONE)
                }
            }
        }

        override fun requestRetry(assist: AssistPlay, bundle: Bundle) {
            if (PUtil.isTopActivity(this@VideoPlayerActivity)) {
                super.requestRetry(assist, bundle)
            }
        }
    }

    private fun checkIntent(intent: Intent?): Boolean {
        if (intent == null || intent.data == null) {
            finish()
            return false
        }

        mUri = intent.data!!
        mPath = intent.getStringExtra(PATH)
        mPos = intent.getIntExtra(POSITION, 0)

        //Panorama형 video인가 검사
        val uri = mUri.toString()
        var realPath: String? = null
        var isPanorama = false
        if (uri.startsWith("content:/") && uri.contains("/storage/")) {
            val guessedPath = uri.substring(uri.indexOf("/storage/"))
            if (getDoesFilePathExist(guessedPath)) {
                realPath = guessedPath
            }
        }
        if(!realPath.isNullOrEmpty() && getDoesFilePathExist(realPath)){
            val fis = FileInputStream(File(realPath))
            parseFileChannel(realPath, fis.channel, 0, 0, 0) {
                isPanorama = true
            }
        }

        //Panorama형이면 현재의 activity를 종료하고 PanoramaVideoActivity를 기동시킨다
        if(isPanorama) {
            finish()

            Intent(applicationContext, PanoramaVideoActivity::class.java).apply {
                putExtra(PATH, realPath)
                startActivity(this)
            }
            return false
        }

        return true
    }

    private fun initPlay() {
        if (!hasStart) {
            val dataSource = DataSource()
            dataSource.uri = mUri
            dataSource.title = getFileName(mUri)
            mAssist.setDataSource(dataSource)
            mAssist.play()
            hasStart = true
        }
    }

    override fun onPlayerEvent(eventCode: Int, bundle: Bundle?) {
        when (eventCode) {
            OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE ->
                // play end, return to viewpager
                onBackPressed()
            OnPlayerEventListener.PLAYER_EVENT_ON_START ->
                if (mPos > 0) mAssist.seekTo(mPos)
        }
    }

    private fun updateLayout(landscape: Boolean) {
        val layoutParams = mVideoView.layoutParams as RelativeLayout.LayoutParams
        if (landscape) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.setMargins(0, 0, 0, 0)
        } else {
            layoutParams.width = PUtil.getScreenW(this) - margin * 2
            layoutParams.height = PUtil.getScreenH(this) - margin * 2
            layoutParams.setMargins(margin, margin, margin, margin)
        }
        mVideoView.layoutParams = layoutParams
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true
            updateLayout(true)
        } else {
            isLandscape = false
            updateLayout(false)
        }
        mReceiverGroup.groupValue.putBoolean(DataInter.Key.KEY_IS_LANDSCAPE, isLandscape)
    }

    override fun onBackPressed() {
        if (isLandscape) {
            quitFullScreen()
        }
        if (mPath != null && !mPath!!.isEmpty()) {
            //현재 video의 재생 위치를 자료기지에 보관한다
            val position: Int
            position = if (mAssist.state == IPlayer.STATE_PLAYBACK_COMPLETE) 0 else mAssist.currentPosition
            val thread = Thread(Runnable {
                val mediumDao = getInstance(this@VideoPlayerActivity).MediumDao()
                mediumDao.updateVideoTime(mPath!!, position)
                runOnUiThread { super.onBackPressed() }
            })
            thread.start()
        } else {
            super.onBackPressed()
        }
    }

    private var playBeforePause = true
    override fun onPause() {
        super.onPause()
        val state = mAssist.state
        if (state == IPlayer.STATE_PLAYBACK_COMPLETE) return
        playBeforePause = state == IPlayer.STATE_STARTED
        if (mAssist.isInPlaybackState) {
            if (!mFloatWindow.isWindowShow) mAssist.pause()
        } else {
            mAssist.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        val state = mAssist.state
        if (state == IPlayer.STATE_PLAYBACK_COMPLETE) return
        if (mAssist.isInPlaybackState) {
            if (playBeforePause) mAssist.resume()
        } else {
            mAssist.rePlay(0)
        }
        initPlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeFloatWindow()
        mAssist.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        WindowPermissionCheck.onActivityResult(this, requestCode, resultCode, data, null)
    }

    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor!!.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    /**
     * 화일쓰기부분
     *
     * screenshot보관
     */
    private fun saveScreenShot() {
        if (mScreenshotAnimRunning) return

        val bitmap = mAssist.screenShot
        if (bitmap != null) {
            var filename = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            filename = "Videoframe_" + filename + "_com.kr.movie.jpg"

            //부모등록부 생성
            val saveDirectory = Environment.getExternalStorageDirectory().toString() + "/Pictures/Screenshots"
            val dir = File(saveDirectory)
            if(!dir.exists()) dir.mkdir()

            val file = File("$saveDirectory/$filename")
            try {
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // media scan
            Thread(Runnable { MediaScannerConnection.scanFile(applicationContext, arrayOf(file.toString()), arrayOf(file.name), null) }).start()

            // save animation
            startScreenshotAnimation(bitmap)
        }
    }

    private fun startScreenshotAnimation(bitmap: Bitmap) {
        mScreenshotAnimRunning = true

        val thumb_view = findViewById<ImageView>(R.id.screenshot_anim_image_view)
        thumb_view.setImageBitmap(bitmap)
        val container = findViewById<LinearLayout>(R.id.screenshot_anim_view_container)
        container.visibility = View.VISIBLE
        val scaleAnim: Animation = ScaleAnimation(1f, 0.2f, 1f, 0.2f, Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f)
        scaleAnim.duration = 500
        scaleAnim.fillAfter = true
        screenW = PUtil.getScreenW(this)
        val translateAnim: Animation = TranslateAnimation(0f, -screenW * 0.2f - 100, 0f, 0f)
        translateAnim.duration = 500
        translateAnim.fillAfter = false
        scaleAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                val lp = container.layoutParams as RelativeLayout.LayoutParams
                lp.height = (container.height * 0.2f).toInt()
                lp.width = (container.width * 0.2f).toInt()
                container.layoutParams = lp
                val thumb_lp = thumb_view.layoutParams as LinearLayout.LayoutParams
                thumb_lp.setMargins(dp2px(1), dp2px(1), dp2px(1), dp2px(1))
                thumb_view.layoutParams = thumb_lp
                container.startAnimation(translateAnim)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        translateAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                val lp = container.layoutParams as RelativeLayout.LayoutParams
                lp.height = RelativeLayout.LayoutParams.MATCH_PARENT
                lp.width = RelativeLayout.LayoutParams.MATCH_PARENT
                container.layoutParams = lp
                val thumb_lp = thumb_view.layoutParams as LinearLayout.LayoutParams
                thumb_lp.setMargins(dp2px(5), dp2px(5), dp2px(5), dp2px(5))
                thumb_view.layoutParams = thumb_lp
                container.visibility = View.GONE

                mScreenshotAnimRunning = false
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        container.startAnimation(scaleAnim)
    }

    private fun dp2px(dp: Int): Int {
        return PUtil.dip2px(this, dp.toFloat())
    }

    fun switchWindowPlay() {
        if (mFloatWindow.isWindowShow) {
            normalPlay()
        } else {
            if (WindowPermissionCheck.checkPermission(this)) {
                floatWindowPlay()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
//        enterFullScreen();
    }

    private fun enterFullScreen() {
        //현재의 activity가 topActivity인지 따질 필요가 없음
        /*
        if (PUtil.isTopActivity(this)) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            if (mWhoIntentFullScreen == WINDOW_INTENT_FULL_SCREEN) {
                normalPlay()
            }
        } else {
            startActivity(Intent(applicationContext, VideoPlayerActivity::class.java))
        }
         */

        //fullscreen상태에 들어갈때는 항상 orientation을 landscape으로 설정한다.
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (mWhoIntentFullScreen == WINDOW_INTENT_FULL_SCREEN) {
            normalPlay()
        }
    }

    private fun quitFullScreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (mWhoIntentFullScreen == WINDOW_INTENT_FULL_SCREEN) {
            floatWindowPlay()
        }
    }

    private fun normalPlay() {
        changeMode(false)
        mAssist.attachContainer(mVideoView)
        closeFloatWindow()
        mReceiverGroup.groupValue.putBoolean(DataInter.Key.KEY_IS_FLOAT_WINDOW, false)
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun floatWindowPlay() {
        if (!mFloatWindow.isWindowShow) {
            changeMode(true)
            mFloatWindow.setElevationShadow(20f)
            mFloatWindow.setRoundRectShape(50f)
            mFloatWindow.show()
            mAssist.attachContainer(mFloatVideoContainer)
            mReceiverGroup.groupValue.putBoolean(DataInter.Key.KEY_IS_FLOAT_WINDOW, true)
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
        }
    }

    private fun closeFloatWindow() {
        if (mFloatWindow.isWindowShow) {
            mFloatWindow.close()
        }
    }

    private fun changeMode(window: Boolean) {
        if (window) {
            mReceiverGroup.removeReceiver(DataInter.ReceiverKey.KEY_GESTURE_COVER)
            mReceiverGroup.addReceiver(DataInter.ReceiverKey.KEY_CLOSE_COVER, CloseCover(this))
        } else {
            mReceiverGroup.removeReceiver(DataInter.ReceiverKey.KEY_CLOSE_COVER)
            mReceiverGroup.addReceiver(DataInter.ReceiverKey.KEY_GESTURE_COVER, GestureCover(this))
        }
        mReceiverGroup.groupValue.putBoolean(DataInter.Key.KEY_CONTROLLER_TOP_ENABLE, !window)
    }
}
