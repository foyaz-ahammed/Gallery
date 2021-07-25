package doodle;

import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.List;

import cn.forward.androids.ScaleGestureDetectorApi27;
import cn.forward.androids.TouchGestureDetector;

import static doodle.DrawUtil.computeAngle;

/**
 * DoodleView에서 Touch와 관련한 모든 처리 진행
 * Created on 30/06/2018.
 */

public class DoodleOnTouchGestureListener extends TouchGestureDetector.OnTouchGestureListener {
    private static final float VALUE = 1f;

    // touch 관련 변수
    private float mTouchX, mTouchY;
    private float mLastTouchX, mLastTouchY;
    private float mTouchDownX, mTouchDownY;

    // 확대축소 관련
    private Float mLastFocusX;
    private Float mLastFocusY;
    private float mTouchCentreX, mTouchCentreY;


    private float mStartX, mStartY;
    // item의 회전을 시작할때의 각도편차. touch하였을때 갑짜기 돌아가는것을 방지하기 위하여
    private float mRotateDiff;

    // 현재 그리기 경로
    private Path mCurrPath;
    private DoodlePath mCurrDoodlePath;

    private DoodleView mDoodle;

    // animation 관련변수
    private ValueAnimator mScaleAnimator;
    private float mScaleAnimTransX, mScaleAnimTranY;
    private ValueAnimator mTranslateAnimator;
    private float mTransAnimOldY, mTransAnimY;

    // 편집방식일때 선택된 item
    private IDoodleSelectableItem mSelectedItem;
    private ISelectionListener mSelectionListener;

    private boolean mSupportScaleItem = true;

    public DoodleOnTouchGestureListener(DoodleView doodle, ISelectionListener listener) {
        mDoodle = doodle;
        mSelectionListener = listener;
    }

    /**
     * 새로 선택된 item으로 교체.
     * @param selectedItem 새로 선택된 item
     */
    public void setSelectedItem(IDoodleSelectableItem selectedItem) {
        IDoodleSelectableItem old = mSelectedItem;
        mSelectedItem = selectedItem;

        if (old != null) {
            // 이전에 선택한 item은 선택취소하기
            old.setSelected(false);
            if (mSelectionListener != null) {
                mSelectionListener.onSelectedItem(mDoodle, old, false);
            }
            mDoodle.notifyItemFinishedDrawing(old);
        }
        if (mSelectedItem != null) {
            // 새로 선택한 item은 선택설정
            mSelectedItem.setSelected(true);
            if (mSelectionListener != null) {
                mSelectionListener.onSelectedItem(mDoodle, mSelectedItem, true);
            }
            mDoodle.markItemToOptimizeDrawing(mSelectedItem);
        }

    }

    public IDoodleSelectableItem getSelectedItem() {
        return mSelectedItem;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mTouchX = mTouchDownX = e.getX();
        mTouchY = mTouchDownY = e.getY();
        return true;
    }

