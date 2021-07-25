package com.kr.gallery.pro.views

import android.content.Context
import android.graphics.Bitmap
import com.kr.gallery.pro.models.HistoryItem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * 편집화상과 비교화상들의 리력를 화일로 관리한다
 */
class HistoryManager(val mContext: Context) {

    // 편집화상을 보관한다는것을 나타냄
    private val SAVE_NORMAL = "save_normal"
    // 비교화상을 보관한다는것을 나타냄
    private val SAVE_COMPARE = "save_crop"

    // 편집화상 목록
    private var historyList = ArrayList<HistoryItem>()
    // 비교화상 목록
    private var compareHistoryList = ArrayList<HistoryItem>()
    // historyList의 매개 화상은 비교화상을 가진다
    // compareHistoryList에서의 index가 추가된다
    private var indexInCropList = ArrayList<Int>()

    private var seeker = -1

    /**
     * 편집화상 보관
     */
    fun addHistory(bmp: Bitmap) {
        // undo를 진행하여 필요없는 화상들 지우기
        if (historyList.size > 0) {
            val itemsToDelete = historyList.subList(seeker + 1, historyList.size)
            deleteHistory(itemsToDelete)
            indexInCropList.subList(seeker + 1, indexInCropList.size).clear()
        }
        seeker++;

        saveFile(SAVE_NORMAL, bmp)
        indexInCropList.add(compareHistoryList.size - 1)

        if (historyList.size == 1) {
            compareHistoryList.add(historyList[0])
            indexInCropList[indexInCropList.size - 1] = compareHistoryList.size - 1
        }
    }

    /**
     * 비교화상 보관
     */
    fun addCompareHistory(bmp: Bitmap) {
        saveFile(SAVE_COMPARE, bmp)
        indexInCropList[indexInCropList.size - 1] = compareHistoryList.size - 1
    }

    /**
     * Undo를 진행하였을때 편집화상과 그에 대응한 비교화상을 되돌린다
     */
    fun undo(): ArrayList<Bitmap>? {
        if (historyList.size == 0) return null
        seeker = max(-1, seeker - 1)

        val bitmap = readFile(historyList[seeker])
        val cropBitmap = readFile(compareHistoryList[indexInCropList[seeker]])

        val list = ArrayList<Bitmap>()
        list.add(bitmap)
        list.add(cropBitmap)

        return list
    }

    /**
     * Redo를 진행하였을때 편집화상과 그에 대응한 비교화상을 되돌린다
     */
    fun redo(): ArrayList<Bitmap>? {
        seeker = min(seeker + 1, historyList.size - 1)

        val bitmap = readFile(historyList[seeker])
        val cropBitmap = readFile(compareHistoryList[indexInCropList[seeker]])

        val list = ArrayList<Bitmap>()
        list.add(bitmap)
        list.add(cropBitmap)

        return list
    }

    /**
     * Undo를 할수있는가를 되돌린다
     */
    fun canUndo(): Boolean {
        return seeker > 0
    }

    /**
     * Redo를 할수있는가를 되돌린다
     */
    fun canRedo(): Boolean {
        return historyList.size > 1 && seeker < historyList.size - 1
    }

    /**
     * 초기 원본화상의 Bitmap을 되돌린다
     */
    fun getNativeImage(): Bitmap {
        return readFile(historyList[0])
    }

    /**
     * 보관된 편집화상과 비교화상 화일들을 모두 삭제한다
     */
    fun clearHistory() {
        deleteHistory(historyList)
        indexInCropList.subList(0, indexInCropList.size).clear()
        deleteHistory(compareHistoryList)
    }

    /**
     * 보관된 원본화상을 제외하고 모두 삭제한다
     */
    fun resetHistory() {
        var itemsToDelete = historyList.subList(1, historyList.size)
        deleteHistory(itemsToDelete)
        indexInCropList.subList(1, indexInCropList.size).clear()
        itemsToDelete = compareHistoryList.subList(1, compareHistoryList.size)
        deleteHistory(itemsToDelete)

        seeker = 0
    }

    /**
     * 화상보관화일 지우기를 진행한다
     */
    private fun deleteHistory(itemsToDelete: MutableList<HistoryItem>) {
        for (item in itemsToDelete) {
            val filename = item.fileName
            val path: File? = mContext.getExternalFilesDir(null)
            val file = File(path, filename)
            file.delete()
        }
        itemsToDelete.clear()
    }

    /**
     * 화일쓰기부분 <br/>
     * 화상을 압축하지 않고 화일로 보관한다
     * @param type : 편집화상인가 비교화상인가 나타낸다
     * @param bmp : 보관하려는 화상의 Bitmap
     */
    private fun saveFile(type: String, bmp: Bitmap) {
        val fileName = if(type == SAVE_NORMAL) "history_${historyList.size}" else "crop_${compareHistoryList.size}"

        // save bitmap
        val bytesCount = bmp.byteCount
        val buffer = ByteBuffer.allocate(bytesCount)
        bmp.copyPixelsToBuffer(buffer)
        val bufferAry = buffer.array()

        val path: File? = mContext.getExternalFilesDir(null)
        val file = File(path, fileName)
        try {
            val fos = FileOutputStream(file)
            fos.write(bufferAry)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            val historyItem = HistoryItem(bmp.width, bmp.height, fileName)
            if (type == SAVE_NORMAL) historyList.add(historyItem)
            else compareHistoryList.add(historyItem)
        }
    }

    /**
     * 화상보관화일을 읽어 Bitmap을 돌려준다
     */
    private fun readFile(historyItem: HistoryItem): Bitmap {

        val width = historyItem.width
        val height = historyItem.height
        val fileName = historyItem.fileName

        val path: File? = mContext.getExternalFilesDir(null)
        val file = File(path, fileName)
        val length = file.length().toInt()
        val bytes = ByteArray(length)
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            fis.read(bytes)
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val result_bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.wrap(bytes)
        result_bmp.copyPixelsFromBuffer(buffer)

        return result_bmp
    }
}
