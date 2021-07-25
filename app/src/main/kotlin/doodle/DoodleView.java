package doodle;/*
  MIT License

  Copyright (c) 2018 huangziwei

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
 *
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.kr.gallery.pro.R;

import java.util.ArrayList;
import java.util.List;

import cn.forward.androids.utils.ImageUtils;
import cn.forward.androids.utils.LogUtil;
import cn.forward.androids.utils.Util;

import static doodle.DrawUtil.drawCircle;
import static doodle.DrawUtil.drawRect;

/**
 * 그리기 View
 * Created by huangziwei on 2016/9/3.
 */
public class DoodleView extends FrameLayout implements IDoodle {

    public static final String TAG = "doodle.DoodleView";
    public final static float MAX_SCALE = 5f; // 최대 확대 비률
    public final static float MIN_SCALE = 0.25f; // 최소 확대 비률
    public final static int DEFAULT_SIZE = 6; // 표준 그리기 두께

    // 그리기중이거나 편집중인 item들을 제외하고 다시 그려주기 위한 기발
    private static final int FLAG_RESET_BACKGROUND = 1 << 1;
    // 그리기가 완료되였거나 편집이 끝났을때의 item들을 배경에 그려주기 위한 기발
    private static final int FLAG_DRAW_PENDINGS_TO_BACKGROUND = 1 << 2;
    // 현재까지 그려진 결과화상을 배경에 재그리기 위한 기발
    private static final int FLAG_REFRESH_BACKGROUND = 1 << 3;

    // More의 Draw에서 Style을 나타내는 index
    public static final int MORE_DRAW_STYLE_RECT = 0;
    public static final int MORE_DRAW_STYLE_CIRCLE = 1;
    public static final int MORE_DRAW_STYLE_BLUR_CIRCLE = 2;
    public static final int MORE_DRAW_STYLE_BLUR_BRUSH = 3;
    public static final int MORE_DRAW_STYLE_BLUR_DOTS = 4;

    private IDoodleListener mDoodleListener;

    // 원본화상
    private Bitmap mBitmap;

    // 화상이 View의 크기에 맞게 앉혀질때의 실지 확대축소비률이다
    private float mCenterScale;
    // 화상이 View에서 차지하는 높이, 너비이다
    private int mCenterHeight, mCenterWidth;
    // 화상이 View에 앉혀졌을때의 왼쪽웃구석자리표
    private float mCentreTranX, mCentreTranY;
    // 회전후 확대축소값
    private float mRotateScale = 1;
    // 회전 후 화면 중심으로 조정시의 편차
    private float mRotateTranX, mRotateTranY;
    // 화면에 화상이 앉혀진후 확대축소값. 처음 1f이다.(실지 화상의 확대축소값은 mCenterScale * mScale)
    private float mScale = 1;
    // 화면중심위치에 맞게 조정한 화상의 편차 (그림의 실제 편차는 View좌표계의 편차인 mCentreTranX + mTransX입니다)
    private float mTransX = 0, mTransY = 0;
    // 최소 축소 비률
    private float mMinScale = MIN_SCALE;
    // 최대 확대 비률
    private float mMaxScale = MAX_SCALE;

    // 그리기 두께
    private float mSize;
    // 그리기 색갈
    private IDoodleColor mColor;
    // 원본화상만 그리기할것인가 여부 기발
    private boolean isJustDrawOriginal;
    // touch시 화상령역 외부에 item의 그림자리길을 그릴 지 여부
    private boolean mIsDrawableOutside = false;
    // 초기화가 되였는지 여부
    private boolean mReady = false;

    // Undo를 위하여 그리기들을 보관. Undo할때 mRedoItemStack으로 넘어간다
    /** 여기에 보관되는 item들은 비트맵련결 canvas에 그려진다 {@link #mDoodleBitmapCanvas} */
    private final List<IDoodleItem> mItemStack = new ArrayList<>();
    // Redo를 위하여 그리기들을 보관. Redo할때 mItemStack으로 넘어간다
    private final List<IDoodleItem> mRedoItemStack = new ArrayList<>();
    // 현재 선택된 펜
    private IDoodlePen mPen;
    // 현재 선택된 모양
    private IDoodleShape mShape;

    private float mTouchX, mTouchY;
    // 확대경 켜기 여부 기발
    private boolean mEnableZoomer = false;
    // 확대경의 마지막 Y 자리표
    private float mLastZoomerY;
    // 확대경 반경
    private float mZoomerRadius;
    // 확대경 그리기 반경
    private Path mZoomerPath;
    // 확대경 배수
    private float mZoomerScale = 0;
    private Paint mZooomerPaint, mZoomerTouchPaint;
    // 수평으로 중앙에 위치하도록 확대경 위치의 x 좌표
    private int mZoomerHorizonX;
    // 확대경을 현시하는데 리용. 화면을 touch하여 끌기가 진행되는가 여부 기발
    private boolean mIsScrollingDoodle = false;
    //크기가 다른 화상의 경우 길이 단위가 다르다. 이 변수의 의미는 화상의 크기에 관계없는 dp와 류사하다
    private float mDoodleSizeUnit = 1;
    // 초기 화상을 기준으로 한 회전 각도
    private int mDoodleRotateDegree = 0;

    // 기본 touch gesture detector
    private IDoodleTouchDetector mDefaultTouchDetector;

