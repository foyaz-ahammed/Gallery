package doodle;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import cn.forward.androids.utils.LogUtil;

/**
 * Created by Administrator on 2016/9/3.
 */
public class DrawUtil {

    // 각도로 회전한 벡터 (px, py)의 새 길이를 계산합니다.
    public static double[] rotateVec(float px, float py, double ang,
                                     boolean isChLen, double newLen) {
        double mathstr[] = new double[2];
        // 矢量旋转函数，参数含义分别是x分量、y分量、旋转角、是否改变长度�?�新长度
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        if (isChLen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
        }
        mathstr[0] = vx;
        mathstr[1] = vy;
        return mathstr;
    }

    public static void drawCircle(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        canvas.drawCircle(cx, cy, radius, paint);
    }

    public static void drawRect(Canvas canvas, float sx, float sy, float dx, float dy, Paint paint) {

        // 왼쪽웃 모서리 및 오른쪽아래 모서리 사이의 대응을 보장합니다.
        if (sx < dx) {
            if (sy < dy) {
                canvas.drawRect(sx, sy, dx, dy, paint);
            } else {
                canvas.drawRect(sx, dy, dx, sy, paint);
            }
        } else {
            if (sy < dy) {
                canvas.drawRect(dx, sy, sx, dy, paint);
            } else {
                canvas.drawRect(dx, dy, sx, sy, paint);
            }
        }
    }

    /**
     * p1을 중심으로한 점 p2의 시계 방향 회전 각도를 계산합니다.
     *
     * @param px1
     * @param py1
     * @param px2
     * @param py2
     * @return 회전각도
     */
    public static float computeAngle(float px1, float py1, float px2, float py2) {

        float x = px2 - px1;
        float y = py2 - py1;

        float arc = (float) Math.atan(y / x);

        float angle = (float) (arc / (Math.PI * 2) * 360);

        if (x >= 0 && y == 0) {
            angle = 0;
        } else if (x < 0 && y == 0) {
            angle = 180;
        } else if (x == 0 && y > 0) {
            angle = 90;
        } else if (x == 0 && y < 0) {
            angle = 270;
        } else if (x > 0 && y > 0) { // 1

        } else if (x < 0 && y > 0) { //2
            angle = 180 + angle;
        } else if (x < 0 && y < 0) { //3
            angle = 180 + angle;
        } else if (x > 0 && y < 0) { //4
            angle = 360 + angle;
        }

        LogUtil.i("hzw", "[" + px1 + "," + py1 + "]:[" + px2 + "," + py2 + "] = " + angle);

        return angle;
    }

    /**
     * @return 시계바늘방향으로 (x, y)위치를 (px, py)를 중심으로 degree만큼 회전하여 coords에 설정하고 반환한다
     */
    public static PointF rotatePoint(PointF coords, float degree, float x, float y, float px, float py) {
        if (degree % 360 == 0) {
            coords.x = x;
            coords.y = y;
            return coords;
        }
        /*각도를 라디안으로 변환*/
        float radian = (float) (degree * Math.PI / 180);
        coords.x = (float) ((x - px) * Math.cos(radian) - (y - py) * Math.sin(radian) + px);
        coords.y = (float) ((x - px) * Math.sin(radian) + (y - py) * Math.cos(radian) + py);

        return coords;
    }

    public static void scaleRect(Rect rect, float scale, float px, float py) {
        rect.left = (int) (px - scale * (px - rect.left) + 0.5f);
        rect.right = (int) (px - scale * (px - rect.right) + 0.5f);
        rect.top = (int) (py - scale * (py - rect.top) + 0.5f);
        rect.bottom = (int) (py - scale * (py - rect.bottom) + 0.5f);
    }
}
