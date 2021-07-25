package doodle;

/**
 * 그리기 속성
 */

public class DoodlePaintAttrs {
    private IDoodlePen mPen; // 그리기 류형
    private IDoodleShape mShape; // 그리기 모양
    private float mSize; // 그리기 두께
    private IDoodleColor mColor; // 그리기 색갈

    public IDoodlePen pen() {
        return mPen;
    }

    public IDoodleShape shape() {
        return mShape;
    }

    public float size() {
        return mSize;
    }

    public IDoodleColor color() {
        return mColor;
    }
}