    /**
     * 끌기 시작
     *
     * @param event
     */
    @Override
    public void onScrollBegin(MotionEvent event) {
        mLastTouchX = mTouchX = event.getX();
        mLastTouchY = mTouchY = event.getY();
        mDoodle.setScrollingDoodle(true);

        if (mDoodle.isEditMode()) {
            // 편집방식일때
            if (mSelectedItem != null) {
                // 선택된 item이 있을때
                PointF xy = mSelectedItem.getLocation();
                mStartX = xy.x;
                mStartY = xy.y;

                if (mSelectedItem instanceof DoodleRotatableItemBase
                        && (((DoodleRotatableItemBase) mSelectedItem).canRotate(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY)))) {
                    // 회전중 설정
                    ((DoodleRotatableItemBase) mSelectedItem).setIsRotating(true);
                    // 초기 회전편차 구하기
                    mRotateDiff = mSelectedItem.getItemRotate() -
                            computeAngle(mSelectedItem.getPivotX(), mSelectedItem.getPivotY(), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                }
            } else {
                // 선택된 item이 없을때
                mStartX = mDoodle.getDoodleTranslationX();
                mStartY = mDoodle.getDoodleTranslationY();
            }
        } else {
            // 새로운 그리기 시작
            mCurrPath = new Path();
            mCurrPath.moveTo(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
            if (mDoodle.getShape() == DoodleShape.PAINTBRUSH) {
                // 일반 붓 그리기
                mCurrDoodlePath = DoodlePath.toPath(mDoodle, mCurrPath);
            } else {
                // 도형 그리기
                mCurrDoodlePath = DoodlePath.toShape(mDoodle,
                        mDoodle.toX(mTouchDownX), mDoodle.toY(mTouchDownY), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
            }
            if (mDoodle.isOptimizeDrawing()) {
                mDoodle.markItemToOptimizeDrawing(mCurrDoodlePath);
            } else {
                mDoodle.addItem(mCurrDoodlePath);
            }
        }

        // 그리기/편집 시작이므로 ForegroundView 갱신
        mDoodle.refresh();
    }

    @Override
    public void onScrollEnd(MotionEvent e) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e.getX();
        mTouchY = e.getY();
        mDoodle.setScrollingDoodle(false);

        if (mDoodle.isEditMode()) {
            if (mSelectedItem instanceof DoodleRotatableItemBase) {
                // 회전 끝 설정
                ((DoodleRotatableItemBase) mSelectedItem).setIsRotating(false);
            }
            // 화면 중심으로 이동
            limitBound(true);
        }

        if (mCurrDoodlePath != null) {
            if (mDoodle.isOptimizeDrawing()) {
                // 그리기가 끝났다는것을 알려준다
                mDoodle.notifyItemFinishedDrawing(mCurrDoodlePath);
            }
            mCurrDoodlePath = null;
        }

        // ForegroundView 갱신
        mDoodle.refresh();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e2.getX();
        mTouchY = e2.getY();

        if (mDoodle.isEditMode()) {
            // 편집방식일때
            if (mSelectedItem != null) {
                if ((mSelectedItem instanceof DoodleRotatableItemBase) && (((DoodleRotatableItemBase) mSelectedItem).isRotating())) {
                    // item 회전시키기
                    mSelectedItem.setItemRotate(mRotateDiff + computeAngle(
                            mSelectedItem.getPivotX(), mSelectedItem.getPivotY(), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY)
                    ));
                } else {
                    // item 이동시키기
                    mSelectedItem.setLocation(
                            mStartX + mDoodle.toX(mTouchX) - mDoodle.toX(mTouchDownX),
                            mStartY + mDoodle.toY(mTouchY) - mDoodle.toY(mTouchDownY), true);
                }
            } else {
                // 화상 이동하기
                mDoodle.setDoodleTranslation(mStartX + mTouchX - mTouchDownX,
                        mStartY + mTouchY - mTouchDownY);
            }
        } else {
            if (mDoodle.getShape() == DoodleShape.PAINTBRUSH) {
                // 붓 그리기 Path 갱신
                mCurrPath.quadTo(
                        mDoodle.toX(mLastTouchX),
                        mDoodle.toY(mLastTouchY),
                        mDoodle.toX((mTouchX + mLastTouchX) / 2),
                        mDoodle.toY((mTouchY + mLastTouchY) / 2));
                mCurrDoodlePath.updatePath(mCurrPath);
            } else {
                // 도형 그리기 Path 갱신
                mCurrDoodlePath.updateXY(mDoodle.toX(mTouchDownX), mDoodle.toY(mTouchDownY), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
            }
        }

        // 그리기/편집중이므로 ForegroundView 갱신
        mDoodle.refresh();
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e.getX();
        mTouchY = e.getY();

        if (mDoodle.isEditMode()) {
            // 편집방식일때 item 선택하기
            boolean found = false;
            IDoodleSelectableItem item;
            List<IDoodleItem> items = mDoodle.getAllItem();
            for (int i = items.size() - 1; i >= 0; i--) {
                IDoodleItem elem = items.get(i);
                // 편집할수 있는 item인지 검사
                if (!elem.isDoodleEditable()) {
                    continue;
                }
                // 선택할수 있는 item인지 검사
                if (!(elem instanceof IDoodleSelectableItem)) {
                    continue;
                }

                item = (IDoodleSelectableItem) elem;

                if (item.contains(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY))) {
                    //item이 선택되였다
                    found = true;
                    setSelectedItem(item);
                    PointF xy = item.getLocation();
                    mStartX = xy.x;
                    mStartY = xy.y;
                    break;
                }
            }
            if (!found) {
                // 새로 선택한 item이 없다
                if (mSelectedItem != null) {
                    // 이미 선택한 item은 선택 취소
                    IDoodleSelectableItem old = mSelectedItem;
                    setSelectedItem(null);
                    if (mSelectionListener != null) {
                        mSelectionListener.onSelectedItem(mDoodle, old, false);
                    }
                }
            }
        } else {
            // scroll을 실행하여 그리기 진행 (점 그리기)
            onScrollBegin(e);
            e.offsetLocation(VALUE, VALUE);
            onScroll(e, e, VALUE, VALUE);
            onScrollEnd(e);
        }
        mDoodle.refresh();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetectorApi27 detector) {
        mLastFocusX = null;
        mLastFocusY = null;
        return true;
    }

