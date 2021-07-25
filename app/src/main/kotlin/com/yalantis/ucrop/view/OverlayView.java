package com.yalantis.ucrop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


import com.kr.gallery.pro.R;
import com.yalantis.ucrop.callback.OverlayViewChangeListener;
import com.yalantis.ucrop.util.RectUtils;

import androidx.annotation.NonNull;

import static com.yalantis.ucrop.view.CropImageView.KEEP_ASPECT_RATIO;

/**
 * 이 View는 화상우에 자르기령역을 그려주기 위한 View이다
 * 이 View는 그리기를 위하여 layer type이 LAYER_TYPE_SOFTWARE로 되여야 한다.
 */
public class OverlayView extends View {

    public static final int DEFAULT_CROP_GRID_ROW_COUNT = 2;
    public static final int DEFAULT_CROP_GRID_COLUMN_COUNT = 2;

    // 초기 화상설정시 자르기구역의 최대 Rect (padding 계산)
    private RectF mOriginCropRect = null;
    // View의 최대 Rect (padding 계산)
    private RectF mMaxRect = null;
    // 자르기하려는 구역(padding 계산)
    private final RectF mCropViewRect = new RectF();
    // 자르기구역의 림시보관 변수
    private final RectF mTempRect = new RectF();

    // 초기 View의 너비/높이 (padding 제외)
    protected int mThisWidth, mThisHeight;
    // 자르기구역의 구석점들의 자리표
    protected float[] mCropGridCorners;
    // 자르기구역의 중심점의 자리표
    protected float[] mCropGridCenter;

    // 격자의 종횡수
    private int mCropGridRowCount, mCropGridColumnCount;

    // 목표 종횡비률
    private float mTargetAspectRatio;
    // 실지화상과의 확대축소비률이다
    // 자르기구역의 크기 문자렬 계산에 리용
    private float mCurrentImageScale = 1f;

    // 수직/수평 격자를 그릴때 자리표를 보관하는 배렬
    private float[] mGridPoints = null;

    // 자르기구역의 바깥부분 색갈
    private int mDimmedColor;
    // 격자선을 위한 Paint 객체
    private final Paint mCropGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 경계선을 위한 Paint 객체
    private final Paint mCropFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 경계구석을 위한 Paint 객체
    private final Paint mCropFrameCornersPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 자르기구역 크기문자렬을 위한 Paint 객체
    private final Paint mCropSizeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mPreviousTouchX = -1, mPreviousTouchY = -1;

    // 크기조절을 할때 끌기를 하고있는 핸들 형태
    private CropViewDragType mCurrentTouchDragIndex = null;

    // 자르기구역의 최소 크기
    private final int mCropRectMinSize;
    // 끌기핸들의 두께
    private final int mResizeHandleThickness;

    private OverlayViewChangeListener mCallback;

    private boolean mShouldSetupCropBounds;

    // 자르기구역 크기문자렬을 현시하겠는가를 나타낸다
    private boolean mShowCropText = false;

    // region: Inner class: Type

    /** 자르기구역의 움직임형태. */
    public enum CropViewDragType {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        CENTER
    }
    // endregion

