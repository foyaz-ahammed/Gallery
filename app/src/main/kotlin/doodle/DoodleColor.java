package doodle;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * 그리기 색갈
 */
public class DoodleColor implements IDoodleColor {

    public enum Type {
        COLOR, // 색갈
        BITMAP // 화상
    }

    private int mColor;
    // mosaic의 색갈을 위한 Bitmap
    private Bitmap mBitmap;
    private Type mType;
    private Matrix mMatrix;

    // mosaic의 세기정도
    private int mLevel = 1;

    // 화상 뒤집기 관련
    private Shader.TileMode mTileX = Shader.TileMode.MIRROR;
    private Shader.TileMode mTileY = Shader.TileMode.MIRROR;

    public DoodleColor(int color) {
        mType = Type.COLOR;
        mColor = color;
    }

    public DoodleColor(Bitmap bitmap) {
        this(bitmap, null);
    }

    public DoodleColor(Bitmap bitmap, Matrix matrix) {
        this(bitmap, matrix, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
    }

    public DoodleColor(Bitmap bitmap, Matrix matrix, Shader.TileMode tileX, Shader.TileMode tileY) {
        mType = Type.BITMAP;
        mMatrix = matrix;
        mBitmap = bitmap;
        mTileX = tileX;
        mTileY = tileY;
    }

    @Override
    public void config(IDoodleItem item, Paint paint) {
        if (mType == Type.COLOR) {
            paint.setColor(mColor);
            paint.setShader(null);
        } else if (mType == Type.BITMAP) {
            BitmapShader shader = new BitmapShader(mBitmap, mTileX, mTileY);
            shader.setLocalMatrix(mMatrix);
            paint.setShader(shader);
        }
    }

    public void setColor(int color) {
        mType = Type.COLOR;
        mColor = color;
    }

    public void setColor(Bitmap bitmap) {
        mType = Type.BITMAP;
        mBitmap = bitmap;
    }

    public void setColor(Bitmap bitmap, Matrix matrix) {
        mType = Type.BITMAP;
        mMatrix = matrix;
        mBitmap = bitmap;
    }

    public void setColor(Bitmap bitmap, Matrix matrix, Shader.TileMode tileX, Shader.TileMode tileY) {
        mType = Type.BITMAP;
        mBitmap = bitmap;
        mMatrix = matrix;
        mTileX = tileX;
        mTileY = tileY;
    }

    public void setMatrix(Matrix matrix) {
        mMatrix = matrix;
    }

    public Matrix getMatrix() {
        return mMatrix;
    }

    public int getColor() {
        return mColor;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public Type getType() {
        return mType;
    }

    @Override
    public IDoodleColor copy() {
        final DoodleColor color;
        if (mType == Type.COLOR) {
            color = new DoodleColor(mColor);
        } else {
            color = new DoodleColor(mBitmap);
        }
        color.mTileX = mTileX;
        color.mTileY = mTileY;
        color.mMatrix = new Matrix(mMatrix);
        color.mLevel = mLevel;
        return color;
    }

    /**
     * mosaic 색갈의 세기를 설정한다
     * @param level 색갈의 세기
     */
    public void setLevel(int level) {
        mLevel = level;
    }

    /**
     * @return mosaic 색갈의 세기를 되돌린다.
     */
    public int getLevel() {
        return mLevel;
    }
}