    private float pendingX, pendingY, pendingScale = 1;

    @Override
    public boolean onScale(ScaleGestureDetectorApi27 detector) {
        mTouchCentreX = detector.getFocusX();
        mTouchCentreY = detector.getFocusY();

        if (mLastFocusX != null && mLastFocusY != null) {
            // 초점 이동거리
            final float dx = mTouchCentreX - mLastFocusX;
            final float dy = mTouchCentreY - mLastFocusY;
            // 화상이동
            if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                if (mSelectedItem == null || !mSupportScaleItem) {
                    mDoodle.setDoodleTranslationX(mDoodle.getDoodleTranslationX() + dx + pendingX);
                    mDoodle.setDoodleTranslationY(mDoodle.getDoodleTranslationY() + dy + pendingY);
                } else {
                    // nothing
                }
                pendingX = pendingY = 0;
            } else {
                // 미세한 이동은 무시
                pendingX += dx;
                pendingY += dy;
            }
        }

        if (Math.abs(1 - detector.getScaleFactor()) > 0.005f) {
            if (mSelectedItem == null || !mSupportScaleItem) {
                // 화상 확대축소
                float scale = mDoodle.getDoodleScale() * detector.getScaleFactor() * pendingScale;
                mDoodle.setDoodleScale(scale, mDoodle.toX(mTouchCentreX), mDoodle.toY(mTouchCentreY));
            } else {
                // 현재 선택된 item의 확대축소 진행
                mSelectedItem.setScale(mSelectedItem.getScale() * detector.getScaleFactor() * pendingScale);
            }
            pendingScale = 1;
        } else {
            // 미세한 확대축소는 무시
            pendingScale *= detector.getScaleFactor();
        }

