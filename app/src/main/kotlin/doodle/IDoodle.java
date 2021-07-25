package doodle;

import android.graphics.Bitmap;

import java.util.List;

/**
 * Created on 27/06/2018.
 */

public interface IDoodle {
    /**
     * 현재의 좌표계에서 unit크기 얻기, 화상과 무관하다.
     * @return
     */
    public float getUnitSize();

    /**
     * 화상 회전값 설정
     *
     * @param degree
     */
    public void setDoodleRotation(int degree);

    /**
     * 화상 회전값 얻기
     *
     * @return
     */
    public int getDoodleRotation();

    /**
     * 화상 확대/축소 비률 설정
     *
     * @param scale
     * @param pivotX
     * @param pivotY
     */
    public void setDoodleScale(float scale, float pivotX, float pivotY);

    /**
     * 이미지 확대 / 축소 비률 가져 오기
     */
    public float getDoodleScale();

    /**
     * pen 설정
     *
     * @param pen
     */
    public void setPen(IDoodlePen pen);

    /**
     * pen 얻기
     */
    public IDoodlePen getPen();

    /**
     * shape 설정
     *
     * @param shape
     */
    public void setShape(IDoodleShape shape);

    /**
     * shape 얻기
     */
    public IDoodleShape getShape();

    /**
     * 화상 편차 x, y 설정
     *
     * @param transX
     */
    public void setDoodleTranslation(float transX, float transY);


    /**
     * 화상 편차 x 설정
     *
     * @param transX
     */
    public void setDoodleTranslationX(float transX);

    /**
     * 화상 편차 x 얻기
     *
     * @return
     */
    public float getDoodleTranslationX();

    /**
     * 화상 편차 y 설정
     *
     * @param transY
     */
    public void setDoodleTranslationY(float transY);

    /**
     * 화상 편차 y 얻기
     *
     * @return
     */
    public float getDoodleTranslationY();

    /**
     * 두께 설정
     *
     * @param paintSize
     */
    public void setSize(float paintSize);

    /**
     * 두께 얻기
     *
     * @return
     */
    public float getSize();

    /**
     * 색갈 설정
     *
     * @param color
     */
    public void setColor(IDoodleColor color);

    /**
     * 색갈 얻기
     *
     * @return
     */
    public IDoodleColor getColor();

    /**
     * 최소 축소 비률 설정
     *
     * @param minScale
     */
    public void setDoodleMinScale(float minScale);

    /**
     * 최소 확대 / 축소 비률 얻기
     *
     * @return
     */
    public float getDoodleMinScale();

    /**
     * 최대 확대 비률 설정
     *
     * @param maxScale
     */
    public void setDoodleMaxScale(float maxScale);

    /**
     * 최대 확대 비률 얻기
     *
     * @return
     */
    public float getDoodleMaxScale();

    /**
     * item 추가
     *
     * @param doodleItem
     */
    public void addItem(IDoodleItem doodleItem);

    /**
     * item 삭제
     *
     * @param doodleItem
     */
    public void removeItem(IDoodleItem doodleItem);

    /**
     * @return 모든 item개수 얻기 (redo item들은 제외)
     */
    public int getItemCount();

    /**
     * @return 모든 item 얻기 (redo item들은 제외)
     */
    public List<IDoodleItem> getAllItem();

    /**
     * @return 모든 redo item개수 얻기
     */
    public int getRedoItemCount();

    /**
     * @return 모든 redo item 얻기
     */
    public List<IDoodleItem> getAllRedoItem();

    /**
     * 확대경 비률 설정
     *
     * @param scale
     */
    public void setZoomerScale(float scale);

    /**
     * 확대경 비률 얻기
     *
     * @return
     */
    public float getZoomerScale();


    /**
     * 화상경계외부에 item을 그릴수 있는지 여부 설정
     *
     * @param isDrawableOutside
     */
    public void setIsDrawableOutside(boolean isDrawableOutside);

    /**
     * 화상경계외부에 item을 그릴수 있는지 여부 얻기
     */
    public boolean isDrawableOutside();

    /**
     * 원본 화상만 현시 여부 설정
     *
     * @param justDrawOriginal
     */
    public void setShowOriginal(boolean justDrawOriginal);

    /**
     * 원본 화상만 현시 여부 얻기
     */
    public boolean isShowOriginal();

    /**
     * 현재의 화상 보관
     */
    public void save();

    /**
     * 모든 item들을 지우기
     */
    public void clear();

    /**
     * item을 앞으로 내오기 (z order)
     *
     * @param item
     */
    public void topItem(IDoodleItem item);

    /**
     * item을 뒤로 가져가기 (z order)
     *
     * @param item
     */
    public void bottomItem(IDoodleItem item);

    /**
     * 한 단계 실행 취소
     */
    public void undo();

    /**
     * 한 단계 실행 다시하기
     */
    public void redo();
    /**
     * 실행 취소할 단계수 설정
     * @param step
     */
    public void undo(int step);

    /**
     * 다시 실행할 단계수 설정
     * @param step
     */
    public void redo(int step);

    /**
     * @return 현재 화상 얻기 (item 없음)
     */
    public Bitmap getBitmap();

    /**
     * @return 현재 화상 얻기 (item 포함)
     */
    public Bitmap getDoodleBitmap();

    /**
     * 재그리기
     */
    public void refresh();

    /**
     * 그리기의 Bitmap index설정
     * @param style
     */
    public void setBrushBMPStyle(int style);

    public Bitmap getBrushBMP();

    public int getBrushStyle();
}
