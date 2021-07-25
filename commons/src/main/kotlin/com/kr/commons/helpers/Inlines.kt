package com.kr.commons.helpers

import com.kr.commons.R

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long) = this.map { selector(it) }.sum()

inline fun <T> Iterable<T>.sumByInt(selector: (T) -> Int) = this.map { selector(it) }.sum()

val VIEW_HOLDER_TAG = R.string.deleting_folder
