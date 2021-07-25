// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"

package com.yalantis.ucrop.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

/** 화상의 자르기를 진행하는 클라스. */
public final class BitmapUtils {

  /**
   * 화상의 자르기를 진행한다.
   * 메모리 넘침오유가 발생하면 화상크기를 2배씩 줄여서 다시 실행한다
   */
  public static Bitmap cropBitmapObjectHandleOOM(
          Bitmap bitmap,
          float[] imagePoints,
          Rect cropRect,
          int degreesRotated,
          boolean flipHorizontally,
          boolean flipVertically) {
    int scale = 1;
    while (true) {
      try {
          return cropBitmapObjectWithScale(
              bitmap,
              imagePoints,
              cropRect,
              degreesRotated,
              1 / (float) scale,
              flipHorizontally,
              flipVertically);
      } catch (OutOfMemoryError e) {
        scale *= 2;
        if (scale > 8) {
          throw e;
        }
      }
    }
  }

  /**
   * 화상을 회전, 확대축소를 진행하고 자르기구역으로 자르기를 진행한다.<br>
   *
   * @param scale 얼마나 화상을 축소시켰는가를 나타낸다 (OOM으로 하여 화상을 축소시켜 자르기)
   */
  private static Bitmap cropBitmapObjectWithScale(
      Bitmap bitmap,
      float[] imagePoints,
      Rect cropRect,
      int degreesRotated,
      float scale,
      boolean flipHorizontally,
      boolean flipVertically) {

    // 회전한 화상자리표들로부터 그 화상을 포함하는 최소 구역 구하기
    Rect rect =
        getRectFromPoints(
            imagePoints,
            bitmap.getWidth(),
            bitmap.getHeight());

    // 화상을 회전, 확대축소시키고 우에서 구한 구역만한 화상을 얻는다
    // 즉 테두리에는 검은 3각구역들이 생긴다
    Matrix matrix = new Matrix();
    matrix.setRotate(degreesRotated, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
    matrix.postScale(flipHorizontally ? -scale : scale, flipVertically ? -scale : scale);
    Bitmap result =
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);

    if (result == bitmap) {
      // 자를 필요가 없는경우
      result = bitmap.copy(bitmap.getConfig(), false);
    }

    // 파라메터로 들어온 자르기구역으로 자르기 진행
    Bitmap bitmapTmp = result;
    result = Bitmap.createBitmap(result, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    if (bitmapTmp != result) {
        bitmapTmp.recycle();
    }

    return result;
  }

  /** 파라메터로 들어온 구역자리표들로부터 left 값을 얻는다. */
  public static float getRectLeft(float[] points) {
    return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
  }

  /** 파라메터로 들어온 구역자리표들로부터 top 값을 얻는다. */
  public static float getRectTop(float[] points) {
    return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
  }

  /** 파라메터로 들어온 구역자리표들로부터 right 값을 얻는다. */
  public static float getRectRight(float[] points) {
    return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
  }

  /** 파라메터로 들어온 구역자리표들로부터 bottom 값을 얻는다. */
  public static float getRectBottom(float[] points) {
    return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
  }

  /**
   * 파라메터로 들어온 구역자리표들로부터 그 구역을 포함하는 최소 구역을 구햐여 돌려준다
   */
  static Rect getRectFromPoints(
      float[] points,
      int imageWidth,
      int imageHeight) {
    int left = Math.round(Math.max(0, getRectLeft(points)));
    int top = Math.round(Math.max(0, getRectTop(points)));
    int right = Math.round(Math.min(imageWidth, getRectRight(points)));
    int bottom = Math.round(Math.min(imageHeight, getRectBottom(points)));

    Rect rect = new Rect(left, top, right, bottom);

    return rect;
  }
}
