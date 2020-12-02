package cn.yinxm.media.video.gesture.touch.handler;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import cn.yinxm.lib.utils.log.LogUtil;
import cn.yinxm.media.video.gesture.touch.adapter.IVideoTouchAdapter;
import cn.yinxm.media.video.gesture.touch.anim.VideoScaleEndAnimator;
import cn.yinxm.media.video.gesture.touch.ui.TouchScaleResetView;


/**
 *  播放器画面双指手势缩放处理：
 *  <p>
 *  1. 双指缩放
 *  2. 双指平移
 *  3. 缩放结束后，若为缩小画面，居中动效
 *  4. 缩放结束后，若为放大画面，自动吸附屏幕边缘动效
 *  5. 暂停播放下，实时更新缩放画面
 *
 * @author yinxuming
 * @date 2020/12/2
 */
public class VideoTouchScaleHandler implements IVideoTouchHandler, ScaleGestureDetector.OnScaleGestureListener {
    private static final String TAG = "VideoTouchScaleHandler";


    private Context mContext;
    public FrameLayout mContainer;
    private boolean openScaleTouch = true; // 开启缩放
    private boolean mIsScaleTouch;
    private Matrix mScaleTransMatrix; // 缓存了上次的矩阵值，所以需要计算每次变化量
    private float mStartCenterX, mStartCenterY, mLastCenterX, mLastCenterY, centerX, centerY;
    private float mStartSpan, mLastSpan, mCurrentSpan;
    private float mScale;
    private float[] mMatrixValue = new float[9];
    private float mMinScale = 0.1F, mMaxScale = 3F;
    private VideoScaleEndAnimator mScaleAnimator;

    IVideoTouchAdapter mTouchAdapter;
    TouchScaleResetView mScaleRestView;

    public VideoTouchScaleHandler(Context context, FrameLayout container,
                                  IVideoTouchAdapter videoTouchAdapter) {
        mContext = context;
        mContainer = container;
        mTouchAdapter = videoTouchAdapter;
        initView();
    }

    private void initView() {
        mScaleRestView = new TouchScaleResetView(mContext, mContainer) {
            @Override
            public void clickResetScale() {
                mScaleRestView.setVisibility(View.GONE);
                if (isScaled()) {
                    cancelScale();
                }
            }
        };
    }

    private Context getContext() {
        return mContext;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {

        TextureView mTextureView = mTouchAdapter.getTextureView();
        if (mTextureView != null) {
            mIsScaleTouch = true;
            if (mScaleTransMatrix == null) {
                mScaleTransMatrix = new Matrix(mTextureView.getMatrix());
                onScaleMatrixUpdate(mScaleTransMatrix);
            }
        }
        mStartCenterX = detector.getFocusX();
        mStartCenterY = detector.getFocusY();
        mStartSpan = detector.getCurrentSpan();

        mLastCenterX = mStartCenterX;
        mLastCenterY = mStartCenterY;
        mLastSpan = mStartSpan;
        return true;
    }

    private void updateMatrixToTexture(Matrix newMatrix) {
        TextureView mTextureView = mTouchAdapter.getTextureView();
        if (mTextureView != null) {
            mTextureView.setTransform(newMatrix);
        }
        onScaleMatrixUpdate(newMatrix);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mIsScaleTouch && openScaleTouch) {
            mCurrentSpan = detector.getCurrentSpan();
            centerX = detector.getFocusX();
            centerY = detector.getFocusY();
            if (processOnScale(detector)) {
                mLastCenterX = centerX;
                mLastCenterY = centerY;
                mLastSpan = mCurrentSpan;
            }
        }

        return false;
    }

