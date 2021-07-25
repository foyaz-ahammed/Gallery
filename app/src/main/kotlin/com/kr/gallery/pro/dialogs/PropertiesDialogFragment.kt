package com.kr.gallery.pro.dialogs

import android.app.Activity
import android.content.res.Resources
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.*
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.commons.helpers.isNougatPlus
import com.kr.commons.models.FileDirItem
import com.kr.gallery.pro.R
import com.kr.gallery.pro.helpers.MAX_CLOSE_DOWN_GESTURE_DURATION
import kotlinx.android.synthetic.main.fragment_properties.*
import kotlinx.android.synthetic.main.fragment_properties.view.*
import kotlinx.android.synthetic.main.property_item.view.*
import kotlin.math.abs

/**
 * 화상정보화면
 * 화상보기화면에서 '정보'단추를 눌렀을때 펼쳐지는 화면, [Activity]가 아니라 [DialogFragment]이다.
 */
class PropertiesDialogFragment : DialogFragment() {
    private  var mInflater: LayoutInflater? = null
    private  var mPropertyView: ViewGroup? = null
    private  var mResources: Resources? = null
    private lateinit var mActivity: Activity

    companion object {
        const val PATH = "path"
        const val COUNT_HIDDEN_ITEMS = "countHiddenItems"

        fun newInstance(path: String, countHiddenItems: Boolean = false): PropertiesDialogFragment? {
            val fragment = PropertiesDialogFragment()
            val bundle = Bundle().apply {
                putString(PATH, path)
                putBoolean(COUNT_HIDDEN_ITEMS, countHiddenItems)
            }
            fragment.arguments = bundle
            return fragment
        }

        fun newInstance(path: List<String>, countHiddenItems: Boolean = false): PropertiesDialogFragment? {
            val fragment = PropertiesDialogFragment()
            val bundle = Bundle().apply {
                putStringArrayList(PATH, ArrayList(path))
                putBoolean(COUNT_HIDDEN_ITEMS, countHiddenItems)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_App_Dialog_FullScreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View =  inflater.inflate(R.layout.fragment_properties, container, false)
        val path: String = arguments?.getString(PATH)!!
        val countHiddenItems = arguments?.getBoolean(COUNT_HIDDEN_ITEMS)

        mActivity = requireActivity()
        mPropertyView = rootView.properties_holder!!
        mResources = mActivity.resources
        mInflater = LayoutInflater.from(activity)

        if (!path.let { mActivity.getDoesFilePathExist(it) } && !path.startsWith("content://")) {
            mActivity.toast(String.format(mActivity.getString(R.string.source_file_doesnt_exist), path))
            return null
        }

        val fileDirItem = path?.let { FileDirItem(it, path.getFilenameFromPath(), mActivity.getIsPathDirectory(path)) }

        //화일이름 label 설정
        rootView.findViewById<TextView>(R.id.property_file_name).text = fileDirItem.name.getFilenameContent()

        //촬영날자
        val exif = if (isNougatPlus() && mActivity.isPathOnOTG(path)) {
            ExifInterface((activity as BaseSimpleActivity).getFileInputStreamSync(path)!!)
        } else if (isNougatPlus() && path.startsWith("content://")) {
            try {
                ExifInterface(mActivity.contentResolver.openInputStream(Uri.parse(path)))
            } catch (e: Exception) {
                null
            }
        } else {
            ExifInterface(path)
        }

        val dateTaken = exif?.getExifDateTaken(mActivity)
        if (!dateTaken.isNullOrEmpty()) {
            addProperty(R.string.date_taken, dateTaken)
        }

        //수정시간
        addProperty(R.string.last_modified, "…", R.id.properties_last_modified)

        //해상도
        fileDirItem.getResolution(mActivity)?.let { addProperty(R.string.resolution, it.formatAsResolution()) }

        //크기
        addProperty(R.string.size, "…", R.id.properties_size)
        ensureBackgroundThread {
            val fileCount = mActivity.let {
                if (countHiddenItems != null) {
                    fileDirItem.getProperFileCount(it, countHiddenItems)
                }
            }
            val size = fileDirItem.getProperSize(mActivity).formatSize()
            mActivity.runOnUiThread {
                rootView.findViewById<TextView>(R.id.properties_size).property_content.text = getPropertyContent(R.string.size, size.toString())

                if (fileDirItem.isDirectory) {
                    mActivity.findViewById<TextView>(R.id.properties_file_count).property_content.text = getPropertyContent(R.string.files_count, fileCount.toString())
                }
            }

            if (!fileDirItem.isDirectory) {
                val projection = arrayOf(MediaStore.Images.Media.DATE_MODIFIED)
                val uri = MediaStore.Files.getContentUri("external")
                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                val selectionArgs = arrayOf(path)
                val cursor = mActivity.contentResolver?.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        val dateModified = cursor.getLongValue(MediaStore.Images.Media.DATE_MODIFIED) * 1000L
                        updateLastModified(mActivity, rootView, dateModified)
                    } else {
                        updateLastModified(mActivity, rootView, fileDirItem.getLastModified(mActivity))
                    }
                }

                val exif = if (isNougatPlus() && mActivity.isPathOnOTG(fileDirItem.path)) {
                    ExifInterface((activity as BaseSimpleActivity).getFileInputStreamSync(fileDirItem.path)!!)
                } else if (isNougatPlus() && fileDirItem.path.startsWith("content://")) {
                    try {
                        ExifInterface(mActivity.contentResolver.openInputStream(Uri.parse(fileDirItem.path)))
                    } catch (e: Exception) {
                        return@ensureBackgroundThread
                    }
                } else {
                    ExifInterface(fileDirItem.path)
                }

                val latLon = FloatArray(2)
                if (exif.getLatLong(latLon)) {
                    mActivity.runOnUiThread {
                        addProperty(R.string.gps_coordinates, "${latLon[0]}, ${latLon[1]}")
                    }
                }

                val altitude = exif.getAltitude(0.0)
                if (altitude != 0.0) {
                    mActivity.runOnUiThread {
                        addProperty(R.string.altitude, "${altitude}m")
                    }
                }
            }
        }

        //경로
        addProperty(R.string.path, path)

        if(fileDirItem.path.isVideoSlow()) {
            fileDirItem.getDuration(mActivity)?.let { addProperty(R.string.duration, it) }
            fileDirItem.getArtist(mActivity)?.let { addProperty(R.string.artist, it) }
            fileDirItem.getAlbum(mActivity)?.let { addProperty(R.string.album, it) }
        }

        //작성자
        val manufacturer = exif?.getManufacturer(mActivity)
        if(!manufacturer.isNullOrEmpty()) {
            addProperty(R.string.manufacturer, manufacturer)
        }

        //형
        val model = exif?.getModel()
        if(!model.isNullOrEmpty()) {
            addProperty(R.string.model, model)
        }

        //백색도
        val whiteBalance = exif?.getWhiteBalance(mActivity)
        if(!whiteBalance.isNullOrEmpty()) {
            addProperty(R.string.white_balance, whiteBalance)
        }

        //순간조명
        val flash = exif?.getFlash(mActivity)
        if(!whiteBalance.isNullOrEmpty()) {
            addProperty(R.string.flash, flash)
        }

        //로출시간
        val exposureTime = exif?.getExposureTime()
        if(!exposureTime.isNullOrEmpty()) {
            addProperty(R.string.exposure_time, exposureTime)
        }

        //초점길이
        val focalLength = exif?.getExifFocalLength()
        if(!focalLength.isNullOrEmpty()){
            addProperty(R.string.focal_length, focalLength)
        }

        //렌즈구경
        val lensAperture = exif?.getLensAperture()
        if(!lensAperture.isNullOrEmpty()) {
            addProperty(R.string.lens_aperture, lensAperture)
        }

        return rootView
    }

