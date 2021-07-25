package com.kr.gallery.pro.services

import android.app.*
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.Intent
import android.os.*
import android.util.Log
import com.kr.commons.helpers.*
import com.kr.gallery.pro.R
import com.kr.gallery.pro.extensions.hiddenMediumDao
import com.kr.gallery.pro.extensions.hideFile
import com.kr.gallery.pro.extensions.unHideFile
import com.kr.gallery.pro.models.HiddenMedium
import com.kr.gallery.pro.models.Medium
import java.io.Serializable

/**
 * App이 꺼졌을때 화일 숨기기/숨김해제동작을 계속하는 service
 * Progress bar를 가진 notification을 띄워준다
 * @see MediaBackgroundService
 */
class MediaForegroundService: Service() {
    //상수들
    companion object {
        const val NOTIFICATION_CLOSE_DELAY = 500L
        const val NOTIFICATION_UPDATE_DURATION = 200L
        const val CHANNEL_ID = "my_foreground_channel"
        const val NOTIFICATION_ID = 1997
        const val DELAY_LIMIT = 2000L
    }

    //한개 화일 숨기기/숨김해제가 끝날때
    private var oneActionFinished = true
    private var actionShouldStop = false

    //Notification
    private var notificationBuilder: Notification.Builder? = null

    /**
     * [onBind]함수에서 돌려주는 Binder변수
     */
    private val mBinder: IBinder = MyBinder()

    //남은 화일들의 개수, 정보
    var mAction = ""
    var mMediaCount = 0
    var mCurrentProgress = 0
    var mRemainingData = ArrayList<Serializable>()
    var mFailedMediaCount = 0

    override fun onCreate() {
        createNotificationChannel()
        notificationBuilder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
        actionShouldStop = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        if(intent == null || intent.action == null)
            return null

        if(intent.action == REQUEST_STOP_SERVICE) {
            actionShouldStop = true
            val startTime = System.currentTimeMillis()
            while (!oneActionFinished){
                val currentTime = System.currentTimeMillis()
                if(currentTime - startTime >= DELAY_LIMIT)
                    break
            }

            stopForeground(true)
            stopSelf()
        }

        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null || intent.action == null || intent.extras == null)
            return START_NOT_STICKY

        if(actionShouldStop)
            return START_NOT_STICKY

        //action을 따져주어 해당한 동작들을 계속한다
        when(intent.action!!) {
            HIDE_MEDIA_ACTION -> {
                continueHidingMedia(intent)
            }
            SHOW_MEDIA_ACTION -> {
                continueShowingMedia(intent)
            }
        }

