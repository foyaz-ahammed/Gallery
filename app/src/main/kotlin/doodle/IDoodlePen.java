package doodle;

import android.graphics.Paint;

public interface IDoodlePen {

    /**
     * 설정
     * @param doodleItem
     * @param paint
     */
    public void config(IDoodleItem doodleItem, Paint paint);

    /**
     * pen 복사하기
     * @return 복사하려는 DoodlePen을 돌려준다
     */
    public IDoodlePen copy();
}
