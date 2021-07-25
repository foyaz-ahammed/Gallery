package doodle;

import android.graphics.Rect;

public interface IDoodleSelectableItem extends IDoodleItem {

    /**
     * 선택 여부 설정
     * @param isSelected
     */
    public void setSelected(boolean isSelected);

    /**
     * 선택 여부
     * @return
     */
    public boolean isSelected();

    /**
     * item의 사각형 (크기조정 후) 령역
     * @return
     */
    public Rect getBounds();

    /**
     * @return (x, y)자리표가 item 구역안에 있는지 여부를 판단한다, item을 선택할때 리용
     */
    public boolean contains(float x, float y);

}
