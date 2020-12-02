package cn.yinxm.media.video.gesture.touch;

import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * <p>
 *
 * @author yinxuming
 * @date 2020/11/24
 */
public interface IGestureLayer {
    FrameLayout getContainer();

    /**
     * 事件处理器
     */
    void initTouchHandler();

    /**
     * 分发touch事件
     *
     * @param event
     * @return
     */
    boolean onGestureTouchEvent(MotionEvent event);

    void onLayerRelease();
}