    {
        mCropRectMinSize = getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_min_size);
        mResizeHandleThickness = getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_rect_corner_touch_area_line_length);
    }

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public OverlayViewChangeListener getOverlayViewChangeListener() {
        return mCallback;
    }

    public void setOverlayViewChangeListener(OverlayViewChangeListener callback) {
        mCallback = callback;
    }

    /**
     * padding을 제외한 자르기 구역을 되돌린다
     */
    public RectF getNoPaddingCropViewRect() {
        return new RectF(
                mCropViewRect.left - getPaddingLeft(),
                mCropViewRect.top - getPaddingTop(),
                mCropViewRect.right - getPaddingLeft(),
                mCropViewRect.bottom - getPaddingTop() );
    }

    /**
     * 새로운 자르기 비률로 자르기령역 설정
     *
     * @param targetAspectRatio - 자르기 비률 (e.g. 16:9일때 1.77(7))
     */
    public void setTargetAspectRatio(final float targetAspectRatio) {
        mTargetAspectRatio = targetAspectRatio;
        if (mThisWidth > 0) {
            setupCropBounds();
            postInvalidate();
        } else {
            mShouldSetupCropBounds = true;
        }
    }

    /**
     * 실지 화상과의 확대축소비률을 설정한다
     * @param scale : 확대축소비률
     */
    public void setCurrentImageScale(float scale) {
        if (Math.abs(mCurrentImageScale - scale) < 0.0001) return;
        mCurrentImageScale = scale;
        invalidate();
    }

    /**
     * 자르기 크기를 나타내는 문자렬을 현시하겠는가를 설정한다
     * @param isShow true이면 현시, false이면 현시하지 않는다
     */
    public void setShowCropText(boolean isShow) {
        mShowCropText = isShow;
        invalidate();
    }
    /**
     * 설정된 {@link #mTargetAspectRatio} 비률과 View 크기에 기초하여 자르기 령역 설정
     * 초기설정일때에는 {@link #mOriginCropRect}에 령역 보관
     */
    public void setupCropBounds() {
        float mainWidth = mThisWidth;
        float mainHeight = mThisHeight;
        float paddingLeft = getPaddingLeft();
        float paddingTop = getPaddingTop();
        if (mOriginCropRect != null) {
            // 보관된 초기 령역 리용
            mainWidth = mOriginCropRect.width();
            mainHeight = mOriginCropRect.height();
            paddingLeft = mOriginCropRect.left;
            paddingTop = mOriginCropRect.top;
        }

        float height = mainWidth / mTargetAspectRatio;

        if (height > mainHeight) {
            float width = mainHeight * mTargetAspectRatio;
            float halfDiff = (mainWidth - width) / 2;
            mCropViewRect.set(paddingLeft + halfDiff, paddingTop,
                    paddingLeft + width + halfDiff, paddingTop + mainHeight);
        } else {
            float halfDiff = (mainHeight - height) / 2;
            mCropViewRect.set(paddingLeft, paddingTop + halfDiff,
                    paddingLeft + mainWidth, paddingTop + height + halfDiff);
        }

        if (mOriginCropRect == null) mOriginCropRect = new RectF(mCropViewRect);

        if (mCallback != null) {
            mCallback.onCropRectUpdated(mCropViewRect);
        }

        updateGridPoints();
    }

    /**
     * padding을 포함한 View의 최대 Rect를 설정한다.
     */
    public void setMaxRect() {
        if (mMaxRect == null) {
            mMaxRect = new RectF(getLeft() + getPaddingLeft(), getTop() + getPaddingTop(), getRight() - getPaddingRight(), getBottom() - getPaddingBottom());
        }
    }

    /**
     * 파라메터들을 리용하여 자르기구역을 갱신시켜 animation 구현
     *
     * @param startRect  animation 시작자르기구역
     * @param endRect  animation 마감자르기구역을 포함하는 original 구역 (최대구역)
     * @param interpolateValue animation 변화값
     * @param aspectRatio  선택된 자르기구역 비률
     * @param useStartRect  startRect를 그대로 리용하는가를 나타낸다
     * @param useEndRect  endRect를 그대로 리용하는가를 나타낸다
     */
    public void animateCropViewRect(RectF startRect, RectF endRect, float interpolateValue, float aspectRatio, boolean useStartRect, boolean useEndRect) {

        if (aspectRatio > 0) {
            if (!useStartRect) startRect.set(calcRectByAspectRatio(startRect, aspectRatio));
            if (!useEndRect) endRect.set(calcRectByAspectRatio(endRect, aspectRatio));
        }
        float left = startRect.left + (endRect.left - startRect.left) * interpolateValue;
        float top = startRect.top + (endRect.top - startRect.top) * interpolateValue;
        float right = startRect.right + (endRect.right - startRect.right) * interpolateValue;
        float bottom = startRect.bottom + (endRect.bottom - startRect.bottom) * interpolateValue;

        mCropViewRect.set( getPaddingLeft() + left, getPaddingTop() + top, getPaddingLeft() + right, getPaddingTop() + bottom);

        updateGridPoints();
        invalidate();
    }

    /**
     * rect구역안에 포함되면서 aspectRatio 비률을 가지는 최대 중심구역 계산.
     * @param rect 최대구역
     * @param aspectRatio 구해야할 중심구역의 비률
     * @return  중심구역을 돌려준다
     */
    private RectF calcRectByAspectRatio(RectF rect, float aspectRatio) {
        float mainWidth = rect.width();
        float mainHeight = rect.height();
        float paddingLeft = rect.left;
        float paddingTop = rect.top;

        float height = mainWidth / aspectRatio;

        if (height > mainHeight) {
            float width = mainHeight * aspectRatio;
            float halfDiff = (mainWidth - width) / 2;
            rect.set(paddingLeft + halfDiff, paddingTop,
                    paddingLeft + width + halfDiff, paddingTop + mainHeight);
        } else {
            float halfDiff = (mainHeight - height) / 2;
            rect.set(paddingLeft, paddingTop + halfDiff,
                    paddingLeft + mainWidth, paddingTop + height + halfDiff);
        }

        return rect;
    }

    /**
     * 초기 구역을 설정하면서 자르기구역도 그에 맞게 다시 설정한다.
     * 실례로 회전을 진행한 경우 orientation에 맞게 다시 설정할 필요가 있다
     * @param rect 새로운 초기 구역
     */
    public void setOriginCropRect(RectF rect) {
        if (rect != null) {
            mOriginCropRect.set(getPaddingLeft() + rect.left, getPaddingTop() + rect.top, getPaddingLeft() + rect.right, getPaddingTop() + rect.bottom);
            mCropViewRect.set(mOriginCropRect);
            updateGridPoints();
            postInvalidate();
            if (mCallback != null) {
                mCallback.onCropRectUpdated(mCropViewRect);
            }
        } else mOriginCropRect = null;
    }

    /**
     * 자르기구역의 구석점들과 중심점의 자리표들을 얻어 {@link #mCropGridCorners} {@link #mCropGridCenter}들을 갱신한다.
     */
    private void updateGridPoints() {
        mCropGridCorners = RectUtils.getCornersFromRect(mCropViewRect);
        mCropGridCenter = RectUtils.getCenterFromRect(mCropViewRect);

        mGridPoints = null;
    }

    protected void init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;

            if (mShouldSetupCropBounds) {
                mShouldSetupCropBounds = false;
                setTargetAspectRatio(mTargetAspectRatio);
            }

            setMaxRect();
        }
    }

    /**
     * 모든 그리기 진행
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDimmedLayer(canvas);
        drawCropGrid(canvas);
        drawCropSizeText(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCropViewRect.isEmpty()) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // 어느 끌기핸들이 선택되였는지 검사
            mCurrentTouchDragIndex = getCurrentTouchIndex(x, y);
            boolean shouldHandle = mCurrentTouchDragIndex != null;
            if (!shouldHandle) {
                mPreviousTouchX = -1;
                mPreviousTouchY = -1;
            } else if (mPreviousTouchX < 0) {
                // 접촉편차구하기
                calculateTouchOffset(mCropViewRect, x, y);

                mPreviousTouchX = x + mTouchOffset.x;
                mPreviousTouchY = y + mTouchOffset.y;

                // 크기문자렬 현시
                mShowCropText = true;
            }

            return shouldHandle;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() == 1 && mCurrentTouchDragIndex != null) {

                updateCropViewRect(x + mTouchOffset.x, y + mTouchOffset.y);

                mPreviousTouchX = x + mTouchOffset.x;
                mPreviousTouchY = y + mTouchOffset.y;

                return true;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mPreviousTouchX = -1;
            mPreviousTouchY = -1;
            mCurrentTouchDragIndex = null;

            mShowCropText = false;

            if (mCallback != null) {
                mCallback.onCropRectUpdated(mCropViewRect);
            }
        }

        return false;
    }

    /**
     * * The order of the corners is:
     * 0------->1
     * ^        |
     * |   4    |
     * |        v
     * 3<-------2
     */
    private void updateCropViewRect(float touchX, float touchY) {
        mTempRect.set(mCropViewRect);

        // 구역 움직이기
        if (mCurrentTouchDragIndex == CropViewDragType.CENTER) {
            mTempRect.offset(touchX - mPreviousTouchX, touchY - mPreviousTouchY);
            if (mTempRect.left < mMaxRect.left) {
                mTempRect.left = mMaxRect.left;
                mTempRect.right = mTempRect.left + mCropViewRect.width();
            }
            if (mTempRect.top < mMaxRect.top) {
                mTempRect.top = mMaxRect.top;
                mTempRect.bottom = mTempRect.top + mCropViewRect.height();
            }
            if (mTempRect.right > mMaxRect.right) {
                mTempRect.right = mMaxRect.right;
                mTempRect.left = mTempRect.right - mCropViewRect.width();
            }
            if (mTempRect.bottom > mMaxRect.bottom) {
                mTempRect.bottom = mMaxRect.bottom;
                mTempRect.top = mTempRect.bottom - mCropViewRect.height();
            }

            mCropViewRect.set(mTempRect);
            updateGridPoints();
            postInvalidate();

            return;
        }

        // 한계를 넘지 못하게 한다
        if (touchX < mMaxRect.left) touchX = mMaxRect.left;
        if (touchX > mMaxRect.right) touchX = mMaxRect.right;
        if (touchY < mMaxRect.top) touchY = mMaxRect.top;
        if (touchY > mMaxRect.bottom) touchY = mMaxRect.bottom;

        if (KEEP_ASPECT_RATIO) {
            if (mCurrentTouchDragIndex == CropViewDragType.TOP_LEFT) {
                // 새로운 종횡비 계산
                float ratio = calculateAspectRatio(touchX, touchY, mTempRect.right, mTempRect.bottom);
                if (ratio > 0) {
                    if (ratio < mTargetAspectRatio) {
                        // 새로운 top을 설정하고 그에 맞게 left를 조절한다
                        adjustTop(mTempRect, touchY, mTargetAspectRatio);
                        adjustLeftByAspectRatio(mTempRect, mTargetAspectRatio);
                    } else {
                        // 새로운 left를 설정하고 그에 맞게 top을 조절한다
                        adjustLeft(mTempRect, touchX, mTargetAspectRatio);
                        adjustTopByAspectRatio(mTempRect, mTargetAspectRatio);
                    }
                }
            } else if (mCurrentTouchDragIndex == CropViewDragType.TOP_RIGHT) {
                // 새로운 종횡비 계산
                float ratio = calculateAspectRatio(mTempRect.left, touchY, touchX, mTempRect.bottom);
                if (ratio > 0) {
                    if (ratio < mTargetAspectRatio) {
                        // 새로운 top을 설정하고 그에 맞게 right를 조절한다
                        adjustTop(mTempRect, touchY, mTargetAspectRatio);
                        adjustRightByAspectRatio(mTempRect, mTargetAspectRatio);
                    } else {
                        // 새로운 right를 설정하고 그에 맞게 top을 조절한다
                        adjustRight(mTempRect, touchX, mTargetAspectRatio);
                        adjustTopByAspectRatio(mTempRect, mTargetAspectRatio);
                    }
                }
            } else if (mCurrentTouchDragIndex == CropViewDragType.BOTTOM_LEFT) {
                // 새로운 종횡비 계산
                float ratio = calculateAspectRatio(touchX, mTempRect.top, mTempRect.right, touchY);
                if (ratio > 0) {
                    if (ratio < mTargetAspectRatio) {
                        // 새로운 bottom을 설정하고 그에 맞게 left를 조절한다
                        adjustBottom(mTempRect, touchY, mTargetAspectRatio);
                        adjustLeftByAspectRatio(mTempRect, mTargetAspectRatio);
                    } else {
                        // 새로운 left를 설정하고 그에 맞게 bottom을 조절한다
                        adjustLeft(mTempRect, touchX, mTargetAspectRatio);
                        adjustBottomByAspectRatio(mTempRect, mTargetAspectRatio);
                    }
                }
            } else if (mCurrentTouchDragIndex == CropViewDragType.BOTTOM_RIGHT) {
                // 새로운 종횡비 계산
                float ratio = calculateAspectRatio(mTempRect.left, mTempRect.top, touchX, touchY);
                if (ratio > 0) {
                    if (ratio < mTargetAspectRatio) {
                        // 새로운 bottom을 설정하고 그에 맞게 right를 조절한다
                        adjustBottom(mTempRect, touchY, mTargetAspectRatio);
                        adjustRightByAspectRatio(mTempRect, mTargetAspectRatio);
                    } else {
                        // 새로운 right를 설정하고 그에 맞게 bottom을 조절한다
                        adjustRight(mTempRect, touchX, mTargetAspectRatio);
                        adjustBottomByAspectRatio(mTempRect, mTargetAspectRatio);
                    }
                }
            }
        } else {
            // 자유비률일때 크기 조절하기
            switch (mCurrentTouchDragIndex) {
                case TOP_LEFT:
                    mTempRect.set(touchX, touchY, mCropViewRect.right, mCropViewRect.bottom);
                    break;
                case TOP_RIGHT:
                    mTempRect.set(mCropViewRect.left, touchY, touchX, mCropViewRect.bottom);
                    break;
                case BOTTOM_RIGHT:
                    mTempRect.set(mCropViewRect.left, mCropViewRect.top, touchX, touchY);
                    break;
                case BOTTOM_LEFT:
                    mTempRect.set(touchX, mCropViewRect.top, mCropViewRect.right, touchY);
                    break;
                case LEFT:
                    mTempRect.set(touchX, mCropViewRect.top, mCropViewRect.right, mCropViewRect.bottom);
                    break;
                case TOP:
                    mTempRect.set(mCropViewRect.left, touchY, mCropViewRect.right, mCropViewRect.bottom);
                    break;
                case RIGHT:
                    mTempRect.set(mCropViewRect.left, mCropViewRect.top, touchX, mCropViewRect.bottom);
                    break;
                case BOTTOM:
                    mTempRect.set(mCropViewRect.left, mCropViewRect.top, mCropViewRect.right, touchY);
                    break;
            }
        }

        // 최소크기보다 클때에만 설정 및 그리기 갱신
        boolean changeHeight = mTempRect.height() >= mCropRectMinSize;
        boolean changeWidth = mTempRect.width() >= mCropRectMinSize;
        if (mMaxRect.contains(mTempRect)) {
            mCropViewRect.set(
                    changeWidth ? mTempRect.left : mCropViewRect.left,
                    changeHeight ? mTempRect.top : mCropViewRect.top,
                    changeWidth ? mTempRect.right : mCropViewRect.right,
                    changeHeight ? mTempRect.bottom : mCropViewRect.bottom);
        }
        if (changeHeight || changeWidth) {
            updateGridPoints();
            postInvalidate();
        }
    }

    // 처음으로 touch하였을때 끌기핸들과의 편차
    private final PointF mTouchOffset = new PointF();

    /**
     * 처음으로 Touch하였을때의 편차를 계산하여 보관한다.
     * 편차는 끌기에 리용되며 자르기구역이 처음에 갑자기 커지지 않게 한다.
     */
    private void calculateTouchOffset(RectF rect, float touchX, float touchY) {

        float touchOffsetX = 0;
        float touchOffsetY = 0;

        // 선택된 끌기핸들로부터 접촉자리표까지의 편차를 구한다
        switch (mCurrentTouchDragIndex) {
            case TOP_LEFT:
                touchOffsetX = rect.left - touchX;
                touchOffsetY = rect.top - touchY;
                break;
            case TOP_RIGHT:
                touchOffsetX = rect.right - touchX;
                touchOffsetY = rect.top - touchY;
                break;
            case BOTTOM_LEFT:
                touchOffsetX = rect.left - touchX;
                touchOffsetY = rect.bottom - touchY;
                break;
            case BOTTOM_RIGHT:
                touchOffsetX = rect.right - touchX;
                touchOffsetY = rect.bottom - touchY;
                break;
            case LEFT:
                touchOffsetX = rect.left - touchX;
                touchOffsetY = 0;
                break;
            case TOP:
                touchOffsetX = 0;
                touchOffsetY = rect.top - touchY;
                break;
            case RIGHT:
                touchOffsetX = rect.right - touchX;
                touchOffsetY = 0;
                break;
            case BOTTOM:
                touchOffsetX = 0;
                touchOffsetY = rect.bottom - touchY;
                break;
            case CENTER:
                touchOffsetX = rect.centerX() - touchX;
                touchOffsetY = rect.centerY() - touchY;
                break;
            default:
                break;
        }

        mTouchOffset.x = touchOffsetX;
        mTouchOffset.y = touchOffsetY;
    }

    /** 주어진 사각형의 종횡비를 계산하여 되돌린다. */
    private float calculateAspectRatio(float left, float top, float right, float bottom) {
        return (right - left) / (bottom - top);
    }

    /**
     * 파라메터로 들어오는 rect와 aspectRatio를 리용하여 rect의 새로운 left를 구하여 설정하기.
     * 이때 rect의 right는 고정이다
     * @param rect 설정하려는 구역
     * @param aspectRatio 종횡비
     */
    private void adjustLeftByAspectRatio(RectF rect, float aspectRatio) {
        rect.left = rect.right - rect.height() * aspectRatio;
    }

    /**
     * 파라메터로 들어오는 rect와 aspectRatio를 리용하여 rect의 새로운 top를 구하여 설정하기.
     * 이때 rect의 bottom은 고정이다
     * @param rect 설정하려는 구역
     * @param aspectRatio 종횡비
     */
    private void adjustTopByAspectRatio(RectF rect, float aspectRatio) {
        rect.top = rect.bottom - rect.width() / aspectRatio;
    }

    /**
     * 파라메터로 들어오는 rect와 aspectRatio를 리용하여 rect의 새로운 right를 구하여 설정하기.
     * 이때 rect의 left는 고정이다
     * @param rect 설정하려는 구역
     * @param aspectRatio 종횡비
     */
    private void adjustRightByAspectRatio(RectF rect, float aspectRatio) {
        rect.right = rect.left + rect.height() * aspectRatio;
    }

    /**
     * 파라메터로 들어오는 rect와 aspectRatio를 리용하여 rect의 새로운 bottom을 구하여 설정하기.
     * 이때 rect의 top은 고정이다
     * @param rect 설정하려는 구역
     * @param aspectRatio 종횡비
     */
    private void adjustBottomByAspectRatio(RectF rect, float aspectRatio) {
        rect.bottom = rect.top + rect.width() / aspectRatio;
    }


    /**
     * 파라메터로 들어오는 top 이 너무 낮은지 검사하고 rect의 top으로 설정
     *
     * @param top 끌기가 진행되는 웃모서리의 y 자리표
     * @param rect 조절하려는 구역
     * @param aspectRatio 유지하려는 종횡비
     */
    private void adjustTop(
            RectF rect,
            float top,
            float aspectRatio) {

        float newTop = top;

        if (aspectRatio > 0) {
            // 구역의 높이가 너무 낮은지 검사
            if (rect.bottom - newTop < mCropRectMinSize) {
                newTop = rect.bottom - mCropRectMinSize;
            }

            // 종횡비에 의한 새로운 너비 계산
            float newWidth = (rect.bottom - newTop) * aspectRatio;
            // 새로운 너비가 너무 작은지 검사
            if (newWidth < mCropRectMinSize) {
                newTop = rect.bottom - (mCropRectMinSize / aspectRatio);
            }
        }

        rect.top = newTop;
    }

    /**
     * 파라메터로 들어오는 left가 너무 좁은지 검사하고 rect의 left로 설정
     *
     * @param left 끌기가 진행되는 왼쪽모서리의 x 자리표
     * @param rect 조절하려는 구역
     * @param aspectRatio 유지하려는 종횡비
     */
    private void adjustLeft(
            RectF rect,
            float left,
            float aspectRatio) {

        float newLeft = left;

        if (aspectRatio > 0) {
            // 너비가 너무 작은지 검사
            if (rect.right - newLeft < mCropRectMinSize) {
                newLeft = rect.right - mCropRectMinSize;
            }
            // 종횡비에 의한 새로운 높이 계산
            float newHeight = (rect.right - newLeft) / aspectRatio;
            // 새 높이가 너무 작은지 검사
            if (newHeight < mCropRectMinSize) {
                newLeft = rect.right - mCropRectMinSize * aspectRatio;
            }
        }

        rect.left = newLeft;
    }

    /**
     * 파라메터로 들어오는 right가 너무 좁은지 검사하고 rect의 right로 설정
     *
     * @param right 끌기가 진행되는 오른쪽모서리의 x 자리표
     * @param rect 조절하려는 구역
     * @param aspectRatio 유지하려는 종횡비
     */
    private void adjustRight(
            RectF rect,
            float right,
            float aspectRatio) {

        float newRight = right;

        if (aspectRatio > 0) {
            // 너비가 너무 작은지 검사
            if (newRight - rect.left < mCropRectMinSize) {
                newRight = rect.left + mCropRectMinSize;
            }
            // 새 높이 계산
            float newHeight = (newRight - rect.left) / aspectRatio;
            // 새 높이가 너무 작은지 검사
            if (newHeight < mCropRectMinSize) {
                newRight = rect.left + mCropRectMinSize * aspectRatio;
            }
        }

        rect.right = newRight;
    }

    /**
     * 파라메터로 들어오는 bottom이 너무 작은지 검사하고 rect의 bottom으로 설정
     *
     * @param bottom 끌기가 진행되는 아래모서리의 y 자리표
     * @param rect 조절하려는 구역
     * @param aspectRatio 유지하려는 종횡비
     */
    private void adjustBottom(
            RectF rect,
            float bottom,
            float aspectRatio) {

        float newBottom = bottom;

        if (aspectRatio > 0) {
            // 높이가 너무 작은지 검사
            if (newBottom - rect.top < mCropRectMinSize) {
                newBottom = rect.top + mCropRectMinSize;
            }
            // 새 너비 계산
            float newWidth = (newBottom - rect.top) * aspectRatio;

            // 새 너비가 너무 작은지 검사
            if (newWidth < mCropRectMinSize) {
                newBottom = rect.top + mCropRectMinSize / aspectRatio;
            }
        }

        rect.bottom = newBottom;
    }
    /**
     * 파라메터로 들어오는 x, y자리표에서 끌기 handle 검사를 하고 {@link CropViewDragType} 을 돌려준다
     * @param x  끌기 X 자리표
     * @param y  끌기 Y 자리표
     */
    private CropViewDragType getCurrentTouchIndex(float x, float y) {

        float targetRadius = 50;
        CropViewDragType moveType = null;

        // 참고: 첫째 구석, 둘째 테두리, 셋쩨 중심
        if (isInCornerTargetZone(x, y, mCropViewRect.left, mCropViewRect.top, targetRadius)) {
            moveType = CropViewDragType.TOP_LEFT;
        } else if (isInCornerTargetZone(
                x, y, mCropViewRect.right, mCropViewRect.top, targetRadius)) {
            moveType = CropViewDragType.TOP_RIGHT;
        } else if (isInCornerTargetZone(
                x, y, mCropViewRect.left, mCropViewRect.bottom, targetRadius)) {
            moveType = CropViewDragType.BOTTOM_LEFT;
        } else if (isInCornerTargetZone(
                x, y, mCropViewRect.right, mCropViewRect.bottom, targetRadius)) {
            moveType = CropViewDragType.BOTTOM_RIGHT;
        } else if (isInHorizontalTargetZone(
                x, y, mCropViewRect.left, mCropViewRect.right, mCropViewRect.top, targetRadius)) {
            moveType = CropViewDragType.TOP;
        } else if (isInHorizontalTargetZone(
                x, y, mCropViewRect.left, mCropViewRect.right, mCropViewRect.bottom, targetRadius)) {
            moveType = CropViewDragType.BOTTOM;
        } else if (isInVerticalTargetZone(
                x, y, mCropViewRect.left, mCropViewRect.top, mCropViewRect.bottom, targetRadius)) {
            moveType = CropViewDragType.LEFT;
        } else if (isInVerticalTargetZone(
                x, y, mCropViewRect.right, mCropViewRect.top, mCropViewRect.bottom, targetRadius)) {
            moveType = CropViewDragType.RIGHT;
        } else if (isInCenterTargetZone(
                x, y, mCropViewRect.left, mCropViewRect.top, mCropViewRect.right, mCropViewRect.bottom)) {
            moveType = CropViewDragType.CENTER;
        }
        return moveType;
    }

    /**
     * 파라메터로 들어오는 자리표가 자르기구역의 구석에 놓이는지 검사한다
     *
     * @param x 검사하려는 x 자리표
     * @param y 검사하려는 y 자리표
     * @param handleX 구석의 x 자리표
     * @param handleY 구석의 y 자리표
     * @param targetRadius 목표반경 pixel
     * @return 목표구역안에 들어가면 true, 아니면 false
     */
    private static boolean isInCornerTargetZone(
            float x, float y, float handleX, float handleY, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius;
    }

    /**
     * 파라메터로 들어오는 자리표가 자르기구역의 수평경계면에 놓이는지 검사한다
     *
     * @param x 검사하려는 x 자리표
     * @param y 검사하려는 y 자리표
     * @param handleXStart 수평경계의 왼쪽 x 자리표
     * @param handleXEnd 수평경계의 오른쪽 x 자리표
     * @param handleY 수평경계의 y 자리표
     * @param targetRadius 목표반경 pixel
     * @return 목표구역안에 들어가면 true, 아니면 false
     */
    private static boolean isInHorizontalTargetZone(
            float x, float y, float handleXStart, float handleXEnd, float handleY, float targetRadius) {
        return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius;
    }

    /**
     * 파라메터로 들어오는 자리표가 자르기구역의 수직경계면에 놓이는지 검사한다
     *
     * @param x 검사하려는 x 자리표
     * @param y 검사하려는 y 자리표
     * @param handleX 수직경계의 x 자리표
     * @param handleYStart 수직경계의 웃쪽 y 자리표
     * @param handleYEnd 수직경계의 아래쪽 y 자리표
     * @param targetRadius 목표반경 pixel
     * @return 목표구역안에 들어가면 true, 아니면 false
     */
    private static boolean isInVerticalTargetZone(
            float x, float y, float handleX, float handleYStart, float handleYEnd, float targetRadius) {
        return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd;
    }

    /**
     * 파라메터로 들어오는 자리표가 주어진 구역안에 놓이는지 검사한다
     *
     * @param x 검사하려는 x 자리표
     * @param y 검사하려는 y 자리표
     * @param left 구역의 왼쪽 x 자리표
     * @param top 구역의 웃쪽 y 자리표
     * @param right 구역의 오른쪽 x 자리표
     * @param bottom 구역의 아래쪽 y 자리표
     * @return 주어진 구역안에 들어가면 true, 아니면 false
     */
    private static boolean isInCenterTargetZone(
            float x, float y, float left, float top, float right, float bottom) {
        return x > left && x < right && y > top && y < bottom;
    }

    /**
     * 자르기구역 바깥부분을 어둡게 그려준다
     *
     * @param canvas  그리기하는 canvas
     */
    protected void drawDimmedLayer(@NonNull Canvas canvas) {
        canvas.save();

        canvas.clipRect(mCropViewRect, Region.Op.DIFFERENCE);

        canvas.drawColor(mDimmedColor);
        canvas.restore();
    }

    /**
     * 자르기구역의 격자, 테두리, handle 그리기
     */
    protected void drawCropGrid(@NonNull Canvas canvas) {
        // 수직/수평 그리기
        if (mGridPoints == null && !mCropViewRect.isEmpty()) {

            mGridPoints = new float[(mCropGridRowCount) * 4 + (mCropGridColumnCount) * 4];

            int index = 0;
            for (int i = 0; i < mCropGridRowCount; i++) {
                mGridPoints[index++] = mCropViewRect.left;
                mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                mGridPoints[index++] = mCropViewRect.right;
                mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
            }

            for (int i = 0; i < mCropGridColumnCount; i++) {
                mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                mGridPoints[index++] = mCropViewRect.top;
                mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                mGridPoints[index++] = mCropViewRect.bottom;
            }
        }

        if (mGridPoints != null) {
            canvas.drawLines(mGridPoints, mCropGridPaint);
        }

        // 테두리 그리기
        canvas.drawRect(mCropViewRect, mCropFramePaint);

        canvas.save();

        if (KEEP_ASPECT_RATIO) {
            // 테두리의 구석들에 handle 그리기
            mTempRect.set(mCropViewRect);
            mTempRect.inset(mResizeHandleThickness, -mResizeHandleThickness);
            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

            mTempRect.set(mCropViewRect);
            mTempRect.inset(-mResizeHandleThickness, mResizeHandleThickness);
            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
        } else {
            // 테두리와 구석들에 handle 그리기
            // 왼쪽웃부분 그리기
            {
                // 왼쪽웃부분 수직 Rect
                mTempRect.set(mCropViewRect);
                mTempRect.right = mTempRect.right - mTempRect.width() / 2f + mResizeHandleThickness / 2f;
                mTempRect.bottom = mTempRect.bottom - mTempRect.height() / 2f + mResizeHandleThickness / 2f;
                mTempRect.inset(mResizeHandleThickness, -mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
                // 왼쪽웃부분 수평 Rect
                mTempRect.set(mCropViewRect);
                mTempRect.right = mTempRect.right - mTempRect.width() / 2f + mResizeHandleThickness / 2f;
                mTempRect.bottom = mTempRect.bottom - mTempRect.height() / 2f + mResizeHandleThickness / 2f;
                mTempRect.inset(-mResizeHandleThickness, mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
            }
            // 오른쪽웃부분 그리기
            {
                mTempRect.set(mCropViewRect);
                mTempRect.left = mTempRect.left + mTempRect.width() / 2f - mResizeHandleThickness / 2f;
                mTempRect.bottom = mTempRect.bottom - mTempRect.height() / 2f + mResizeHandleThickness / 2f;
                mTempRect.inset(mResizeHandleThickness, -mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

                mTempRect.set(mCropViewRect);
                mTempRect.left = mTempRect.left + mTempRect.width() / 2f - mResizeHandleThickness / 2f;
                mTempRect.bottom = mTempRect.bottom - mTempRect.height() / 2f + mResizeHandleThickness / 2f;
                mTempRect.inset(-mResizeHandleThickness, mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
            }
            // 왼쪽아래부분 그리기
            {
                mTempRect.set(mCropViewRect);
                mTempRect.right = mTempRect.right - mTempRect.width() / 2f + mResizeHandleThickness / 2f;
                mTempRect.top = mTempRect.top + mTempRect.height() / 2f - mResizeHandleThickness / 2f;
                mTempRect.inset(mResizeHandleThickness, -mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

                mTempRect.set(mCropViewRect);
                mTempRect.right = mTempRect.right - mTempRect.width() / 2f + mResizeHandleThickness / 2f;
                mTempRect.top = mTempRect.top + mTempRect.height() / 2f - mResizeHandleThickness / 2f;
                mTempRect.inset(-mResizeHandleThickness, mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
            }
            // 오른쪽아래부분 그리기
            {
                mTempRect.set(mCropViewRect);
                mTempRect.left = mTempRect.left + mTempRect.width() / 2f - mResizeHandleThickness / 2f;
                mTempRect.top = mTempRect.top + mTempRect.height() / 2f - mResizeHandleThickness / 2f;
                mTempRect.inset(mResizeHandleThickness, -mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

                mTempRect.set(mCropViewRect);
                mTempRect.left = mTempRect.left + mTempRect.width() / 2f - mResizeHandleThickness / 2f;
                mTempRect.top = mTempRect.top + mTempRect.height() / 2f - mResizeHandleThickness / 2f;
                mTempRect.inset(-mResizeHandleThickness, mResizeHandleThickness);
                canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
            }
        }

        canvas.drawRect(mCropViewRect, mCropFrameCornersPaint);

        canvas.restore();
    }

    /**
     * 자르기구역의 크기문자렬 그리기
     */
    public void drawCropSizeText(Canvas canvas) {
        if (!mShowCropText) return;

        // 자르기구역크기를 실지화상크기에 대응시킨다
        float width = mCropViewRect.width() / mCurrentImageScale;
        float height = mCropViewRect.height() / mCurrentImageScale;

        String text = (int)width + "\u00D7" + (int)height;
        Rect bounds = new Rect();
        mCropSizeTextPaint.getTextBounds(text, 0, text.length(), bounds);

        canvas.drawText(text,
                mCropViewRect.left + mCropViewRect.width() / 2 - bounds.width() / 2f,
                mCropViewRect.top + mCropViewRect.height() / 2 + bounds.height() / 2f, mCropSizeTextPaint);
    }

    /**
     * View를 그리는데 필요한 속성들을 얻고 설정한다
     */
    @SuppressWarnings("deprecation")
    protected void processStyledAttributes(@NonNull TypedArray a) {
        mDimmedColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_dimmed_color,
                getResources().getColor(R.color.ucrop_color_default_dimmed));

        mCropSizeTextPaint.setTextSize(40f);
        mCropSizeTextPaint.setColor(Color.WHITE);
        mCropSizeTextPaint.setStrokeWidth(10);
        mCropSizeTextPaint.setStyle(Paint.Style.FILL);
        mCropSizeTextPaint.setShadowLayer(4.0f, 2.0f, 2.0f, Color.BLACK);

        initCropFrameStyle(a);
        initCropGridStyle(a);
    }

    /**
     * 자르기구역의 경계 형태를 설정
     */
    @SuppressWarnings("deprecation")
    private void initCropFrameStyle(@NonNull TypedArray a) {
        int cropFrameStrokeSize = a.getDimensionPixelSize(R.styleable.ucrop_UCropView_ucrop_frame_stroke_size,
                getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width));
        int cropFrameColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_frame_color,
                getResources().getColor(R.color.ucrop_color_default_crop_frame));

        mCropFramePaint.setStrokeWidth(cropFrameStrokeSize);
        mCropFramePaint.setColor(cropFrameColor);
        mCropFramePaint.setStyle(Paint.Style.STROKE);

        mCropFrameCornersPaint.setStrokeWidth(cropFrameStrokeSize * 3);
        mCropFrameCornersPaint.setColor(cropFrameColor);
        mCropFrameCornersPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * 자르기구역의 격자선들의 형태를 설정
     */
    @SuppressWarnings("deprecation")
    private void initCropGridStyle(@NonNull TypedArray a) {
        int cropGridStrokeSize = a.getDimensionPixelSize(R.styleable.ucrop_UCropView_ucrop_grid_stroke_size,
                getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width));
        int cropGridColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_grid_color,
                getResources().getColor(R.color.ucrop_color_default_crop_grid));
        mCropGridPaint.setStrokeWidth(cropGridStrokeSize);
        mCropGridPaint.setColor(cropGridColor);

        mCropGridRowCount = a.getInt(R.styleable.ucrop_UCropView_ucrop_grid_row_count, DEFAULT_CROP_GRID_ROW_COUNT);
        mCropGridColumnCount = a.getInt(R.styleable.ucrop_UCropView_ucrop_grid_column_count, DEFAULT_CROP_GRID_COLUMN_COUNT);
    }
}
