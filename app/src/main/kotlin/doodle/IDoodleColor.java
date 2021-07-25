package doodle;

import android.graphics.Paint;

public interface IDoodleColor {

    /**
     * 설정
     * @param doodleItem
     * @param paint
     */
    public void config(IDoodleItem doodleItem, Paint paint);

    /**
     * 색갈 복사
     * @return
     */
    public IDoodleColor copy();
}
