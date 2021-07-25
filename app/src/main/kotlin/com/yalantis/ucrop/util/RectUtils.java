package com.yalantis.ucrop.util;

import android.graphics.RectF;

public class RectUtils {

    /**
     * 파라메터로 들어오는 구역의 자리표들을 float배렬로 반환한다
     * float배렬에서 구석순서
     * 0------->1
     * ^        |
     * |        |
     * |        v
     * 3<-------2
     *
     * @param r 구석자리표들을 얻으려는 구역
     * @return 구석자리표들의 float 배렬
     */
    public static float[] getCornersFromRect(RectF r) {
        return new float[]{
                r.left, r.top,
                r.right, r.top,
                r.right, r.bottom,
                r.left, r.bottom
        };
    }

    /**
     * @return 구역자리표들로부터 구역의 수직수평 길이를 반환한다
     */
    public static float[] getRectSidesFromCorners(float[] corners) {
        return new float[]{(float) Math.sqrt(Math.pow(corners[0] - corners[2], 2) + Math.pow(corners[1] - corners[3], 2)),
                (float) Math.sqrt(Math.pow(corners[2] - corners[4], 2) + Math.pow(corners[3] - corners[5], 2))};
    }

    /**
     * @return 구역의 중심점을 배렬로 반환한다
     */
    public static float[] getCenterFromRect(RectF r) {
        return new float[]{r.centerX(), r.centerY()};
    }

    /**
     * 파라메터로 들어온 구역자리표로부터 그 구역을 포함하는 최소 구역을 구하여 반환한다
     *
     * @param array 구역자리표 배렬
     * @return 포함하는 최소 구역
     */
    public static RectF trapToRect(float[] array) {
        RectF r = new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int i = 1; i < array.length; i += 2) {
            float x = Math.round(array[i - 1] * 10) / 10.f;
            float y = Math.round(array[i] * 10) / 10.f;
            r.left = Math.min(x, r.left);
            r.top = Math.min(y, r.top);
            r.right = Math.max(x, r.right);
            r.bottom = Math.max(y, r.bottom);
        }
        r.sort();
        return r;
    }

}
