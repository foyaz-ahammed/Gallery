package doodle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import static doodle.DrawUtil.rotatePoint;

/**
 * 선택할수있는 item
 * Created by huangziwei on 2017/7/16.
 */

public abstract class DoodleSelectableItemBase extends DoodleItemBase implements IDoodleSelectableItem {

    public final static int ITEM_CAN_ROTATE_BOUND = 35;
    public final static int ITEM_PADDING = 3; // item의 padding

    // item의 경계 Rect
    private Rect mRect = new Rect();
    private Rect mRectTemp = new Rect();
    // item의 경계 그리기 paint
    private Paint mPaint = new Paint();

    private PointF mTemp = new PointF();
    // 편집중 item의 선택여부
    private boolean mIsSelected = false;

    public DoodleSelectableItemBase(IDoodle doodle, int itemRotate, float x, float y) {
        this(doodle, null, itemRotate, x, y);
    }

    public DoodleSelectableItemBase(IDoodle doodle, DoodlePaintAttrs attrs, int itemRotate, float x, float y) {
        super(doodle, attrs);
        setLocation(x, y, true);
        setItemRotate(itemRotate);

        resetBoundsScaled(mRect);
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);
        resetBoundsScaled(mRect);
        refresh();
    }

    /**
     * item의 경계사각형을 돌려준다.
     * @return 경계사각형을 되돌린다
     */
    @Override
    public Rect getBounds() {
        return mRect;
    }

    /**
     * item의 두께를 설정하고 경계사각형과 위치를 새로 설정
     * @param size 설정하려는 두께
     */

    @Override
    public void setSize(float size) {
        super.setSize(size);
        resetBounds(getBounds());
        setLocation(getPivotX() - getBounds().width() / 2, getPivotY() - getBounds().height() / 2,
                false);
        resetBoundsScaled(getBounds());
    }

    /**
     * @return (x, y)자리표가 item 구역안에 있는지 여부를 판단한다, item을 선택할때 리용
     */
    @Override
    public boolean contains(float x, float y) {
        resetBoundsScaled(mRect);
        PointF location = getLocation();
        x = x - location.x;
        y = y - location.y;
        mTemp = rotatePoint(mTemp, (int) -getItemRotate(), x, y, getPivotX() - getLocation().x, getPivotY() - getLocation().y);
        mRectTemp.set(mRect);
        float unit = getDoodle().getUnitSize();
        mRectTemp.left -= ITEM_PADDING * unit;
        mRectTemp.top -= ITEM_PADDING * unit;
        mRectTemp.right += ITEM_PADDING * unit;
        mRectTemp.bottom += ITEM_PADDING * unit;
        return mRectTemp.contains((int) mTemp.x, (int) mTemp.y);
    }

    /**
     * View에만 그려주기. 보관할때에는 그려지지 않는다.
     * @param canvas ForegroundView의 canvas
     */
    @Override
    public void drawBefore(Canvas canvas) {

    }
    /**
     * View에만 그려주기. 보관할때에는 그려지지 않는다.
     * @param canvas ForegroundView의 canvas
     */
    @Override
    public void drawAfter(Canvas canvas) {

    }

    /**
     * 모든 item들의 우에 그리기
     * @param canvas ForegroundView의 canvas
     */
    @Override
    public void drawAtTheTop(Canvas canvas) {
        int count = canvas.save();
        PointF location = getLocation();
        canvas.translate(location.x, location.y);
        canvas.rotate(getItemRotate(), getPivotX() - getLocation().x, getPivotY() - getLocation().y);

        doDrawAtTheTop(canvas);

        canvas.restoreToCount(count);
    }

    /**
     * 선택시에 다른 item에 의해 가려지지 않게 우에 그리기
     * @param canvas 그리기하는 canvas
     */
    public void doDrawAtTheTop(Canvas canvas) {
        if (isSelected()) {
            canvas.save();
            canvas.scale(1 / getDoodle().getDoodleScale(), 1 / getDoodle().getDoodleScale(), getPivotX() - getLocation().x, getPivotY() - getLocation().y);
            mRectTemp.set(getBounds());
            DrawUtil.scaleRect(mRectTemp, getDoodle().getDoodleScale(), getPivotX() - getLocation().x, getPivotY() - getLocation().y);

            float unit = getDoodle().getUnitSize();
            mRectTemp.left -= ITEM_PADDING * unit;
            mRectTemp.top -= ITEM_PADDING * unit;
            mRectTemp.right += ITEM_PADDING * unit;
            mRectTemp.bottom += ITEM_PADDING * unit;
            mPaint.setShader(null);
            mPaint.setColor(0x00888888);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(1);
            canvas.drawRect(mRectTemp, mPaint);

            // border
            mPaint.setColor(0x88ffffff);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2 * unit);
            canvas.drawRect(mRectTemp, mPaint);
            // border line
            mPaint.setColor(0x44888888);
            mPaint.setStrokeWidth(0.8f * unit);
            canvas.drawRect(mRectTemp, mPaint);

            canvas.restore();
        }
    }

    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    /**
     * 편집중 비/선택되였다는것을 설정
     * @param isSelected 선택여부
     */
    @Override
    public void setSelected(boolean isSelected) {
        mIsSelected = isSelected;
        setNeedClipOutside(!isSelected);
        refresh();
    }

    /**
     * item의 경계사각형을 새로 설정한다.
     * @param rect 결과를 담는 rect
     */
    protected void resetBoundsScaled(Rect rect) {
        resetBounds(rect);
        float px = getPivotX() - getLocation().x;
        float py = getPivotY() - getLocation().y;
        DrawUtil.scaleRect(rect, getScale(), px, py);
    }

    /**
     * @param rect item의 경계사각형
     */
    protected abstract void resetBounds(Rect rect);

    @Override
    public boolean isDoodleEditable() {
        return true;
    }
}
