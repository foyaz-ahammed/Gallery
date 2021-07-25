package doodle;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * 그리기 경로
 */

public class DoodlePath extends DoodleRotatableItemBase {

    // mosaic 세기 정도
    public static final int MOSAIC_LEVEL_1 = 5;
    public static final int MOSAIC_LEVEL_2 = 20;
    public static final int MOSAIC_LEVEL_3 = 50;

    private final Path mPath = new Path(); // 그리기 경로
    private final Path mOriginPath = new Path();

    // touch down하였을때의 위치
    private final PointF mSxy = new PointF();
    // touch up하였을때의 위치
    private final PointF mDxy = new PointF();

    private Paint mPaint = new Paint();

    private final Matrix mTransform = new Matrix();
    private Rect mRect = new Rect();
    private Matrix mBitmapColorMatrix = new Matrix();

    public DoodlePath(IDoodle doodle) {
        super(doodle, 0, 0, 0);
    }

    public DoodlePath(IDoodle doodle, DoodlePaintAttrs attrs) {
        super(doodle, attrs, 0, 0, 0);
    }

    /**
     * 도형그리기의 경로 갱신
     *
     * @param sx 시작시의 x좌표
     * @param sy 시작시의 y좌표
     * @param dx 마감시의 x좌표
     * @param dy 마감시의 y좌표
     */
    public void updateXY(float sx, float sy, float dx, float dy) {
        mSxy.set(sx, sy);
        mDxy.set(dx, dy);
        mOriginPath.reset();

        if (DoodleShape.ARROW.equals(getShape())) {
            updateArrowPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        } else if (DoodleShape.HOLLOW_CIRCLE.equals(getShape())) {
            updateCirclePath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        } else if (DoodleShape.HOLLOW_RECT.equals(getShape())) {
            updateRectPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        }

        adjustPath(true);
    }

    public void updatePath(Path path) {
        mOriginPath.reset();
        this.mOriginPath.addPath(path);
        adjustPath(true);
    }

    public Path getPath() {
        return mPath;
    }

    private PointF getDxy() {
        return mDxy;
    }

    private PointF getSxy() {
        return mSxy;
    }

    /**
     * 도형 그리기 시작일때의 경로 생성하기
     * @return 새 경로를 돌려준다
     */
    public static DoodlePath toShape(IDoodle doodle, float sx, float sy, float dx, float dy) {
        DoodlePath path = new DoodlePath(doodle);
        path.setPen(doodle.getPen().copy());
        path.setShape(doodle.getShape().copy());
        path.setSize(doodle.getSize());
        path.setColor(doodle.getColor().copy());
        path.setBrushBMP(doodle.getBrushBMP());
        path.setBrushStyle(doodle.getBrushStyle());

        path.updateXY(sx, sy, dx, dy);

        return path;
    }

    /**
     * 붓 그리기 시작일때의 경로 생성하기
     * @return 새 경로를 돌려준다
     */
    public static DoodlePath toPath(IDoodle doodle, Path p) {
        DoodlePath path = new DoodlePath(doodle);
        path.setPen(doodle.getPen().copy());
        path.setShape(doodle.getShape().copy());
        path.setSize(doodle.getSize());
        path.setColor(doodle.getColor().copy());
        path.setBrushBMP(doodle.getBrushBMP());
        path.setBrushStyle(doodle.getBrushStyle());

        path.updatePath(p);

        return path;
    }

