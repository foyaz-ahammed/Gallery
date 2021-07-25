package com.kr.gallery.pro.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kr.commons.dialogs.RadioGroupDialog
import com.kr.commons.extensions.toast
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.commons.models.RadioItem
import com.kr.gallery.pro.R
import kotlinx.android.synthetic.main.activity_set_wallpaper.*
import kotlinx.android.synthetic.main.bottom_set_wallpaper_actions.*

/**
 * 배경화면설정 activity
 * 이 activity는 화상선택화면에서 [Intent.ACTION_SET_WALLPAPER]이 intent action으로 들어왔을때 화상선택후 기동된다.
 * @see ImagePickActivity
 */
class SetWallpaperActivity : AppCompatActivity() {
    private var wallpaperFlag = -1

    //ImagepickActivity로부터 선택된 화상의 uri
    lateinit var uri: Uri

    lateinit var wallpaperManager: WallpaperManager

    //Wallpaper를 설정하는동안 단추사건들을 막아주기 위한 flag변수
    private var mIsSettingWallpaper = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //전화면설정
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_set_wallpaper)

        //action bar는 필요없으므로 숨긴다.
        supportActionBar?.hide()

        //단추사건, 화상을 image view에 설정한다.
        setupBottomActions()
        handleImage(intent)
    }

    /**
     * @param intent
     * intent로부터 화상uri를 받아서 imageview에 설정한다
     */
    private fun handleImage(intent: Intent) {
        uri = intent.data!!
        if (uri.scheme != "file" && uri.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        image_view.setImageURI(uri)
        wallpaperManager = WallpaperManager.getInstance(applicationContext)
    }

    /**
     * 화면아래에 있는 save, cancel단추들에 대한 동작들을 추가한다.
     */
    private fun setupBottomActions() {
        bottom_save.setOnClickListener {
            //배경화면을 설정한다.
            if(!mIsSettingWallpaper)
                confirmWallpaper()
        }
        bottom_cancel.setOnClickListener {
            //결과로 result_canceled를 돌려주며 activity를 종료한다.
            if(!mIsSettingWallpaper) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    /**
     * 배경화면 설정
     */
    @SuppressLint("InlinedApi")
    private fun confirmWallpaper() {
        //'Home화면'/'Lock화면'/'둘다' 이 중에 한가지 option을 선택한다.
        val items = arrayListOf(
                RadioItem(WallpaperManager.FLAG_SYSTEM, getString(R.string.home_screen)),
                RadioItem(WallpaperManager.FLAG_LOCK, getString(R.string.lock_screen)),
                RadioItem(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, getString(R.string.home_and_lock_screen)))

        //Radio단추를 가진 대화창 현시
        RadioGroupDialog(this, items) {
            wallpaperFlag = it as Int

            //배경화면 설정하는동안 다른 ui동작들이 진행되지 않도록 flag변수를 true로 설정한다.
            mIsSettingWallpaper = true

            //snackbar띄우기
            val snackBar = Snackbar.make(activity_set_wallpaper_holder, R.string.setting_wallpaper, Snackbar.LENGTH_INDEFINITE)
            snackBar.show()

            //thread를 통해 배경화면을 설정한다.
            ensureBackgroundThread {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                try {
                    wallpaperManager.setBitmap(bitmap, null, true, wallpaperFlag)
                    setResult(Activity.RESULT_OK)
                } catch (e: OutOfMemoryError) {
                    toast(R.string.out_of_memory_error)
                    setResult(Activity.RESULT_CANCELED)
                }

                //snack bar를 닫고 flag변수를 다시 false로 설정한다.
                snackBar.dismiss()
                mIsSettingWallpaper = false

                //activity를 완료한다.
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if(!mIsSettingWallpaper)
            super.onBackPressed()
    }

    /**
     * 화면의 우위치에 있는 back단추를 누르면 cancel단추를 눌렀을때와 같은 동작을 진행한다.
     */
    fun onBackSelect(view: View) {
        if(!mIsSettingWallpaper) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
