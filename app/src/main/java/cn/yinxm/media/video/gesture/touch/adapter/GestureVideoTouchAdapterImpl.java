package cn.yinxm.media.video.gesture.touch.adapter;

import android.view.TextureView;

import cn.yinxm.media.video.controller.VideoPlayController;


/**
 * 播放器手势触摸适配，兼容HkBaseVideoView升级到新播放器BaseVideoPlayer
 * <p>
 *
 * @author yinxuming
 * @date 2020/5/18
 */
public class GestureVideoTouchAdapterImpl implements IVideoTouchAdapter {
    VideoPlayController mPlayController;

    public GestureVideoTouchAdapterImpl(VideoPlayController playController) {
        mPlayController = playController;
    }

    @Override
    public TextureView getTextureView() {
        if (mPlayController instanceof TextureView) {
            return (TextureView) mPlayController;
        }
        return null;
    }

    @Override
    public boolean isPlaying() {
        return mPlayController.isPlaying();
    }



    @Override
    public boolean isFullScreen() {
        return false;
    }
}
