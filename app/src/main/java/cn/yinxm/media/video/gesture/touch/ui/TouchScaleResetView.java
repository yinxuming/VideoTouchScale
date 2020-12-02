package cn.yinxm.media.video.gesture.touch.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.yinxm.media.video.R;

/**
 * <p>
 *
 * @author yinxuming
 * @date 2020/11/25
 */
public abstract class TouchScaleResetView implements View.OnClickListener {
    private Context mContext;
    private View mScaleResetContent;
    private View mScaleResetView;

    public TouchScaleResetView(Context context, ViewGroup container) {
        mContext = context;
        View view = LayoutInflater.from(mContext).inflate(R.layout.touch_scale_rest_view, container);
        mScaleResetContent = view.findViewById(R.id.view_scale_reset);
        mScaleResetView = view.findViewById(R.id.tv_scale_reset);
        mScaleResetView.setOnClickListener(this);
    }

    public void setVisibility(int visibility) {
        mScaleResetContent.setVisibility(visibility);
    }

    public int getVisibility() {
        return mScaleResetContent.getVisibility();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_scale_reset:
                clickResetScale();
                break;
        }
    }

    public abstract void clickResetScale();
}
