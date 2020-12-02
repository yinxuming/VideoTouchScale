package cn.yinxm.media.video.gesture.touch.anim;

import android.animation.ValueAnimator;
import android.graphics.Matrix;

/**
 * 缩放动画
 * <p>
 * 在给定时间内从一个矩阵的变化逐渐动画到另一个矩阵的变化
 */
public abstract class VideoScaleEndAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "VideoScaleEndAnimator";

    /**
     * 图片缩放动画时间
     */
    public static final int SCALE_ANIMATOR_DURATION = 300;

    Matrix mTransMatrix = new Matrix();
    float[] mTransSpan = new float[2];
    float mLastValue;

    /**
     * 构建一个缩放动画
     * <p>
     * 从一个矩阵变换到另外一个矩阵
     *
     * @param start 开始矩阵
     * @param end   结束矩阵
     */
    public VideoScaleEndAnimator(Matrix start, Matrix end) {
        this(start, end, SCALE_ANIMATOR_DURATION);
    }

    /**
     * 构建一个缩放动画
     * <p>
     * 从一个矩阵变换到另外一个矩阵
     *
     * @param start    开始矩阵
     * @param end      结束矩阵
     * @param duration 动画时间
     */
    public VideoScaleEndAnimator(Matrix start, Matrix end, long duration) {
        super();
        setFloatValues(0, 1f);
        setDuration(duration);
        addUpdateListener(this);

        float[] startValues = new float[9];
        float[] endValues = new float[9];
        start.getValues(startValues);
        end.getValues(endValues);
        mTransSpan[0] = endValues[Matrix.MTRANS_X] - startValues[Matrix.MTRANS_X];
        mTransSpan[1] = endValues[Matrix.MTRANS_Y] - startValues[Matrix.MTRANS_Y];
        mTransMatrix.set(start);
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        // 获取动画进度
        float value = (Float) animation.getAnimatedValue();
        // 计算相对于上次位置的偏移量
        float transX = mTransSpan[0] * (value - mLastValue);
        float transY = mTransSpan[1] * (value - mLastValue);
        mTransMatrix.postTranslate(transX, transY);
        updateMatrixToView(mTransMatrix);
        mLastValue = value;
    }

    protected abstract void updateMatrixToView(Matrix transMatrix);
}