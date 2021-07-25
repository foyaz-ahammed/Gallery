package doodle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;

/**
 * Created on 27/06/2018.
 */

public interface IDoodleItem {

    public void setDoodle(IDoodle doodle);

    public IDoodle getDoodle();

    /**
     * pen 얻기
     *
     * @return
     */
    public IDoodlePen getPen();

    /**
     * pen 설정하기
     *
     * @param pen
     */
    public void setPen(IDoodlePen pen);

    /**
     * pen의 그리기모양 Bitmap 설정
     * @param brushBMP
     */
    public void setBrushBMP(Bitmap brushBMP);

    /**
     * pen의 그리기모양 Bitmap 얻기
     * @return
     */
    public Bitmap getBrushBMP();

    /**
     * pen의 그리기 모양 설정
     * @param style
     */
    public void setBrushStyle(int style);

    /**
     * pen의 그리기 모양 얻기
     * @return
     */
    public int getBrushStyle();

    /**
     * shape 모양 얻기
     *
     * @return
     */
    public IDoodleShape getShape();

    /**
     * shape 모양 설정
     *
     * @param shape
     */
    public void setShape(IDoodleShape shape);

    /**
     * 두께 얻기
     *
     * @return
     */
    public float getSize();

    /**
     * 두께 설정
     *
     * @param size
     */
    public void setSize(float size);

    /**
     * 색갈 얻기
     *
     * @return
     */
    public IDoodleColor getColor();

    /**
     * 색갈 설정
     *
     * @param color
     */
    public void setColor(IDoodleColor color);

    /**
     * item 그리기
     *
     * @param canvas
     */
    public void draw(Canvas canvas);

    /**
     * 모든 item우에 그리기
     * @param canvas
     */
    void drawAtTheTop(Canvas canvas);

    /**
     * 현재 item의 왼쪽상단을 설정
     *
     * @param x
     * @param y
     */
    public void setLocation(float x, float y, boolean changePivot);

    /**
     * 현재 item의 왼쪽상단좌표를 가져옵니다.
     */
    public PointF getLocation();

    /**
     * item의 중심 x 설정
     *
     * @param pivotX
     */
    public void setPivotX(float pivotX);

    /**
     * item의 중심 x 얻기
     */
    public float getPivotX();

    /**
     * item의 중심 y 설정
     *
     * @param pivotY
     */
    public void setPivotY(float pivotY);

    /**
     * item의 중심 y 얻기
     */
    public float getPivotY();

    /**
     * item의 회전값을 설정하고，중심점 Pivot를 중심으로 회전
     *
     * @param degree
     */
    public void setItemRotate(float degree);

    /**
     * item의 회전값 얻기
     *
     * @return
     */
    public float getItemRotate();

    /**
     * 화상령역밖의 부분을 잘라야 하는가의 여부 되돌리기
     * @return 자르기 여부를 되돌린다
     */
    public boolean isNeedClipOutside();

    /**
     * 화상령역밖의 부분을 잘라야 하는지 여부 설정
     *
     * @param clip
     */
    public void setNeedClipOutside(boolean clip);

    /**
     * item을 추가할때의 callback
     */
    public void onAdd();

    /**
     * item을 지울때의 callback
     */
    public void onRemove();

    /**
     * 갱신하기
     */
    public void refresh();

    /**
     * @return item을 편집 할수 있는지 여부입니다. 편집방식에서 item작업에 사용
     */
    public boolean isDoodleEditable();

    /**
     * 확대축소값 설정하기
     */
    public void setScale(float scale);

    /**
     * 확대축소값 얻기
     * @return
     */
    public float getScale();


    public void addItemListener(IDoodleItemListener listener);
    public void removeItemListener(IDoodleItemListener listener);
}
