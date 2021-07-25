package com.kr.gallery.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kr.commons.helpers.*
import com.kr.gallery.pro.models.Medium
import com.kr.gallery.pro.services.MediaForegroundService

/**
 * [android.content.ContextWrapper.sendBroadcast]에서 보낸 broadcast를 접수하는 클라스이다
 * @see MediaForegroundService
 */
class MediaBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent == null)
            return

        //Background service가 destroy되여 동작이 중지되면 foreground service를 호출하여 동작을 계속 하도록 해준다
        val action = intent.action?: return
        val mediaCount = intent.getIntExtra(DATA_COUNT, 0)
        val currentProgress = intent.getIntExtra(CURRENT_PROGRESS, 0)
        val failedMediaCount = intent.getIntExtra(FAILED_MEDIA_COUNT, 0)
        val remainingData = intent.getSerializableExtra(REMAINING_DATA)
        if(action == HIDE_MEDIA_ACTION || action == SHOW_MEDIA_ACTION) {
            val serviceIntent = Intent()
                    .setClass(context!!, MediaForegroundService::class.java)
                    .setAction(action)
                    .putExtra(DATA_COUNT, mediaCount)
                    .putExtra(CURRENT_PROGRESS, currentProgress)
                    .putExtra(REMAINING_DATA, remainingData)
                    .putExtra(FAILED_MEDIA_COUNT, failedMediaCount)

            context.startForegroundService(serviceIntent)
        }
    }
}
