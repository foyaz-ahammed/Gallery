package com.kr.gallery.pro.helpers

import android.app.Dialog
import android.content.Context
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.view.Gravity
import com.kr.commons.helpers.DATE_FORMAT_ONE
import com.kr.gallery.pro.models.TimeThumbnailItem
import java.io.File

class Utils {
    companion object{
        /**
         * @return 날자문자렬을 돌려준다.
         * @param timeMillis 미리초로 표현한 시간
         */
        fun formatDate(timeMillis: Long): String {
            if(timeMillis <= 0)
                return ""
            return DateFormat.format(DATE_FORMAT_ONE, timeMillis).toString()
        }

        /**
         * @return dp를 pixel로 변환하여 돌려준다.
         * @param dp
         * @param context
         */
        fun convertDpToPixel(dp: Float, context: Context): Float {
            return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        }

        /**
         * 대화창을 화면아래에 떨구어주기
         * @param dialog
         */
        fun makeBottomDialog(dialog: Dialog) {
            val window = dialog.window!!
            window.setGravity(Gravity.BOTTOM)
        }
    }
}
