package com.yalantis.ucrop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kr.gallery.pro.R;
import com.yalantis.ucrop.callback.CropBoundsChangeListener;
import com.yalantis.ucrop.task.BitmapCroppingWorkerTask;
import com.yalantis.ucrop.util.CubicEasing;
import com.yalantis.ucrop.util.RectUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.yalantis.ucrop.util.BitmapUtils.getRectBottom;
import static com.yalantis.ucrop.util.BitmapUtils.getRectLeft;
import static com.yalantis.ucrop.util.BitmapUtils.getRectRight;
import static com.yalantis.ucrop.util.BitmapUtils.getRectTop;

/**
 * 이 클라스는 자르기기능을 지원하며 격자를 그려주며 화상이 옳바른 상태에 놓이게 한다
 * 또한 확대/축소, 회전 등을 진행한다
 */
public class CropImageView extends TransformImageView {

    public static final float DEFAULT_MAX_SCALE_MULTIPLIER = 10.0f;
    public static final float FREE_ASPECT_RATIO = 0f;
    public static final float DEFAULT_ASPECT_RATIO = FREE_ASPECT_RATIO;

    public static boolean KEEP_ASPECT_RATIO = false;

    // padding을 포함한 령역
    private final RectF mCropRect = new RectF();
    // 화상의 Matrix를 림시 보관하는 Matirx형 변수
    private final Matrix mTempMatrix = new Matrix();

    private CropBoundsChangeListener mCropBoundsChangeListener;

    // 주어진 점을 중심으로 화상의 확대축소를 진행하는 animation runnable
    private Runnable mZoomImageToPositionRunnable = null;

    // 자르기령역크기에 따른 최대/최소 확대비률
    private float mMaxScale, mMinScale;

    /** bitmap을 자르기 하는 AsyncTask */
    private WeakReference<BitmapCroppingWorkerTask> mBitmapCroppingWorkerTask;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * @return  현재 자르기구역크기에 따른 화상의 최대 확대비률을 돌려준다
     */
    public float getMaxScale() {
        return mMaxScale;
    }

    /**
     * @return  현재 자르기구역크기에 따른 화상의 최소 축소비률을 돌려준다
     */
    public float getMinScale() {
        return mMinScale;
    }

    /**
     * @return  aspect ratio for crop bounds
     */
    public float getTargetAspectRatio() {
        return mTargetAspectRatio;
    }

    /**
     *
     * 자르기 령역 갱신.
     * 화상의 위치 맟 확대/축소 계산
     *
     * @param cropRect  설정하려는 새 자르기 령역
     */
    public void setCropRect(RectF cropRect) {
        mTargetAspectRatio = cropRect.width() / cropRect.height();
        // 령역 설정 (padding 제외)
        mCropRect.set(cropRect.left - getPaddingLeft(), cropRect.top - getPaddingTop(),
                cropRect.right - getPaddingLeft(), cropRect.bottom - getPaddingTop());
        calculateImageScaleBounds();
        setImageToWrapCropBounds();

        if (mMaxRect == null) {
            // ImageView의 최대령역 계산 (padding 포함)
            mMaxRect = new RectF(getLeft() + getPaddingLeft(), getTop() + getPaddingTop(), getRight() - getPaddingRight(), getBottom() - getPaddingBottom());

            // 초기 Portrait(회전하지 않은) 령역 보관
            mOriginRectPortrait = new RectF(mCropRect);
            {
                // 초기 Landscape(90도 회전한) 령역 계산 & 보관
                mOriginRectLandscape = new RectF(mCropRect);

                float scaleWidth = mThisWidth / mOriginRectPortrait.height();
                float scaleHeight = mThisHeight / mOriginRectPortrait.width();
                float scale = Math.min(scaleWidth, scaleHeight);

                final Matrix matrix = new Matrix();
                matrix.postScale(scale, scale, mOriginRectLandscape.centerX(), mOriginRectLandscape.centerY());
                matrix.postRotate(90, mOriginRectLandscape.centerX(), mOriginRectLandscape.centerY());
                matrix.mapRect(mOriginRectLandscape);
            }
            // 초기 령역너비 대 화상너비 비률 계산 (Portrait일때)
            mOriginScalePortrait = mOriginRectPortrait.width() / (mInitialImageCorners[2] - mInitialImageCorners[0]);
            // 초기 령역너비 대 화상너비 비률 계산 (Landscape일때)
            mOriginScaleLandscape = mOriginRectLandscape.width() / (mInitialImageCorners[7] - mInitialImageCorners[1]);
            // 초기 화상 확대/축소 비률 보관
            mInitialScale = getCurrentScale();
        }
    }

