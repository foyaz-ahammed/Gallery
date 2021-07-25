package com.kr.gallery.pro.extensions

import android.app.ProgressDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.PictureDrawable
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.kr.commons.extensions.*
import com.kr.commons.helpers.*
import com.kr.gallery.pro.R
import com.kr.gallery.pro.databases.GalleryDatabase
import com.kr.gallery.pro.helpers.*
import com.kr.gallery.pro.interfaces.*
import com.kr.gallery.pro.models.*
import com.kr.gallery.pro.svg.SvgSoftwareLayerSetter
import kotlinx.coroutines.yield
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.collections.ArrayList

val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager

//등록부경로로부터 등록부이름 얻어내기
fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}

val Context.config: Config get() = Config.newInstance(applicationContext)

val Context.widgetsDB: WidgetsDao get() = GalleryDatabase.getInstance(applicationContext).WidgetsDao()

val Context.mediaDB: MediumDao get() = GalleryDatabase.getInstance(applicationContext).MediumDao()

val Context.coverPageDB: CoverPageDao get() = GalleryDatabase.getInstance(applicationContext).CoverPageDao()

val Context.directoryDao: DirectoryDao get() = GalleryDatabase.getInstance(applicationContext).DirectoryDao()

val Context.hiddenMediumDao: HiddenMediumDao get() = GalleryDatabase.getInstance(applicationContext).HiddenMediumDao()

val Context.recycleBinPath: String get() = filesDir.absolutePath

//화일 숨기기
fun Context.hideFile(filePath: String): Boolean{
    val file = File(filePath)
    if(file.exists() && !file.isHidden){
        val pos = filePath.lastIndexOf("/")
        val fileName = filePath.substring(pos + 1)
        val parentPath = filePath.substring(0, pos)
        val newFileName = ".$fileName"

        //화일이름을 바꾼다
        val toFile = File(parentPath, newFileName)
        return renameFile(file, toFile, true)
    }
    return false
}

//숨김해제하기
fun Context.unHideFile(filePath: String): Boolean{
    val file = File(filePath)
    if(file.exists() && file.isHidden) {
        val pos = filePath.lastIndexOf("/")
        val fileName = filePath.substring(pos + 1)
        val parentPath = filePath.substring(0, pos)

        //화일이름앞에 '.'이 놓여있으면 없앤다
        if(fileName.length > 2 && fileName[0] == '.'){
            val newFileName = fileName.substring(1, fileName.length)

            //화일이름을 바꾼다
            val toFile = File(parentPath, newFileName)
            return renameFile(file, toFile)
        }
    }

    //실패
    return false
}

//화일이름바꾸기
fun Context.renameFile(oldFile: File, newFile: File, isHide: Boolean = false): Boolean{
    var fileRenamed = true
    val oldPath = oldFile.absolutePath
    val newPath = newFile.absolutePath

    if(isPathOnSD(oldPath)) {
        if(baseConfig.treeUri != "") {
            val document = getDocumentFile(oldPath) ?: return false
            try {
                DocumentsContract.renameDocument(contentResolver, document.uri, newPath.getFilenameFromPath()) != null
            } catch (ignored: FileNotFoundException) {
            }
        } else {
            fileRenamed = false
        }
    }

    else {
        fileRenamed = oldFile.renameTo(newFile)
    }

    if(fileRenamed) {
        //Media store에서 이전 화일 삭제
        val where = "${MediaStore.MediaColumns.DATA} = ?"
        contentResolver.delete(getFileUri(oldPath), where, arrayOf(oldPath))

        if(isHide) {
            contentResolver.delete(getFileUri(newPath), where, arrayOf(newPath))
        }
        else {
            //Media store에 새 화일 추가
            MediaScannerConnection.scanFile(applicationContext, arrayOf(newFile.toString()),
                    arrayOf(newFile.name), object : MediaScannerConnection.OnScanCompletedListener {
                override fun onScanCompleted(path: String?, uri: Uri?) {
                }
            })
        }
        return true
    }
    return false
}

//Progress dialog생성
fun Context.makeProgressDialog(count: Int, message: String): ProgressDialog {
    val dialog = ProgressDialog(this)
    dialog.apply {
        max = count // Progress Dialog Max Value
        setMessage(message)
        setProgressStyle(ProgressDialog.STYLE_HORIZONTAL) // Progress Dialog Style Horizontal
        setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progressbar_states))
        window!!.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.dialog_bg))
        setCancelable(false)
        Utils.makeBottomDialog(this)
    }
    return dialog
}

