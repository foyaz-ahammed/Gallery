package com.kr.gallery.pro.extensions

import android.annotation.TargetApi
import android.app.Activity
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kr.commons.activities.BaseSimpleActivity
import com.kr.commons.extensions.*
import com.kr.commons.helpers.CONFLICT_KEEP_BOTH
import com.kr.commons.helpers.CONFLICT_SKIP
import com.kr.commons.helpers.ensureBackgroundThread
import com.kr.commons.helpers.getConflictResolution
import com.kr.commons.interfaces.CopyMoveListener
import com.kr.commons.models.FileDirItem
import com.kr.gallery.pro.BuildConfig
import com.kr.gallery.pro.R
import com.kr.gallery.pro.fragments.PickDirectoryDialogFragment
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

fun Activity.sharePath(path: String, target: Int) {
    sharePathIntent(path, BuildConfig.APPLICATION_ID, target)
}

fun Activity.sharePaths(paths: ArrayList<String>, target: Int) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID, target)
}

fun Activity.shareMediumPath(path: String, target: Int) {
    sharePath(path, target)
}

fun Activity.shareMediaPaths(paths: ArrayList<String>, target: Int) {
    sharePaths(paths, target)
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}

@RequiresApi(Build.VERSION_CODES.O)
fun AppCompatActivity.showSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        supportActionBar?.show()
    }

    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
}

fun AppCompatActivity.hideSystemUI(toggleActionBarVisibility: Boolean) {
    if (toggleActionBarVisibility) {
        supportActionBar?.hide()
    }

    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE
}

@RequiresApi(Build.VERSION_CODES.O)
fun AppCompatActivity.updateStatusbarColor() {
    val nightModeFlags: Int = applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    val view = window.decorView
//    if(nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
        view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        window.statusBarColor = Color.BLACK
//    } else {
//        view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//        window.statusBarColor = Color.WHITE
//    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun AppCompatActivity.updateNavigationBarColor() {
    val nightModeFlags: Int = applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    val view = window.decorView
//    if(nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {

        view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.navigationBarColor = Color.BLACK
//    } else {

//        view.systemUiVisibility = view.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
//        window.navigationBarColor = Color.WHITE
//    }
}

fun BaseSimpleActivity.moveFilesTo(fileDirItems: ArrayList<FileDirItem>, callback: (success: Boolean, failedCount: Int) -> Unit) {

    val source = fileDirItems[0].getParentPath()

    //이동할 화일들과 목표등록부경로
    val paths = fileDirItems.map { it.path } as ArrayList<String>

    val pickDirectoryDialogFragment: DialogFragment = PickDirectoryDialogFragment.newInstance(source) { it ->
        val destination = it
        var fileCountToCopy = fileDirItems.size
        paths.add(destination)

        //OTG 혹은 SD Card 로/로부터 화일 이동
        if (isPathOnOTG(paths) || isPathOnSD(paths)) {
            handleSAFDialog(paths){
                //Progress dialog를 만들어주기
                val progressDialog = makeProgressDialog(fileDirItems.size, getString(R.string.moving))

                //이동동작 감지
                val copyMoveListener = object : CopyMoveListener{
                    override fun onSucceeded(copyOnly: Boolean, failedCount: Int) {
                        progressDialog.dismiss()
                        callback(true, failedCount)
                    }

                    override fun onFailed() {
                        progressDialog.dismiss()
                        callback(false, 0)
                    }

                    override fun onProgress(progress: Int) {
                        progressDialog.incrementProgressBy(1)
                    }
                }

                ensureBackgroundThread {
                    startCopyMove(fileDirItems, destination, false, copyPhotoVideoOnly = true, listener = copyMoveListener) {
                        progressDialog.show()
                    }
                }
            }
        }

        //일반등록부들에서 화일 이동
        else {
            checkConflicts(fileDirItems, destination, 0, LinkedHashMap()) {
                //Progress dialog를 만들어주기
                val progressDialog = makeProgressDialog(fileDirItems.size, getString(R.string.moving))
                progressDialog.show()

                //Background thread를 통해 화일 옮기기를 진행한다
                ensureBackgroundThread {
                    val destinationFolder = File(destination)
                    for (oldFileDirItem in fileDirItems) {
                        var newFile = File(destinationFolder, oldFileDirItem.name)
                        if (newFile.exists()) {
                            when {
                                getConflictResolution(it, newFile.absolutePath) == CONFLICT_SKIP -> fileCountToCopy--
                                getConflictResolution(it, newFile.absolutePath) == CONFLICT_KEEP_BOTH -> newFile = getAlternativeFile(newFile)
                                else -> newFile.delete()
                            }
                        }

                        if (!newFile.exists() && File(oldFileDirItem.path).renameTo(newFile)) {
                            if (!baseConfig.keepLastModified) {
                                newFile.setLastModified(System.currentTimeMillis())
                            }

                            //Media store에서 본래 화일을 삭제하고 새로운 화일을 추가한다
                            deleteFromMediaStore(oldFileDirItem.path)
                            MediaScannerConnection.scanFile(applicationContext, arrayOf(newFile.toString()),
                                    arrayOf(newFile.name)) { path, uri -> }
                        }

                        //화일 하나를 옮길때마다 progress를 갱신한다
                        progressDialog.incrementProgressBy(1)
                        if(progressDialog.progress == progressDialog.max) {
                            progressDialog.dismiss()
                        }
                    }

                    runOnUiThread { callback(true, 0) }
                }
            }
        }
    }

    //등록부선택대화창 띄워주기
    pickDirectoryDialogFragment.show(supportFragmentManager, "PickDirectoryDialogFragment")
}

