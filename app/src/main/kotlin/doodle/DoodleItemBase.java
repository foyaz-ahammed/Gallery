package doodle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 29/06/2018.
 */

public abstract class DoodleItemBase implements IDoodleItem, IDoodleItemListener {

    public static final float MIN_SCALE = 0.01f;
    public static final float MAX_SCALE = 100f;

    // item의 회전각도
    private float mItemRotate;
    private IDoodle mDoodle;
    // item의 왼쪽웃구석 좌표
    private PointF mLocation = new PointF();
    private IDoodlePen mPen; // 그리기 류형
    private IDoodleShape mShape; // 그리기 형태

    // draw의 모양 bitmap
    private Bitmap mBrushBMP;
    // draw의 형태를 나타낸다
    private int mBrushStyle = 0;

    // 그리기 두께
    private float mSize;
    private IDoodleColor mColor; // 색갈
    // 화상 령역밖의 부분을 자르는 여부
    private boolean mIsNeedClipOutside = true;
    // item의 중심 X, Y
    private float mPivotX, mPivotY;
    private float mScale = 1;
    // item이 생성 & 추가되였다는것을 나타내는 기발
    private boolean mHasAdded = false;

    private List<IDoodleItemListener> mItemListeners = new ArrayList<>();

    public DoodleItemBase(IDoodle doodle) {
        this(doodle, null);
    }

    public DoodleItemBase(IDoodle doodle, DoodlePaintAttrs attrs) {
        setDoodle(doodle);
        if (attrs != null) {
            mPen = attrs.pen();
            mShape = attrs.shape();
            mSize = attrs.size();
            mColor = attrs.color();
        }
    }

    @Override
    public void setDoodle(IDoodle doodle) {
        if (doodle != null && mDoodle != null) {
            throw new RuntimeException("item's doodle object is not null");
        }
        mDoodle = doodle;
        if (doodle == null) {
        }
    }

    @Override
    public IDoodle getDoodle() {
        return mDoodle;
    }

    /**
     * item의 중심 x 좌표 설정
     * @param pivotX 설정하려는 중심 x 좌표
     */
    @Override
    public void setPivotX(float pivotX) {
        mPivotX = pivotX;
        onPropertyChanged(PROPERTY_PIVOT_X);
    }

    /**
     * item의 중심 x 좌표 얻기
     * @return item의 중심 x 좌표를 돌려준다.
     */
    @Override
    public float getPivotX() {
        return mPivotX;
    }

    /**
     * item의 중심 y 좌표 설정
     * @param pivotY 설정하려는 중심 y 좌표
     */
    @Override
    public void setPivotY(float pivotY) {
        mPivotY = pivotY;
        onPropertyChanged(PROPERTY_PIVOT_Y);
    }

    /**
     * item의 중심 y 좌표 얻기
     * @return item의 중심 y 좌표를 돌려준다.
     */
    @Override
    public float getPivotY() {
        return mPivotY;
    }

    /**
     * item의 회전각도 설정
     * @param textRotate 설정하려는 회전각도
     */
    @Override
    public void setItemRotate(float textRotate) {
        mItemRotate = textRotate;
        onPropertyChanged(PROPERTY_ROTATE);
        refresh();
    }

    /**
     * @return item의 회전각도 반환
     */
    @Override
    public float getItemRotate() {
        return mItemRotate;
    }

    /**
     * item의 위치를 설정한다.
     *
     * @param x 설정위치의 x 자리표
     * @param y 설정위치의 y 자리표
     * @param changePivot 중심점의 위치도 따라 변경할지 여부
     */
    @Override
    public void setLocation(float x, float y, boolean changePivot) {
        float diffX = x - mLocation.x, diffY = y - mLocation.y;
        mLocation.x = x;
        mLocation.y = y;

        onPropertyChanged(PROPERTY_LOCATION);

        if (changePivot) {
            mPivotX = mPivotX + diffX;
            mPivotY = mPivotY + diffY;
            onPropertyChanged(PROPERTY_PIVOT_X);
            onPropertyChanged(PROPERTY_PIVOT_Y);
        }

        refresh();
    }

    /**
     * item의 위치 자리표를 돌려준다.
     * @return 왼쪽웃구석 자리표를 돌려준다.
     */
    @Override
    public PointF getLocation() {
        return mLocation;
    }

    @Override
    public IDoodlePen getPen() {
        return mPen;
    }

