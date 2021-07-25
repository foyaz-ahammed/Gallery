package doodle;

import android.content.Context;

import cn.forward.androids.TouchGestureDetector;

public class DoodleTouchDetector extends TouchGestureDetector implements IDoodleTouchDetector {
    public DoodleTouchDetector(Context context, IOnTouchGestureListener listener) {
        super(context, listener);
        // 다음 두 줄의 값들을 1보다 크거나 같게 설정해햐 합니다
        this.setScaleSpanSlop(1); // gesture전에 확대/축소로 인식되는 두 손가락 끌기의 최소 거리
        this.setScaleMinSpan(1); // 확대/축소중에 확대/축소 동작으로 인식되는 두 손가락 사이의 최소 거리

        this.setIsLongpressEnabled(false);
        this.setIsScrollAfterScaled(false);
    }
}
