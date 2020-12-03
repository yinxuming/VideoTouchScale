[toc]

# Android 视频手势缩放与自动吸附动效实现

## 1. 功能需求
![avatar](/doc/scale.gif)
1. 双指缩放视频播放画面，支持设定最小、最大缩放范围
2. 双指拖动画面可任意方向移动
3. 如果是缩小画面，最后需要在屏幕居中显示，并且需要有动画效果
4. 如果是放大画面，有画面边缘在屏幕内的，需要自动吸附到屏幕边缘
5. 视频暂停状态下也能缩放

## 2. 实现原理
1. 先进行缩放平移。
   通过`View.getMatrix()`获取当前播放画面的Matrix，进行矩阵变换：缩放、平移，改变画面位置和大小，实现播放画面缩放功能。
2. 缩放结束后，进行属性动画。
   当前画面对应的矩阵变换为`mScaleTransMatrix`，计算动画结束应该移动的位`scaleEndAnimMatrix`，进行属性动画从`mScaleTransMatrix`变化为`scaleEndAnimMatrix`。

### 2.1 如何检测手势缩放？
1. `View.onTouchEvent`。分别监听手指按下（`MotionEvent.ACTION_POINTER_DOWN`）、抬起（`MotionEvent.ACTION_POINTER_UP`）、移动（`MotionEvent.ACTION_MOVE`）
1. `ScaleGestureDetector`。直接使用手势缩放检测`ScaleGestureDetector`对View#onTouchEvent中的手势变化进行识别，通过`ScaleGestureDetector.OnScaleGestureListener`得到onScaleBegin-onScale-onScale ... -onScaleEnd的缩放回调，在回调中处理响应的缩放逻辑。

#### 1. View.onTouchEvent关键代码
```java
public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction() & MotionEvent.ACTION_MASK;
    switch (action) {
        case MotionEvent.ACTION_POINTER_DOWN:
            onScaleBegin(event);
            break;
        case MotionEvent.ACTION_POINTER_UP:
            onScaleEnd(event);
            break;
        case MotionEvent.ACTION_MOVE:
            onScale(event);
            break;
        case MotionEvent.ACTION_CANCEL:
            cancelScale(event);
            break;
    }
    return true;
}
```
#### 2. ScaleGestureDetector
使用`ScaleGestureDetector`来识别onTouchEvent中的手势触摸操作，得到`onScaleBegin`、`onScale`、`onScaleEnd`三种回调，在回调里面通过`VideoTouchScaleHandler`对视频进行缩放、平移操作。

1. 添加手势触摸层`GestureLayer`，使用`ScaleGestureDetector`识别手势
    ```java
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

    ...
    }
    ```

2. **ScaleGestureDetector.OnScaleGestureListener** 手势缩放回调处理

    ```java
    /**
     * 手势缩放 播放画面
     */
    public class VideoScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
        private static final String TAG = "VideoScaleGestureListener";
        private IGestureLayer mGestureLayer;
        public VideoTouchScaleHandler mScaleHandler;

        public VideoScaleGestureListener(IGestureLayer gestureLayer) {
            mGestureLayer = gestureLayer;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mScaleHandler != null) {
                return mScaleHandler.onScale(detector);
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (mScaleHandler != null) {
                boolean isConsume = mScaleHandler.onScaleBegin(detector);
                if (isConsume) {
                    return true;
                }
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mScaleHandler != null) {
                mScaleHandler.onScaleEnd(detector);
            }

        }
    }
    ```

### 2.2 缩放平移处理
1. **双指缩放**
    使用`Matrix.postScale(float sx, float sy, float px, float py)`，这里有几个参数，前两个指定x，y轴上的缩放倍数，后两个指定缩放中心点位置。
    - 如何计算**缩放倍数**？
        本次缩放倍数 = 本次两指间距 / 上次两指间距：`currentDiffScale = detector.getCurrentSpan() / mLastSpan`
    - 如何确定**缩放中心点**？
        缩放中心为两指开始触摸时的中心位置点，即`onScaleBegin`时，`scaleCenterX = detector.getFocusX(); scaleCenterY = detector.getFocusY();`
    - **postXXX**和**preXXX**的区别？
        postXXX为右乘，preXXX为前乘。出现这两种操作，主要是**矩阵乘法不满足交换律**，实际使用过程中，固定选择一种方式即可。为了方便理解，直接来段代码，令：原矩阵M，位移变换矩阵T(x, y)，则：
        ```java
        M.postTranslate(tx, ty); // 等价 M' = T * M
        M.preTranslate(tx, ty); // 等价 M' = M * T
        ```
2. **双指平移**
    双指可平移拖动画面到新位置，平移使用：`Matrix.postTranslate(float dx, float dy)
`，dx和dy表示相对当前的Matrix的位置需要移动的**距离**，注意一定是相对于当前的Matrix位置，而不是相对onScaleBegin时的Matrix初始位置。
   - 如何确定**平移距离**？
    本次移动距离 = 本次中心点 - 上次中心点
    ```java
     dx = detector.getFocusX() - mLastCenterX
     dy = detector.getFocusY() - mLastCenterY
    ```
