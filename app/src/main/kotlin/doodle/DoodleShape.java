package doodle;

import android.graphics.Paint;

public enum DoodleShape implements IDoodleShape {
    PAINTBRUSH, // 붓
    ARROW, // 화살표
    HOLLOW_CIRCLE, // 빈 원
    HOLLOW_RECT, // 빈 사각형
    SHAPE_MOSAIC;   // 모자이크


    @Override
    public void config(IDoodleItem doodleItem, Paint paint) {
        if (doodleItem.getShape() == DoodleShape.ARROW) {
            paint.setStyle(Paint.Style.FILL);
        } else {
            paint.setStyle(Paint.Style.STROKE);
        }
    }

    @Override
    public IDoodleShape copy() {
        return this;
    }
}
