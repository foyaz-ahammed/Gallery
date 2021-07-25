package doodle;

import android.graphics.Bitmap;

public interface IDoodleListener {

    /**
     * 화상이 보관될때 호출된다.
     *
     * @param doodle
     * @param doodleBitmap       보관하려는 화상
     * @param callback  그리기를 계속하기 위하여 보관후 호출되는 callback
     */
    void onSaved(IDoodle doodle, Bitmap doodleBitmap, Runnable callback);

    /**
     * view의 크기가 결정고 그리기를 할 준비가 되였을때 호출된다. 이때 두께, 색, 형태, 모양등을 설정할수 있다.
     * @param doodle
     */
    void onReady(IDoodle doodle);

}
