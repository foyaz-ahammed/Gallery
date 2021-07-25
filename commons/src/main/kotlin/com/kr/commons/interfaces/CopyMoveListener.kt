package com.kr.commons.interfaces

/**
 * 복사/이동 동작 listener
 */
interface CopyMoveListener {
    /**
     * 동작완료
     * @param copyOnly 복사|이동?
     * @param failedCount 실패한 화일개수
     */
    fun onSucceeded(copyOnly: Boolean, failedCount: Int)

    /**
     * 동작실패
     */
    fun onFailed()

    /**
     * 1개 화일 동작이 완료
     * @param progress 완료된 화일개수
     */
    fun onProgress(progress: Int)
}
