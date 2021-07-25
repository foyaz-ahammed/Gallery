package com.kr.gallery.pro.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.kr.gallery.pro.R
import kotlinx.android.synthetic.main.activity_settings.*

/**
 * 설정화면
 * 사진첩에 그림추가, 숨긴 등록부관리, 정보
 * [ManageCoverActivity], [HiddenFoldersActivity], [AboutActivity]
 */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //Back단추, 매 설정항목들을 눌렀을때의 동작들을 추가해준다.
        setupBackKey()
        setupAddPicturesToCover()
        setupManageHiddenFolders()
        setupManageAbout()
    }

    //화면의 우에 있는 Back단추를 눌렀을때
    private fun setupBackKey(){
        //Activity를 종료시킨다
        back_button.setOnClickListener {
            finish()
        }
    }

    private fun setupAddPicturesToCover() {
        //'사진첩에 그림추가'화면 activity를 띄워준다
        settings_manage_add_pictures_to_cover_holder.setOnClickListener {
            startActivity(Intent(this, ManageCoverActivity::class.java))
        }
    }

    private fun setupManageHiddenFolders() {
        //'숨긴등록부관리'화면 activity를 띄워주기
        settings_manage_hidden_folders_holder.setOnClickListener {
            startActivity(Intent(this, HiddenFoldersActivity::class.java))
        }
    }

    private fun setupManageAbout() {
        //정보화면 activity를 띄워준다
        settings_manage_about_holder.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