### 2.3 暂停画面下缩放
默认不处理，暂停画面情况下，Matrix变换后，更新到TextureView上，画面是不会发生变化的，要想画面实时更新，调用`TextureView.invalidate()`即可。


### 2.4 缩放移动结束后动效
缩放结束后（onScaleEnd），为了增强交互体验，需要根据缩放的大小、位置，重新调整画面，动画移动到指定位置。指定位置主要有**居中**和**吸附屏幕边缘**两种。
动画的移动，主要采用属性动画`ValueAnimator`.

#### 1. 缩小居中
缩放结束后，画面如果处于缩小模式，需要将画面移动到屏幕中央。
1. 如何计算**居中位置矩阵**变换值？
    缩放位移结束后得到变换后的矩阵`mScaleTransMatrix`，这也是动画的起始值，现在要推导动画的结束位置矩阵`scaleEndAnimMatrix`，要求在屏幕中居中，如果要直接用`mScaleTransMatrix`进行变换得到动画结束矩阵，
    需要在xy上平移一定距离，但是该距离具体指并不好计算。
    这里我们从另一个方向下手，知道当前的缩放倍速`mScale`，视频TextureView占的区域，那么直接以该区域中心点进行矩阵缩放变化，就可以得到中心位置矩阵`scaleEndAnimMatrix`
     ```java
     RectF videoRectF = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
     if (mScale > 0 && mScale <= 1.0f) { // 缩小居中
         scaleEndAnimMatrix.reset();
         scaleEndAnimMatrix.postScale(mScale, mScale, videoRectF.right / 2, videoRectF.bottom / 2);
     }
     ```
2. 属性动画中间值，如何得到中间位置变换矩阵？
    - 动画开始矩阵：`mScaleTransMatrix`；
    - 动画开始矩阵：`scaleEndAnimMatrix`；
    当从`mScaleTransMatrix`动画移动到`scaleEndAnimMatrix`位置时，中间的矩阵无非就是在x、y上位移了一定距离。以x轴为例：
    1. x轴总位移：totalTransX = scaleEndAnimMatrix矩阵中取出MTRANS_X分量值 - mScaleTransMatrix矩阵中取出MTRANS_X分量值
    1. 本次x轴移动距离：transX = totalTransX * 本次动画变化值 = totalTransX * (animation.getAnimatedValue() - mLastValue);

#### 2. 放大吸边
缩放结束后，如果画面处于放大，且有画面边缘在屏幕内的，需要自动吸附到屏幕边缘。
1. 如何判断是否有**画面边缘在屏幕内部**？
    需要考虑四边：left、top、right、bottom位置的情况。如果要考虑画面在屏幕内部的总情况数，比较繁琐和复杂，比如以left为例：有3种情况：
     1. left：仅left边在屏幕内部，top、bottom边在屏幕外部，只需要移动画面left边到**屏幕左边**即可
     2. left + top：left边和top边在屏幕内部，需要移动画面到屏幕**左上角**顶点位置
     3. left + bottom：同上，需要移动画面到屏幕**左下角**顶点位置

     总共有8种情况，那有没有简单的方法？
     有的，实际上，不管哪种情况，我们只需要关注**画面的x、y方向需要移动的距离**即可。问题简化为求画面在x、y轴上移动的距离：`transAnimX`、`transAnimY`
     只要知道上述两个值，将当前画面位移进行位移，即可得到动画结束位置矩阵`scaleEndAnimMatrix`。
     ```java
      scaleEndAnimMatrix.set(mScaleTransMatrix);
      scaleEndAnimMatrix.postTranslate(transAnimX, transAnimY);
     ```
2. 如何计算画面在屏幕内部需要移动到各屏幕边缘的距离`transAnimX`、`transAnimY`？
    要解决这个问题，需要知道**屏幕位置**，**播放画面位置**。
    屏幕的位置很好办，实际上就是画面原始大小位置：`RectF videoRectF = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());`
    当前缩放移动后画面的位置呢？
    它对应的矩阵变化是`mScaleTransMatrix`，那能不能**根据这个矩阵推导出当前画面的位置**？
    可以的，我们去找Matrix对外提供的接口，会发现有一个`Matrix.mapRect(RectF)`方法，这个方法就是用来测量**矩形区域经过矩阵变化**后，新的矩形区域所在**位置**。直接上代码：
    ```java
    if (mScale > 1.0F) { // 放大，检测4边是否有在屏幕内部，有的话自动吸附到屏幕边缘
        RectF rectF = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
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
            // 注意这里的处理方式：分别处理x轴位移和y轴位移即可全部覆盖上述8种情况
            if (rectF.top > videoRectF.top) {  // 上移吸边
                transAnimY = videoRectF.top - rectF.top;
            } else if (rectF.bottom < videoRectF.bottom) { // 下移吸边
                transAnimY = videoRectF.bottom - rectF.bottom;
            }
            // 计算移动到屏幕边缘位置后的矩阵
            scaleEndAnimMatrix.postTranslate(transAnimX, transAnimY);
    }
    ```

## 3. 项目完整代码
[github完整源码](https://github.com/yinxuming/VideoTouchScale)

### 3.1 手势缩放处理：VideoTouchScaleHandler
```java
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

```
### 3.2 动画：VideoScaleEndAnimator
```java

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
```