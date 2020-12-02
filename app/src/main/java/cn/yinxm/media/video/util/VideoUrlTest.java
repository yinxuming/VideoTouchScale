package cn.yinxm.media.video.util;

import android.text.TextUtils;

/**
 *
 */
public class VideoUrlTest {
    private static String[] URLS = new String[]{
            "https://vd2.bdstatic.com/mda-kfnpxpc86wm7xa9t/v2-patchcae/mda-kfnpxpc86wm7xa9t.mp4",
            "https://video19.ifeng.com/video06/2012/04/11/629da9ec-60d4-4814-a940-997e6487804a.mp4"
    };

    public static String getPlayUrl() {
        return getPlayUrl(0);
    }

    public static String getPlayUrl(int index) {
        return URLS[index];
    }

    public static String getNextUrl(String playUrl) {
        if (!TextUtils.isEmpty(playUrl)) {
            for (int i = 0; i < URLS.length; i++) {
                if (playUrl.equals(URLS[i])) {
                    return getPlayUrl((i + 1) % URLS.length);
                }
            }
        }
        return getPlayUrl(0);
    }
}