    @Override
    public void setPen(IDoodlePen pen) {
        mPen = pen;
        refresh();
    }

    @Override
    public IDoodleShape getShape() {
        return mShape;
    }

    @Override
    public void setShape(IDoodleShape shape) {
        mShape = shape;
        refresh();
    }

    @Override
    public float getSize() {
        return mSize;
    }

    @Override
    public void setSize(float size) {
        mSize = size;
        onPropertyChanged(PROPERTY_SIZE);
        refresh();
    }

    @Override
    public IDoodleColor getColor() {
        return mColor;
    }

    @Override
    public void setColor(IDoodleColor color) {
        mColor = color;
        onPropertyChanged(PROPERTY_COLOR);
        refresh();
    }

    /**
     * item 그리기
     * @param canvas
     */
    @Override
    public void draw(Canvas canvas) {
        drawBefore(canvas);

        int count = canvas.save();
        mLocation = getLocation(); // 위치 얻기
        canvas.translate(mLocation.x, mLocation.y);
        float px = mPivotX - mLocation.x, py = mPivotY - mLocation.y; // 중심점과 위치점 편차
        canvas.rotate(mItemRotate, px, py); // 회전
        canvas.scale(mScale, mScale, px, py); // 확대축소
        doDraw(canvas);
        canvas.restoreToCount(count);

        drawAfter(canvas);
    }

    /**
     * 화상령역밖의 부분을 잘라야 하는가의 여부 되돌리기
     * @return 자르기 여부를 되돌린다
     */
    @Override
    public boolean isNeedClipOutside() {
        return mIsNeedClipOutside;
    }

    /**
     * item의 화상령역바깥부분을 자르도록 기발 설정
     * @param clip 자르기 여부 기발
     */
    @Override
    public void setNeedClipOutside(boolean clip) {
        mIsNeedClipOutside = clip;
    }

    /**
     * item이 생성되여 추가된 후의 callback
     */
    @Override
    public void onAdd() {
        mHasAdded = true;
    }

    /**
     * item이 삭제된 후의 callback
     */
    @Override
    public void onRemove() {
        mHasAdded = false;
    }

    @Override
    public void refresh() {
        if (mHasAdded && mDoodle != null) {
            mDoodle.refresh();
        }
    }

    @Override
    public boolean isDoodleEditable() {
        return false;
    }

    /**
     * item의 확대축소값 설정하기
     */
    @Override
    public void setScale(float scale) {
        if (scale <= MIN_SCALE) {
            scale = MIN_SCALE;
        } else if (scale > MAX_SCALE) {
            scale = MAX_SCALE;
        }
        mScale = scale;
        onPropertyChanged(PROPERTY_SCALE);
        refresh();
    }

    @Override
    public float getScale() {
        return mScale;
    }

    @Override
    public void addItemListener(IDoodleItemListener listener) {
        if (listener == null || mItemListeners.contains(listener)) {
            return;
        }
        mItemListeners.add(listener);
    }

    @Override
    public void removeItemListener(IDoodleItemListener listener) {
        mItemListeners.remove(listener);
    }

    /**
     * 속성 변경시 callback
     * @param property 변경된 속성
     */
    @Override
    public void onPropertyChanged(int property) {
        for (int i = 0; i < mItemListeners.size(); i++) {
            mItemListeners.get(i).onPropertyChanged(property);
        }
    }

    /**
     * View에만 그려주기. 보관할때에는 그려지지 않는다.
     *
     * @param canvas ForegroundView의 canvas
     */
    protected void drawBefore(Canvas canvas) {

    }

    /**
     * canvas에 관계없이 그리기
     *
     * @param canvas
     */
    protected abstract void doDraw(Canvas canvas);

    /**
     * View에만 그려주기. 보관할때에는 그려지지 않는다.
     * @param canvas ForegroundView의 canvas
     */
    protected void drawAfter(Canvas canvas) {

    }

    /**
     * 모든 item들의 제일 우에 그려주기
     * @param canvas ForegroundView의 canvas
     */
    @Override
    public void drawAtTheTop(Canvas canvas) {

    }

    @Override
    public void setBrushBMP(Bitmap brushBMP) {
        mBrushBMP = brushBMP;
    }

    @Override
    public Bitmap getBrushBMP() {
        return mBrushBMP;
    }

    @Override
    public void setBrushStyle(int style) {
        mBrushStyle = style;
    }

    @Override
    public int getBrushStyle() {
        return mBrushStyle;
    }
}