        return START_NOT_STICKY
    }

    //숨기기를 계속한다
    private fun continueHidingMedia(intent: Intent) {
        mAction = HIDE_MEDIA_ACTION
        mMediaCount = intent.getIntExtra(DATA_COUNT, 0)
        mCurrentProgress = intent.getIntExtra(CURRENT_PROGRESS, 0)
        mFailedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)
        val remainingMedia = intent.getSerializableExtra(REMAINING_DATA) as ArrayList<Medium>
        mRemainingData.clear()
        mRemainingData.addAll(remainingMedia)

        if(actionShouldStop)
            return

        //Notification builder초기설정
        notificationBuilder!!.setContentTitle(getString(R.string.hiding))
                .setProgress(mMediaCount, mCurrentProgress, false)
        startForeground(NOTIFICATION_ID, notificationBuilder!!.build())

        if(actionShouldStop)
            return
        ensureBackgroundThread {
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            var lastTime = System.currentTimeMillis()
            remainingMedia.forEach {
                oneActionFinished = false
                //하나씩 숨기기를 진행한다
                if(hideFile(it.path)) {
                    val hiddenMedium = HiddenMedium(it)
                    hiddenMedium.name = ".${it.name}"
                    hiddenMedium.path = it.parentPath + "/" + hiddenMedium.name

                    //Hidden Media Table에 하나씩 추가한다
                    hiddenMediumDao.insert(hiddenMedium)
                } else {
                    mFailedMediaCount ++
                }

                mCurrentProgress ++
                mRemainingData.remove(it)
                oneActionFinished = true

                if(actionShouldStop) {
                    return@ensureBackgroundThread
                }
                val curTime = System.currentTimeMillis()
                if(curTime - lastTime >= NOTIFICATION_UPDATE_DURATION)
                {
                    //Notification의 progress 를 갱신한다.
                    notificationBuilder!!.setOngoing(true)
                            .setContentText("$mCurrentProgress/$mMediaCount")
                            .setProgress(mMediaCount, mCurrentProgress, false)

                    mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder!!.build())
                    lastTime = curTime
                }
                if(actionShouldStop) {
                    return@ensureBackgroundThread
                }
            }

            //동작이 완료되였다는 notification띄우기
            if(mFailedMediaCount == 0) {
                notificationBuilder!!.setContentTitle(getString(R.string.directory_hide_complete))
                        .setContentText("$mCurrentProgress/$mMediaCount").setProgress(mMediaCount, mCurrentProgress, false)
            } else {
                notificationBuilder!!.setContentTitle(getString(R.string.directory_hide_failed, mFailedMediaCount))
                        .setContentText("$mCurrentProgress/$mMediaCount").setProgress(mMediaCount, mCurrentProgress, false)
            }
            mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder!!.build())

            if(actionShouldStop) {
                return@ensureBackgroundThread
            }

            //notification을 닫아준다
            Thread.sleep(NOTIFICATION_CLOSE_DELAY)
            stopForeground(true)
            stopSelf()
        }
    }

    //숨김해제를 계속한다
    private fun continueShowingMedia(intent: Intent) {
        mAction = SHOW_MEDIA_ACTION
        mMediaCount = intent.getIntExtra(DATA_COUNT, 0)
        mCurrentProgress = intent.getIntExtra(CURRENT_PROGRESS, 0)
        mFailedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)
        val remainingMediaPaths = intent.getSerializableExtra(REMAINING_DATA) as ArrayList<String>
        mRemainingData.clear()
        mRemainingData.addAll(remainingMediaPaths)

        if(actionShouldStop)
            return

        //Notification builder초기설정
        notificationBuilder!!.setContentTitle(getString(R.string.unhiding))
                .setProgress(mMediaCount, mCurrentProgress, false)

        startForeground(NOTIFICATION_ID, notificationBuilder!!.build())

        if(actionShouldStop)
            return

        ensureBackgroundThread {
            val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            var lastTime = System.currentTimeMillis()
            remainingMediaPaths.forEach {
                oneActionFinished = false
                //하나씩 숨기기를 진행한다
                if(unHideFile(it)) {
                    //Table에서 하나씩 삭제한다.
                    hiddenMediumDao.deleteItem(it)
                } else {
                    mFailedMediaCount ++
                }

                mCurrentProgress ++
                mRemainingData.remove(it)
                oneActionFinished = true

                if(actionShouldStop) {
                    return@ensureBackgroundThread
                }
                val curTime = System.currentTimeMillis()
                if(curTime - lastTime >= NOTIFICATION_UPDATE_DURATION)
                {
                    notificationBuilder!!.setOngoing(true)
                            .setContentText("$mCurrentProgress/$mMediaCount")
                            .setProgress(mMediaCount, mCurrentProgress, false)
                    mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder!!.build())
                    lastTime = curTime
                }
                if(actionShouldStop) {
                    return@ensureBackgroundThread
                }
            }

            //동작이 완료되였다는 notification띄우기
            if(mFailedMediaCount == 0) {
                notificationBuilder!!.setContentTitle(getString(R.string.directory_unhide_complete))
                        .setContentText("$mCurrentProgress/$mMediaCount").setProgress(mMediaCount, mCurrentProgress, false)
            } else {
                notificationBuilder!!.setContentTitle(getString(R.string.directory_unhide_failed, mFailedMediaCount))
                        .setContentText("$mCurrentProgress/$mMediaCount").setProgress(mMediaCount, mCurrentProgress, false)
            }
            mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder!!.build())
            if(actionShouldStop) {
                return@ensureBackgroundThread
            }

            //notification을 닫아준다
            Thread.sleep(NOTIFICATION_CLOSE_DELAY)
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
                CHANNEL_ID, "FOREGROUND Service Channel", IMPORTANCE_HIGH
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    /**
     * [onBind]함수에서 리용하는 [Binder]를 계승한 클라스
     */
    inner class MyBinder: Binder() {
        fun getService(): MediaForegroundService {
            return this@MediaForegroundService
        }
    }
}
