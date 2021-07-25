package com.kr.gallery.pro.models

import android.graphics.Point
import com.kr.gallery.pro.R
import java.util.*

public final class FilterPack {
    /***
     * Pair목록을 만들어 되돌린다,
     * 매개 Pair는 이름 resID와 filter설정문자렬을 가진다
     */
    companion object {
        fun getFilterPack(): List<Pair<Int, String>>? {
            val filters: MutableList<Pair<Int, String>> = ArrayList()
            filters.add(getAweStruckVibeFilter())
            filters.add(getClarendon())
            filters.add(getOldManFilter())
            filters.add(getMarsFilter())
            filters.add(getRiseFilter())
            filters.add(getAprilFilter())
            filters.add(getAmazonFilter())
            filters.add(getStarLitFilter())
            filters.add(getNightWhisperFilter())
            filters.add(getLimeStutterFilter())
            filters.add(getHaanFilter())
            filters.add(getBlueMessFilter())
            filters.add(getAdeleFilter())
            filters.add(getCruzFilter())
            filters.add(getMetropolis())
            filters.add(getAudreyFilter())
            filters.add(getNegative())
            return filters
        }

        private fun getStarLitFilter(): Pair<Int, String> {
            val rgbKnots: Array<Point?> = arrayOfNulls(8)
            rgbKnots[0] = Point(0, 0)
            rgbKnots[1] = Point(34, 6)
            rgbKnots[2] = Point(69, 23)
            rgbKnots[3] = Point(100, 58)
            rgbKnots[4] = Point(150, 154)
            rgbKnots[5] = Point(176, 196)
            rgbKnots[6] = Point(207, 233)
            rgbKnots[7] = Point(255, 255)

            val configStr = "@curve " + getCurveString("RGB", rgbKnots)
            return Pair(R.string.starlit, configStr)
        }

        private fun getBlueMessFilter(): Pair<Int, String> {
            val redKnots: Array<Point?> = arrayOfNulls(8)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(86, 34)
            redKnots[2] = Point(117, 41)
            redKnots[3] = Point(146, 80)
            redKnots[4] = Point(170, 151)
            redKnots[5] = Point(200, 214)
            redKnots[6] = Point(225, 242)
            redKnots[7] = Point(255, 255)

            var configStr = "@curve " + getCurveString("R", redKnots)
            configStr += getBrightnessString(0.3f)
            return Pair(R.string.bluemess, configStr)
        }

        private fun getAweStruckVibeFilter(): Pair<Int, String> {
            val rgbKnots: Array<Point?> = arrayOfNulls(5)
            rgbKnots[0] = Point(0, 0)
            rgbKnots[1] = Point(80, 43)
            rgbKnots[2] = Point(149, 102)
            rgbKnots[3] = Point(201, 173)
            rgbKnots[4] = Point(255, 255)
            val redKnots: Array<Point?> = arrayOfNulls(5)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(125, 147)
            redKnots[2] = Point(177, 199)
            redKnots[3] = Point(213, 228)
            redKnots[4] = Point(255, 255)
            val greenKnots: Array<Point?> = arrayOfNulls(6)
            greenKnots[0] = Point(0, 0)
            greenKnots[1] = Point(57, 76)
            greenKnots[2] = Point(103, 130)
            greenKnots[3] = Point(167, 192)
            greenKnots[4] = Point(211, 229)
            greenKnots[5] = Point(255, 255)
            val blueKnots: Array<Point?> = arrayOfNulls(7)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(38, 62)
            blueKnots[2] = Point(75, 112)
            blueKnots[3] = Point(116, 158)
            blueKnots[4] = Point(171, 204)
            blueKnots[5] = Point(212, 233)
            blueKnots[6] = Point(255, 255)

            var configStr = "@curve " + getCurveString("RGB", rgbKnots)
            configStr += getCurveString("R", redKnots)
            configStr += getCurveString("G", greenKnots)
            configStr += getCurveString("B", blueKnots)

            return Pair(R.string.struck, configStr)
        }

        private fun getLimeStutterFilter(): Pair<Int, String> {
            val blueKnots: Array<Point?> = arrayOfNulls(3)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(165, 114)
            blueKnots[2] = Point(255, 255)

            val configStr = "@curve " + getCurveString("B", blueKnots)

            return Pair(R.string.lime, configStr)
        }

        private fun getNightWhisperFilter(): Pair<Int, String> {
            val rgbKnots: Array<Point?> = arrayOfNulls(3)
            rgbKnots[0] = Point(0, 0)
            rgbKnots[1] = Point(174, 109)
            rgbKnots[2] = Point(255, 255)
            val redKnots: Array<Point?> = arrayOfNulls(4)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(70, 114)
            redKnots[2] = Point(157, 145)
            redKnots[3] = Point(255, 255)
            val greenKnots: Array<Point?> = arrayOfNulls(3)
            greenKnots[0] = Point(0, 0)
            greenKnots[1] = Point(109, 138)
            greenKnots[2] = Point(255, 255)
            val blueKnots: Array<Point?> = arrayOfNulls(3)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(113, 152)
            blueKnots[2] = Point(255, 255)

            var configStr = "@curve " + getCurveString("RGB", rgbKnots)
            configStr += getCurveString("R", redKnots)
            configStr += getCurveString("G", greenKnots)
            configStr += getCurveString("B", blueKnots)

            configStr += getContrastString(1.5f)

            return Pair(R.string.whisper, configStr)
        }

        private fun getAmazonFilter(): Pair<Int, String> {
            val blueKnots: Array<Point?> = arrayOfNulls(6)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(11, 40)
            blueKnots[2] = Point(36, 99)
            blueKnots[3] = Point(86, 151)
            blueKnots[4] = Point(167, 209)
            blueKnots[5] = Point(255, 255)

            var configStr = "@curve " + getCurveString("B", blueKnots)
            configStr += getContrastString(1.15f)
            return Pair(R.string.amazon, configStr)
        }

        private fun getAdeleFilter(): Pair<Int, String> {
            val configStr = getSaturationString(0f)

            return Pair(R.string.adele, configStr)
        }

        private fun getCruzFilter(): Pair<Int, String> {
            var configStr = getContrastString(1.25f)
            configStr += getSaturationString(0f)
            configStr += getBrightnessString(0.18f)

            return Pair(R.string.cruz, configStr)
        }

        private fun getMetropolis(): Pair<Int, String> {
            var configStr = getContrastString(1.7f)
            configStr += getSaturationString(0f)
            configStr += getBrightnessString(0.6f)

            return Pair(R.string.metropolis, configStr)
        }

        private fun getAudreyFilter(): Pair<Int, String> {
            val redKnots: Array<Point?> = arrayOfNulls(3)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(124, 138)
            redKnots[2] = Point(255, 255)

            var configStr = "@curve " + getCurveString("R", redKnots)
            configStr += getSaturationString(0f)
            configStr += getBrightnessString(0.18f)
            configStr += getContrastString(1.25f)

            return Pair(R.string.audrey, configStr)
        }

        private fun getRiseFilter(): Pair<Int, String> {
            val blueKnots: Array<Point?> = arrayOfNulls(4)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(39, 70)
            blueKnots[2] = Point(150, 200)
            blueKnots[3] = Point(255, 255)
            val redKnots: Array<Point?> = arrayOfNulls(4)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(45, 64)
            redKnots[2] = Point(170, 190)
            redKnots[3] = Point(255, 255)

            var configStr = "@curve " + getCurveString("R", redKnots)
            configStr += getCurveString("B", blueKnots)
            configStr += getBrightnessString(0.3f)
            configStr += getContrastString(1.65f)
            configStr += getVignetteString(0.35f, 0.6f)

            return Pair(R.string.rise, configStr)
        }

        private fun getMarsFilter(): Pair<Int, String> {
            var configStr = getContrastString(1.5f)
            configStr += getBrightnessString(0.09f)

            return Pair(R.string.mars, configStr)
        }

        private fun getAprilFilter(): Pair<Int, String> {
            val blueKnots: Array<Point?> = arrayOfNulls(4)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(39, 70)
            blueKnots[2] = Point(150, 200)
            blueKnots[3] = Point(255, 255)
            val redKnots: Array<Point?> = arrayOfNulls(4)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(45, 64)
            redKnots[2] = Point(170, 190)
            redKnots[3] = Point(255, 255)

            var configStr = "@curve " + getCurveString("R", redKnots)
            configStr += getCurveString("B", blueKnots)
            configStr += getContrastString(1.5f)
            configStr += getVignetteString(0.4f, 0.5f)

            return Pair(R.string.filter_april, configStr)
        }

        private fun getHaanFilter(): Pair<Int, String> {
            val greenKnots: Array<Point?> = arrayOfNulls(3)
            greenKnots[0] = Point(0, 0)
            greenKnots[1] = Point(113, 142)
            greenKnots[2] = Point(255, 255)

            var configStr = "@curve " + getCurveString("G", greenKnots)
            configStr += getVignetteString(0.3f, 0.4f)
            configStr += getBrightnessString(0.5f)
            configStr += getContrastString(1.25f)

            return Pair(R.string.haan, configStr)
        }

        private fun getOldManFilter(): Pair<Int, String> {

            var configStr = getBrightnessString(0.3f)
            configStr += getSaturationString(0.8f)
            configStr += getContrastString(1.25f)
            configStr += getVignetteString(0.37f, 0.7f)
            configStr += getColorBalanceString(0.15f, 0.15f, 0.075f)

            return Pair(R.string.oldman, configStr)
        }

        private fun getClarendon(): Pair<Int, String> {
            val redKnots: Array<Point?> = arrayOfNulls(4)
            redKnots[0] = Point(0, 0)
            redKnots[1] = Point(56, 68)
            redKnots[2] = Point(196, 206)
            redKnots[3] = Point(255, 255)
            val greenKnots: Array<Point?> = arrayOfNulls(4)
            greenKnots[0] = Point(0, 0)
            greenKnots[1] = Point(46, 77)
            greenKnots[2] = Point(160, 200)
            greenKnots[3] = Point(255, 255)
            val blueKnots: Array<Point?> = arrayOfNulls(4)
            blueKnots[0] = Point(0, 0)
            blueKnots[1] = Point(33, 86)
            blueKnots[2] = Point(126, 220)
            blueKnots[3] = Point(255, 255)

            var configStr = "@curve " + getCurveString("R", redKnots)
            configStr += getCurveString("G", greenKnots)
            configStr += getCurveString("B", blueKnots)
            configStr += getContrastString(1.5f)
            configStr += getBrightnessString(-0.08f)

            return Pair(R.string.clarendon, configStr)
        }

        private fun getNegative(): Pair<Int, String> {
            val configStr = "@curve RGB(0,255)(255,0)"
            return Pair(R.string.Negative, configStr)
        }

        private fun getCurveString(channel: String, points: Array<Point?>): String {
            var configStr = channel
            for (item in points) configStr += "(" + item!!.x + ", " + item.y + ")"
            return configStr
        }

        private fun getBrightnessString(value: Float): String {
            return "@adjust brightness $value"
        }

        private fun getContrastString(value: Float): String {
            return "@adjust contrast $value"
        }

        private fun getSaturationString(value: Float): String {
            return "@adjust saturation $value"
        }

        private fun getVignetteString(start: Float, range: Float): String {
            return "@vignette $start $range 0.5 0.5"
        }

        private fun getColorBalanceString(red: Float, green: Float, blue: Float): String {
            return "@adjust colorbalance $red $green $blue"
        }
    }
}