    // item들의 현시만을 위한 View. 여기에 그려지는 item들은 편집중이거나 그리기중인 item들이다.
    private ForegroundView mForegroundView;
    // 편집방식인가를 나타낸다.
    private boolean mIsEditMode = false;
    // 화상이 보관중인가를 나타낸다.
    private boolean mIsSaving = false;

    /**
     * 그리기의 최적화 여부. 그리기 속도와 실행을 최적화하기 위하여 true로 설정할것을 권장한다.
     **/
    private final boolean mOptimizeDrawing; // 최적화를 위해 그리기는 화상에 즉시 그려진다.
    /** 여기에 보관되는 item들은 비트맵과 련결된 canvas에 그려지지 않고 현시를 위한 {@link ForegroundView}에 그려진다 */
    private final List<IDoodleItem> mItemStackOnViewCanvas = new ArrayList<>();
    /** {@link #mItemStack}에 추가되였지만 아직 비트맵과 련결된 {@link #mDoodleBitmapCanvas}에 그려지지 않은 item들이 추가된다  */
    private final List<IDoodleItem> mPendingItemsDrawToBitmap = new ArrayList<>();
    private Bitmap mDoodleBitmap;
    private int mFlags = 0;
    private Canvas mDoodleBitmapCanvas;
    private final BackgroundView mBackgroundView;

    // draw에서 그리기 형태들을 Bitmap으로 보관한다
    private Bitmap brush_blur_circle;
    private Bitmap brush_blur_brush;
    private Bitmap brush_blur_dots;

    // draw에서 현재 선택되여 있는 그리기 형태
    private int currentBrushBMPStyle = 0;

    public DoodleView(Context context, Bitmap bitmap, IDoodleListener listener) {
        this(context, bitmap, false, listener, null);
    }

    public DoodleView(Context context, Bitmap bitmap, IDoodleListener listener, IDoodleTouchDetector defaultDetector) {
        this(context, bitmap, false, listener, defaultDetector);
    }

    public DoodleView(Context context, Bitmap bitmap, boolean optimizeDrawing, IDoodleListener listener) {
        this(context, bitmap, optimizeDrawing, listener, null);

        brush_blur_circle = BitmapFactory.decodeResource(context.getResources(), R.drawable.brush_blur_circle);
        brush_blur_brush = BitmapFactory.decodeResource(context.getResources(), R.drawable.brush_blur_brush);
        brush_blur_dots = BitmapFactory.decodeResource(context.getResources(), R.drawable.brush_blur_dots);
    }

    /**
     * @param context
     * @param bitmap
     * @param optimizeDrawing true인 경우 item을 그리거나 수정할때 {@link #addItem (IDoodleItem)}을 호출하는 대신 {@link #markItemToOptimizeDrawing (IDoodleItem)}을 호출해야합니다.
     *                        끝나면 {@link #notifyItemFinishedDrawing(IDoodleItem)}을 호출해야 한다.
     *                        {@link #mOptimizeDrawing}
     * @param listener
     * @param defaultDetector 기본 gesture detector
     */
    public DoodleView(Context context, Bitmap bitmap, boolean optimizeDrawing, IDoodleListener listener, IDoodleTouchDetector defaultDetector) {
        super(context);
        setClipChildren(false);

        mBitmap = bitmap;

        mDoodleListener = listener;
        if (mDoodleListener == null) {
            throw new RuntimeException("doodle.IDoodleListener is null!!!");
        }
        if (mBitmap == null) {
            throw new RuntimeException("Bitmap is null!!!");
        }

        mOptimizeDrawing = optimizeDrawing;

        mScale = 1f;
        mColor = new DoodleColor(Color.RED);

        mPen = DoodlePen.BRUSH;
        mShape = DoodleShape.PAINTBRUSH;

        mZooomerPaint = new Paint();
        mZooomerPaint.setColor(0xaaffffff);
        mZooomerPaint.setStyle(Paint.Style.STROKE);
        mZooomerPaint.setAntiAlias(true);
        mZooomerPaint.setStrokeJoin(Paint.Join.ROUND);
        mZooomerPaint.setStrokeCap(Paint.Cap.ROUND);// 圆滑
        mZooomerPaint.setStrokeWidth(Util.dp2px(getContext(), 10));

        mZoomerTouchPaint = new Paint();
        mZoomerTouchPaint.setStyle(Paint.Style.STROKE);
        mZoomerTouchPaint.setAntiAlias(true);
        mZoomerTouchPaint.setStrokeJoin(Paint.Join.ROUND);
        mZoomerTouchPaint.setStrokeCap(Paint.Cap.ROUND);// 圆滑

        mDefaultTouchDetector = defaultDetector;

        mForegroundView = new ForegroundView(context);
        mBackgroundView = new BackgroundView(context);
        addView(mBackgroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mForegroundView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
        if (!mReady) {
            mDoodleListener.onReady(this);
            mReady = true;
        }
    }

    private Matrix mTouchEventMatrix = new Matrix();
    private OnTouchListener mOnTouchListener;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mOnTouchListener != null) {
            if (mOnTouchListener.onTouch(this, event)) {
                return true;
            }
        }
        mTouchX = event.getX();
        mTouchY = event.getY();

