package doodle;

import android.graphics.Paint;

/**
 * 图形
 */
public interface IDoodleShape {


     /**
      * 설정
      * @param doodleItem
      * @param paint
      */
     void config(IDoodleItem doodleItem, Paint paint);

     /**
      * shape 복사하기
      * @return 복사하려는 DoodleShape를 돌려준다
      */
     IDoodleShape copy();
}
