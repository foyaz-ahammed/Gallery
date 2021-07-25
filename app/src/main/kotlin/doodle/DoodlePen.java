package doodle;

import android.graphics.Paint;

/**
 * 붓설정
 */
public enum DoodlePen implements IDoodlePen {

    BRUSH, // 붓
    MOSAIC; // 모자이크

    @Override
    public void config(IDoodleItem item, Paint paint) {

    }

    @Override
    public IDoodlePen copy() {
        return this;
    }
}