    private boolean processOnScale(ScaleGestureDetector detector) {
        float diffScale = mCurrentSpan / mLastSpan;
        if (mTouchAdapter.isFullScreen()) {
            if (mScaleTransMatrix != null) {
                postScale(mScaleTransMatrix, diffScale, mStartCenterX, mStartCenterY);
                mScaleTransMatrix.postTranslate(detector.getFocusX() - mLastCenterX,
                        detector.getFocusY() - mLastCenterY);
                onScaleMatrixUpdate(mScaleTransMatrix);
                TextureView mTextureView = mTouchAdapter.getTextureView();
                if (mTextureView != null) {
                    Matrix matrix = new Matrix(mTextureView.getMatrix());
                    matrix.set(mScaleTransMatrix);
                    mTextureView.setTransform(matrix);
                }
                int scaleRatio = (int) (mScale * 100);
                Toast.makeText(getContext(), "" + scaleRatio + "%", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    private void postScale(Matrix matrix, float scale, float x, float y) {
        matrix.getValues(mMatrixValue);
        float curScale = mMatrixValue[Matrix.MSCALE_X];
        if (scale < 1 && Math.abs(curScale - mMinScale) < 0.001F) {
            scale = 1;
        } else if (scale > 1 && Math.abs(curScale - mMaxScale) < 0.001F) {
            scale = 1;
        } else {
            curScale *= scale;
            if (scale < 1 && curScale < mMinScale) {
                curScale = mMinScale;
                scale = curScale / mMatrixValue[Matrix.MSCALE_X];
            } else if (scale > 1 && curScale > mMaxScale) {
                curScale = mMaxScale;
                scale = curScale / mMatrixValue[Matrix.MSCALE_X];
            }
            matrix.postScale(scale, scale, x, y);
        }
    }


    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (mIsScaleTouch) { // 取消多手势操作
            mIsScaleTouch = false;
            doScaleEndAnim();
        }
    }

    public void cancelScale() {
        TextureView mTextureView = mTouchAdapter.getTextureView();
        if (mScaleTransMatrix != null && mTextureView != null) {
            mIsScaleTouch = false;
            mScaleTransMatrix.reset();
            onScaleMatrixUpdate(mScaleTransMatrix);
            Matrix matrix = new Matrix(mTextureView.getMatrix());
            matrix.reset();
            mTextureView.setTransform(matrix);
        }
    }

    /**
     * 计算缩放结束后动画位置：scaleEndAnimMatrix
     */
    private void doScaleEndAnim() {
        TextureView mTextureView = mTouchAdapter.getTextureView();
        if (mTextureView == null) {
            return;
        }
        Matrix scaleEndAnimMatrix = new Matrix();
        RectF videoRectF = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
        if (mScale > 0 && mScale <= 1.0f) { // 缩小居中
            scaleEndAnimMatrix.postScale(mScale, mScale, videoRectF.right / 2, videoRectF.bottom / 2);
            startTransToAnimEnd(mScaleTransMatrix, scaleEndAnimMatrix);
        } else if (mScale > 1.0F) { // 放大，检测4边是否有在屏幕内部，有的话自动吸附到屏幕边缘
            RectF rectF = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
            // 测量经过缩放位移变换后的播放画面位置
            mScaleTransMatrix.mapRect(rectF);
            float transAnimX = 0f;
            float transAnimY = 0f;
            scaleEndAnimMatrix.set(mScaleTransMatrix);
            if (rectF.left > videoRectF.left
                    || rectF.right < videoRectF.right
                    || rectF.top > videoRectF.top
                    || rectF.bottom < videoRectF.bottom) { // 放大情况下，有一边缩放后在屏幕内部，自动吸附到屏幕边缘
                if (rectF.left > videoRectF.left) { // 左移吸边
                    transAnimX = videoRectF.left - rectF.left;
                } else if (rectF.right < videoRectF.right) {  // 右移吸边
                    transAnimX = videoRectF.right - rectF.right;
                }
                if (rectF.top > videoRectF.top) {  // 上移吸边
                    transAnimY = videoRectF.top - rectF.top;
                } else if (rectF.bottom < videoRectF.bottom) { // 下移吸边
                    transAnimY = videoRectF.bottom - rectF.bottom;
                }

                scaleEndAnimMatrix.postTranslate(transAnimX, transAnimY);
                startTransToAnimEnd(mScaleTransMatrix, scaleEndAnimMatrix);
            }
        }
    }

    private void startTransToAnimEnd(Matrix startMatrix, Matrix endMatrix) {
        LogUtil.d(TAG, "startTransToAnimEnd \nstart=" + startMatrix + "\nend=" + endMatrix);
        // 令 A = startMatrix；B = endMatrix
        // 方法1：直接将画面更新为结束矩阵位置B
//        updateMatrixToView(endMatrix); //
        // 方法2：将画面从现有位置A，移动到结束矩阵位置B，移动的距离T。B = T * A; 根据矩阵乘法的计算规则，反推出：T(x) = B(x) - A(x); T(y) = B(y) - A(y)
//        float[] startArray = new float[9];
//        float[] endArray = new float[9];
//        startMatrix.getValues(startArray);
//        endMatrix.getValues(endArray);
//        float transX = endArray[Matrix.MTRANS_X] - startArray[Matrix.MTRANS_X];
//        float transY = endArray[Matrix.MTRANS_Y] - startArray[Matrix.MTRANS_Y];
//        startMatrix.postTranslate(transX, transY);
//        LogUtil.d(TAG, "transToCenter1 \nstart=" + startMatrix + "\nend" + endMatrix);
//        updateMatrixToView(startMatrix);

        // 方法3：在方法2基础上，增加动画移动效果
        if (mScaleAnimator != null) {
            mScaleAnimator.cancel();
            mScaleAnimator = null;
        }
        if (mScaleAnimator == null) {
            mScaleAnimator = new VideoScaleEndAnimator(startMatrix, endMatrix) {

                @Override
                protected void updateMatrixToView(Matrix transMatrix) {
                    updateMatrixToTexture(transMatrix);
                }
            };
            mScaleAnimator.start();
        }

        mScaleTransMatrix = endMatrix;
    }

    public void showScaleReset() {
        if (isScaled() && mTouchAdapter != null && mTouchAdapter.isFullScreen()) {
            if (mScaleRestView != null && mScaleRestView.getVisibility() != View.VISIBLE) {
                mScaleRestView.setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //  缩放模式下，是否需要单手滚动
//        if (isScaled(mScale) && mScaleTransMatrix != null) {
//            TextureView mTextureView = mTouchAdapter.getTextureView();
//            if (mTextureView != null) {
//                postTranslate(mScaleTransMatrix, -distanceX, -distanceY);
//                onScaleMatrixUpdate(mScaleTransMatrix);
//                Matrix matrix = new Matrix(mTextureView.getMatrix());
//                matrix.set(mScaleTransMatrix);
//                mTextureView.setTransform(matrix);
//                return true;
//            }
//        }
        return false;
    }



    private void onScaleMatrixUpdate(Matrix matrix) {
        matrix.getValues(mMatrixValue);
        mScale = mMatrixValue[Matrix.MSCALE_X];
        // 暂停下，实时更新缩放画面
        if (!mTouchAdapter.isPlaying()) {
            TextureView mTextureView = mTouchAdapter.getTextureView();
            if (mTextureView != null) {
                mTextureView.invalidate();
            }
        }
    }

    /**
     * 是否处于已缩放 or 缩放中
     *
     * @return
     */
    public boolean isInScaleStatus() {
        return isScaled(mScale) || mIsScaleTouch;
    }

    public boolean isScaled() {
        return isScaled(mScale);
    }

    private boolean isScaled(float scale) {
        return scale > 0 && scale <= 0.99F || scale >= 1.01F;
    }
}