        // 령역밖에서 touch할수 없도록 event를 innerView로 전달합니다.
        MotionEvent transformedEvent = MotionEvent.obtain(event);
//        final float offsetX = mForegroundView.getScrollX() - mForegroundView.getLeft();
//        final float offsetY = mForegroundView.getScrollY() - mForegroundView.getTop();
//        transformedEvent.offsetLocation(offsetX, offsetY);
        mTouchEventMatrix.reset();
        mTouchEventMatrix.setRotate(-mDoodleRotateDegree, getWidth() / 2, getHeight() / 2);
        transformedEvent.transform(mTouchEventMatrix);
        boolean handled = mForegroundView.onTouchEvent(transformedEvent);
        transformedEvent.recycle();

        return handled;
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        mOnTouchListener = l;
        super.setOnTouchListener(l);
    }

    private void init() {
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float nw = w * 1f / getWidth();
        float nh = h * 1f / getHeight();
        if (nw > nh) {
            mCenterScale = 1 / nw;
            mCenterWidth = getWidth();
            mCenterHeight = (int) (h * mCenterScale);
        } else {
            mCenterScale = 1 / nh;
            mCenterWidth = (int) (w * mCenterScale);
            mCenterHeight = getHeight();
        }
        // 화상이 View에 앉혀졌을때 왼쪽웃구석 X 자리표
        mCentreTranX = (getWidth() - mCenterWidth) / 2f;
        // 화상이 View에 앉혀졌을때 왼쪽웃구석 Y 자리표
        mCentreTranY = (getHeight() - mCenterHeight) / 2f;

        mZoomerRadius = Math.min(getWidth(), getHeight()) / 4f;
        mZoomerPath = new Path();
        mZoomerPath.addCircle(mZoomerRadius, mZoomerRadius, mZoomerRadius, Path.Direction.CCW);
        mZoomerHorizonX = (int) (Math.min(getWidth(), getHeight()) / 2 - mZoomerRadius);

        mDoodleSizeUnit = Util.dp2px(getContext(), 1) / mCenterScale;

        if (!mReady) {
            mSize = DEFAULT_SIZE * mDoodleSizeUnit;
        }
        // 화면 중심
        mTransX = mTransY = 0;
        mScale = 1;

        initDoodleBitmap();

        refreshWithBackground();
    }

    /**
     * 그리기화상과 Canvas 초기화
     */
    private void initDoodleBitmap() {
        if (!mOptimizeDrawing) {
            return;
        }

        if (mDoodleBitmap != null) {
            mDoodleBitmap.recycle();
        }
        mDoodleBitmap = mBitmap.copy(mBitmap.getConfig(), true);
        mDoodleBitmapCanvas = new Canvas(mDoodleBitmap);
    }

    /**
     * @return View 좌표계에서 현재 화상의 직사각형 령역을 가져옵니다.
     */
    public RectF getDoodleBound() {
        float width = mCenterWidth * mRotateScale * mScale;
        float height = mCenterHeight * mRotateScale * mScale;

        PointF mTempPoint = new PointF(toTouchX(0), toTouchY(0));
        return new RectF(mTempPoint.x, mTempPoint.y, mTempPoint.x + width, mTempPoint.y + height);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mBitmap.isRecycled()) {
            return;
        }

        if (hasFlag(FLAG_RESET_BACKGROUND)) {
            LogUtil.d(TAG, "FLAG_RESET_BACKGROUND");
            clearFlag(FLAG_RESET_BACKGROUND);
            clearFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND);
            clearFlag(FLAG_REFRESH_BACKGROUND);
            refreshDoodleBitmap(false);
            mPendingItemsDrawToBitmap.clear();
            mBackgroundView.invalidate();
        } else if (hasFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND)) {
            LogUtil.d(TAG, "FLAG_DRAW_PENDINGS_TO_BACKGROUND");
            clearFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND);
            clearFlag(FLAG_REFRESH_BACKGROUND);
            drawToDoodleBitmap(mPendingItemsDrawToBitmap);
            mPendingItemsDrawToBitmap.clear();
            mBackgroundView.invalidate();
        } else if (hasFlag(FLAG_REFRESH_BACKGROUND)) {
            LogUtil.d(TAG, "FLAG_REFRESH_BACKGROUND");
            clearFlag(FLAG_REFRESH_BACKGROUND);
            mBackgroundView.invalidate();
        }

        int count = canvas.save();
        super.dispatchDraw(canvas);
        canvas.restoreToCount(count);

        /*// test
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(20);
        canvas.drawRect(getDoodleBound(), mPaint);*/

        if (mIsScrollingDoodle
                && mEnableZoomer && mZoomerScale > 0) {
            // 확대경 현시
            canvas.save(); // ***

            float unitSize = getUnitSize();
            // 확대경 위치설정
            if (mTouchY <= mZoomerRadius * 2) {
                mLastZoomerY = getHeight() - mZoomerRadius * 2;
            } else if (mTouchY >= getHeight() - mZoomerRadius * 2) {
                mLastZoomerY = 0;
            }
            canvas.translate(mZoomerHorizonX, mLastZoomerY);
            canvas.clipPath(mZoomerPath);
            canvas.drawColor(0xff000000);

            canvas.save();
            float scale = mZoomerScale / mScale;
            canvas.scale(scale, scale);
            canvas.translate(-mTouchX + mZoomerRadius / scale, -mTouchY + mZoomerRadius / scale);
            // draw inner
            super.dispatchDraw(canvas);

            float left = getAllTranX();
            float top = getAllTranY();
            // canvas와 화상은 동일한 좌표계를 공유하며 View좌표계와 화상(canvas)좌표계 간의 대응관계만 처리하면됩니다.
            canvas.translate(left, top);
            scale = getAllScale();
            canvas.scale(scale, scale);
            mZoomerTouchPaint.setStrokeWidth(unitSize / 2);
            float radius = mSize / 2 - unitSize / 2;
            float radius2 = radius - unitSize / 2;
            if (radius <= 1) {
                radius = 1;
                radius2 = radius / 2;
                mZoomerTouchPaint.setStrokeWidth(mSize);
            }
            mZoomerTouchPaint.setColor(0xaa000000);
            drawCircle(canvas, toX(mTouchX), toY(mTouchY), radius, mZoomerTouchPaint);
            mZoomerTouchPaint.setColor(0xaaffffff);
            drawCircle(canvas, toX(mTouchX), toY(mTouchY), radius2, mZoomerTouchPaint);
            canvas.restore();

            // 확대경 경계 그리기
            drawCircle(canvas, mZoomerRadius, mZoomerRadius, mZoomerRadius, mZooomerPaint);

            canvas.restore(); // ***

            // overview
            canvas.save();
            canvas.translate(mZoomerHorizonX, mLastZoomerY);
            scale = (mZoomerRadius / 2) / getWidth();
            canvas.scale(scale, scale);
            float strokeWidth = 1 / scale;
            canvas.clipRect(-strokeWidth, -strokeWidth, getWidth() + strokeWidth, getHeight() + strokeWidth);
            canvas.drawColor(0x88888888);
            canvas.save();
            float tempScale = mScale;
            float tempTransX = mTransX;
            float tempTransY = mTransY;
            mScale = 1;
            mTransX = mTransY = 0;
            super.dispatchDraw(canvas);
            mScale = tempScale;
            mTransX = tempTransX;
            mTransY = tempTransY;
            canvas.restore();
            mZoomerTouchPaint.setStrokeWidth(strokeWidth);
            mZoomerTouchPaint.setColor(0xaa000000);
            drawRect(canvas, 0, 0, getWidth(), getHeight(), mZoomerTouchPaint);
            mZoomerTouchPaint.setColor(0xaaffffff);
            drawRect(canvas, strokeWidth, strokeWidth, getWidth() - strokeWidth, getHeight() - strokeWidth, mZoomerTouchPaint);
            canvas.restore();
        }

    }

    private boolean hasFlag(int flag) {
        return (mFlags & flag) != 0;
    }

    private void addFlag(int flag) {
        mFlags = mFlags | flag;
    }

    private void clearFlag(int flag) {
        mFlags = mFlags & ~flag;
    }

    public float getAllScale() {
        return mCenterScale * mRotateScale * mScale;
    }

    public float getAllTranX() {
        return mCentreTranX + mRotateTranX + mTransX;
    }

    public float getAllTranY() {
        return mCentreTranY + mRotateTranY + mTransY;
    }

    /**
     * @return 화면 touch좌표 x를 화상의 좌표로 변환
     */
    public final float toX(float touchX) {
        return (touchX - getAllTranX()) / getAllScale();
    }

    /**
     * @return 화면 touch좌표 y를 화상의 좌표로 변환
     */
    public final float toY(float touchY) {
        return (touchY - getAllTranY()) / getAllScale();
    }

    /**
     * @return 화상에서의 x자리표를 View 좌표계에서의 자리표로 변환
     */
    public final float toTouchX(float x) {
        return x * getAllScale() + getAllTranX();
    }

    /**
     * @return 화상에서의 y자리표를 View 좌표계에서의 자리표로 변환
     */
    public final float toTouchY(float y) {
        return y * getAllScale() + getAllTranY();
    }

    /**
     * 좌표변환
     * (공식은 toX()의 공식에서 계산됩니다)
     *
     * @param touchX  touch의 x좌표
     * @param doodleX item의 x좌표
     * @return x편차를 되돌린다
     */
    public final float toTransX(float touchX, float doodleX) {
        return -doodleX * getAllScale() + touchX - mCentreTranX - mRotateTranX;
    }

    /**
     * 좌표변환
     * (공식은 toY()의 공식에서 계산됩니다)
     *
     * @param touchY  touch의 y좌표
     * @param doodleY item의 y좌표
     * @return y편차를 되돌린다
     */
    public final float toTransY(float touchY, float doodleY) {
        return -doodleY * getAllScale() + touchY - mCentreTranY - mRotateTranY;
    }

    /**
     * {@link #mDefaultTouchDetector} 설정
     * @param touchGestureDetector 설정하려는 detector
     */
    public void setDefaultTouchDetector(IDoodleTouchDetector touchGestureDetector) {
        mDefaultTouchDetector = touchGestureDetector;
    }

    /**
     * @return {@link #mDefaultTouchDetector} 를 되돌린다
     */
    public IDoodleTouchDetector getDefaultTouchDetector() {
        return mDefaultTouchDetector;
    }

    /**
     * {@link #mDoodleBitmapCanvas} 에 item들을 그려준다.
     * @param items 그리려는 item들
     */
    private void drawToDoodleBitmap(List<IDoodleItem> items) {
        if (!mOptimizeDrawing) {
            return;
        }

        for (IDoodleItem item : items) {
            item.draw(mDoodleBitmapCanvas);             // set current brush bitmap
        }
    }

    /**
     * 편집중에 선택된 item은 bitmap에 그려지지 않는다
     * 이 함수는 편집중의 item을 mDoodleBitmapCanvas에 그려준다
     */
    public void drawPendingItems() {
        drawToDoodleBitmap(mItemStackOnViewCanvas);
    }

    /**
     * 화상에 item들을 다시 그려준다
     * @param drawAll 편집중의 item들도 모두 그려줄것인가의 여부
     */
    private void refreshDoodleBitmap(boolean drawAll) {
        if (!mOptimizeDrawing) {
            return;
        }

        initDoodleBitmap();
        final List<IDoodleItem> items;
        if (drawAll) {
            items = mItemStack;
        } else {
            items = new ArrayList<>(mItemStack);
            items.removeAll(mItemStackOnViewCanvas);
        }
        for (IDoodleItem item : items) {
            item.draw(mDoodleBitmapCanvas);
        }
    }

    private void refreshWithBackground() {
        addFlag(FLAG_REFRESH_BACKGROUND);
        refresh();
    }

    // ========================= api ================================

    @Override
    public void invalidate() {
        refresh();
    }

    /**
     * 재그리기
     */
    @Override
    public void refresh() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate();
            mForegroundView.invalidate();
        } else {
            super.postInvalidate();
            mForegroundView.postInvalidate();
        }
    }

    @Override
    public void setBrushBMPStyle(int style) {
        currentBrushBMPStyle = style;
    }

    @Override
    public Bitmap getBrushBMP() {
        final Bitmap brush;
        switch (currentBrushBMPStyle) {
            case MORE_DRAW_STYLE_BLUR_BRUSH:
                brush = Bitmap.createScaledBitmap(brush_blur_brush, (int)mSize, (int)mSize, true);
                break;
            case MORE_DRAW_STYLE_BLUR_CIRCLE:
                brush = Bitmap.createScaledBitmap(brush_blur_circle, (int)mSize, (int)mSize, true);
                break;
            case MORE_DRAW_STYLE_BLUR_DOTS:
                brush = Bitmap.createScaledBitmap(brush_blur_dots, (int)mSize, (int)mSize, true);
                break;
            default:
                brush = null;
        }
        return brush;
    }

    @Override
    public int getBrushStyle() {
        return currentBrushBMPStyle;
    }

    @Override
    public int getDoodleRotation() {
        return mDoodleRotateDegree;
    }

    /**
     * 초기 화상을 기준으로 한 회전각도 설정
     *
     * @param degree 정수이면 오른쪽으로 회전, 부수이면 왼쪽으로 회전
     */
    @Override
    public void setDoodleRotation(int degree) {
        mDoodleRotateDegree = degree;
        mDoodleRotateDegree = mDoodleRotateDegree % 360;
        if (mDoodleRotateDegree < 0) {
            mDoodleRotateDegree = 360 + mDoodleRotateDegree;
        }

        // centered
        RectF rectF = getDoodleBound();
        int w = (int) (rectF.width() / getAllScale());
        int h = (int) (rectF.height() / getAllScale());
        float nw = w * 1f / getWidth();
        float nh = h * 1f / getHeight();
        float scale;
        float tx, ty;
        if (nw > nh) {
            scale = 1 / nw;
        } else {
            scale = 1 / nh;
        }

        int pivotX = mBitmap.getWidth() / 2;
        int pivotY = mBitmap.getHeight() / 2;

        mTransX = mTransY = 0;
        mRotateTranX = mRotateTranY = 0;
        this.mScale = 1;
        mRotateScale = 1;
        float touchX = toTouchX(pivotX);
        float touchY = toTouchY(pivotY);
        mRotateScale = scale / mCenterScale;

        // 확대후, 일정한 점의 주위를 확대한 효과를 만들기 위하여 화상이동
        tx = toTransX(touchX, pivotX);
        ty = toTransY(touchY, pivotY);

        mRotateTranX = tx;
        mRotateTranY = ty;

        refreshWithBackground();
    }

    public boolean isOptimizeDrawing() {
        return mOptimizeDrawing;
    }

    /**
     * item을 {@link #mItemStackOnViewCanvas} 목록에 추가하고 {@link ForegroundView} 갱신
     *
     * @param item
     */
    public void markItemToOptimizeDrawing(IDoodleItem item) {
        if (!mOptimizeDrawing) {
            return;
        }

        if (mItemStackOnViewCanvas.contains(item)) {
            throw new RuntimeException("The item has been added");
        }

        mItemStackOnViewCanvas.add(item);

        if (mItemStack.contains(item)) {
            addFlag(FLAG_RESET_BACKGROUND);
        }

        refresh();
    }

    /**
     * 그리기가 끝났으므로 {@link #mItemStackOnViewCanvas}에서 삭제하고 {@link #addItem(IDoodleItem)} 호출
     *
     * @param item
     */
    public void notifyItemFinishedDrawing(IDoodleItem item) {
        if (!mOptimizeDrawing) {
            return;
        }

        if (mItemStackOnViewCanvas.remove(item)) {
            if (mItemStack.contains(item)) {
                addFlag(FLAG_RESET_BACKGROUND);
            } else {
                addItem(item);
            }
        }

        refresh();
    }

    /**
     * 화상을 보관한다.
     */
    @SuppressLint("StaticFieldLeak")
    @Override
    public void save() {
        if (mIsSaving) {
            return;
        }

        mIsSaving = true;

        new AsyncTask<Void, Void, Bitmap>() {

            @SuppressLint("WrongThread")
            @Override
            protected Bitmap doInBackground(Void... voids) {
                Bitmap savedBitmap;

                if (mOptimizeDrawing) {
                    refreshDoodleBitmap(true);
                    savedBitmap = mDoodleBitmap;
                } else {
                    savedBitmap = mBitmap.copy(mBitmap.getConfig(), true);
                    Canvas canvas = new Canvas(savedBitmap);
                    for (IDoodleItem item : mItemStack) {
                        item.draw(canvas);
                    }
                }

                savedBitmap = ImageUtils.rotate(savedBitmap, mDoodleRotateDegree, true);
                return savedBitmap;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                mDoodleListener.onSaved(DoodleView.this, bitmap, new Runnable() {
                    @Override
                    public void run() {
                        mIsSaving = false;
                        if (mOptimizeDrawing) {
                            refreshDoodleBitmap(false);
                        }
                        refresh();
                    }
                });
            }
        }.execute();
    }

    /**
     * 화면 지우기
     */
    @Override
    public void clear() {
        List<IDoodleItem> temp = new ArrayList<>(mItemStack);
        mItemStack.clear();
        mRedoItemStack.clear();
        mItemStackOnViewCanvas.clear();
        mPendingItemsDrawToBitmap.clear();

        for (int i = temp.size() - 1; i >= 0; i--) {
            IDoodleItem item = temp.get(i);
            item.onRemove();
        }

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    /**
     * 그리기 취소하기
     * @param step 취소하려는 회수
     */
    @Override
    public void undo(int step) {
        step = Math.min(mItemStack.size(), step);
        if (step == 0) return;

        List<IDoodleItem> list = new ArrayList<IDoodleItem>(mItemStack.subList(mItemStack.size() - step, mItemStack.size()));
        for (IDoodleItem item : list) {
            removeItem(item);
            mRedoItemStack.add(0, item);
        }
    }

    /**
     * 그리기 다시하기
     * @param step 다시 그리려는 회수
     */
    @Override
    public void redo(int step) {
        step = Math.min(step, mRedoItemStack.size());
        if (step == 0 ) return;

        for (int i = 0; i < step; i++) {
            addItemInner(mRedoItemStack.remove(0));
        }
    }

    /**
     * 그리기 취소하기
     */
    @Override
    public void undo() {
        undo(1);
    }
    /**
     * 그리기 다시하기
     */
    public void redo() {
        redo(1);
    }

    /**
     * 원본화상만 그리기 여부 설정
     *
     * @param justDrawOriginal 원본그리기 여부
     */
    @Override
    public void setShowOriginal(boolean justDrawOriginal) {
        isJustDrawOriginal = justDrawOriginal;
        refreshWithBackground();
    }
    /**
     * @return  원본화상 그리기인가 여부 설정
     */
    @Override
    public boolean isShowOriginal() {
        return isJustDrawOriginal;
    }

    /**
     * 색갈 설정
     *
     * @param color 그리려는 색갈
     */
    @Override
    public void setColor(IDoodleColor color) {
        mColor = color;
        refresh();
    }

    @Override
    public IDoodleColor getColor() {
        return mColor;
    }

    /**
     * 특정 지점 주변 확대.<br/>
     * 화상의 실제 확대/축소 비율은 mCenterScale * mScale입니다.
     *
     * @param scale 비률
     * @param pivotX 확대축소 중심 x
     * @param pivotY 확대축소 중심 y
     */
    @Override
    public void setDoodleScale(float scale, float pivotX, float pivotY) {
        if (scale < mMinScale) {
            scale = mMinScale;
        } else if (scale > mMaxScale) {
            scale = mMaxScale;
        }

        float touchX = toTouchX(pivotX);
        float touchY = toTouchY(pivotY);
        this.mScale = scale;

        // 확대/축소 후 화상을 움직여 특정 지점 주변을 확대/축소하는 효과를 냅니다.
        mTransX = toTransX(touchX, pivotX);
        mTransY = toTransY(touchY, pivotY);

        addFlag(FLAG_REFRESH_BACKGROUND);
        refresh();
    }

    @Override
    public float getDoodleScale() {
        return mScale;
    }

    /**
     * pen 설정
     *
     * @param pen 설정하려는 DoodlePen
     */
    @Override
    public void setPen(IDoodlePen pen) {
        if (pen == null) {
            throw new RuntimeException("Pen can't be null");
        }
        mPen = pen;
        refresh();
    }

    @Override
    public IDoodlePen getPen() {
        return mPen;
    }

    /**
     * shape 설정
     *
     * @param shape 설정하려는 DoodleShape
     */
    @Override
    public void setShape(IDoodleShape shape) {
        if (shape == null) {
            throw new RuntimeException("Shape can't be null");
        }
        mShape = shape;
        refresh();
    }

    @Override
    public IDoodleShape getShape() {
        return mShape;
    }

    @Override
    public void setDoodleTranslation(float transX, float transY) {
        mTransX = transX;
        mTransY = transY;
        refreshWithBackground();
    }

    /**
     * 화상의 x 이동편차 설정
     * @param transX 설정하려는 x편차
     */
    @Override
    public void setDoodleTranslationX(float transX) {
        this.mTransX = transX;
        refreshWithBackground();
    }
    /**
     * 화상의 x 이동편차 얻기
     */
    @Override
    public float getDoodleTranslationX() {
        return mTransX;
    }

    /**
     * 화상의 y 이동편차 설정
     * @param transY 설정하려는 y편차
     */
    @Override
    public void setDoodleTranslationY(float transY) {
        this.mTransY = transY;
        refreshWithBackground();
    }

    /**
     * 화상의 y 이동편차 얻기
     */
    @Override
    public float getDoodleTranslationY() {
        return mTransY;
    }


    @Override
    public void setSize(float paintSize) {
        mSize = paintSize;
        refresh();
    }

    @Override
    public float getSize() {
        return mSize;
    }

    /**
     * 화상령역 외부에 item을 그릴지 여부 설정
     *
     * @param isDrawableOutside 그리기 여부
     */
    @Override
    public void setIsDrawableOutside(boolean isDrawableOutside) {
        mIsDrawableOutside = isDrawableOutside;
    }

    /**
     * 화상령역 외부에 item을 그릴지 여부 얻기
     */
    @Override
    public boolean isDrawableOutside() {
        return mIsDrawableOutside;
    }

    /**
     * 확대경 배률을 설정하십시오. 0보다 작거나 같으면 확대경 기능이 사용되지 않습니다.
     *
     * @param scale 설정하려는 확대경 배률
     */
    @Override
    public void setZoomerScale(float scale) {
        mZoomerScale = scale;
        refresh();
    }

    /**
     * @return 확대경 배률을 얻는다.
     */
    @Override
    public float getZoomerScale() {
        return mZoomerScale;
    }

    /**
     * 확대경 활성 여부 설정
     *
     * @param enable 활성 여부
     */
    public void enableZoomer(boolean enable) {
        mEnableZoomer = enable;
    }

    /**
     * 확대경 활성 여부 얻기
     */
    public boolean isEnableZoomer() {
        return mEnableZoomer;
    }

    /**
     * touch하여 끌기가 진행중이라는것을 설정하며 ForegroundView를 갱신한다.<br/>
     * 확대경현시를 하는데 리용한다
     */
    public void setScrollingDoodle(boolean scrollingDoodle) {
        mIsScrollingDoodle = scrollingDoodle;
        refresh();
    }

    /**
     * item을 제일 앞으로 내온다. (z order)
     * @param item 내오려는 item
     */
    @Override
    public void topItem(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }

        mItemStack.remove(item);
        mItemStack.add(item);

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    /**
     * item을 제일 뒤로 가져간다. (z order)
     * @param item 뒤로 가는 item
     */
    @Override
    public void bottomItem(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }

        mItemStack.remove(item);
        mItemStack.add(0, item);

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    @Override
    public void setDoodleMinScale(float minScale) {
        mMinScale = minScale;
        setDoodleScale(mScale, 0, 0);
    }

    @Override
    public float getDoodleMinScale() {
        return mMinScale;
    }

    @Override
    public void setDoodleMaxScale(float maxScale) {
        mMaxScale = maxScale;
        setDoodleScale(mScale, 0, 0);
    }

    @Override
    public float getDoodleMaxScale() {
        return mMaxScale;
    }

    @Override
    public float getUnitSize() {
        return mDoodleSizeUnit;
    }

    /**
     * {@link #mItemStack}에 새 item 추가
     * @param item 추가되는 item
     */
    @Override
    public void addItem(IDoodleItem item) {
        addItemInner(item);
        mRedoItemStack.clear();
    }

    /**
     * {@link #mItemStack}에 새 item 추가
     * @param item 추가되는 item
     */
    private void addItemInner(IDoodleItem item) {
        if (item == null) {
            throw new RuntimeException("item is null");
        }

        if (this != item.getDoodle()) {
            throw new RuntimeException("the object Doodle is illegal");
        }
        if (mItemStack.contains(item)) {
            throw new RuntimeException("the item has been added");
        }

        mItemStack.add(item);
        // callback 호출
        item.onAdd();

        // mDoodleBitmapCanvas에 그려주기 위하여 추가
        mPendingItemsDrawToBitmap.add(item);
        addFlag(FLAG_DRAW_PENDINGS_TO_BACKGROUND);

        refresh();
    }

    /**
     * item을 삭제하고 다시 그리기
     * @param doodleItem 삭제되는 item
     */
    @Override
    public void removeItem(IDoodleItem doodleItem) {
        if (!mItemStack.remove(doodleItem)) {
            return;
        }

        mItemStackOnViewCanvas.remove(doodleItem);
        mPendingItemsDrawToBitmap.remove(doodleItem);
        doodleItem.onRemove();

        addFlag(FLAG_RESET_BACKGROUND);

        refresh();
    }

    /**
     * @return 모든 item개수 얻기 (redo item들은 제외)
     */
    @Override
    public int getItemCount() {
        return mItemStack.size();
    }

    /**
     * @return 모든 item 얻기 (redo item들은 제외)
     */
    @Override
    public List<IDoodleItem> getAllItem() {
        return new ArrayList<>(mItemStack);
    }

    /**
     * @return 모든 redo item개수 얻기
     */
    @Override
    public int getRedoItemCount() {
        return mRedoItemStack.size();
    }

    /**
     * @return 모든 redo item 얻기
     */
    @Override
    public List<IDoodleItem> getAllRedoItem() {
        return new ArrayList<>(mRedoItemStack);
    }
    /**
     * @return 현재 화상 얻기 (item 없음)
     */
    @Override
    public Bitmap getBitmap() {
        return mBitmap;
    }
    /**
     * @return 현재 화상 얻기 (item 포함)
     */
    @Override
    public Bitmap getDoodleBitmap() {
        return mDoodleBitmap;
    }

    /**
     * doodle에 새로운 화상 설정
     * @param bitmap - 설정하려는 새 bitmap
     */
    public void setNewBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public int getCenterWidth() {
        return mCenterWidth;
    }

    public int getCenterHeight() {
        return mCenterHeight;
    }

    public float getCenterScale() {
        return mCenterScale;
    }

    public float getCentreTranX() {
        return mCentreTranX;
    }

    public float getCentreTranY() {
        return mCentreTranY;
    }

    public float getRotateScale() {
        return mRotateScale;
    }

    public float getRotateTranX() {
        return mRotateTranX;
    }

    public float getRotateTranY() {
        return mRotateTranY;
    }

    /**
     * @return 편집방식인가를 나타낸다
     */
    public boolean isEditMode() {
        return mIsEditMode;
    }

    /**
     * 편집방식을 설정할수있다
     * @param editMode true이면 편집방식 설정, false이면 아님
     */
    public void setEditMode(boolean editMode) {
        mIsEditMode = editMode;
        refresh();
    }

    /**
     * 배경층, 배경이 바뀔때만 그려진다.<br/>
     * 원본화상을 그릴때, 그리고 최적화설정시에 선택이 되지않은 item들을 그릴때 리용된다.
     */
    private class BackgroundView extends View {

        public BackgroundView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (LogUtil.sIsLog) {
                LogUtil.d(TAG, "BackgroundView>>onDraw");
            }
            int count = canvas.save();
            canvas.rotate(mDoodleRotateDegree, getWidth() / 2, getHeight() / 2);
            doDraw(canvas);
            canvas.restoreToCount(count);
        }

        private void doDraw(Canvas canvas) {
            float left = getAllTranX();
            float top = getAllTranY();

            // canvas와 그림은 동일한 좌표계를 공유하며 View좌표계와 그림(canvas) 좌표계간의 관계만 처리하면됩니다.
            canvas.translate(left, top);
            float scale = getAllScale();
            canvas.scale(scale, scale);

            if (isJustDrawOriginal) { // 원본화상만 그리기
                canvas.drawBitmap(mBitmap, 0, 0, null);
                return;
            }

            Bitmap bitmap = mOptimizeDrawing ? mDoodleBitmap : mBitmap;

            // bitmap: item들을 그린 그림
            canvas.drawBitmap(bitmap, 0, 0, null);
        }

    }

    /**
     * refresh()할때마다 그려지는 전경층.
     * 새로 생성되거나 선택된 item을 그리는데 리용됩니다.
     */
    private class ForegroundView extends View {
        public ForegroundView(Context context) {
            super(context);

            // 하드웨어 가속을 끄십시오. 일부 그리기 작업은 하드웨어 가속을 지원하지 않습니다.
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (mDefaultTouchDetector != null) {
                return mDefaultTouchDetector.onTouchEvent(event);
            }
            return false;
        }

        protected void onDraw(Canvas canvas) {
            int count = canvas.save();
            canvas.rotate(mDoodleRotateDegree, getWidth() / 2, getHeight() / 2);
            doDraw(canvas);
            canvas.restoreToCount(count);
        }

        private void doDraw(Canvas canvas) {
            if (isJustDrawOriginal) { // 원본화상만 그리기
                return;
            }

            float left = getAllTranX();
            float top = getAllTranY();

            canvas.translate(left, top);
            float scale = getAllScale();
            canvas.scale(scale, scale);

            Bitmap bitmap = mOptimizeDrawing ? mDoodleBitmap : mBitmap;

            int saveCount = canvas.save(); // 1
            List<IDoodleItem> items = mItemStack;
            if (mOptimizeDrawing) {
                // 그리기중이거나 편집중인 item들
                items = mItemStackOnViewCanvas;
            }
            boolean canvasClipped = false;
            if (!mIsDrawableOutside) { // 그림령역으로 자르기
                canvasClipped = true;
                canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            }
            for (IDoodleItem item : items) {
                if (!item.isNeedClipOutside()) {
                    if (canvasClipped) {
                        canvas.restore();
                    }

                    item.draw(canvas);

                    if (canvasClipped) {
                        canvas.save();
                        canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    }
                } else {
                    item.draw(canvas);
                }
            }

            // 맨우에 그려주기
            for (IDoodleItem item : items) {
                if (!item.isNeedClipOutside()) {
                    if (canvasClipped) {
                        canvas.restore();
                    }
                    item.drawAtTheTop(canvas);

                    if (canvasClipped) {
                        canvas.save();
                        canvas.clipRect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    }
                } else {
                    item.drawAtTheTop(canvas);
                }
            }
            canvas.restoreToCount(saveCount);
        }
    }
}
