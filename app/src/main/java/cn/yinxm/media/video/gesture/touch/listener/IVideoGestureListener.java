package cn.yinxm.media.video.gesture.touch.listener;

import android.view.GestureDetector;
import android.view.MotionEvent;

public interface IVideoGestureListener extends GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {
    boolean onTouchEvent(MotionEvent event);
}