    @Override
    protected void doDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setStrokeWidth(getSize());
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);

        int style = getBrushStyle();
        if (style == 0) mPaint.setStrokeCap(Paint.Cap.SQUARE);
        else if(style == 1) mPaint.setStrokeCap(Paint.Cap.ROUND);

        getPen().config(this, mPaint);
        getColor().config(this, mPaint);
        getShape().config(this, mPaint);

        Bitmap brushBMP = getBrushBMP();

        if (style < 2) {
            // 사각형과 원일때
            canvas.drawPath(getPath(), mPaint);
        } else {
            // 나머지 경우 화상으로 경로 그리기
            float[] pos = new float[2];

            PathMeasure pm = new PathMeasure(getPath(), false);
            float length = pm.getLength();

            ColorFilter filter = new PorterDuffColorFilter(mPaint.getColor(), PorterDuff.Mode.SRC_IN);
            mPaint.setColorFilter(filter);
            // 현시속도를 위하여 증가값 조절
            int inc = Math.max(2, brushBMP.getWidth() / 8);
            for (int i = 0; i < length - 1; i += inc) {
                pm.getPosTan(i, pos, null);
                canvas.drawBitmap(brushBMP, pos[0], pos[1], mPaint);
            }
        }
    }

    // path의 경계사각형
    private final RectF mBound = new RectF();

    /**
     * path의 경계사각형 계산
     * @param rect 결과를 담는다.
     */
    private void resetLocationBounds(Rect rect) {
        if (mOriginPath == null) {
            return;
        }

        int diff = (int) (getSize() / 2 + 0.5f);
        mOriginPath.computeBounds(mBound, false);
        if (getShape() == DoodleShape.ARROW) {
            diff = (int) getDoodle().getUnitSize();
        }
        rect.set((int) (mBound.left - diff), (int) (mBound.top - diff), (int) (mBound.right + diff), (int) (mBound.bottom + diff));
    }

    @Override
    protected void resetBounds(Rect rect) {
        resetLocationBounds(rect);
        rect.set(0, 0, rect.width(), rect.height());
    }

    @Override
    public boolean isDoodleEditable() {
        return super.isDoodleEditable();
    }

    // 화살표의 삼각경로
    private Path mArrowTrianglePath;

    /**
     * 화살표의 경로 계산
     * @param path 갱신시키려는 새 경로
     * @param sx    경로의 시작 x좌표
     * @param sy    경로의 시작 y좌표
     * @param ex    경로의 마감 x좌표
     * @param ey    경로의 마감 y좌표
     * @param size  그리기 두께
     */
    private void updateArrowPath(Path path, float sx, float sy, float ex, float ey, float size) {
        double H = size; // 화살 높이
        double L = size / 2;

        double awrad = Math.atan(L / 2 / H); // 화살의 각도
        double arraow_len = Math.sqrt(L / 2 * L / 2 + H * H) - 5; // 화살길이
        double[] arrXY_1 = DrawUtil.rotateVec(ex - sx, ey - sy, awrad, true, arraow_len);
        double[] arrXY_2 = DrawUtil.rotateVec(ex - sx, ey - sy, -awrad, true, arraow_len);
        float x_3 = (float) (ex - arrXY_1[0]);
        float y_3 = (float) (ey - arrXY_1[1]);
        float x_4 = (float) (ex - arrXY_2[0]);
        float y_4 = (float) (ey - arrXY_2[1]);
        // 선 그리기
        path.moveTo(sx, sy);
        path.lineTo(x_3, y_3);
        path.lineTo(x_4, y_4);
        path.close();

        awrad = Math.atan(L / H); // 화살의 각도
        arraow_len = Math.sqrt(L * L + H * H); // 화살길이
        arrXY_1 = DrawUtil.rotateVec(ex - sx, ey - sy, awrad, true, arraow_len);
        arrXY_2 = DrawUtil.rotateVec(ex - sx, ey - sy, -awrad, true, arraow_len);
        x_3 = (float) (ex - arrXY_1[0]);
        y_3 = (float) (ey - arrXY_1[1]);
        x_4 = (float) (ex - arrXY_2[0]);
        y_4 = (float) (ey - arrXY_2[1]);
        if (mArrowTrianglePath == null) {
            mArrowTrianglePath = new Path();
        }
        mArrowTrianglePath.reset();
        mArrowTrianglePath.moveTo(ex, ey);
        mArrowTrianglePath.lineTo(x_4, y_4);
        mArrowTrianglePath.lineTo(x_3, y_3);
        mArrowTrianglePath.close();
        path.addPath(mArrowTrianglePath);
    }

    /**
     * 빈 원의 경로 갱신
     * @param path  갱신시키려는 경로
     * @param sx    경로의 시작 x좌표
     * @param sy    경로의 시작 y좌표
     * @param dx    경로의 마감 x좌표
     * @param dy    경로의 마감 y좌표
     * @param size  그리기 두께
     */
    private void updateCirclePath(Path path, float sx, float sy, float dx, float dy, float size) {
        float radius = (float) Math.sqrt((sx - dx) * (sx - dx) + (sy - dy) * (sy - dy));
        path.addCircle(sx, sy, radius, Path.Direction.CCW);

    }

    /**
     * 빈 사각형의 경로 갱신
     * @param path  갱신시키려는 경로
     * @param sx    경로의 시작 x좌표
     * @param sy    경로의 시작 y좌표
     * @param dx    경로의 마감 x좌표
     * @param dy    경로의 마감 y좌표
     * @param size  그리기 두께
     */
    private void updateRectPath(Path path, float sx, float sy, float dx, float dy, float size) {
        // 왼쪽웃 모서리 및 오른쪽아래 모서리 사이의 대응을 보장합니다.
        if (sx < dx) {
            if (sy < dy) {
                path.addRect(sx, sy, dx, dy, Path.Direction.CCW);
            } else {
                path.addRect(sx, dy, dx, sy, Path.Direction.CCW);
            }
        } else {
            if (sy < dy) {
                path.addRect(dx, sy, sx, dy, Path.Direction.CCW);
            } else {
                path.addRect(dx, dy, sx, sy, Path.Direction.CCW);
            }
        }
    }

    private static WeakHashMap<IDoodle, HashMap<Integer, Bitmap>> sMosaicBitmapMap = new WeakHashMap<>();

    /**
     * mosaic의 색갈을 얻는다.
     * @param doodle 현재의 DoodleView
     * @param level 색의 세기
     * @return DoodleColor형 색을 돌려준다.
     */
    public static DoodleColor getMosaicColor(IDoodle doodle, int level) {
        HashMap<Integer, Bitmap> map = sMosaicBitmapMap.get(doodle);
        if (map == null) {
            map = new HashMap<>();
            sMosaicBitmapMap.put(doodle, map);
        }
        Matrix matrix = new Matrix();
        matrix.setScale(1f / level, 1f / level);
        Bitmap mosaicBitmap = map.get(level);
        if (mosaicBitmap == null) {
            mosaicBitmap = Bitmap.createBitmap(doodle.getBitmap(),
                    0, 0, doodle.getBitmap().getWidth(), doodle.getBitmap().getHeight(), matrix, true);
            map.put(level, mosaicBitmap);
        }
        matrix.reset();
        matrix.setScale(level, level);
        DoodleColor doodleColor = new DoodleColor(mosaicBitmap, matrix, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        doodleColor.setLevel(level);
        return doodleColor;
    }

    /**
     * item의 위치를 설정한다.
     * @param x 설정위치의 x 자리표
     * @param y 설정위치의 y 자리표
     * @param changePivot 중심점의 위치도 따라 변경할지 여부
     */
    @Override
    public void setLocation(float x, float y, boolean changePivot) {
        super.setLocation(x, y, changePivot);
        adjustMosaic();
    }

    /**
     * item의 색갈을 설정한다.
     * @param color 설정하려는 색
     */
    @Override
    public void setColor(IDoodleColor color) {
        super.setColor(color);
        if (getPen() == DoodlePen.MOSAIC) {
            setLocation(getLocation().x, getLocation().y, false);
        }
        adjustPath(false);
    }

    /**
     * 경로의 두께설정
     * @param size 설정하려는 두께
     */
    @Override
    public void setSize(float size) {
        super.setSize(size);


        if (mTransform == null) {
            return;
        }

        if (DoodleShape.ARROW.equals(getShape())) {
            mOriginPath.reset();
            updateArrowPath(mOriginPath, mSxy.x, mSxy.y, mDxy.x, mDxy.y, getSize());
        }

        adjustPath(false);
    }

    /**
     * 경로의 확대축소 진행
     * @param scale 확대축소값
     */
    @Override
    public void setScale(float scale) {
        super.setScale(scale);
        adjustMosaic();
    }

    /**
     * 모자이크 색에 대한 확대축소, 회전, 이동 진행
     */
    private void adjustMosaic() {
        if (getPen() == DoodlePen.MOSAIC
                && getColor() instanceof DoodleColor) {
            DoodleColor doodleColor = ((DoodleColor) getColor());
            Matrix matrix = doodleColor.getMatrix();
            matrix.reset();
            matrix.preScale(1 / getScale(), 1 / getScale(), getPivotX(), getPivotY()); // restore scale
            matrix.preTranslate(-getLocation().x * getScale(), -getLocation().y * getScale());
            matrix.preRotate(-getItemRotate(), getPivotX(), getPivotY());
            matrix.preScale(doodleColor.getLevel(), doodleColor.getLevel());
            doodleColor.setMatrix(matrix);
            refresh();
        }
    }

    /**
     * 경로의 회전진행
     * @param textRotate 설정하려는 회전각도
     */
    @Override
    public void setItemRotate(float textRotate) {
        super.setItemRotate(textRotate);
        adjustMosaic();
    }

    private void adjustPath(boolean changePivot) {
        resetLocationBounds(mRect);
        mPath.reset();
        this.mPath.addPath(mOriginPath);
        mTransform.reset();
        mTransform.setTranslate(-mRect.left, -mRect.top);
        mPath.transform(mTransform);
        if (changePivot) {
            setPivotX(mRect.left + mRect.width() / 2);
            setPivotY(mRect.top + mRect.height() / 2);
            setLocation(mRect.left, mRect.top, false);
        }

        if ((getColor() instanceof DoodleColor)) {
            DoodleColor color = (DoodleColor) getColor();
            if (color.getType() == DoodleColor.Type.BITMAP && color.getBitmap() != null) {
                mBitmapColorMatrix.reset();

                if (getPen() == DoodlePen.MOSAIC) {
                    adjustMosaic();
                } else {
                    mBitmapColorMatrix.setTranslate(-mRect.left, -mRect.top);

                    int level = color.getLevel();
                    mBitmapColorMatrix.preScale(level, level);
                    color.setMatrix(mBitmapColorMatrix);
                    refresh();
                }
            }
        }

        refresh();
    }
}

