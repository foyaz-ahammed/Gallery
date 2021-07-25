package com.yalantis.ucrop.task;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;

import com.yalantis.ucrop.util.BitmapUtils;
import com.yalantis.ucrop.view.CropImageView;

import java.lang.ref.WeakReference;

/** UI 스레드와 비동기적으로 화상의 자르기를 진행하는 AsyncTask. */
public final class BitmapCroppingWorkerTask
    extends AsyncTask<Void, Void, BitmapCroppingWorkerTask.Result> {

  // region: Fields and Consts

  /** ImageView가 GC가 되도록 WeakReference 를 리용*/
  private final WeakReference<CropImageView> mUCropImageViewReference;

  /** 자르기를 하려는 bitmap */
  private final Bitmap mBitmap;

  /** 필요되는 화상의 4개 자리표들 (x0,y0,x1,y1,x2,y2,x3,y3) */
  private final float[] mImagePoints;

  /** 화상의 회전각도 */
  private final int mDegreesRotated;

  /** 화상이 수평으로 회전되였는가를 나타낸다 */
  private final boolean mFlipHorizontally;

  /** 화상이 수직으로 회전되였는가를 나타낸다 */
  private final boolean mFlipVertically;

  /** 자르기 구역 */
  private final Rect mCropRect;
  // endregion

  public BitmapCroppingWorkerTask(
          CropImageView cropImageView,
          Bitmap bitmap,
          float[] imagePoints,
          int degreesRotated,
          boolean flipHorizontally,
          boolean flipVertically,
          Rect cropRect) {

      mUCropImageViewReference = new WeakReference<>(cropImageView);
      mBitmap = bitmap;
      mImagePoints = imagePoints;
      mDegreesRotated = degreesRotated;
      mFlipHorizontally = flipHorizontally;
      mFlipVertically = flipVertically;
      mCropRect = cropRect;
}

  /**
   * background에서 화상 자르기 진행
   *
   * @return 자르기 결과 화상
   */
  @Override
  protected Result doInBackground(Void... params) {
    try {
      if (!isCancelled()) {

        Bitmap resultBitmap;
        if (mBitmap != null) {
            resultBitmap =
              BitmapUtils.cropBitmapObjectHandleOOM(
                  mBitmap,
                  mImagePoints,
                  mCropRect,
                  mDegreesRotated,
                  mFlipHorizontally,
                  mFlipVertically);
        } else {
          return new Result((Bitmap) null);
        }

        return new Result(resultBitmap);
      }
      return null;
    } catch (Exception e) {
      return new Result(e);
    }
  }

  /**
   * 자르기가 완성되면 결과화상을 CropImageVIew에 되돌린다
   *
   * @param result 잘려진 화상결과
   */
  @Override
  protected void onPostExecute(Result result) {
    if (result != null) {
      boolean completeCalled = false;
      if (!isCancelled()) {
          CropImageView uCropImageView = mUCropImageViewReference.get();
          if (uCropImageView != null ){
              completeCalled = true;
              uCropImageView.onImageCroppingAsyncComplete(result);
          }
      }
      if (!completeCalled && result.bitmap != null) {
        // fast release of unused bitmap
        result.bitmap.recycle();
      }
    }
  }

  /** BitmapCroppingWorkerTask 의 결과클라스. */
  public static final class Result {

    /** 자르기 결과 화상 */
    public final Bitmap bitmap;

    /** 비동기화상자르기에서 생긴 오유. */
    final Exception error;

    Result(Bitmap bitmap) {
      this.bitmap = bitmap;
      this.error = null;
    }

    Result(Exception error) {
      this.bitmap = null;
      this.error = error;
    }
  }
}