/**
 * Sort directories and return
 * @param source input directory list
 */
@Suppress("UNCHECKED_CAST")
fun getSortedDirectories(source: ArrayList<Directory>): ArrayList<Directory> {
    val dirs = source.clone() as ArrayList<Directory>

    //Camera, movies등록부는 앞으로 옮겨준다
    var cameraDirectory: Directory? = null
    var movieDirectory: Directory? = null
    dirs.forEach {
        if(it.path == CAMERA_PATH) {
            cameraDirectory = it
        }
        else if(it.path == MOVIE_PATH){
            movieDirectory = it
        }
    }
    if(movieDirectory != null) {
        dirs.remove(movieDirectory!!)
        dirs.add(0, movieDirectory!!)
    }
    if(cameraDirectory != null) {
        dirs.remove(cameraDirectory!!)
        dirs.add(0, cameraDirectory!!)
    }

    return dirs
}

fun Context.getFolderNameFromPath(path: String): String {
    return when (path) {
        internalStoragePath -> getString(R.string.internal)
        sdCardPath -> getString(R.string.sd_card)
        otgPath -> getString(R.string.usb)
        else -> path.getFilenameFromPath()
    }
}

fun Context.loadImage(type: Int, path: String, target: ImageView,
                      skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    if (type == TYPE_IMAGES || type == TYPE_VIDEOS || type == TYPE_RAWS) {
        if (type == TYPE_IMAGES && path.isPng()) {
            loadPng(path, target, true, skipMemoryCacheAtPaths)
        } else {
            loadJpg(path, target, true, skipMemoryCacheAtPaths)
        }
    } else if (type == TYPE_GIFS) {
        try {
            val gifDrawable = GifDrawable(path)
            target.setImageDrawable(gifDrawable)
            gifDrawable.start()

            target.scaleType = ImageView.ScaleType.CENTER_CROP
        } catch (e: Exception) {
            loadStaticGIF(path, target, true, skipMemoryCacheAtPaths)
        } catch (e: OutOfMemoryError) {
            loadStaticGIF(path, target, true, skipMemoryCacheAtPaths)
        }
    } else if (type == TYPE_SVGS) {
        loadSVG(path, target, true)
    }
}

fun Context.getPathLocation(path: String): Int {
    return when {
        isPathOnSD(path) -> LOCATION_SD
        isPathOnOTG(path) -> LOCATION_OTG
        else -> LOCATION_INTERNAL
    }
}

fun Context.loadPng(path: String, target: ImageView, cropThumbnails: Boolean, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
        .signature(path.getFileSignature())
        .skipMemoryCache(true/*skipMemoryCacheAtPaths?.contains(path) == true*/)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .priority(Priority.LOW)
        .format(DecodeFormat.PREFER_ARGB_8888)

    val builder = Glide.with(applicationContext)
        .asBitmap()
        .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options).into(target)
}

fun Context.loadJpg(path: String, target: ImageView, cropThumbnails: Boolean, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
        .signature(path.getFileSignature())
        .skipMemoryCache(true/*skipMemoryCacheAtPaths?.contains(path) == true*/)
        .priority(Priority.LOW)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    val builder = Glide.with(applicationContext)
        .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(target)
}

fun Context.loadStaticGIF(path: String, target: ImageView, cropThumbnails: Boolean, skipMemoryCacheAtPaths: ArrayList<String>? = null) {
    val options = RequestOptions()
        .signature(path.getFileSignature())
        .skipMemoryCache(skipMemoryCacheAtPaths?.contains(path) == true)
        .priority(Priority.LOW)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

    val builder = Glide.with(applicationContext)
        .asBitmap() // make sure the GIF wont animate
        .load(path)

    if (cropThumbnails) options.centerCrop() else options.fitCenter()
    builder.apply(options)
        .into(target)
}

fun Context.loadSVG(path: String, target: ImageView, cropThumbnails: Boolean) {
    target.scaleType = if (cropThumbnails) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER

    val options = RequestOptions().signature(path.getFileSignature())
    Glide.with(applicationContext)
        .`as`(PictureDrawable::class.java)
        .listener(SvgSoftwareLayerSetter())
        .load(path)
        .apply(options)
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(target)
}

/**
 * Get filtered directories
 * @param callback Function that will be ran after getting directories
 */
