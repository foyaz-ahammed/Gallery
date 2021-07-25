package com.kr.gallery.pro.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.ColorInt;

import com.kr.gallery.pro.R;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class HorizontalProgressWheelView extends View {

    private final Rect mCanvasClipBounds = new Rect();

    private ScrollingListener mScrollingListener;
    private float mLastTouchedPosition;

    private Paint mTickPaint;
    private Paint mProgressLinePaint;
    private Paint mProgressMiddleLinePaint;
    private int mProgressLineWidth, mProgressLineHeight;
    private int mProgressLineMargin;

    private boolean mScrollStarted;
    private float mTotalScrollDistance;

    private int mMiddleLineColor;
    private final Rect tickBound = new Rect();

    public HorizontalProgressWheelView(Context context) {
        this(context, null);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HorizontalProgressWheelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setScrollingListener(ScrollingListener scrollingListener) {
        mScrollingListener = scrollingListener;
    }

    public void setMiddleLineColor(@ColorInt int middleLineColor) {
        mMiddleLineColor = middleLineColor;
        mProgressMiddleLinePaint.setColor(mMiddleLineColor);
        invalidate();
    }

    public void setValue( float value ) {
        mTotalScrollDistance = value * (mProgressLineWidth + mProgressLineMargin);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchedPosition = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                if (mScrollingListener != null) {
                    mScrollStarted = false;
                    mScrollingListener.onScrollEnd();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - mLastTouchedPosition;
                if (distance != 0) {
                    if (!mScrollStarted) {
                        mScrollStarted = true;
                        if (mScrollingListener != null) {
                            mScrollingListener.onScrollStart();
                        }
                    }
                    onScrollEvent(event, distance);
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.getClipBounds(mCanvasClipBounds);

        for (int j = 0; j < 80; j++) {
            // 중심으로부터 오른쪽 방향으로 눈금 그리기
            float tickX = mCanvasClipBounds.centerX() + j * (mProgressLineWidth + mProgressLineMargin) - mTotalScrollDistance;
            float tickYTop = mCanvasClipBounds.centerY() - mProgressLineHeight / 4.0f;
            float tickYBottom = mCanvasClipBounds.centerY() + mProgressLineHeight / 4.0f;
            canvas.drawLine(
                    tickX, tickYTop,
                    tickX, tickYBottom, mProgressLinePaint);

            // 중심으로부터 왼쪽 방향으로 눈금 그리기
            tickX = mCanvasClipBounds.centerX() - j * (mProgressLineWidth + mProgressLineMargin) - mTotalScrollDistance;
            canvas.drawLine(
                    tickX, tickYTop,
                    tickX, tickYBottom, mProgressLinePaint);

            if (j % 10 == 0) {
                // 큰 눈금 및 수자 그리기
                // positive ticks
                String tickStr = Integer.toString(j);
                mTickPaint.getTextBounds(tickStr, 0, tickStr.length(), tickBound);
                float tickWidth = tickBound.width();
                canvas.drawText(
                        tickStr,
                        mCanvasClipBounds.centerX() + j * (mProgressLineWidth + mProgressLineMargin) - tickWidth / 2 - mTotalScrollDistance,
                        mCanvasClipBounds.centerY() - mProgressLineHeight,
                        mTickPaint);
                tickX = mCanvasClipBounds.centerX() + j * (mProgressLineWidth + mProgressLineMargin) - mTotalScrollDistance;
                tickYTop = mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f;
                canvas.drawLine(
                        tickX, tickYTop,
                        tickX, tickYBottom, mProgressLinePaint);

                // negative ticks
                if (j > 0) {
                    tickStr = "-" + j;
                    mTickPaint.getTextBounds(tickStr, 0, tickStr.length(), tickBound);
                    tickWidth = tickBound.width();
                    canvas.drawText(
                            tickStr,
                            mCanvasClipBounds.centerX() - j * (mProgressLineWidth + mProgressLineMargin) - tickWidth / 2 - mTotalScrollDistance,
                            mCanvasClipBounds.centerY() - mProgressLineHeight,
                            mTickPaint);
                    tickX = mCanvasClipBounds.centerX() - j * (mProgressLineWidth + mProgressLineMargin) - mTotalScrollDistance;
                    tickYTop = mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f;
                    canvas.drawLine(
                            tickX, tickYTop,
                            tickX, tickYBottom, mProgressLinePaint);
                }
            }
        }

        canvas.drawLine(mCanvasClipBounds.centerX(), mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f, mCanvasClipBounds.centerX(), mCanvasClipBounds.centerY() + mProgressLineHeight / 2.0f, mProgressMiddleLinePaint);

    }

    private void onScrollEvent(MotionEvent event, float distance) {
        int oneTickWidth = mProgressLineWidth + mProgressLineMargin;

        if (mTotalScrollDistance - distance < 45 * oneTickWidth && mTotalScrollDistance - distance > -45 * oneTickWidth) {
            mTotalScrollDistance -= distance;
            postInvalidate();
            mLastTouchedPosition = event.getX();
            if (mScrollingListener != null) {
                mScrollingListener.onScroll(-distance, mTotalScrollDistance);
            }
        } else  {
            float end = mTotalScrollDistance > 0 ? 45 * oneTickWidth : -45 * oneTickWidth;
            distance = end - mTotalScrollDistance;
            mTotalScrollDistance = end;
            if (mScrollingListener != null) {
                mScrollingListener.onScroll(distance, mTotalScrollDistance);
            }
        }
    }

    public void reset() {
        float distance = mTotalScrollDistance;
        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        CustomValueAnimator animator = new CustomValueAnimator(interpolator);
        animator.addAnimatorListener(new SimpleValueAnimatorListener(){
            @Override
            public void onAnimationFinished() { }

            @Override
            public void onAnimationUpdated(float scale) {
                mTotalScrollDistance = distance - distance * scale;
                invalidate();
            }

            @Override
            public void onAnimationStarted() { }
        });
        animator.startAnimation(150);
    }

    private void init() {
        mMiddleLineColor = Color.WHITE;

        mProgressLineWidth = getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_width_horizontal_wheel_progress_line);
        mProgressLineHeight = getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_height_horizontal_wheel_progress_line);
        mProgressLineMargin = getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_margin_horizontal_wheel_progress_line);

        mProgressLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mProgressLinePaint.setStyle(Paint.Style.STROKE);
        mProgressLinePaint.setStrokeWidth(mProgressLineWidth);
        mProgressLinePaint.setColor(Color.WHITE);

        mProgressMiddleLinePaint = new Paint(mProgressLinePaint);
        mProgressMiddleLinePaint.setColor(mMiddleLineColor);
        mProgressMiddleLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressMiddleLinePaint.setStrokeWidth(getContext().getResources().getDimensionPixelSize(R.dimen.ucrop_width_middle_wheel_progress_line));

        mTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTickPaint.setStyle(Paint.Style.FILL);
        mTickPaint.setTextSize(34);
        mTickPaint.setColor(Color.WHITE);
    }

    public interface ScrollingListener {

        void onScrollStart();

        void onScroll(float delta, float totalDistance);

        void onScrollEnd();
    }

}
