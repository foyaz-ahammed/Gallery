package com.yalantis.ucrop.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import com.yalantis.ucrop.util.FastBitmapDrawable;
import com.yalantis.ucrop.util.RectUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import static com.yalantis.ucrop.view.CropImageView.FREE_ASPECT_RATIO;

/**
 * 화상을 설정하고 matrix를 리용하여 이동/확대/축소/회전을 진행한다
 */
public class TransformImageView extends AppCompatImageView {

    private static final String TAG = "TransformImageView";

    private static final int RECT_CORNER_POINTS_COORDS = 8;
    private static final int RECT_CENTER_POINT_COORDS = 2;
    private static final int MATRIX_VALUES_COUNT = 9;

    protected final float[] mCurrentImageCorners = new float[RECT_CORNER_POINTS_COORDS];
    protected final float[] mCurrentImageCenter = new float[RECT_CENTER_POINT_COORDS];

    private final float[] mMatrixValues = new float[MATRIX_VALUES_COUNT];

    // 화상이 Portrait인가 아닌가를 나타낸다. 처음 화상을 열였을떄는 true 이다.
    protected boolean mPortrait = true;

    // ImageView의 최대령역 (padding 계산)
    protected RectF mMaxRect = null;

    // 초기 Portrait(회전하지 않은) 령역 (padding 제외)
    protected RectF mOriginRectPortrait = null;
    // 초기 Landscape(90도 회전한) 령역 (padding 제외)
    protected RectF mOriginRectLandscape = null;

    // 초기 령역너비 대 화상너비 비률 (Portrait일때)
    protected float mOriginScalePortrait = 1f;
    // 초기 령역너비 대 화상너비 비률 (Landscape일때)
    protected float mOriginScaleLandscape = 1f;

    // 현재 화상의 Matrix를 나타난다
    protected Matrix mCurrentImageMatrix = new Matrix();
    // 화상의 Matrix를 림시 보관하는 배렬
    float[] matrixTempAry = new float[9];
    // ImageView의 너비, 높이
    protected int mThisWidth, mThisHeight;

    protected TransformImageListener mTransformImageListener;

    // 초기 화상의 구석점들과 중심점 위치 보관 배렬
    protected float[] mInitialImageCorners;
    protected float[] mInitialImageCenter;
    // 초기 화상의 확대/축소 비률 보관
    protected float mInitialScale;

    protected boolean mBitmapDecoded = false;
    protected boolean mBitmapLaidOut = false;

    protected int mBitmapLoadSampleSize = 1;
    protected float mTargetAspectRatio;
    /**
     * 회전과 확대축소 변화를 알리는 interface
     */
    public interface TransformImageListener {

        void onLoadComplete(Bitmap original);

        void onLoadFailure(@NonNull Exception e);

        void onRotate(float currentAngle);

        void onScale(float currentScale);

        void onScaleBegin();

        void onScaleEnd();

    }

    public TransformImageView(Context context) {
        this(context, null);
    }

