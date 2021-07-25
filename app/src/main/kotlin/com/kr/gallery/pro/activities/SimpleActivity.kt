package com.kr.gallery.pro.activities

import android.annotation.SuppressLint
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.helpers.isPiePlus
import com.kr.gallery.pro.R
import com.kr.gallery.pro.views.KrBottomSheet
import kotlinx.android.synthetic.main.bottom_sheet_behavior.*

open class SimpleActivity : BaseSimpleActivity() {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<KrBottomSheet>

    override fun onSwipeDown() {
        if(bottomSheet == null)
            return

        //bottom sheet가 expand된 상태일때 아래로 끌기 하면 expand상태를 해제한다.
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        if(bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            bottomSheet.toggleExpand()
        }
    }

    override fun getAppIconIDs() = arrayListOf(
            R.mipmap.ic_launcher_red,
            R.mipmap.ic_launcher_pink,
            R.mipmap.ic_launcher_purple,
            R.mipmap.ic_launcher_deep_purple,
            R.mipmap.ic_launcher_indigo,
            R.mipmap.ic_launcher_blue,
            R.mipmap.ic_launcher_light_blue,
            R.mipmap.ic_launcher_cyan,
            R.mipmap.ic_launcher_teal,
            R.mipmap.ic_launcher_green,
            R.mipmap.ic_launcher_light_green,
            R.mipmap.ic_launcher_lime,
            R.mipmap.ic_launcher_yellow,
            R.mipmap.ic_launcher_amber,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_deep_orange,
            R.mipmap.ic_launcher_brown,
            R.mipmap.ic_launcher_blue_grey,
            R.mipmap.ic_launcher_grey_black
    )

    override fun getAppLauncherName() = getString(R.string.app_launcher_name)

    @SuppressLint("InlinedApi")
    protected fun checkNotchSupport() {
        if (isPiePlus()) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }

    override fun showBottomSheet() {
    }

    override fun hideBottomSheet() {
    }

    override fun showBottomShareBar(show: Boolean) {

    }

    override fun updateSelectedCounts(count: Int, allSelected: Boolean) {
    }
}
