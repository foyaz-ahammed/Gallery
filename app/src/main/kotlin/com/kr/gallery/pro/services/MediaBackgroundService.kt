package com.kr.gallery.pro.services

import android.app.IntentService
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kr.commons.helpers.*
import com.kr.gallery.pro.extensions.hiddenMediumDao
import com.kr.gallery.pro.extensions.hideFile
import com.kr.gallery.pro.extensions.unHideFile
import com.kr.gallery.pro.models.HiddenMedium
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.receivers.MediaBroadcastReceiver
import java.io.Serializable

/**
 * 선택된 등록부를 숨기기/숨김해제할때 리용하는 service
 * @see MediaForegroundService
 * @see MediaBroadcastReceiver
 */
class MediaBackgroundService(name: String) : IntentService(name) {
    constructor(): this(BACKGROUND_SERVICE_NAME)

    companion object {
        const val BACKGROUND_SERVICE_NAME = "background_service"
        const val DELAY_LIMIT = 2000L
    }

    private var action: String = ""

    /**
     * App이 꺼졌을때 service가 중단되여야 한다는것을 나타내는 상태변수
     * [onHandleIntent]에서 매 단락마다 상태를 검사하여 값이 true로 설정되있으면 함수를 끝낸다
     */
    private var actionShouldStop = false

    //순환을 돌면서 한개 media씩 숨기기하는데 매 순환이 도중에 중지되지 않도록 하기위해 리용되는 상태변수
    private var oneActionFinished = true

    //기본동작숨기기동작이 시작되였을때부터 끝나기전까지 false로 설정된다.
    private var serviceActionFinished = true

    //현재의 progress상태들
    private val remainingData = ArrayList<Serializable>()
    private var currentProgress = 0
    private var totalMediaCount = 0
    private var failedMediaCount = 0

    override fun onCreate() {
        super.onCreate()
        actionShouldStop = false
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null || intent.action == null || intent.extras == null) {
            return
        }

        if(actionShouldStop)
            return

        action = intent.action!!
        val broadcastManager = LocalBroadcastManager.getInstance(this)
        when(action) {
            //입력값으로 들어온 Media에 대한 숨기기를 진행한다
            HIDE_MEDIA_ACTION -> {
                val mediaToHide = intent.extras!!.get(REMAINING_DATA) as ArrayList<Medium>
                totalMediaCount = intent.getIntExtra(DATA_COUNT, 0)
                var progress = intent.getIntExtra(CURRENT_PROGRESS, 0)
                failedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)
                remainingData.clear()
                remainingData.addAll(mediaToHide)

                if(actionShouldStop)
                    return
                broadcastManager.sendBroadcast(
                        Intent(HIDE_START).putExtra(DATA_COUNT, totalMediaCount)
                                .putExtra(CURRENT_PROGRESS, progress)
                )

                serviceActionFinished = false
                if(actionShouldStop)
                    return
                mediaToHide.forEach {
                    if(actionShouldStop)
                        return

                    oneActionFinished = false
                    if(hideFile(it.path)) {
                        val hiddenMedium = HiddenMedium(it)
                        hiddenMedium.name = ".${it.name}"
                        hiddenMedium.path = it.parentPath + "/" + hiddenMedium.name

                        //Hidden Media Table에 하나씩 추가한다
                        hiddenMediumDao.insert(hiddenMedium)
                    } else {
                        failedMediaCount ++
                    }

                    progress ++

                    //상태들 보관
                    remainingData.remove(it)
                    currentProgress = progress

                    oneActionFinished = true

                    if(actionShouldStop)
                        return
                    broadcastManager.sendBroadcast(
                            Intent(ACTION_PROGRESS).putExtra(ACTION_PROGRESS, progress)
                    )
                }
            }

            //입력값으로 들어온 Media에 대한 숨기기해제를 진행한다
            SHOW_MEDIA_ACTION -> {
                val hiddenFilePaths = intent.extras!!.get(REMAINING_DATA) as ArrayList<String>
                totalMediaCount = intent.getIntExtra(DATA_COUNT, 0)
                var progress = intent.getIntExtra(CURRENT_PROGRESS, 0)
                failedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)
                remainingData.clear()
                remainingData.addAll(hiddenFilePaths)

                if(actionShouldStop)
                    return
                broadcastManager.sendBroadcast(
                        Intent(SHOW_START).putExtra(DATA_COUNT, totalMediaCount)
                                .putExtra(CURRENT_PROGRESS, progress)
                )

                serviceActionFinished = false
                if(actionShouldStop)
                    return
                hiddenFilePaths.forEach {
                    if(actionShouldStop)
                        return

                    oneActionFinished = false
                    if(unHideFile(it)) {
                        //Table에서 하나씩 삭제한다.
                        hiddenMediumDao.deleteItem(it)
                    }

                    progress ++

                    //상태들 보관
                    remainingData.remove(it)
                    currentProgress = progress

                    oneActionFinished = true

                    if(actionShouldStop)
                        return
                    broadcastManager.sendBroadcast(
                            Intent(ACTION_PROGRESS).putExtra(ACTION_PROGRESS, progress)
                    )
                }
            }
        }

        serviceActionFinished = true
        if(actionShouldStop)
            return
        broadcastManager.sendBroadcast(
                Intent(ACTION_COMPLETE).putExtra(FAILED_MEDIA_COUNT, failedMediaCount)
        )
    }

    /**
     * Service 가 꺼질때 호출된다.
     * @see serviceActionFinished
     */
    override fun onDestroy() {
        if(!serviceActionFinished) {
            if(action == HIDE_MEDIA_ACTION || action == SHOW_MEDIA_ACTION) {
                sendBroadcast(
                        Intent(action)
                                .setClass(this, MediaBroadcastReceiver::class.java)
                                .putExtra(DATA_COUNT, totalMediaCount)
                                .putExtra(CURRENT_PROGRESS, currentProgress)
                                .putExtra(REMAINING_DATA, remainingData)
                                .putExtra(FAILED_MEDIA_COUNT, failedMediaCount)
                )
            }
        }
        super.onDestroy()
    }

    /**
     * Service 실행도중에 App이 꺼지면 호출된다.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        actionShouldStop = true

        //순환을 계속 돌다가 2초가 지난후에도 oneActionFinished 가 계속 false로 설정되면 순환을 끝낸다
        val startTime = System.currentTimeMillis()
        while (!oneActionFinished){
            val currentTime = System.currentTimeMillis()
            if(currentTime - startTime >= DELAY_LIMIT)
                break
        }

        //순환이 끝난다음 service 를 중지시킨다
        stopSelf()
    }
}
