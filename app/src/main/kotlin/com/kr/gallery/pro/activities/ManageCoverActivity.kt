package com.kr.gallery.pro.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.commons.views.MyGridLayoutManager
import com.kr.gallery.pro.R
import com.kr.gallery.pro.adapters.DirectoryGridAdapter
import com.kr.gallery.pro.adapters.SelectionUpdateListener
import com.kr.gallery.pro.extensions.config
import com.kr.gallery.pro.extensions.coverPageDB
import com.kr.gallery.pro.helpers.SELECTED_KEYS
import com.kr.gallery.pro.models.CoverPage
import com.kr.gallery.pro.models.Directory
import com.kr.gallery.pro.viewmodels.DirectoryViewModel
import kotlinx.android.synthetic.main.activity_manage_cover.*
import java.util.LinkedHashSet

/**
 * 사진첩에 그림추가 페지
 *
 * 설정화면 - '사진첩에 그림추가'를 눌렀을때, Coverpage가 빈 경우 '+'단추를 눌렀을때 이 화면이 펼쳐진다
 * @see SettingsActivity
 * @see CoverPage
 */
class ManageCoverActivity: SimpleActivity(), SelectionUpdateListener {
    private val coverPageDirectories = ArrayList<String>()

    //등록부 recycler view에 련결된 adapter
    private lateinit var mAdapter: DirectoryGridAdapter

    companion object {
        const val COVER_DIRECTORIES = "cover_directories"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_manage_cover)
        supportActionBar?.hide()

        //RecyclerView설정
        mAdapter = DirectoryGridAdapter(this, directory_thumbnails, this)
        directory_thumbnails.adapter = mAdapter
        setupGridLayoutManager()

        //확인, 취소단추를 눌렀을때
        cancel_btn.setOnClickListener {
            finish()
        }
        ok_btn.setOnClickListener {
            saveAndExit()
        }

        //회전같은것을 하여 activity가 재창조되였을때
        if(savedInstanceState != null){
            //상태들을 복귀해준다
            val selectedKeys: LinkedHashSet<Int> = savedInstanceState.getSerializable(SELECTED_KEYS)!! as LinkedHashSet<Int>
            mAdapter.selectedKeys = selectedKeys

            val size = selectedKeys.size
            selected_counts_label.text = getString(R.string.select_with_count, size)
            enableOkButton(size > 0)

            coverPageDirectories.clear()
            coverPageDirectories.addAll(savedInstanceState.getSerializable(COVER_DIRECTORIES)!! as ArrayList<String>)
        }

        //activity가 처음 창조되였을때
        else {
            ensureBackgroundThread {
                //자료기지에서 cover page table의 등록부경로들을 얻는다.
                val directories = coverPageDB.getDirectories()
                coverPageDirectories.clear()
                coverPageDirectories.addAll(directories)

                //Cover page등록부들에 대해서는 기정적으로 선택이 되있도록 한다.
                mAdapter.selectedKeys.clear()
                directories.forEach {
                    mAdapter.selectedKeys.add(it.hashCode())
                }

                runOnUiThread {
                    val size = coverPageDirectories.size
                    selected_counts_label.text = getString(R.string.select_with_count, size)
                    enableOkButton(size > 0)

                    //만약의 경우 view model observer에 먼저 걸리는 경우 ui를 갱신시킨다.
                    if(mAdapter.itemCount > 0)
                        mAdapter.notifyDataSetChanged()
                }
            }
        }

        //View model을 추가한다.
        val directoryViewModel = ViewModelProvider(this).get(DirectoryViewModel::class.java)
        directoryViewModel.allDirectories.observe(this, Observer {
            mAdapter.submitList(it as ArrayList<Directory>)
        })
    }

    //LayoutManager 설정
    private fun setupGridLayoutManager() {
        val layoutManager = directory_thumbnails.layoutManager as MyGridLayoutManager
        (directory_thumbnails.layoutParams as LinearLayout.LayoutParams).apply {
            topMargin = 0
            bottomMargin = 0
        }

        layoutManager.orientation = RecyclerView.VERTICAL

        layoutManager.spanCount = config.mediaColumnCnt
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }
    }

    /**
     * 확인단추를 능동/비능동시킨다.
     * @param enable
     * @see onSelectionUpdate
     */
    private fun enableOkButton(enable: Boolean) {
        val textColor = getColor(if(enable) R.color.common_primary_color else R.color.disabledTextColor)
        ok_btn.isEnabled = enable
        ok_btn.setTextColor(textColor)
    }

    /**
     * 선택이 갱신되였을때 호출된다.
     * 선택개수 label을 갱신하고 확인단추를 enable/disable시킨다.
     */
    override fun onSelectionUpdate() {
        val count = mAdapter.getSelectedCounts()
        selected_counts_label.text = getString(R.string.select_with_count, count)
        enableOkButton(count > 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //상태들 보관
        outState.putSerializable(SELECTED_KEYS, mAdapter.selectedKeys)
        outState.putSerializable(COVER_DIRECTORIES, coverPageDirectories)
    }

    /**
     * 확인단추를 눌렀을때 호출된다. [ok_btn]
     * 변경을 보관하고 activity를 끝낸다.
     */
    private fun saveAndExit() {
        //새로 추가할것, 삭제할 등록부들을 갈라낸다
        val newDirectories = mAdapter.getSelectedPaths()
        val dirsToAdd = newDirectories.clone() as ArrayList<String>
        val dirsToRemove = coverPageDirectories.clone() as ArrayList<String>
        dirsToAdd.removeAll(coverPageDirectories)
        dirsToRemove.removeAll(newDirectories)

        if(dirsToAdd.isNotEmpty() || dirsToRemove.isNotEmpty()) {
            ensureBackgroundThread {
                //자료기지에 삭제 및 추가를 진행한다
                coverPageDB.deleteAndInsert(dirsToRemove, dirsToAdd.map { CoverPage(null, it) })

                //Toast띄워주기
                runOnUiThread {
                    Toast.makeText(applicationContext, R.string.saved_changes_successfully, Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
        else {
            //Toast띄워주기
            Toast.makeText(applicationContext, R.string.nothing_changed, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
