package cn.yinxm.media.video;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.FragmentActivity;


import cn.yinxm.lib.screen.StatusBarUtils;
import cn.yinxm.media.video.gesture.GestureLayer;
import cn.yinxm.media.video.gesture.touch.adapter.GestureVideoTouchAdapterImpl;
import cn.yinxm.media.video.surface.SimpleTextureViewPlayer;

import static cn.yinxm.media.video.util.VideoUrlTest.getPlayUrl;


public class ScaleVideoActivity extends FragmentActivity {
    private ViewGroup mVideoContent;
    public SimpleTextureViewPlayer mTextureViewPlayer;
    private ImageButton mPlayPauseView;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarUtils.fullScreen(getWindow());
        setContentView(R.layout.activity_scale_video);
        mVideoContent = findViewById(R.id.video_content);
        mTextureViewPlayer = findViewById(R.id.texture_player);
        mPlayPauseView = findViewById(R.id.btn_play_pause);
        initPlayer();
        initGesture(mVideoContent);
        initData();
    }

    private void initData() {
        mPlayPauseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTextureViewPlayer.isPlaying()) {
                    mTextureViewPlayer.pause();
                    changePlayBtnStyle(false);
                } else if (isPaused) {
                    mTextureViewPlayer.start();
                    isPaused = false;
                    changePlayBtnStyle(true);
                } else {
                    mTextureViewPlayer.startPlay(getPlayUrl());
                    changePlayBtnStyle(true);
                }
            }
        });
//        mTextureViewPlayer.startPlay(getPlayUrl());
    }


    private void changePlayBtnStyle(boolean isPlaying) {
        if (isPlaying) {
            mPlayPauseView.setImageResource(R.drawable.small_new_pip_pause);
        } else {
            mPlayPauseView.setImageResource(R.drawable.small_new_pip_play);
        }
    }

    private void initGesture(ViewGroup videoContent) {
        GestureLayer gestureLayer = new GestureLayer(this,
                new GestureVideoTouchAdapterImpl(mTextureViewPlayer) {
                    @Override
                    public boolean isFullScreen() {
                        return true;
                    }
                });
        videoContent.addView(gestureLayer.getContainer());
    }

    private void initPlayer() {
        mTextureViewPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mTextureViewPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

            }
        });
        mTextureViewPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                String newUrl = getPlayUrl(0);
                if (mTextureViewPlayer.getPlayUrl().equals(getPlayUrl(0))) {
                    newUrl = getPlayUrl(1);
                }
                mTextureViewPlayer.startPlay(newUrl);
            }
        });
    }
}