fun Context.getCachedDirectories(callback: (ArrayList<Directory>) -> Unit) {
    ensureBackgroundThread {
        Log.d("---Context.kt---", "getCachedDirectories()")
        //Get all directories at first
        val directories = try {
            directoryDao.getAll() as ArrayList<Directory>
        } catch (e: Exception) {
            ArrayList<Directory>()
        }

        //Finally send the filtered directories as parameter of the callback
        val clone = directories.clone() as ArrayList<Directory>
        callback(clone.distinctBy { it.path.getDistinctPath() } as ArrayList<Directory>)
        removeInvalidDBDirectories(directories)
    }
}

//Media store에 있는 화상들 다 얻기
suspend fun Context.getImagesFromMediaStore(): ArrayList<Medium>{
    val selection = "${Images.Media.SIZE} > 0 AND ${Images.Media.DATE_MODIFIED} > 0"
    val media = ArrayList<Medium>()

    //수정시간, 촬영시간, 화일크기, 화일경로들을 얻는다.
    val projection = arrayOf(
            Images.Media.DATE_MODIFIED,
            Images.Media.DATE_TAKEN,
            Images.Media.SIZE,
            Images.Media.DATA
    )

    val cursor = contentResolver.query(Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "")
    yield()

    if(cursor != null) {
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_MODIFIED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(Images.Media.SIZE)
        val dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA)

        while (cursor.moveToNext()){
            yield()
            //돌림값이 초수로 나오는데 자료기지에는 미리초로 보관한다
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000
            var dateTaken = cursor.getLong(dateTakenColumn)
            val size = cursor.getLong(sizeColumn)
            val fullPath = cursor.getString(dataColumn)

            if(dateTaken <= 0L)
                dateTaken = dateModified

            //File Name, Parent path, 변경된 날자들을 얻어내기
            val fileName = fullPath.getFilenameFromPath()
            val parentPath = fullPath.getParentPath()
            val dateTakenString = Utils.formatDate(dateTaken)

            //Medium객체를 만들어서 추가한다
            val medium = Medium(null, fileName, fullPath, parentPath, dateModified, dateTaken, dateTakenString, size, TYPE_IMAGES, 0, 0)
            media.add(medium)
        }
        cursor.close()
    }

    return media
}

//Media store에 있는 동영상들 다 얻기
suspend fun Context.getVideosFromMediaStore(): ArrayList<Medium>{
    val selection = "${MediaStore.Video.Media.SIZE} > 0 AND ${MediaStore.Video.Media.DATE_MODIFIED} > 0 AND ${MediaStore.Video.Media.DURATION} > 0"
    val media = ArrayList<Medium>()

    //수정시간, 촬영시간, 크기, 화일경로, 동영상길이들을 얻는다.
    val projection = arrayOf(
            MediaStore.Video.Media.DATE_MODIFIED,     //0
            Images.Media.DATE_TAKEN,                  //1
            Images.Media.SIZE,                        //2
            Images.Media.DATA,                        //3
            MediaStore.Video.Media.DURATION           //4
    )

    yield()
    val cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "")
    if(cursor != null) {
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_MODIFIED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(Images.Media.SIZE)
        val dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA)
        val durationColumn = cursor.getColumnIndexOrThrow(Images.Media.DURATION)

        while (cursor.moveToNext()){
            yield()

            //돌림값이 초수로 나오는데 자료기지에는 미리초로 보관한다
            val dateModified = cursor.getLong(dateModifiedColumn) * 1000
            var dateTaken = cursor.getLong(dateTakenColumn)
            val size = cursor.getLong(sizeColumn)
            val fullPath = cursor.getString(dataColumn)
            var duration = (cursor.getInt(durationColumn) / 1000).coerceAtLeast(1)
            if(duration < 1)
                duration = 1

            if(dateTaken <= 0L)
                dateTaken = dateModified

            //File Name, Parent path, 변경된 날자들을 얻어내기
            val fileName = fullPath.getFilenameFromPath()
            val parentPath = fullPath.getParentPath()
            val dateTakenString = Utils.formatDate(dateTaken)

            //Medium객체를 만들어서 추가한다
            val medium = Medium(null, fileName, fullPath, parentPath, dateModified, dateTaken, dateTakenString, size, TYPE_VIDEOS, duration, 0)
            media.add(medium)
        }
        cursor.close()
    }

    return media
}