    public TransformImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setTransformImageListener(TransformImageListener transformImageListener) {
        mTransformImageListener = transformImageListener;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        } else {
            Log.w(TAG, "Invalid ScaleType. Only ScaleType.MATRIX can be used");
        }
    }

    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        setImageDrawable(new FastBitmapDrawable(bitmap));
        mBitmapDecoded = true;
        mBitmapLaidOut = false;
        mMaxRect = null;
        mTargetAspectRatio = FREE_ASPECT_RATIO;
        mPortrait = true;
    }

    /**
     * @return  현재 화상의 확대축소값을 반환한다
     * [1.0f - 원본크기, 2.0f - 2배 크기, etc.]
     */
    public float getCurrentScale() {
        return getMatrixScale(mCurrentImageMatrix);
    }

    /**
     * @return 현재 화상의 X축 확대비률을 반환한다
     */
    public float getCurrentScaleX() {
        return getMatrixValue(mCurrentImageMatrix, Matrix.MSCALE_X);
    }

    /**
     * @return 현재 화상의 Y축 확대비률을 반환한다
     */
    public float getCurrentScaleY() {
        return getMatrixValue(mCurrentImageMatrix, Matrix.MSCALE_Y);
    }

    /**
     * 파라메터로 들어오는 matrix에서 확대축소비률을 얻어 되돌린다
     * @return 확대축소 비률
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2)
                + Math.pow(getMatrixValue(matrix, Matrix.MSKEW_Y), 2));
    }

    /**
     * @return 현재 화상의 각도를 반환한다
     */
    public float getCurrentAngle() {
        return getMatrixAngle(mCurrentImageMatrix);
    }

    /**
     * @return 초기 화상 확대/축소 비률을 돌려준다
     */
    public float getInitialScale() {
        return mInitialScale;
    }

    /**
     * 파라메터로 들어오는 matrix에서 각도를 구하여 반환한다
     * @param matrix 리용하려는 matrix
     * @return 각도
     */
    public float getMatrixAngle(@NonNull Matrix matrix) {
        return (float) -(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X),
                getMatrixValue(matrix, Matrix.MSCALE_X)) * (180 / Math.PI));
    }

    /**
     * ImageView에 파라메터로 들어오는 matrix 설정
     * @param matrix 설정하려는 Matrix
     */
    @Override
    public void setImageMatrix(Matrix matrix) {
        // 결과 matrix를 적용하여 화상에 변화를 준다
        super.setImageMatrix(matrix);
        mCurrentImageMatrix.set(matrix);
        updateCurrentImagePoints();
    }

    /**
     * @return 현재의 화상을 반환한다
     */
    @Nullable
    public Bitmap getViewBitmap() {
        if (getDrawable() == null || !(getDrawable() instanceof FastBitmapDrawable)) {
            return null;
        } else {
            return ((FastBitmapDrawable) getDrawable()).getBitmap();
        }
    }

    /**
     * 현재의 화상을 이동시킨다
     *
     * @param deltaX  수평 이동 값
     * @param deltaY  수직 이동 값
     */
    public void postTranslate(float deltaX, float deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            mCurrentImageMatrix.postTranslate(deltaX, deltaY);
            setImageMatrix(mCurrentImageMatrix);
        }
    }

    /**
     * 현재의 화상을 확대축소 시킨다
     *
     * @param deltaScale 확대축소 비률
     * @param px         확대축소 중심 X
     * @param py         확대축소 중심 Y
     */
    public void postScale(float deltaScale, float px, float py) {
        if (deltaScale != 0) {
            mCurrentImageMatrix.postScale(deltaScale, deltaScale, px, py);
            setImageMatrix(mCurrentImageMatrix);
            if (mTransformImageListener != null) {
                mTransformImageListener.onScale(getMatrixScale(mCurrentImageMatrix));
            }
        }
    }

    /**
     * 현재의 화상을 px, py를 중심으로 회전시킨다
     *
     * @param deltaAngle  회전 각도
     * @param px          회전 중심 X
     * @param py          회전 중심 Y
     */
    public void postRotate(float deltaAngle, float px, float py) {
        if (deltaAngle != 0) {
            mCurrentImageMatrix.postRotate(deltaAngle, px, py);
            setImageMatrix(mCurrentImageMatrix);
            if (mTransformImageListener != null) {
                mTransformImageListener.onRotate(getMatrixAngle(mCurrentImageMatrix));
            }
        }
    }

    /**
     * 화상을 보관된 matrix로 재설정하고 들어오는 파라메터들로 회전시킨다
     * @param rate  회전 비률
     * @param px  회전 중심 X
     * @param py  회전 중심 Y
     */
    public void rotate(float rate, float px, float py) {
        // temp로 보관하였던 matrix를 설정하여 회전하기전 matrix상태로 복귀 (회전하기전에 temp에 보관하였다)
        mCurrentImageMatrix.setValues(matrixTempAry);

        float scale;
        if (mPortrait) scale = mOriginScaleLandscape / mOriginScalePortrait;
        else scale = mOriginScalePortrait / mOriginScaleLandscape;

        mCurrentImageMatrix.postRotate(90 * rate, px, py);
        mCurrentImageMatrix.postScale(1 - (1 - scale) * rate, 1 - (1 - scale) * rate, px, py);
        setImageMatrix(mCurrentImageMatrix);
    }

    /**
     * 화상의 확대축소를 진행한다
     * @param scaleX  X축 확대축소 값
     * @param scaleY  Y축 확대축소 값
     * @param px  확대축소 중심 X
     * @param py  확대축소 중심 Y
     */
    public void scale(float scaleX, float scaleY, float px, float py) {
        mCurrentImageMatrix.postScale(scaleX, scaleY, px, py);
        setImageMatrix(mCurrentImageMatrix);
//        if (mTransformImageListener != null) {
//            mTransformImageListener.onRotate(getMatrixAngle(mCurrentImageMatrix));
//        }
    }

    /**
     * 현재화상의 Matrix값을 float 배렬에 보관
     */
    public void tempMatrix() {
        mCurrentImageMatrix.getValues(matrixTempAry);
    }

    /**
     * float배렬에 보관된 값을 {@link #mCurrentImageMatrix}에 설정
     */
    public void resetMatrixByTemp() {
        mCurrentImageMatrix.setValues(matrixTempAry);
    }

    protected void init() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || (mBitmapDecoded && !mBitmapLaidOut)) {
            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;

            onImageLaidOut();
        }
    }

    /**
     * 화상이 설정될때 호출되며 {@link #mInitialImageCenter} & {@link #mInitialImageCenter} 은 설정이 되여야 한다
     */
    protected void onImageLaidOut() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        float w = drawable.getIntrinsicWidth();
        float h = drawable.getIntrinsicHeight();

        Log.d(TAG, String.format("Image size: [%d:%d]", (int) w, (int) h));

        RectF initialImageRect = new RectF(0, 0, w, h);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        mBitmapLaidOut = true;
    }

    /**
     * 파라메터로 들어오는 matrix에서 index번째 값을 반환한다
     *
     * @param matrix     리용하려는 matrix
     * @param valueIndex 필요한 값의 index. 참고 {@link Matrix#MSCALE_X}
     * @return matrix에서 index번째 값
     */
    protected float getMatrixValue(@NonNull Matrix matrix, @IntRange(from = 0, to = MATRIX_VALUES_COUNT) int valueIndex) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[valueIndex];
    }

    /**
     * 변화된 화상의 구석점들과 중심점들을 구햐여 {@link #mCurrentImageCorners} 와 {@link #mCurrentImageCenter} 배렬들에 보관.
     * 이 변수들은 여러가지 계산에 리용된다.
     */
    private void updateCurrentImagePoints() {
        mCurrentImageMatrix.mapPoints(mCurrentImageCorners, mInitialImageCorners);
        mCurrentImageMatrix.mapPoints(mCurrentImageCenter, mInitialImageCenter);
    }

    /**
     * @return 최대 령역 구역을 돌려준다
     */
    public RectF getMaxRect(){
        return mMaxRect;
    }

    public RectF getOriginRectPortrait() {
        return mOriginRectPortrait;
    }
    public RectF getOriginRectLandscape() {
        return mOriginRectLandscape;
    }

    public float getOriginScalePortrait() {
        return mOriginScalePortrait;
    }
    public float getOriginScaleLandscape() {
        return mOriginScaleLandscape;
    }

    /**
     * 90도 회전후 {@link #mPortrait} 갱신
     */
    public void orientationChanged() {
        mPortrait = !mPortrait;
    }

    /**
     * 현재 화상이 초기보다 0/180도 회전되였는지를 나타낸다.
     * @return 0/180도 회전되였으면 true, 아니면 false
     */
    public boolean isPortrait() {
        return mPortrait;
    }

    /**
     * @return 현재 화상의 중심을 되돌린다
     */
    public float[] getCurrentImageCenter() {
        return mCurrentImageCenter;
    }
}
