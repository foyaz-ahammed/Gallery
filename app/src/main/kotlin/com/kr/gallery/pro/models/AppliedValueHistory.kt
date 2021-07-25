package com.kr.gallery.pro.models

import android.graphics.Rect
import doodle.IDoodleItem

class AppliedValueHistory {
    // history 종류를 나타낸다
    // ACTION_CROP, ACTION_FILTER, ACTION_ADJUST, ACTION_DOODLE, ACTION_MOSAIC, ACTION_DRAW
    var historyType = ""

    // 적용된 filter를 표현하는 문자렬. (e.g. "@adjust exposure 0.98 ")
    var filterConfigStr = "";
    // 적용된 adjust를 표현하는 문자렬. (e.g. "@adjust exposure 0.98 ")
    var adjustConfigStr = "";

    // 자르기에 리용된 화상각도
    var cropAngle = 0f;
    // 자르기에 리용된 X축 비례
    var cropScaleX = 0f;
    // 자르기에 리용된 Y축 비례
    var cropScaleY = 0f;
    // 자르기구역이다
    lateinit var cropRectInWrapper : Rect

    // doodle/mosaic/draw에서 그려진 DoodleItem 목록들
    lateinit var doodleItems : ArrayList<IDoodleItem>
    lateinit var mosaicItems : ArrayList<IDoodleItem>
    lateinit var drawItems : ArrayList<IDoodleItem>
}
