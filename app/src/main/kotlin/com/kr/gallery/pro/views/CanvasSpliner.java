package com.kr.gallery.pro.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Adjust의 Curve를 위한 View
 */
public class CanvasSpliner extends View {

    public CanvasSpliner(Context context) {
        super(context);
    }

    public CanvasSpliner(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    // 조종점들의 반경
    int controlPointRadius = 30;

    // 곡선색갈
    String curveColorIdle = "#ffffff";

    // 격자색갈
    String gridColor = "#aaaaaa";

    // 곡선 두께
    int curveThickness = 5;
    // 격자 두께
    int gridThickness = 3;

    // 배경색
    String backgroundColor = "#00000000";

    // Touch사건이 있을때 계산된 위치
    PointF mousePos = new PointF();

    // 선택된 조종점의 index
    int pointGrabbedIndex = -1;

    // 전체너비에 대한 격자줄의 너비비률을 나타낸다
    float gridStep = 1/3f;

    int mWidth = 0;
    int mHeight = 0;

    PointCollection pointCollection = null;

    float screenRatio = 1f;

    Paint bgPaint = null;
    Paint gridPaint = null;
    Paint curvePaint = null;
    Paint controlPaint = null;

    GestureDetector gestureDetector;

    boolean initialized = false;

    int paddingLeft = 10;
    int paddingRight = 10;
    int paddingTop = 10;
    int paddingBottom = 10;

    int padding = 10;
    public void initCanvasSpliner(int width, int height) {

        paddingLeft = dpToPx(padding);
        paddingRight = dpToPx(padding);
        paddingTop = dpToPx(padding);
        paddingBottom = dpToPx(padding);

        mWidth = width - paddingLeft - paddingRight;
        mHeight = height - paddingTop - paddingBottom;

        // the point collection
        pointCollection = new PointCollection();
        pointCollection.setMax (mWidth, mHeight);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor(backgroundColor));
        bgPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor(gridColor));
        gridPaint.setStrokeWidth(gridThickness);
        gridPaint.setStyle(Paint.Style.STROKE);

        curvePaint = new Paint();
        curvePaint.setColor(Color.parseColor(curveColorIdle));
        curvePaint.setStrokeWidth(curveThickness);
        curvePaint.setStyle(Paint.Style.STROKE);

        controlPaint = new Paint();
        controlPaint.setColor(Color.parseColor("#ffffff"));
        controlPaint.setStyle(Paint.Style.FILL);

        gestureDetector = new GestureDetector(getContext(), new GestureListener());

        invalidate();