fun BaseSimpleActivity.tryDeleteFileDirItem(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, deleteFromDatabase: Boolean,
                                            callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    deleteFile(fileDirItem, allowDeleteFolder) {
        if (deleteFromDatabase) {
            ensureBackgroundThread {
                runOnUiThread {
                    callback?.invoke(it)
                }
            }
        } else {
            callback?.invoke(it)
        }
    }
}

fun Activity.hasNavBar(): Boolean {
    val display = windowManager.defaultDisplay

    val realDisplayMetrics = DisplayMetrics()
    display.getRealMetrics(realDisplayMetrics)

    val displayMetrics = DisplayMetrics()
    display.getMetrics(displayMetrics)

    return (realDisplayMetrics.widthPixels - displayMetrics.widthPixels > 0) || (realDisplayMetrics.heightPixels - displayMetrics.heightPixels > 0)
}

fun BaseSimpleActivity.saveRotatedImageToFile(oldPath: String, newPath: String, degrees: Int, showToasts: Boolean, callback: () -> Unit) {
    var newDegrees = degrees
    if (newDegrees < 0) {
        newDegrees += 360
    }

    if (oldPath == newPath && oldPath.isJpg()) {
        if (tryRotateByExif(oldPath, newDegrees, showToasts, callback)) {
            return
        }
    }

    val tmpPath = "$recycleBinPath/.tmp_${newPath.getFilenameFromPath()}"
    val tmpFileDirItem = FileDirItem(tmpPath, tmpPath.getFilenameFromPath())
    try {
        getFileOutputStream(tmpFileDirItem) {
            if (it == null) {
                if (showToasts) {
                    toast(R.string.unknown_error_occurred)
                }
                return@getFileOutputStream
            }

            val oldLastModified = File(oldPath).lastModified()
            if (oldPath.isJpg()) {
                copyFile(oldPath, tmpPath)
                saveExifRotation(ExifInterface(tmpPath), newDegrees)
            } else {
                val inputstream = getFileInputStreamSync(oldPath)
                val bitmap = BitmapFactory.decodeStream(inputstream)
                saveFile(tmpPath, bitmap, it as FileOutputStream, newDegrees)
            }

            copyFile(tmpPath, newPath)
            rescanPaths(arrayListOf(newPath))
            fileRotatedSuccessfully(newPath, oldLastModified)

            it.flush()
            it.close()
            callback.invoke()
        }
    } catch (e: OutOfMemoryError) {
        if (showToasts) {
            toast(R.string.out_of_memory_error)
        }
    } catch (e: Exception) {
        if (showToasts) {
            showErrorToast(e)
        }
    } finally {
        tryDeleteFileDirItem(tmpFileDirItem, false, true)
    }
}

@TargetApi(Build.VERSION_CODES.N)
fun Activity.tryRotateByExif(path: String, degrees: Int, showToasts: Boolean, callback: () -> Unit): Boolean {
    return try {
        val file = File(path)
        val oldLastModified = file.lastModified()
        if (saveImageRotation(path, degrees)) {
            fileRotatedSuccessfully(path, oldLastModified)
            callback.invoke()
            if (showToasts) {
                toast(R.string.file_saved)
            }
            true
        } else {
            false
        }
    } catch (e: Exception) {
        if (showToasts) {
            showErrorToast(e)
        }
        false
    }
}

fun Activity.fileRotatedSuccessfully(path: String, lastModified: Long) {
    if (config.keepLastModified) {
        File(path).setLastModified(lastModified)
        updateLastModified(path, lastModified)
    }

    Picasso.get().invalidate(path.getFileKey())
    // we cannot refresh a specific image in Glide Cache, so just clear it all
    val glide = Glide.get(applicationContext)
    glide.clearDiskCache()
    runOnUiThread {
        glide.clearMemory()
    }
}

fun BaseSimpleActivity.copyFile(source: String, destination: String) {
    var inputStream: InputStream? = null
    var out: OutputStream? = null
    try {
        out = getFileOutputStreamSync(destination, source.getMimeType())
        inputStream = getFileInputStreamSync(source)
        inputStream!!.copyTo(out!!)
    } catch (e: Exception) {
        showErrorToast(e)
    } finally {
        inputStream?.close()
        out?.close()
    }
}

fun saveFile(path: String, bitmap: Bitmap, out: FileOutputStream, degrees: Int) {
    val matrix = Matrix()
    matrix.postRotate(degrees.toFloat())
    val bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bmp.compress(path.getCompressionFormat(), 90, out)
}

fun runAfterDelay(callback: () -> Unit) {
    Handler().postDelayed(callback, 150)
}