fun Context.getImage(path: String): Medium? {
    //수정시간, 촬영시간, 화일크기, 화일경로들을 얻는다.
    val projection = arrayOf(
            Images.Media.DATE_MODIFIED,
            Images.Media.DATE_TAKEN,
            Images.Media.SIZE,
            Images.Media.DATA
    )
    val selection = "${MediaStore.Video.Media.SIZE} > 0 AND ${MediaStore.Video.Media.DATE_MODIFIED} > 0 AND ${MediaStore.Video.Media.DATA} = '$path'"
    val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "")
    if(cursor != null && cursor.count > 0) {
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_MODIFIED)
        val dateTakenColumn = cursor.getColumnIndexOrThrow(Images.Media.DATE_TAKEN)
        val sizeColumn = cursor.getColumnIndexOrThrow(Images.Media.SIZE)
        val dataColumn = cursor.getColumnIndexOrThrow(Images.Media.DATA)

        cursor.moveToFirst()

        //돌림값이 초수로 나오는데 자료기지에는 미리초로 보관한다
        val dateModified = cursor.getLong(dateModifiedColumn) * 1000
        var dateTaken = cursor.getLong(dateTakenColumn)
        val size = cursor.getLong(sizeColumn)
        val fullPath = cursor.getString(dataColumn)

        if(dateTaken <= 0L)
            dateTaken = dateModified

        //File Name, Parent path, 변경된 날자들을 얻어내기
        val fileName = fullPath.getFilenameFromPath()
        val parentPath = fullPath.getParentPath()
        val dateTakenString = Utils.formatDate(dateTaken)

        cursor.close()
        return Medium(null, fileName, fullPath, parentPath, dateModified, dateTaken, dateTakenString, size, TYPE_IMAGES, 0, 0)
    }
    return null
}

//화일경로로부터 uri얻기
fun Context.getImageURI(filePath: String): Uri? {
    val cursor = contentResolver.query(
            Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(Images.Media._ID),
            Images.Media.DATA + "=? ",
            arrayOf(filePath),
            null
    )
    if(cursor != null && cursor.moveToFirst()) {
        val id = cursor.getInt(0)
        return Uri.withAppendedPath(Images.Media.EXTERNAL_CONTENT_URI, id.toString())
    }

    return null
}

fun Context.removeInvalidDBDirectories(dirs: ArrayList<Directory>? = null) {
    val dirsToCheck = dirs ?: directoryDao.getAll()
    val OTGPath = config.OTGPath
    dirsToCheck.filter { !getDoesFilePathExist(it.path, OTGPath) }.forEach {
        try {
            directoryDao.deleteDirPath(it.path)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.updateWidgets() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(ComponentName(applicationContext, MyWidgetProvider::class.java))
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
}

// based on https://github.com/sannies/mp4parser/blob/master/examples/src/main/java/com/google/code/mp4parser/example/PrintStructure.java
fun Context.parseFileChannel(path: String, fc: FileChannel, level: Int, start: Long, end: Long, callback: () -> Unit) {
    val FILE_CHANNEL_CONTAINERS = arrayListOf("moov", "trak", "mdia", "minf", "udta", "stbl")
    try {
        var iteration = 0
        var currEnd = end
        fc.position(start)
        if (currEnd <= 0) {
            currEnd = start + fc.size()
        }

        while (currEnd - fc.position() > 8) {
            // just a check to avoid deadloop at some videos
            if (iteration++ > 50) {
                return
            }

            val begin = fc.position()
            val byteBuffer = ByteBuffer.allocate(8)
            fc.read(byteBuffer)
            byteBuffer.rewind()
            val size = IsoTypeReader.readUInt32(byteBuffer)
            val type = IsoTypeReader.read4cc(byteBuffer)
            val newEnd = begin + size

            if (type == "uuid") {
                val fis = FileInputStream(File(path))
                fis.skip(begin)

                val sb = StringBuilder()
                val buffer = ByteArray(1024)
                while (sb.length < size) {
                    val n = fis.read(buffer)
                    if (n != -1) {
                        sb.append(String(buffer, 0, n))
                    } else {
                        break
                    }
                }

                val xmlString = sb.toString().toLowerCase()
                if (xmlString.contains("gspherical:projectiontype>equirectangular") || xmlString.contains("gspherical:projectiontype=\"equirectangular\"")) {
                    callback.invoke()
                }
                return
            }

            if (FILE_CHANNEL_CONTAINERS.contains(type)) {
                parseFileChannel(path, fc, level + 1, begin + 8, newEnd, callback)
            }

            fc.position(newEnd)
        }
    } catch (ignored: Exception) {
    }
}