        mLastFocusX = mTouchCentreX;
        mLastFocusY = mTouchCentreY;

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetectorApi27 detector) {
        if (mDoodle.isEditMode()) {
            // 편집방식일때 화상을 화면중심으로 이동
            limitBound(true);
            return;
        }

        // 화상이 화면을 채우도록 한다
        center();
    }

    /**
     * 화상을 끌기 & 확대축소하였을떄 화상이 작으면 화면에 차도록 확대시키고 화면중심에 놓이게 한다. <br/>
     * 화상이 크면 화면을 채우도록 이동만 시킨다.
     */
    public void center() {
        if (mDoodle.getDoodleScale() < 1) { //
            if (mScaleAnimator == null) {
                mScaleAnimator = new ValueAnimator();
                mScaleAnimator.setDuration(100);
                mScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        float fraction = animation.getAnimatedFraction();
                        mDoodle.setDoodleScale(value, mDoodle.toX(mTouchCentreX), mDoodle.toY(mTouchCentreY));
                        mDoodle.setDoodleTranslation(mScaleAnimTransX * (1 - fraction), mScaleAnimTranY * (1 - fraction));
                    }
                });
            }
            mScaleAnimator.cancel();
            mScaleAnimTransX = mDoodle.getDoodleTranslationX();
            mScaleAnimTranY = mDoodle.getDoodleTranslationY();
            mScaleAnimator.setFloatValues(mDoodle.getDoodleScale(), 1);
            mScaleAnimator.start();
        } else { //
            limitBound(true);
        }
    }

    /**
     * 편집방식에서 화상끌기를 하였을때 화상이 작으면 화면중심으로, 크면 화면을 채우도록 이동시키는 효과
     *
     * @param anim animation 효과여부
     */
    public void limitBound(boolean anim) {
        final float oldX = mDoodle.getDoodleTranslationX(), oldY = mDoodle.getDoodleTranslationY();
        RectF bound = mDoodle.getDoodleBound();
        float x = mDoodle.getDoodleTranslationX(), y = mDoodle.getDoodleTranslationY();
        float width = mDoodle.getCenterWidth() * mDoodle.getRotateScale(), height = mDoodle.getCenterHeight() * mDoodle.getRotateScale();

        // 움직여야할 수평거리
        if (bound.height() <= mDoodle.getHeight()) {
            y = (height - height * mDoodle.getDoodleScale()) / 2;
        } else {
            float heightDiffTop = bound.top;
            if (bound.top > 0 && bound.bottom >= mDoodle.getHeight()) {
                y = y - heightDiffTop;
            } else if (bound.bottom < mDoodle.getHeight() && bound.top <= 0) { // 只有下在屏幕内
                float heightDiffBottom = mDoodle.getHeight() - bound.bottom;
                y = y + heightDiffBottom;
            }
        }

        // 움직여야할 수직거리
        if (bound.width() <= mDoodle.getWidth()) {
            x = (width - width * mDoodle.getDoodleScale()) / 2;
        } else {
            float widthDiffLeft = bound.left;
            if (bound.left > 0 && bound.right >= mDoodle.getWidth()) {
                x = x - widthDiffLeft;
            } else if (bound.right < mDoodle.getWidth() && bound.left <= 0) { // 只有右在屏幕内
                float widthDiffRight = mDoodle.getWidth() - bound.right;
                x = x + widthDiffRight;
            }
        }
        if (anim) {
            if (mTranslateAnimator == null) {
                mTranslateAnimator = new ValueAnimator();
                mTranslateAnimator.setDuration(100);
                mTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        float fraction = animation.getAnimatedFraction();
                        mDoodle.setDoodleTranslation(value, mTransAnimOldY + (mTransAnimY - mTransAnimOldY) * fraction);
                    }
                });
            }
            mTranslateAnimator.setFloatValues(oldX, x);
            mTransAnimOldY = oldY;
            mTransAnimY = y;
            mTranslateAnimator.start();
        } else {
            mDoodle.setDoodleTranslation(x, y);
        }
    }

    public void setSelectionListener(ISelectionListener doodleListener) {
        mSelectionListener = doodleListener;
    }

    public ISelectionListener getSelectionListener() {
        return mSelectionListener;
    }

    public void setSupportScaleItem(boolean supportScaleItem) {
        mSupportScaleItem = supportScaleItem;
    }

    public boolean isSupportScaleItem() {
        return mSupportScaleItem;
    }

    public interface ISelectionListener {
        /**
         * item이 선택되거나 선택이 취소될때 호출된다.
         * @param selected 선택여부
         */
        void onSelectedItem(IDoodle doodle, IDoodleSelectableItem selectableItem, boolean selected);
    }

}