    private var mTouchDownTime = 0L
    private var mTouchDownX = 0f
    private var mTouchDownY = 0f
    private var mCloseDownThreshold = -100f
    private var mIgnoreCloseDown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 내리밀어 끄기
        properties_scrollview.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                when (event?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        mTouchDownTime = System.currentTimeMillis()
                        mTouchDownX = event.x
                        mTouchDownY = event.y
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> mIgnoreCloseDown = true
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val diffX = mTouchDownX - event.x
                        val diffY = mTouchDownY - event.y

                        val downGestureDuration = System.currentTimeMillis() - mTouchDownTime
                        if (!mIgnoreCloseDown && abs(diffY) > abs(diffX) && downGestureDuration < MAX_CLOSE_DOWN_GESTURE_DURATION) {
                            if (diffY < mCloseDownThreshold) dismiss()
                        }
                        mIgnoreCloseDown = false
                    }
                }
                return true
            }
        })

        requireDialog().window?.setWindowAnimations(R.style.DialogAnimation)

        //확인단추를 눌렀을때 대화창 닫기
        ok_btn.setOnClickListener {
            dismiss()
        }
    }

    /**
     * 최종수정시간 설정
     */
    private fun updateLastModified(activity: Activity, view: View, timestamp: Long) {
        activity.runOnUiThread {
            view.findViewById<TextView>(R.id.properties_last_modified).property_content.text = getPropertyContent(R.string.last_modified, timestamp.formatDate(activity))
        }
    }

    /**
     * 속성을 추가 (촬영날자, 크기, 경로, ... )
     * @param labelId 속성이름(String resource)
     * @param value 속성값
     * @param viewId view의 id, 0을 주는 경우에는 view를 새로 만든다.
     */
    private fun addProperty(labelId: Int, value: String?, viewId: Int = 0) {
        if (value == null)
            return

        val content = getPropertyContent(labelId, value)
        mInflater!!.inflate(R.layout.property_item, mPropertyView, false).apply {
            property_content.text = content
            mPropertyView?.properties_holder?.addView(this)

            setOnLongClickListener {
                mActivity.copyToClipboard(content)
                true
            }

            if (labelId == R.string.gps_coordinates) {
                setOnClickListener {
                    mActivity.showLocationOnMap(value)
                }
            }

            if (viewId != 0) {
                id = viewId
            }
        }
    }

    /**
     * @return 속성이름 + 속성값 을 돌려준다
     * @param labelId: 속성이름
     * @param value: 속성값
     */
    private fun getPropertyContent(labelId: Int, value: String): String {
        return mResources?.getString(labelId) + " : " + value
    }
}
