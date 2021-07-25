package com.kr.gallery.pro.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.kr.commons.helpers.REQUEST_PICK_IMAGE
import com.kr.commons.helpers.REQUEST_SET_WALLPAPER
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.PickDirectoryAdapter
import com.kr.gallery.pro.viewmodels.PickDirectoryViewModel
import kotlinx.android.synthetic.main.activity_pick_image.*

/**
 * 화상선택화면
 * 1. 화상을 선택하는 용도로 쓰인다. 실례로 어떤 app에서 화상설정을 위하여 gallery app을 기동할때 우리의 app을 선택하였다면 이때 이 activity가 실행된다.
 * 2. 배경화상을 설정하는 용도로 쓰인다.
 *   먼저 화상을 선택하고 배경화면설정화면 [SetWallpaperActivity]을 기동하여 선택된 화상을 배경화면으로 설정한다.
 * @see SetWallpaperActivity
 * @see MediaActivity
 */
class ImagePickActivity: AppCompatActivity() {

    //화상선택을 위한 intent인가?
    private var mIsGetImageIntent = false
    //배경화면설정을 위한 intent인가?
    private var mIsSetWallpaperIntent = false

    //RecyclerView에 설정할 adapter
    private lateinit var mAdapter: PickDirectoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(intent == null)
            return

        val action = intent.action
        mIsGetImageIntent = action == Intent.ACTION_PICK
        mIsSetWallpaperIntent = action == Intent.ACTION_SET_WALLPAPER

        setContentView(R.layout.activity_pick_image)
        supportActionBar?.hide()

        //Recyclerview 설정
        mAdapter = PickDirectoryAdapter(this, mIsGetImageIntent, mIsSetWallpaperIntent)
        directories_list.adapter = mAdapter

        //View model추가
        val viewModel = ViewModelProvider(this).get(PickDirectoryViewModel::class.java)
        viewModel.imageDirectories.observe(this) {
            mAdapter.submitList(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        //등록부보기화면에서 RESULT_OK를 돌려주면 화상이 선택된것으로 보고 화상의 uri정보를 결과로 하고 activity를 종료한다
        if(requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK
                && intent != null && intent.data != null) {
            Intent().apply {
                data = intent.data
                setResult(Activity.RESULT_OK, this)
            }
            finish()
        }
        //배경화면설정화면에서 RESULT_OK를 돌려주면 activity를 종료한다.
        else if(requestCode == REQUEST_SET_WALLPAPER) {
            if(resultCode == Activity.RESULT_OK)
                finish()
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    //Back단추누르면 activity종료
    fun onBackSelect(view: View) {
        finish()
    }
}
