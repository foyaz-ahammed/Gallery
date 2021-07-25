package doodle;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 그리기 파라메터
 */
public class DoodleParams implements Parcelable {

    /**
     * 그리기를 할때 화상의 외부에도 그려줄것인가를 나타낸다.
     */
    public boolean mIsDrawableOutside;

    /**
     * 확대경 배률을 설정하십시오. 0보다 작거나 같으면 확대경기능이 사용되지 않음을 의미합니다.
     * 표준값은 2.5배
     */
    public float mZoomerScale = 2.5f;

    /**
     * canvas의 최소축소, 최대확대값
     */
    public float mMinScale = DoodleView.MIN_SCALE;
    public float mMaxScale = DoodleView.MAX_SCALE;

    /**
     * item의 확대축소 여부설정
     */
    public boolean mSupportScaleItem = true;

    /**
     *
     * 그리기의 최적화 진행.
     *
     * {@link DoodleView#mOptimizeDrawing}
     */
    public boolean mOptimizeDrawing = true;

    public static final Creator<DoodleParams> CREATOR = new Creator<DoodleParams>() {
        @Override
        public DoodleParams createFromParcel(Parcel in) {
            DoodleParams params = new DoodleParams();
            params.mIsDrawableOutside = in.readInt() == 1;
            params.mZoomerScale = in.readFloat();
            params.mMinScale = in.readFloat();
            params.mMaxScale = in.readFloat();
            params.mSupportScaleItem = in.readInt() == 1;
            params.mOptimizeDrawing = in.readInt() == 1;

            return params;
        }

        @Override
        public DoodleParams[] newArray(int size) {
            return new DoodleParams[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mIsDrawableOutside ? 1 : 0);
        dest.writeFloat(mZoomerScale);
        dest.writeFloat(mMinScale);
        dest.writeFloat(mMaxScale);
        dest.writeInt(mSupportScaleItem ? 1 : 0);
        dest.writeInt(mOptimizeDrawing ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