        initialized = true;
    }

    private int dpToPx( int dp) {
        return (int) (dp *Resources.getSystem().getDisplayMetrics().density);
    }

    public int getSplinerWidth(){
        return mWidth;
    }

    public int getSplinerHeight() {
        return mHeight;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * canvas 자리표에 기초한 마우스자리표 갱샌
     */
    public void updateMousePosition(MotionEvent evt) {
        mousePos.x = evt.getX() - paddingLeft;
        mousePos.y = mHeight - evt.getY() + paddingTop;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        this.fillBackground(canvas);
        this.drawGrid(canvas);
        this.drawData(canvas);
    }

    boolean mouseDown = false;

    public interface PointMoveListener {
        void onCurvePointUpdate(List<PointF> canvasPoints, List<PointF> curvePoints);
        void onPointUp();
    }

    private PointMoveListener mPointListener;
    public void setPointUpdateListener(PointMoveListener listener ) {
        mPointListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                mouseDown = false;
                updateMousePosition(event);

                if(pointGrabbedIndex != -1) {
                    // 선택된 조종점이 있다. 선택된 조종점 자리표를 마우스위치로 갱신
                    pointGrabbedIndex = pointCollection.updatePoint( pointGrabbedIndex, mousePos);
                    // 갱신 listener에 전달
                    if (mPointListener != null) mPointListener.onCurvePointUpdate(pointCollection.getCanvasPoints(), pointCollection.getCurvePoints(mWidth, mHeight));
                }

                // 조종점이 선택되여 움직일때만 재그리기
                if( pointGrabbedIndex != -1){
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_DOWN:
                mouseDown = true;
                updateMousePosition(event);

                // 마우스위치에서 제일 가까운 조종점의 정보 얻기
                ClosestInfo closestPointInfo = pointCollection.getClosestFrom(mousePos);

                if(closestPointInfo == null)
                    return false;

                // 선택한 조종점이 없을때 조종점 선택검사하여 선택하기
                if(pointGrabbedIndex == -1 && closestPointInfo.getDistance() <= controlPointRadius){
                    pointGrabbedIndex = closestPointInfo.getIndex();
                } else {
                    invalidate();   // check if clicked on line
                }
                break;
            case MotionEvent.ACTION_UP:
                mouseDown = false;
                if (pointGrabbedIndex != -1 && mPointListener != null) mPointListener.onPointUp();
                pointGrabbedIndex = -1;
                invalidate();

                break;
        }

        return gestureDetector.onTouchEvent(event);
    }

    /**
     * 새로운 조종점 추가
     * @param pt 추가 위치
     */
    public int add(PointF pt){
        int index;

        pt.x *= this.mWidth;
        pt.y *= this.mHeight;
        index = this.pointCollection.add( pt );

        invalidate();
        return index;
    }

    /**
     * 조종점들을 삭제하고 초기화 한다
     */
    public void resetPoints() {
        this.pointCollection.removeAll();

        this.pointCollection.add( new PointF(1f, 0f));
        this.pointCollection.add( new PointF(this.mWidth, this.mHeight));
        invalidate();
    }

    /**
     * 조종점들을 교체한다
     * @param points  설정하려는 조종점들
     */
    public void replacePoints(List<PointF> points) {
        this.pointCollection.removeAll();
        this.pointCollection.points = new ArrayList<>(points);
        invalidate();
    }

    /**
     * 배경색갈을 그린다
     */
    public void fillBackground(Canvas canvas){
        canvas.drawRect(paddingLeft, paddingTop, mWidth + paddingLeft, mHeight + paddingTop, bgPaint);
    }

    /**
     * 배경격자 그리기
     */
    public  void drawGrid(Canvas canvas){
        float step = this.gridStep;

        if( step == 0)
            return;

        Path path = new Path();

        // 수평선 자리길들
        path.moveTo(paddingLeft, paddingTop);

        for(float i=step*mHeight/this.screenRatio; i<mHeight/this.screenRatio; i += step*mHeight/this.screenRatio){
            path.moveTo(paddingLeft, Math.round(i) + 0.5f/this.screenRatio + paddingTop);
            path.lineTo( paddingLeft + mWidth, Math.round(i) + 0.5f/this.screenRatio + paddingTop);
        }

        // 수직선 자리길들
        path.moveTo(paddingLeft, paddingTop);
        for(float i=step*mWidth/this.screenRatio; i<mWidth/this.screenRatio; i += step*mWidth/this.screenRatio){
            path.moveTo(paddingLeft + Math.round(i) + 0.5f/this.screenRatio, paddingTop);
            path.lineTo(paddingLeft + Math.round(i) + 0.5f/this.screenRatio , mHeight + paddingTop );
        }

        // 테두리 경계 자리길들
        path.moveTo(paddingLeft, mHeight + paddingTop);
        path.lineTo(paddingLeft, paddingTop);
        path.lineTo(mWidth + paddingLeft, paddingTop);
        path.lineTo(paddingLeft, mHeight + paddingTop);
        path.lineTo(mWidth + paddingLeft, mHeight + paddingTop);
        path.lineTo(mWidth + paddingLeft, paddingTop);

        canvas.drawPath(path, gridPaint);
    }


    /**
     * 곡선과 조종점들을 그린다
     */
    public void drawData(Canvas canvas){

        List<Float> xSeries = pointCollection.getXseries();
        List<Float> ySeries = pointCollection.getYseries();
        CubicSplineInterpolator splineInterpolator = CubicSplineInterpolator.createMonotoneCubicSpline(xSeries, ySeries);
        int w = mWidth;
        int h = mHeight;

        if(xSeries.size() == 0)
            return;

        // 곡선자리길 만들기
        Path path = new Path();

        path.moveTo(xSeries.get(0) / screenRatio + paddingLeft, (h - ySeries.get(0)) / screenRatio + paddingTop);

        // 첫 조종점의 왼쪽부분 자리길
        for(int x = 0; x< Math.ceil(xSeries.get(0)); x++){
            float y = ySeries.get(0);
            y = y < 0 ? 0.5f : y > h ? h - 0.5f : y;
            path.lineTo(paddingLeft + x/screenRatio, (h - y)/screenRatio + paddingTop);
        }

        // 첫 조종점과 마지막 조종점 사이
        for(int x = (int) Math.ceil(xSeries.get(0)); x< Math.ceil(xSeries.get(xSeries.size() - 1)); x++){
            float y = splineInterpolator.interpolate(x);

            y = y < 0 ? 0.5f : y > h ? h - 0.5f : y;
            path.lineTo(paddingLeft + x/screenRatio, (h - y)/screenRatio + paddingTop);

            if (mouseDown) {
                // 곡선에 매우 가까운 점을 선택하면 새로운 조종점 추가
                // 곡선우의 점 자리표
                PointF linePos = new PointF(x + paddingLeft, y + paddingTop);
                float distance = distanceTwoPointF(linePos, mousePos);
                if (distance < 30) {
                    // 선택한 위치가 이미 존재하는 조종점의 가까이에 있으면 새 조종점을 추가하지 않는다
                    ClosestInfo closestPointInfo = pointCollection.getClosestFrom(mousePos);
                    if (closestPointInfo.getDistance() > 100) {
                        mouseDown = false;  // block not call again
                        pointGrabbedIndex = pointCollection.add(linePos);
                    }
                }
            }
        }

        // 마지막 조종점의 오른쪽부분 자리길
        for(int x = (int) Math.ceil(xSeries.get(xSeries.size() - 1)); x<w; x++){
            float y = ySeries.get(ySeries.size() - 1);
            y = y < 0 ? 0.5f : y > h ? h - 0.5f : y;
            path.lineTo(paddingLeft + x/screenRatio, (h - y)/screenRatio + paddingTop);
        }

        curvePaint.setColor(Color.parseColor(curveColorIdle));

        canvas.drawPath(path, curvePaint);

        // 조종점들을 그리기
        for(int i=0; i<xSeries.size(); i++){

            float centerX = xSeries.get(i)/screenRatio + paddingLeft;
            float centerY = (h - ySeries.get(i)) / screenRatio + paddingTop;
            float radius = controlPointRadius/screenRatio;

            float left = centerX - radius;
            float right = centerX + radius;
            float top = centerY - radius;
            float bottom = centerY + radius;

            canvas.drawArc(left, top, right, bottom, 0, 360f, false, controlPaint);
        }

        // release splineInterpolator member elements
        splineInterpolator.release();
        System.gc();
    }

    /**
     * 두 점사이 거리를 계산하여 반환한다.
     * @param p1 첫번째 점의 자리표
     * @param p2 두번째 점의 자리표
     * @return 두점사이 거리
     */
    private float distanceTwoPointF(PointF p1, PointF p2) {
        return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 조종점을 련속두번 누르기하면 삭제하기. 최소 2개의 조종점은 남겨야 한다

            float x = e.getX();
            float y = e.getY();

            for (int i = 0; i < pointCollection.getNumberOfPoints() && pointCollection.getNumberOfPoints() > 2; i++){
                PointF point = pointCollection.getPoint(i);

                float distance = distanceTwoPointF(new PointF(x - paddingLeft, paddingTop + mHeight - y), point);
                if (distance < 30) {
                    pointCollection.remove(i);
                    pointGrabbedIndex = -1;
                    if (mPointListener != null) {
                        mPointListener.onCurvePointUpdate(pointCollection.getCanvasPoints(), pointCollection.getCurvePoints(mWidth, mHeight));
                        mPointListener.onPointUp();
                    }
                    break;
                }
            }

            return true;
        }
    }
}
