package com.kr.commons.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.telecom.TelecomManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.util.Pair
import com.kr.commons.R
import com.kr.commons.asynctasks.CopyMoveTask
import com.kr.commons.dialogs.FileConflictDialog
import com.kr.commons.dialogs.WritePermissionDialog
import com.kr.commons.extensions.*
import com.kr.commons.helpers.*
import com.kr.commons.interfaces.CopyMoveListener
import com.kr.commons.models.FileDirItem
import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.regex.Pattern

abstract class BaseSimpleActivity : AppCompatActivity() {
    var actionOnPermission: ((granted: Boolean) -> Unit)? = null
    var isAskingPermissions = false
    var useDynamicTheme = true
    var checkedDocumentPath = ""
    var configItemsToExport = LinkedHashMap<String, Any>()

    private val GENERIC_PERM_HANDLER = 100

    companion object {
        var funAfterSAFPermission: ((success: Boolean) -> Unit)? = null
    }

    open fun onSwipeDown() {
    }

    abstract fun getAppIconIDs(): ArrayList<Int>

    abstract fun getAppLauncherName(): String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        if (useDynamicTheme) {
            setTheme(getThemeId())
        }

        super.onCreate(savedInstanceState)

        updateStatusbarColor(Color.BLACK)
        updateNavigationBarColor()
    }

    override fun onResume() {
        super.onResume()
        if (useDynamicTheme) {
            setTheme(getThemeId())
            updateBackgroundColor()
        }
        updateActionbarColor()
        updateRecentsAppIcon()
        updateNavigationBarColor()
    }

    override fun onStop() {
        super.onStop()
        actionOnPermission = null
    }

    override fun onDestroy() {
        super.onDestroy()
        funAfterSAFPermission = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun updateBackgroundColor(color: Int = baseConfig.backgroundColor) {
        window.decorView.setBackgroundColor(color)
    }

    fun updateActionbarColor(color: Int = Color.BLACK) {
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
//        updateActionBarTitle(supportActionBar?.title.toString(), color)
        updateStatusbarColor(color)
        setTaskDescription(ActivityManager.TaskDescription(null, null, color))
    }

    fun updateStatusbarColor(color: Int) {
        val view = window.decorView
        view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        window.statusBarColor = color
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateNavigationBarColor(color: Int = Color.BLACK) {
        val view = window.decorView
        view.systemUiVisibility = view.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        window.navigationBarColor = color
    }

    fun updateRecentsAppIcon() {
        if (baseConfig.isUsingModifiedAppIcon) {
            val appIconIDs = getAppIconIDs()
            val currentAppIconColorIndex = getCurrentAppIconColorIndex()
            if (appIconIDs.size - 1 < currentAppIconColorIndex) {
                return
            }

            val recentsIcon = BitmapFactory.decodeResource(resources, appIconIDs[currentAppIconColorIndex])
            val title = getAppLauncherName()
            val color = baseConfig.primaryColor

            val description = ActivityManager.TaskDescription(title, recentsIcon, color)
            setTaskDescription(description)
        }
    }

    fun updateMenuItemColors(menu: Menu?, useCrossAsBack: Boolean = false, baseColor: Int = baseConfig.primaryColor) {
        if (menu == null) {
            return
        }

        val color = baseColor.getContrastColor()
        for (i in 0 until menu.size()) {
            try {
                menu.getItem(i)?.icon?.setTint(color)
            } catch (ignored: Exception) {
            }
        }

        val drawableId = if (useCrossAsBack) R.drawable.ic_cross_vector else R.drawable.ic_arrow_left_vector
        val icon = resources.getColoredDrawableWithColor(drawableId, color)
        supportActionBar?.setHomeAsUpIndicator(icon)
    }

    private fun getCurrentAppIconColorIndex(): Int {
        val appIconColor = baseConfig.appIconColor
        getAppIconColors().forEachIndexed { index, color ->
            if (color == appIconColor) {
                return index
            }
        }
        return 0
    }

    fun setTranslucentNavigation() {
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        val partition = try {
            checkedDocumentPath.substring(9, 18)
        } catch (e: Exception) {
            ""
        }
        val sdOtgPattern = Pattern.compile(SD_OTG_SHORT)

        if (requestCode == OPEN_DOCUMENT_TREE) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition = partition.isEmpty() || !sdOtgPattern.matcher(partition).matches() || (sdOtgPattern.matcher(partition).matches() && resultData.dataString!!.contains(partition))
                if (isProperSDFolder(resultData.data!!) && isProperPartition) {
                    if (resultData.dataString == baseConfig.OTGTreeUri) {
                        toast(R.string.sd_card_usb_same)
                        return
                    }

                    saveTreeUri(resultData)
                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_OTG) {
            if (resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
                val isProperPartition = partition.isEmpty() || !sdOtgPattern.matcher(partition).matches() || (sdOtgPattern.matcher(partition).matches() && resultData.dataString!!.contains(partition))
                if (isProperOTGFolder(resultData.data!!) && isProperPartition) {
                    if (resultData.dataString == baseConfig.treeUri) {
                        funAfterSAFPermission?.invoke(false)
                        toast(R.string.sd_card_usb_same)
                        return
                    }
                    baseConfig.OTGTreeUri = resultData.dataString!!
                    baseConfig.OTGPartition = baseConfig.OTGTreeUri.removeSuffix("%3A").substringAfterLast('/').trimEnd('/')
                    updateOTGPathFromPartition()

                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    applicationContext.contentResolver.takePersistableUriPermission(resultData.data!!, takeFlags)

                    funAfterSAFPermission?.invoke(true)
                    funAfterSAFPermission = null
                } else {
                    toast(R.string.wrong_root_selected_usb)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, requestCode)
                }
            } else {
                funAfterSAFPermission?.invoke(false)
            }
        } else if (requestCode == SELECT_EXPORT_SETTINGS_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportSettingsTo(outputStream, configItemsToExport)
        }
    }

    private fun saveTreeUri(resultData: Intent) {
        val treeUri = resultData.data
        baseConfig.treeUri = treeUri.toString()

        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        applicationContext.contentResolver.takePersistableUriPermission(treeUri!!, takeFlags)
    }

    private fun isProperSDFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isProperOTGFolder(uri: Uri) = isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)

    private fun isRootUri(uri: Uri) = DocumentsContract.getTreeDocumentId(uri).endsWith(":")

    private fun isInternalStorage(uri: Uri) = isExternalStorageDocument(uri) && DocumentsContract.getTreeDocumentId(uri).contains("primary")

    private fun isExternalStorageDocument(uri: Uri) = "com.android.externalstorage.documents" == uri.authority

    // synchronous return value determines only if we are showing the SAF dialog, callback result tells if the SD or OTG permission has been granted
    fun handleSAFDialog(path: String, callback: (success: Boolean) -> Unit): Boolean {
        return if (!packageName.startsWith("com.kr")) {
            callback(true)
            false
        } else if (isShowingSAFDialog(path) || isShowingOTGDialog(path)) {
            funAfterSAFPermission = callback
            true
        } else {
            callback(true)
            false
        }
    }

    fun handleOTGPermission(callback: (success: Boolean) -> Unit) {
        if (baseConfig.OTGTreeUri.isNotEmpty()) {
            callback(true)
            return
        }

        funAfterSAFPermission = callback
        WritePermissionDialog(this, true) {
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                if (resolveActivity(packageManager) == null) {
                    type = "*/*"
                }

                if (resolveActivity(packageManager) != null) {
                    startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                } else {
                    toast(R.string.unknown_error_occurred)
                }
            }
        }
    }

    /**
     * SdCard접근이 요구되는가를 검사하고 SdCard에 대한 접근허가 대화창을 띄워준다.
     * 접근요청이 접수/거절 되면 해당 callback을 실행한다.
     * @param pathList 화일/등록부경로들
     * @param callback 실행하려는 동작
     */
    fun handleSAFDialog(pathList: List<String>, callback: (success: Boolean) -> Unit): Boolean {
        return if(needsSdcardWritePermission(pathList)) {
            if(!isDestroyed && !isFinishing) {
                runOnUiThread {
                    //접근요청대화창 띄우기
                    WritePermissionDialog(this, false) {
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra("android.content.extra.SHOW_ADVANCED", true)
                            if (resolveActivity(packageManager) == null) {
                                type = "*/*"
                            }

                            //Document Tree Activity를 띄운다.
                            if (resolveActivity(packageManager) != null) {
                                startActivityForResult(this, OPEN_DOCUMENT_TREE)
                            } else {
                                toast(R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            }
            funAfterSAFPermission = callback
            true
        } else {
            callback(true)
            false
        }
    }

    fun getAlternativeFile(file: File): File {
        var fileIndex = 1
        var newFile: File?
        do {
            val newName = String.format("%s(%d).%s", file.nameWithoutExtension, fileIndex, file.extension)
            newFile = File(file.parent, newName)
            fileIndex++
        } while (getDoesFilePathExist(newFile!!.absolutePath))
        return newFile
    }

    /**
     * 화일/등록부 복사/이동
     * @param files 복사/이동하려는 화일/등록부목록
     * @param destinationPath 목표등록부경로
     * @param isCopyOperation true: 복사, false: 이동
     * @param copyPhotoVideoOnly 화상,동영상만 복사/이동하기
     * @param listener 복사/이동진행정형을 아려주기 위한 listener
     * @param callback 복사/이동이 완료된다음 실행하려는 동작
     */
    fun startCopyMove(files: ArrayList<FileDirItem>, destinationPath: String, isCopyOperation: Boolean, copyPhotoVideoOnly: Boolean, listener: CopyMoveListener, callback: () -> Unit) {
        val availableSpace = destinationPath.getAvailableStorageB()
        val sumToCopy = files.sumByLong { it.getProperSize(applicationContext) }
        if (availableSpace == -1L || sumToCopy < availableSpace) {
            checkConflicts(files, destinationPath, 0, LinkedHashMap()) {
                runOnUiThread {
                    callback.invoke()

                    val pair = Pair(files, destinationPath)
                    CopyMoveTask(this, isCopyOperation, copyPhotoVideoOnly, it, listener).execute(pair)
                }
            }
        } else {
            val text = String.format(getString(R.string.no_space), sumToCopy.formatSize(), availableSpace.formatSize())
            toast(text, Toast.LENGTH_LONG)
        }
    }

    /**
     * 화일들을 복사/이동할때 같은 화일이름이 있는가를 검사한다.
     * @param files: 이동하려는 화일/등록부목록
     * @param destinationPath: 목표 등록부경로
     * @param conflictResolutions: 결과(덮어쓰기|둘다 존재|띄여넘기)
     * @param callback: 검사후 진행할 동작
     */
    fun checkConflicts(files: ArrayList<FileDirItem>, destinationPath: String, index: Int, conflictResolutions: LinkedHashMap<String, Int>,
                       callback: (resolutions: LinkedHashMap<String, Int>) -> Unit) {
        if (index == files.size) {
            callback(conflictResolutions)
            return
        }

        val file = files[index]
        val newFileDirItem = FileDirItem("$destinationPath/${file.name}", file.name, file.isDirectory)
        if (getDoesFilePathExist(newFileDirItem.path)) {
            runOnUiThread {
                FileConflictDialog(this, newFileDirItem, files.size > 1) { resolution, applyForAll ->
                    if (applyForAll) {
                        conflictResolutions.clear()
                        conflictResolutions[""] = resolution
                        checkConflicts(files, destinationPath, files.size, conflictResolutions, callback)
                    } else {
                        conflictResolutions[newFileDirItem.path] = resolution
                        checkConflicts(files, destinationPath, index + 1, conflictResolutions, callback)
                    }
                }
            }
        } else {
            checkConflicts(files, destinationPath, index + 1, conflictResolutions, callback)
        }
    }

    fun handlePermission(permissionId: Int, callback: (granted: Boolean) -> Unit) {
        actionOnPermission = null
        if (hasPermission(permissionId)) {
            callback(true)
        } else {
            isAskingPermissions = true
            actionOnPermission = callback
            ActivityCompat.requestPermissions(this, arrayOf(getPermissionString(permissionId)), GENERIC_PERM_HANDLER)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        isAskingPermissions = false
        if (requestCode == GENERIC_PERM_HANDLER && grantResults.isNotEmpty()) {
            actionOnPermission?.invoke(grantResults[0] == 0)
        }
    }

    private fun exportSettingsTo(outputStream: OutputStream?, configItems: LinkedHashMap<String, Any>) {
        if (outputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ensureBackgroundThread {
            outputStream.bufferedWriter().use { out ->
                for ((key, value) in configItems) {
                    out.writeLn("$key=$value")
                }
            }

            toast(R.string.settings_exported_successfully)
        }
    }

    open fun showBottomSheet() {
    }

    open fun hideBottomSheet() {
    }

    open fun showBottomShareBar(show: Boolean) {

    }

    open fun updateSelectedCounts(count: Int, allSelected: Boolean) {
    }

    @SuppressLint("InlinedApi")
    protected fun launchSetDefaultDialerIntent() {
        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_DIALER) && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            }
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName).apply {
                if (resolveActivity(packageManager) != null) {
                    startActivityForResult(this, REQUEST_CODE_SET_DEFAULT_DIALER)
                } else {
                    toast(R.string.no_app_found)
                }
            }
        }
    }
}
