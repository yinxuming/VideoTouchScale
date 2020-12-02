package cn.yinxm.media.video.gesture;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.FrameLayout;

import cn.yinxm.media.video.gesture.touch.IGestureLayer;
import cn.yinxm.media.video.gesture.touch.adapter.IVideoTouchAdapter;
import cn.yinxm.media.video.gesture.touch.handler.VideoTouchScaleHandler;
import cn.yinxm.media.video.gesture.touch.listener.VideoScaleGestureListener;


/**
 * 手势处理layer层
 */
public final class GestureLayer implements IGestureLayer, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {
    private static final String TAG = "GestureLayer";

    private Context mContext;
    private FrameLayout mContainer;

    /** 手势检测 */
    private GestureDetector mGestureDetector;

    /** 手势缩放 检测 */
    private ScaleGestureDetector mScaleGestureDetector;
    /** 手势缩放 监听 */
    private VideoScaleGestureListener mScaleGestureListener;
    /** 手势缩放 处理 */
    private VideoTouchScaleHandler mScaleHandler;


    private IVideoTouchAdapter mVideoTouchAdapter;

    public GestureLayer(Context context, IVideoTouchAdapter videoTouchAdapter) {
        mContext = context;
        mVideoTouchAdapter = videoTouchAdapter;
        initContainer();
        initTouchHandler();
    }

    @Override
    public FrameLayout getContainer() {
        return mContainer;
    }

    protected Context getContext() {
        return mContext;
    }

    private void initContainer() {
        mContainer = new FrameLayout(mContext) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                return super.dispatchTouchEvent(ev);
            }

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                boolean isConsume = onGestureTouchEvent(event);
                if (isConsume) {
                    return true;
                } else {
                    return super.onTouchEvent(event);
                }
            }
        };
    }

    public void initTouchHandler() {
        mGestureDetector = new GestureDetector(mContext, this);
        mGestureDetector.setOnDoubleTapListener(this);

        // 手势缩放
        mScaleGestureListener = new VideoScaleGestureListener(this);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), mScaleGestureListener);

        // 缩放 处理
        mScaleHandler = new VideoTouchScaleHandler(getContext(), mContainer, mVideoTouchAdapter);
        mScaleGestureListener.mScaleHandler = mScaleHandler;

    }

    @Override
    public void onLayerRelease() {
        if (mGestureDetector != null) {
            mGestureDetector.setOnDoubleTapListener(null);
        }
    }

    @Override
    public boolean onGestureTouchEvent(MotionEvent event) {
        try {
            int pointCount = event.getPointerCount();
            if (pointCount == 1 && event.getAction() == MotionEvent.ACTION_UP) {
                if (mScaleHandler.isScaled()) {
                    mScaleHandler.showScaleReset();
                }
            }
            if (pointCount > 1) {
                boolean isConsume = mScaleGestureDetector.onTouchEvent(event);
                if (isConsume) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return onSingleTap(e);
    }

    /**
     * 单击事件处理
     *
     * @param event 触摸事件
     */
    private boolean onSingleTap(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mScaleHandler.isInScaleStatus()) {
//                    if (mScaleHandler.isScaled()) {
            return mScaleHandler.onScroll(e1, e2, distanceX, distanceY);
//                    }
        }
                return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        return false;
    }

}
