package com.kr.gallery.pro.adapters

import com.kr.gallery.pro.models.Directory

//모두보기화면의 recyclerview에서 viewtype을 2가지로 정의해주었다.
const val DIRECTORY_HEADER = 0
const val DIRECTORY_CONTENT = 1

/**
 * @see com.kr.gallery.pro.fragments.AllFragment
 * @see com.kr.gallery.pro.adapters.DirectoryListAdapter
 *
 * @param itemType: [DIRECTORY_HEADER], [DIRECTORY_CONTENT]
 * @param directoryName: 등록부이름(String resource)
 * @param count: Camera, Movies등록부이면 등록부의 media개수, 일반등록부라면 일반등록부의 개수로 된다
 * @param directory: itemType가 [DIRECTORY_CONTENT]일때 등록부정보, 아니면 null
 */
class DirectoryItem(var itemType: Int = 0, var directoryName: Int = 0, var count: Int = 0, var directory: Directory?)
