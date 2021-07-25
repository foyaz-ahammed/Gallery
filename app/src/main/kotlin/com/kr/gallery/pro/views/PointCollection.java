package com.kr.gallery.pro.views;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Adjust의 Curve에서 리용되는 조종점들을 괸리힌다
 */
public class PointCollection {

    List<PointF> points = new ArrayList<>();

    PointF min = new PointF(0f, 0f);
    PointF max = new PointF(Float.MAX_VALUE, Float.MAX_VALUE);

    public PointCollection() {

    }

    /**
     * 점들의 최대 X, Y값 설정
     */
    public void setMax(int x, int y) {
        max.x = x;
        max.y = y;
    }

    /**
     * 새 점을 추가한다.
     * 추가후 X자리표로 졍렬 진행.
     * @param p  추가하려는 점의 자리표
     * @return  추가된 점의 정렬후 index
     */
    public int add(PointF p ){
        int newIndex = -1;

        if(p.x >= this.min.x && p.x <= this.max.x && p.y >= this.min.y && p.y <= this.max.y)
        {
            // adding the point
            this.points.add( p );
            this.sortPoints();
            newIndex = this.points.indexOf( p );
        }
        return newIndex;
    }

    /**
     * X자리표에 따라 점들을 정렬한다.
     */
    private void sortPoints(){
        // sorting the array upon x
        points.sort(new Comparator<PointF>() {
            public int compare(PointF p1, PointF p2) {
                return Float.compare(p1.x, p2.x);
            }
        });
    }

    /**
     * 파라메터로 들어온 index에 대한 점을 삭제한다.
     */
    public void remove(int index ){
        if(index >= 0 && index < this.points.size()){
            this.points.remove(index);
        }
    }

    /**
     * 모든 점들을 삭제한다.
     */
    public void removeAll() {
        this.points.clear();
    }

    /**
     * 조종점들중에서 파라메터로 들어온 자리표와 제일 가까운 조종점을 찾아 index와 사이거리를 되돌린다.
     * @return ClosestInfo : 조종점의 index와 사이거리
     */
    public ClosestInfo getClosestFrom(PointF p){

        if(this.points.size() == 0)
            return null;

        double closestDistance = Float.MAX_VALUE;
        int closestPointIndex = -1;

        for(int i=0; i<this.points.size(); i++){
            double d = Math.sqrt( Math.pow(p.x - this.points.get(i).x, 2) + Math.pow(p.y - this.points.get(i).y, 2) );

            if( d < closestDistance ){
                closestDistance = d;
                closestPointIndex = i;
            }
        }

        ClosestInfo obj = new ClosestInfo();
        obj.setIndex(closestPointIndex);
        obj.setDistance(closestDistance);

        return obj;
    }


    /**
     * 파라메터로 들어온 index에 대한 점의 자리표를 얻어 되돌린다.
     * @param index  얻으려는 점의 index
     * @return 점의 자리표
     */
    public PointF getPoint(int index ){
        if(index >= 0 && index < this.points.size()){
            return this.points.get(index);
        }else{
            return null;
        }
    }

    /**
     * 점들의 x, y 자리표들을 파라메터로 들어온 mWidth, mHeight로 나누어 되돌린다.
     * @param mWidth  canvas 너비
     * @param mHeight  canvas 높이
     */
    public List<PointF> getCurvePoints(int mWidth, int mHeight) {
        List<PointF> curvePoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            PointF point = points.get(i);
            PointF curvePointF = new PointF(point.x / mWidth, point.y / mHeight);
            curvePoints.add(curvePointF);
        }
        return curvePoints;
    }

    /**
     * 점들을 모두 되돌린다.
     */
    public List<PointF> getCanvasPoints() {
        return points;
    }

    /**
     * 점들의 개수를 되돌린다.
     */
    public int getNumberOfPoints(){
        return this.points.size();
    }

    /**
     * 조종점의 위치를 갱신한다. 다른 조종점들을 지나지는 못한다
     * @param index  갱신하려는 조종점의 index
     * @param p  조종점의 새로운 위치
     * @return 새로운 index를 반환한다.
     */
    public int updatePoint( int index, PointF p ){

        int newIndex = index;

        if(index >= 0 && index < this.points.size()){
            // 왼쪽/오른쪽 조종점들의 index
            int rightIndex = Math.min(index + 1, points.size() - 1 );
            int leftIndex = Math.max(index - 1, 0);

            // 왼쪽/오른쪽 조종점들의 근방에 닿는지 검사
            boolean passed = false;
            if ( index < points.size() - 1 && p.x >= points.get(rightIndex).x - 10) passed = true;
            if ( index > 0 && p.x <= points.get(leftIndex).x + 10) passed = true;

            if(!passed){
                // 닿으지 않으면 자리표 갱신
                this.points.get(index).x = Math.min(Math.max(p.x, this.min.x), this.max.x);
                this.points.get(index).y = Math.min(Math.max(p.y, this.min.y), this.max.y);

                PointF thePointInArray = this.points.get(index);

                // the point may have changed its index
                newIndex = this.points.indexOf( thePointInArray );
            }
        }

        return newIndex;
    }

    /**
     * 점들의 X 자리표들을 하나의 List에 추가하여 되돌린다
     * @return X 자리표들의 List형
     */
    public List<Float> getXseries(){
        List<Float> xSeries = new ArrayList<>();
        for(int i=0; i<this.points.size(); i++){
            xSeries.add(this.points.get(i).x);
        }
        return xSeries;
    }


    /**
     * 점들의 Y 자리표들을 하나의 List에 추가하여 되돌린다
     * @return Y 자리표들의 List형
     */
    public List<Float> getYseries(){
        List<Float> ySeries = new ArrayList<>();
        for(int i=0; i<this.points.size(); i++){
            ySeries.add( this.points.get(i).y );
        }
        return ySeries;
    }
}