    /**
     * 고정비률을 설정한다
     * @param keepAspectRatio true이면 고정비률, false이면 자유비률이다
     */
    public void setKeepAspectRatio(boolean keepAspectRatio) {
        KEEP_ASPECT_RATIO = keepAspectRatio;
    }

    /**
     * 새로운 자르기 비률을 계산 및 보관하고 listener를 통하여 OverLayView에 비률 설정.
     *
     * @param targetAspectRatio  자르기 비률 (e.g. 16:9는 1.77(7))
     */
    public void setTargetAspectRatio(float targetAspectRatio) {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            mTargetAspectRatio = targetAspectRatio;
            return;
        }

        if (targetAspectRatio == FREE_ASPECT_RATIO) {
            // 자유비률 계산
            if (mPortrait) mTargetAspectRatio = (float)drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
            else mTargetAspectRatio = (float)drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth();
        } else {
            mTargetAspectRatio = targetAspectRatio;
        }

        if (mCropBoundsChangeListener != null) {
            mCropBoundsChangeListener.onCropAspectRatioChanged(mTargetAspectRatio);
        }
    }

    public void setCropBoundsChangeListener(@Nullable CropBoundsChangeListener cropBoundsChangeListener) {
        mCropBoundsChangeListener = cropBoundsChangeListener;
    }

    /**
     * 이 함수는 파라메터로 들어온 centerX, centerY를 중심으로 scale만큼 화상의 확대를 진행한다.
     * @param scale          확대비률
     * @param centerX        확대 중심 X
     * @param centerY        확대 중심 Y
     */
    public void zoomInImage(float scale, float centerX, float centerY) {
        if (scale <= getMaxScale()) {
            postScale(scale / getCurrentScale(), centerX, centerY);
        }
        if (mTransformImageListener != null) {
            mTransformImageListener.onScale(getMatrixScale(mCurrentImageMatrix));
        }
    }

    /**
     * 이 함수는 파라메터로 들어온 px, py를 중심으로 deltaScale만큼 화상의 확대축소를 진행한다.
     *
     * @param deltaScale  확대축소 비률
     * @param px          확대축소 중심 X
     * @param py          확대축소 중심 Y
     */
    public void postScale(float deltaScale, float px, float py) {
        if (deltaScale > 1 && getCurrentScale() * deltaScale <= getMaxScale()) {
            super.postScale(deltaScale, px, py);
        } else if (deltaScale < 1 && getCurrentScale() * deltaScale >= getMinScale()) {
            super.postScale(deltaScale, px, py);
        }
    }

    /**
     * 화상을 현재의 각도에서 deltaAngle 만큼 회전시킨다
     * @param deltaAngle  회전시키려는 각도
     */
    public void postRotate(float deltaAngle) {
        postRotate(deltaAngle, mOriginRectPortrait.centerX(), mOriginRectPortrait.centerY());
    }

    /**
     * 화상을 보관된 matrix로 재설정하고 처음부터 회전시킨다.<br/>
     * 회전 animation구현에 리용
     * @param rate animation 증가값
     */
    public void rotate(float rate) {
        rotate(rate, mOriginRectPortrait.centerX(), mOriginRectPortrait.centerY());
    }

    /**
     * 화상을 확대축소 시킨다
     * @param scaleX
     * @param scaleY
     */
    public void scale(float scaleX, float scaleY) {
        scale(scaleX, scaleY, mOriginRectPortrait.centerX(), mOriginRectPortrait.centerY());
    }

    /**
     * 모든 Runnable animation을 취소시킨다
     */
    public void cancelAllAnimations() {
        removeCallbacks(mZoomImageToPositionRunnable);
    }

    /**
     * 화상이 자르기령역을 포함하도록 이동/확대/축소 진행한다.
     */
    public void setImageToWrapCropBounds() {
        setImageToWrapCropBounds(mCropRect, false);
    }

    /**
     * 화상이 초기령역을 포함하도록 이동/확대/축소 진행한다.
     */
    public void setImageToWrapOriginBounds() {
        if (mPortrait) setImageToWrapCropBounds(mOriginRectPortrait, true);
        else setImageToWrapCropBounds(mOriginRectLandscape, true);
    }

    /**
     * 화상이 사각형을 완전 포함하지 않으면 이동/확대/축소를 진행하여 포함하도록 한다.<br/>
     * 이 함수는 이동할 x, y와 확대축소비률을 얻는다.<br/>
     * 확대축소값은 화상이 사각형의 중심으로 이동한후에도 사각형을 완전 포함하지 않으면 계산해야 한다.<br/>
     * 림시값을 리용하여 이경우를 검사한다.<br/>
     */
    public void setImageToWrapCropBounds(RectF rect, boolean wrapRectAlways) {
        if (mBitmapLaidOut) {

            float currentX = mCurrentImageCenter[0];
            float currentY = mCurrentImageCenter[1];
            float currentScale = getCurrentScale();

            float deltaX = rect.centerX() - currentX;
            float deltaY = rect.centerY() - currentY;
            float deltaScale = 0;

            mTempMatrix.reset();
            mTempMatrix.setTranslate(deltaX, deltaY);

            final float[] tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
            mTempMatrix.mapPoints(tempCurrentImageCorners);

            // 이동후 자르기구역을 포함하는지 검사
            boolean willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners);
            if (willImageWrapCropBoundsAfterTranslate && !wrapRectAlways) {
                // 이동후 포함할때에는 넘어나는 x, y자리표를 계산하여 이동
                final float[] imageIndents = calculateImageIndents();
                deltaX = -(imageIndents[0] + imageIndents[2]);
                deltaY = -(imageIndents[1] + imageIndents[3]);
            } else {
                // 포함하지 않으면 확대축소비률 계산
                RectF tempCropRect = new RectF(rect);
                mTempMatrix.reset();
                mTempMatrix.setRotate(getCurrentAngle());
                mTempMatrix.mapRect(tempCropRect);

                final float[] currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners);

                deltaScale = Math.max(tempCropRect.width() / currentImageSides[0],
                        tempCropRect.height() / currentImageSides[1]);

                deltaScale = deltaScale * currentScale - currentScale;
            }

            postTranslate(deltaX, deltaY);
            zoomInImage(currentScale + deltaScale, rect.centerX(), rect.centerY());
        }
    }

    /**
     * 파라메터로 들어오는 rect를 포함하기 위하여 화상이 확대축소되여야할 비률을 구한다
     * @param rect 포함시키기 위한 구역
     * @return 확대축소 비률
     */
    public float findScaleToWrapRectBound(RectF rect) {
        if (mBitmapLaidOut) {
            final float deltaScale;
            final float currentAngle = getCurrentAngle();

            RectF tempCropRect = new RectF(rect);
            mTempMatrix.reset();
            mTempMatrix.setRotate(currentAngle);
            mTempMatrix.mapRect(tempCropRect);

            final float[] currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners);

            deltaScale = Math.max(tempCropRect.width() / currentImageSides[0],
                    tempCropRect.height() / currentImageSides[1]);

            return deltaScale;
        }
        return 1f;
    }

    /**
     * 처음으로, 화상과 자르기구역을 거꾸로 회전시켜 화상이 축정렬이 되게 한다<br/>
     * 두번째로, 두개의 구역들을 포함하는 구역들을 구하고 그 구역들의 측면 delta를 구한다.<br/>
     * 세번째로, 주어진 delta에 근거하여 배렬에 그대로 혹은 0(delta가 부수)을 넣는다.<br/>
     * 네번째로, Matrix를 리용하여, 배렬의 점들을 다시 원래대로 회전시킨다<br/>
     *
     * @return - 화상의 4개 indent들, [왼쪽, 웃쪽, 오른쪽, 아래쪽] 순서
     */
    private float[] calculateImageIndents() {
        float currentAngle = getCurrentAngle();
        boolean mFlippedY = getCurrentScaleY() < 0;
        boolean mFlippedX = getCurrentScaleX() < 0;
        if (mFlippedX) currentAngle = -(180f + currentAngle);
        if (mFlippedY) currentAngle = -currentAngle;

        // 화상과 자르기구역을 거꾸로 회전시켜 화상이 축정렬이 되게 한다
        mTempMatrix.reset();
        mTempMatrix.setRotate(-currentAngle);

        float[] unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect);

        mTempMatrix.mapPoints(unrotatedImageCorners);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        // 두개의 구역들을 포함하는 구역들을 구하고 그 구역들의 측면 delta를 구한다.
        RectF unrotatedImageRect = RectUtils.trapToRect(unrotatedImageCorners);
        RectF unrotatedCropRect = RectUtils.trapToRect(unrotatedCropBoundsCorners);

        float deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left;
        float deltaTop = unrotatedImageRect.top - unrotatedCropRect.top;
        float deltaRight = unrotatedImageRect.right - unrotatedCropRect.right;
        float deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom;

        // 주어진 delta에 근거하여 배렬에 그대로 혹은 0(delta가 부수)을 넣는다.
        float indents[] = new float[4];
        indents[0] = (deltaLeft > 0) ? deltaLeft : 0;
        indents[1] = (deltaTop > 0) ? deltaTop : 0;
        indents[2] = (deltaRight < 0) ? deltaRight : 0;
        indents[3] = (deltaBottom < 0) ? deltaBottom : 0;

        // Matrix를 리용하여, 배렬의 점들을 다시 원래대로 회전시킨다
        mTempMatrix.reset();
        mTempMatrix.setRotate(currentAngle);
        mTempMatrix.mapPoints(indents);

        return indents;
    }

    /**
     * 화상이 설정될때
     */
    @Override
    protected void onImageLaidOut() {
        super.onImageLaidOut();
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        float drawableWidth = drawable.getIntrinsicWidth();
        float drawableHeight = drawable.getIntrinsicHeight();

        // 초기는 자유비률이므로 자유비률 계산
        mTargetAspectRatio = drawableWidth / drawableHeight;
        // ImageView 크기와 선택된 비률에 기초하여 초기 화상현시 비률 & 위치 계산
        int height = (int) (mThisWidth / mTargetAspectRatio);
        final float initWidth;
        final float initHeight;
        float initLeft = 0f;
        float initTop = 0f;
        if (height > mThisHeight) {
            initWidth = mThisHeight * mTargetAspectRatio;
            initHeight = mThisHeight;
            initLeft = (mThisWidth - initWidth) / 2f;
        } else {
            initWidth = mThisWidth;
            initHeight = height;
            initTop = (mThisHeight - height) / 2f;
        }
        setupInitialImagePosition(drawableWidth, drawableHeight, initWidth, initHeight, initLeft, initTop);

        if (mCropBoundsChangeListener != null) {
            mCropBoundsChangeListener.onCropAspectRatioChanged(mTargetAspectRatio);
        }
        if (mTransformImageListener != null) {
            mTransformImageListener.onScale(getCurrentScale());
            mTransformImageListener.onRotate(getCurrentAngle());
        }
    }

    /**
     *
     * 이 함수는 파라메터로 들어온 사각형구석자리표들이 자르기구역을 포함하는지 검사한다.
     *
     * @param imageCorners  사각형구석자리표
     * @return  포함하면 true, 아니면 false
     */
    protected boolean isImageWrapCropBounds(float[] imageCorners) {
        float currentAngle = getCurrentAngle();
        boolean mFlippedY = getCurrentScaleY() < 0;
        boolean mFlippedX = getCurrentScaleX() < 0;
        if (mFlippedX) currentAngle = -(180f + currentAngle);
        if (mFlippedY) currentAngle = -currentAngle;

        mTempMatrix.reset();
        mTempMatrix.setRotate(-currentAngle);

        float[] unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.length);
        mTempMatrix.mapPoints(unrotatedImageCorners);

        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        return RectUtils.trapToRect(unrotatedImageCorners).contains(RectUtils.trapToRect(unrotatedCropBoundsCorners));
    }

    /**
     * 파라메터로 들어온 centerX, centerY를 중심으로 scale만큼 화상의 확대축소 animation을 진행한다.<br/>
     * animation 기간은 durationMS 이다.
     *
     * @param scale       확대축소 비률
     * @param centerX     확대축소 중심 X
     * @param centerY     확대축소 중심 Y
     * @param durationMs  animation 기간
     */
    protected void zoomImageToPosition(float scale, float centerX, float centerY, long durationMs) {
        if (scale > getMaxScale()) {
            scale = getMaxScale();
        }

        final float oldScale = getCurrentScale();
        final float deltaScale = scale - oldScale;

        post(mZoomImageToPositionRunnable = new ZoomImageToPosition(CropImageView.this,
                durationMs, oldScale, deltaScale, centerX, centerY));
    }
    /**
     * 자르기 령역 크기에 기초하여 화상의 최소/최대 scale 값을 계산한다.
     */
    private void calculateImageScaleBounds() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        calculateImageScaleBounds(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    /**
     * 자르기 령역 크기에 기초하여 화상의 최소/최대 scale 값을 계산한다.
     * @param drawableWidth   화상 실지 너비
     * @param drawableHeight  화상 실지 높이
     */
    private void calculateImageScaleBounds(float drawableWidth, float drawableHeight) {
        float widthScale = Math.min(mCropRect.width() / drawableWidth, mCropRect.width() / drawableHeight);
        float heightScale = Math.min(mCropRect.height() / drawableHeight, mCropRect.height() / drawableWidth);

        mMinScale = Math.min(widthScale, heightScale);
        mMaxScale = mMinScale * DEFAULT_MAX_SCALE_MULTIPLIER;
    }

    /**
     * 초기 화상의 너비와 높이, ImageView의 Rect 크기에 기초하여 확대/축소 비률과 위치를 계산한다.<br/>
     * 계산된 값들을 {@link #mCurrentImageMatrix}에 설정하고 ImageView를 갱신한다
     * @param drawableWidth   화상 실지 너비
     * @param drawableHeight  화상 실지 높이
     * @param initWidth  초기 화상현시 너비
     * @param initHeight  초기 화상현시 높이
     * @param initLeft  초기 화상현시 left
     * @param initTop  초기 화상현시 top
     */
    private void setupInitialImagePosition(float drawableWidth, float drawableHeight, float initWidth, float initHeight, float initLeft, float initTop) {
        float widthScale = initWidth / drawableWidth;
        float heightScale = initHeight / drawableHeight;

        float initialMinScale = Math.max(widthScale, heightScale);

        float tw = (initWidth - drawableWidth * initialMinScale) / 2.0f + initLeft;
        float th = (initHeight - drawableHeight * initialMinScale) / 2.0f + initTop;

        mCurrentImageMatrix.reset();
        mCurrentImageMatrix.postScale(initialMinScale, initialMinScale);
        mCurrentImageMatrix.postTranslate(tw, th);
        setImageMatrix(mCurrentImageMatrix);
    }

    /**
     * 필요한 속성들을 얻어 설정한다
     */
    @SuppressWarnings("deprecation")
    protected void processStyledAttributes(@NonNull TypedArray a) {
        float targetAspectRatioX = Math.abs(a.getFloat(R.styleable.ucrop_UCropView_ucrop_aspect_ratio_x, DEFAULT_ASPECT_RATIO));
        float targetAspectRatioY = Math.abs(a.getFloat(R.styleable.ucrop_UCropView_ucrop_aspect_ratio_y, DEFAULT_ASPECT_RATIO));

        if (targetAspectRatioX == FREE_ASPECT_RATIO || targetAspectRatioY == FREE_ASPECT_RATIO) {
            mTargetAspectRatio = FREE_ASPECT_RATIO;
        } else {
            mTargetAspectRatio = targetAspectRatioX / targetAspectRatioY;
        }
    }

    /**
     * 이 Runnable은 화상의 확대축소 animation을 진행한다
     */
    private static class ZoomImageToPosition implements Runnable {

        private final WeakReference<CropImageView> mCropImageView;

        private final long mDurationMs, mStartTime;
        private final float mOldScale;
        private final float mDeltaScale;
        private final float mDestX;
        private final float mDestY;

        public ZoomImageToPosition(CropImageView cropImageView,
                                   long durationMs,
                                   float oldScale, float deltaScale,
                                   float destX, float destY) {

            mCropImageView = new WeakReference<>(cropImageView);

            mStartTime = System.currentTimeMillis();
            mDurationMs = durationMs;
            mOldScale = oldScale;
            mDeltaScale = deltaScale;
            mDestX = destX;
            mDestY = destY;
        }

        @Override
        public void run() {
            CropImageView cropImageView = mCropImageView.get();
            if (cropImageView == null) {
                return;
            }

            long now = System.currentTimeMillis();
            float currentMs = Math.min(mDurationMs, now - mStartTime);
            float newScale = CubicEasing.easeInOut(currentMs, 0, mDeltaScale, mDurationMs);

            if (currentMs < mDurationMs) {
                cropImageView.zoomInImage(mOldScale + newScale, mDestX, mDestY);
                cropImageView.post(this);
            } else {
                cropImageView.setImageToWrapCropBounds();
            }
        }

    }

    /**
     * 파라메터로 넘어온 bitmap에서 자르기를 진행한다
     * @param bitmap  자르기를 진행하려는 bitmap
     */
    public void getCroppedImageAsync(Bitmap bitmap) {
        startCropWorkerTask(bitmap);
    }

    /**
     * 화상의 자르기를 진행하는 AsyncTask 실행
     */
    public void startCropWorkerTask(
            Bitmap bitmap) {
        if (bitmap != null) {
            BitmapCroppingWorkerTask currentTask =
                    mBitmapCroppingWorkerTask != null ? mBitmapCroppingWorkerTask.get() : null;
            if (currentTask != null) {
                // cancel previous cropping
                currentTask.cancel(true);
            }

            float currentAngle = getCurrentAngle();

            boolean mFlippedY = getCurrentScaleY() < 0;
            boolean mFlippedX = getCurrentScaleX() < 0;

            if (mFlippedX) currentAngle = currentAngle - 180f;

            Rect cropRectInWrapper = getCropRectInWrapper();

            mBitmapCroppingWorkerTask =
                    new WeakReference<>(
                            new BitmapCroppingWorkerTask(
                                    this,
                                    bitmap,
                                    getViewImageCornerPoints(),
                                    (int)currentAngle,
                                    mFlippedX,
                                    mFlippedY,
                                    cropRectInWrapper));

            mBitmapCroppingWorkerTask.get().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /**
     * 화상의 실지 자리표와 그 크기에 대응되는 자르기구역을 리용하여 자르기구역을 다시 얻는다.<br/>
     * 0~7까지의 자리표는 회전된 화상의 자리표이다(realImageCorners).<br/>
     * 다시 얻는 자르기구역은 그림처럼 회전된 화상을 포함하는 점선으로 표현된 큰 사각형구역에서 얻는다.<br/>
     *   _______________
     *  |      01       |
     *  |     ____    23|
     *  |    |    |     |
     *  |    |____|     |
     *  |67             |
     *  |________45_____|
     *
     */
    public Rect getCropRectInWrapper() {
        float[] realImageCorners = getRealImageCornerPoints();
        float[] realCropCorners = getRealCropPoints();

        float currentAngle = getCurrentAngle();
        boolean mFlippedX = getCurrentScaleX() < 0;
        if (mFlippedX) currentAngle = currentAngle - 180f;

        Rect rect = new Rect();
        if (currentAngle > 0) {
            rect.left = (int) (realImageCorners[6] * -1 + realCropCorners[0]);
            rect.top = (int) (realImageCorners[1] * -1 + realCropCorners[1]);
            rect.right = (int) (realImageCorners[6] * -1 + realCropCorners[2]);
            rect.bottom = (int) (realImageCorners[1] * -1 + realCropCorners[5]);
        } else {
            rect.left = (int) (realImageCorners[0] * -1 + realCropCorners[0]);
            rect.top = (int) (realImageCorners[3] * -1 + realCropCorners[1]);
            rect.right = (int) (realImageCorners[0] * -1 + realCropCorners[2]);
            rect.bottom = (int) (realImageCorners[3] * -1 + realCropCorners[5]);
        }

        return rect;
    }

    /**
     * 6MP보다 큰 화상에서 자르기를 진행할떄 리용
     *
     * @param bitmap 화상 Bitmap
     * @param rectInWrapper 자르기구역
     * @param angle 각도
     * @param scaleX X축 확대축소비률
     * @param scaleY Y축 확대축소비률
     */
    public void getMasterCroppedImageAsync(Bitmap bitmap, Rect rectInWrapper, float angle, float scaleX, float scaleY) {
        if (bitmap != null) {
            BitmapCroppingWorkerTask currentTask =
                    mBitmapCroppingWorkerTask != null ? mBitmapCroppingWorkerTask.get() : null;
            if (currentTask != null) {
                // cancel previous cropping
                currentTask.cancel(true);
            }

            boolean mFlippedX = scaleX < 0;
            boolean mFlippedY = scaleY < 0;
            if (mFlippedX) angle = angle - 180f;

            mBitmapCroppingWorkerTask =
                    new WeakReference<>(
                            new BitmapCroppingWorkerTask(
                                    this,
                                    bitmap,
                                    getMasterImageCornerPoints(bitmap.getWidth(), bitmap.getHeight()),
                                    (int)angle,
                                    mFlippedX,
                                    mFlippedY,
                                    rectInWrapper));

            mBitmapCroppingWorkerTask.get().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    OnCropImageCompleteListener mOnCropImageCompleteListener = null;
    public void setOnCropImageCompleteListener(OnCropImageCompleteListener listener) {
        mOnCropImageCompleteListener = listener;
    }

    /**
     * 화상 자르기가 끝나면 호출되며 파라메터로 들어온 결과를 listener에 돌려준다
     * @param result 자르기 결과
     */
    public void onImageCroppingAsyncComplete(BitmapCroppingWorkerTask.Result result) {
        mBitmapCroppingWorkerTask = null;

        OnCropImageCompleteListener listener = mOnCropImageCompleteListener;
        if (listener != null) {
            listener.onCropImageComplete(result.bitmap);
        }
    }

    /**
     * 현재 화상의 Matrix를 적용하여 실지크기의 구석점들을 구하고 float배렬로 반환한다
     * @return 실지 구석점들의 float배렬
     */
    public float[] getViewImageCornerPoints() {

        final float[] points = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);

        Matrix temp = new Matrix();
        mCurrentImageMatrix.invert(temp);
        temp.mapPoints(points);

        return points;
    }

    /**
     * 원본화상의 너비/높이로부터 Rect를 만들고 Matrix를 리용하여 float배렬에 맵핑시켜 되돌린다
     * @param width 화상의 너비
     * @param height 화상의 높이
     * @return float배렬
     */
    public float[] getMasterImageCornerPoints(int width, int height) {

        Matrix matrix = new Matrix();
        RectF masterImageRect = new RectF(0, 0, width, height);
        float[] mInitialMasterImageCorners = RectUtils.getCornersFromRect(masterImageRect);
        float[] mMasterImageCorners = new float[8];

        matrix.mapPoints(mMasterImageCorners, mInitialMasterImageCorners);
        return mMasterImageCorners;
    }

    /**
     * 화상의 구석자리표들로부터 확대/축소값을 적용하여 실지 크기의 자리표를 얻는다
     * @return 자리표들을 float배렬로 되돌린다
     */
    public float[] getRealImageCornerPoints() {

        final float[] tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);

        for (int i = 0; i < tempCurrentImageCorners.length; i++) {
            tempCurrentImageCorners[i] = tempCurrentImageCorners[i] / getCurrentScale();
        }

        RectF rect =
                getRectFromPoints(tempCurrentImageCorners);

        tempCurrentImageCorners[0] = rect.left;
        tempCurrentImageCorners[1] = rect.top;
        tempCurrentImageCorners[2] = rect.right;
        tempCurrentImageCorners[3] = rect.top;
        tempCurrentImageCorners[4] = rect.right;
        tempCurrentImageCorners[5] = rect.bottom;
        tempCurrentImageCorners[6] = rect.left;
        tempCurrentImageCorners[7] = rect.bottom;

        return tempCurrentImageCorners;
    }

    /**
     * 자르기구역에 현재 화상의 확대/축소값을 적용하여 실지 화상크기에 대응되는 구역을 얻는다
     * @return  구역을 float배렬로 반환한다
     */
    public float[] getRealCropPoints() {

        float[] points =
                new float[] {
                        mCropRect.left,
                        mCropRect.top,
                        mCropRect.right,
                        mCropRect.top,
                        mCropRect.right,
                        mCropRect.bottom,
                        mCropRect.left,
                        mCropRect.bottom
                };

        for (int i = 0; i < points.length; i++) {
            points[i] = points[i] / getCurrentScale();
        }

        return points;
    }

    /**
     * 파라메터로 들어오는 자리표배렬로부터 Rect를 구하여 반환한다
     * @param points float형 자리표배렬. 크기는 8
     * @return Rect형을 되돌린다
     */
    public RectF getRectFromPoints( float[] points) {
        int left = (int) getRectLeft(points);
        int top = (int) getRectTop(points);
        int right = (int) getRectRight(points);
        int bottom = (int) getRectBottom(points);

        return new RectF(left, top, right, bottom);
    }

    /**
     * 자르기를 진행해야하는지 검사한다
     * @return 자르기를 해야하면 true, 아니면 false
     */
    public boolean shouldCrop() {
        int loadedSampleSize = mBitmapLoadSampleSize;
        Bitmap bitmap = getViewBitmap();
        if (bitmap == null) {
            return false;
        }
        int orgWidth = bitmap.getWidth() * loadedSampleSize;
        int orgHeight = bitmap.getHeight() * loadedSampleSize;

        float currentScale = getCurrentScale();

        float widthDiff = Math.abs(mCropRect.width() / currentScale - orgWidth);
        float heightDiff = Math.abs(mCropRect.height() / currentScale - orgHeight);

        float currentAngle = getCurrentAngle();

        boolean mFlippedY = getCurrentScaleY() < 0;
        boolean mFlippedX = getCurrentScaleX() < 0;

        return widthDiff > 0.1f || heightDiff > 0.1f || mFlippedY || mFlippedX || Math.abs(currentAngle) > 0.01;
    }

    /** 화상자르기가 끝나면 호출되는 callback interface. */
    public interface OnCropImageCompleteListener {

        /**
         * 화상의 자르기가 끝나면 호출된다
         *
         * @param resultBmp 자르기 결과 Bitmap
         */
        void onCropImageComplete(Bitmap resultBmp);
    }
}
