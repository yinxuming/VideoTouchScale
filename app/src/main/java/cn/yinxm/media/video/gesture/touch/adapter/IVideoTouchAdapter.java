package cn.yinxm.media.video.gesture.touch.adapter;

import android.view.TextureView;

/**
 * 播放器手势触摸适配，手势与播放器之间的适配层
 * <p>
 *
 * @author yinxuming
 * @date 2020/5/14
 */
public interface IVideoTouchAdapter {
    TextureView getTextureView();

    boolean isPlaying();

    boolean isFullScreen();
}
