/*
Copyright 2013-2015 David Morrissey

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package xyz.zpayh.hdimageview;


import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.render.PixelMapHolder;
import ohos.aafwk.ability.Ability;
import ohos.agp.utils.*;
import ohos.agp.window.service.Display;
import ohos.agp.window.service.DisplayAttributes;
import ohos.agp.window.service.DisplayManager;
import ohos.eventhandler.EventRunner;
import ohos.global.resource.NotExistException;
import ohos.global.resource.WrongTypeException;
import ohos.multimodalinput.event.MmiPoint;
import ohos.multimodalinput.event.TouchEvent;
import ohos.utils.PacMap;
import ohos.agp.render.Canvas;
import ohos.app.Context;
import ohos.media.image.PixelMap;
import ohos.agp.render.render3d.BuildConfig;
import ohos.agp.components.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import ohos.agp.render.Paint;
import ohos.utils.net.Uri;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.zpayh.hdimageview.animation.AnimatorListener;
import xyz.zpayh.hdimageview.animation.AnimatorUpdateListener;
import xyz.zpayh.hdimageview.animation.ValueAnimator;
import xyz.zpayh.hdimageview.core.DecelerateInterpolator;
import xyz.zpayh.hdimageview.core.HDImageViewFactory;
import xyz.zpayh.hdimageview.datasource.interceptor.AssetInterceptor;
import xyz.zpayh.hdimageview.datasource.BitmapDataSource;
import xyz.zpayh.hdimageview.datasource.DefaultBitmapDataSource;
import xyz.zpayh.hdimageview.datasource.interceptor.FileInterceptor;
import xyz.zpayh.hdimageview.util.Build;
import xyz.zpayh.hdimageview.state.Orientation;
import xyz.zpayh.hdimageview.state.ScaleType;
import xyz.zpayh.hdimageview.state.Translation;
import xyz.zpayh.hdimageview.state.Zoom;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.InnerEvent;
import static ohos.multimodalinput.event.TouchEvent.*;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_0;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_180;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_270;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_90;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_EXIF;
import static xyz.zpayh.hdimageview.state.ScaleType.CENTER_CROP;
import static xyz.zpayh.hdimageview.state.ScaleType.CENTER_INSIDE;
import static xyz.zpayh.hdimageview.state.ScaleType.CUSTOM;
import static xyz.zpayh.hdimageview.state.Translation.CENTER;
import static xyz.zpayh.hdimageview.state.Translation.COUSTOM;
import static xyz.zpayh.hdimageview.state.Translation.INSIDE;
import static xyz.zpayh.hdimageview.state.Translation.OUTSIDE;
import static xyz.zpayh.hdimageview.state.Zoom.ZOOM_FOCUS_CENTER;
import static xyz.zpayh.hdimageview.state.Zoom.ZOOM_FOCUS_CENTER_IMMEDIATE;
import static xyz.zpayh.hdimageview.state.Zoom.ZOOM_FOCUS_FIXED;
import static xyz.zpayh.hdimageview.util.Utils.fileRect;

/**
 * 文 件 名: HDImageView
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/4/1 14:51
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 * 参考于 Subsampling Scale Image View
 */

import ohos.utils.PlainArray;
import xyz.zpayh.hdimageview.util.LogUtil;
import xyz.zpayh.hdimageview.util.Utils;

public class HDImageView extends Component
        implements
        Component.TouchEventListener,
        Component.DrawTask,
        Component.LayoutRefreshedListener,
        Component.EstimateSizeListener,
        Component.DraggedListener {

    private static final String TAG = "HDImageView";

    public static final int MSG_INIT_SUCCESS = 1;
    public static final int MSG_INIT_FAILED = 2;
    public static final int MSG_TILE_LOAD_SUCCESS = 3;

    public static final String SOURCE_WIDTH = "source width";
    public static final String SOURCE_Height = "source height";
    public static final String SOURCE_ORIENTATION = "source orientation";
    private static final int DEFAULT_DURATION = 500;



//    private View a;

    //Location of images
    private Uri mUri;

//    Component(Context context)

    //
    private int mMaxSampleSize;

    private PlainArray<List<Mapping>> mMappingMap;

    @Orientation
    private int mOrientation = ORIENTATION_EXIF;

    private float mMaxScale = 2F;

    private float mMinScale = 0.1F;

    private Context mContext;
    /**
     * Density reached before loading higher resolution textures
     */
    private int mMinimumMappingDpi = -1;

    @Translation
    private int mTranslateLimit = INSIDE;

    private RectF mCustomRange = new RectF();

    @ScaleType
    private int mScaleType = CENTER_INSIDE;

    public final static int MAPPING_SIZE_AUTO = Integer.MAX_VALUE;
    private int mMaxMappingWidth = MAPPING_SIZE_AUTO;
    private int mMaxMappingHeight = MAPPING_SIZE_AUTO;

    private boolean mTranslateEnabled = true;
    private boolean mZoomEnabled = true;
    private boolean mQuickScaleEnabled = true;

    //Thread processing
    private EventHandler mOriginalHandler;
    private Executor mOriginalExecutor;

    //Double-click zoom behavior
    private float mDoubleTapZoomScale = 1F;
    @Zoom
    private int mDoubleTapZoomStyle = ZOOM_FOCUS_FIXED;

    // The current zoom value and zoom value at the start of zooming
    private float mScale;
    private float mScaleStart;

    // The screen coordinates of the upper left corner of the source image
    private PointF mViewTranslate;
    private final PointF mViewTranslateStart = new PointF(0F, 0F);

    //The source coordinate is the center, used when setting a new position outside before the view is ready
    private float mPendingScale = -1f;
    private PointF mSourcePendingCenter;

    // Source image size and orientation-the size is related to the unrotated image
    private int mSourceWidth;
    private int mSourceHeight;
    private int mSourceOrientation;
    private Rect mSourceRegion;

    private ImageSourceLoadListener mImageSourceLoadListener;

    //Two-finger zoom in progress
    private boolean mIsZooming;
    //One finger pan is in progress
    private boolean mIsPanning;
    //Maximum touch used in current gesture
    private int mMaxTouchCount;

    //Quick swipe detector
    private Component.DraggedListener mFlingDetector;
//    private Component.ClickedListener mSingleClickDetector;
//    private Component.DoubleClickedListener mDoubleClickDetector;
    private Component.TouchEventListener mTouchDetector;
    private Component.TouchEventListener mLongPressDetector;

    //Tiling and image decoding
    private BitmapDataSource mBitmapDataSource;
    final Object mLock = new Object();

    // Debug value
    private final PointF mLastViewCenter = new PointF(0F, 0F);
    private float mLastViewDistance;

    private int mDuration = DEFAULT_DURATION;
 //   public static float mFactor;
    // Zoom and center animation tracking
    private SimpleValueAnimator mValueAnimator;
    private AnimatorListener mAnimatorListener;
    private AnimatorUpdateListener mAnimatorUpdateListener;
    private DecelerateInterpolator mScaleAnimationInterpolator;
    private DecelerateInterpolator mTranslationAnimationInterpolator;
    //Whether the notification has been sent to the subclass
    private boolean mReadySent;
    //Whether the notification loaded by the base layer has been sent to the subclass
    private boolean mImageLoadedSent;

    private OnBitmapLoadListener mOnBitmapLoadListener;

    private LongClickedListener mOnLongClickListener;

    private Paint mBitmapPaint;
    private Paint mMappingBgPaint;

    private final ScaleAndTranslate mSatTemp = new ScaleAndTranslate();
    private final Matrix mMatrix = new Matrix();

   /* private Preferences sharedPreferences;
    private final String PREFERENCE_KEY = "MY_PREFERENCE";
*/
    private float mDensity;

    private float[] mSrcArray = new float[8];
    private float[] mDstArray = new float[8];

    //        <?xml version="1.0" encoding="utf-8"?>
//<resources>
//    <declare-styleable name="HDImageView">
//        <attr name="src" format="reference"/>
//        <attr name="duration" format="integer"/>
//        <attr name="translateEnabled" format="boolean"/>
//        <attr name="zoomEnabled" format="boolean"/>
//        <attr name="quickScaleEnabled" format="boolean"/>
//        <attr name="mappingBackgroundColor" format="color"/>
//    </declare-styleable>
//</resources>

    private static final String Image_Src = "src";
    private static final String Decelerate_Interpolator_Factor = "decelerateInterpolator_Factor";
    private static final String Image_Duration = "duration";
    private static final String Image_translateEnabled = "translateEnabled";
    private static final String Image_zoomEnabled = "zoomEnabled";
    private static final String Image_quickScaleEnabled = "quickScaleEnabled";
    private static final String Image_mappingBackgroundColor = "mappingBackgroundColor";

    private final AnimatorUpdateListener mDefaultAnimatorUpdateListener = new AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            updateScaleAndTranslate();
        }
    };

    private final AnimatorListener mDefaultAnimatorListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationEnd(ValueAnimator animation) {
            updateScaleAndTranslate();
        }
    };

    public HDImageView(Context context) throws NotExistException, WrongTypeException, IOException {
        this(context, null);
    }

    public HDImageView(Context context, @Nullable AttrSet attrset) throws NotExistException, WrongTypeException, IOException {
        this(context, attrset, 0);
    }

    public HDImageView(Context context, @Nullable AttrSet attrset, int defStyleAttr) throws NotExistException, WrongTypeException, IOException {
        super(context, attrset, defStyleAttr);
        init(context, attrset, defStyleAttr);
    }

    private void init(Context context, @Nullable AttrSet attrset, int defStyleAttr) throws NotExistException, WrongTypeException, IOException {

        LogUtil.info(TAG, "init method called");
        mContext = context;
        AssetInterceptor.setContext(context);
        FileInterceptor.setFileInterceptor_context(context);
//        DatabaseHelper databaseHelper = new DatabaseHelper(mContext);
//        sharedPreferences = databaseHelper.getPreferences(PREFERENCE_KEY);
//
//        ResourceManager resourceManager = getResourceManager();
//        Node node = new Node();
//        List<TypedAttribute> typedAttributeList =
//        TypedAttribute typedAttribute = new
        HDImageViewFactory.initializeDefault(mContext);


        //init
        DisplayManager displayManager = DisplayManager.getInstance();
        Optional<Display> display = displayManager.getDefaultDisplay(mContext);
        DisplayAttributes metrics = display.get().getRealAttributes();
        mDensity = metrics.densityDpi;
        createPaints();

        mBitmapDataSource = new DefaultBitmapDataSource();
        mScaleAnimationInterpolator = HDImageViewFactory.getInstance().getScaleAnimationInterpolator();
        mTranslationAnimationInterpolator = HDImageViewFactory.getInstance().getTranslationAnimationInterpolator();

        EventRunner eventRunner = EventRunner.create();
        mOriginalHandler = new OriginalHandler(eventRunner, this);
        mOriginalExecutor = Executors.newSingleThreadExecutor();
        setMinimumDpi(160);
        setDoubleTapZoomDpi(160);
     //   onMeasure();
        setGestureDetector();
        setDraggedListener(DRAG_VERTICAL , this);
     //   run_Runner(this);

        if(attrset != null) {
            mDuration = attrset.getAttr(Image_Duration).isPresent() ?
                    attrset.getAttr(Image_Duration).get().getIntegerValue() : DEFAULT_DURATION;

           /* mFactor = attrset.getAttr(Decelerate_Interpolator_Factor).isPresent() ?
                    attrset.getAttr(Decelerate_Interpolator_Factor).get().getFloatValue() : 1.0F;*/

            String uriString = attrset.getAttr(Image_Src).isPresent() ?
                    attrset.getAttr(Image_Src).get().getStringValue() : null;
            setImageSource(ImageSourceBuilder.newBuilder().setUri(uriString).build());

            boolean translateEnabled = !attrset.getAttr(Image_translateEnabled).isPresent() || attrset.getAttr(Image_translateEnabled).get().getBoolValue();
            setTranslateEnabled(translateEnabled);

            boolean zoomEnabled = !attrset.getAttr(Image_zoomEnabled).isPresent() || attrset.getAttr(Image_zoomEnabled).get().getBoolValue();
            setZoomEnabled(zoomEnabled);

            boolean quickScaleEnabled = !attrset.getAttr(Image_quickScaleEnabled).isPresent() || attrset.getAttr(Image_quickScaleEnabled).get().getBoolValue();
            setQuickScaleEnabled(quickScaleEnabled);

            Color mappingBackgroundColor = attrset.getAttr(Image_mappingBackgroundColor).isPresent() ?
                    attrset.getAttr(Image_mappingBackgroundColor).get().getColorValue() : Color.TRANSPARENT;
            setMappingBackgroundColor(mappingBackgroundColor, mappingBackgroundColor.getValue());


        }

        setLayoutRefreshedListener(this);
        setEstimateSizeListener(this);
        addDrawTask(this);
    }

    private static class OriginalHandler extends EventHandler {

        private final WeakReference<HDImageView> mWef;
        public OriginalHandler(EventRunner runner, HDImageView view) throws IllegalArgumentException {
            super(runner);
            this.mWef = new WeakReference<>(view);
        }

        @Override
        public void processEvent(InnerEvent msg) {
            HDImageView view = mWef.get();
            if (view == null){
                return;
            }

            switch (msg.eventId) {
                case MSG_INIT_SUCCESS:
                    LogUtil.info(TAG, "Process event init Success");
                    PacMap data = msg.getPacMap();
                    view.onTilesInitialized(data.getIntValue(SOURCE_WIDTH),
                            data.getIntValue(SOURCE_Height),
                            data.getIntValue(SOURCE_ORIENTATION));
                    return;
                case MSG_INIT_FAILED:
                    Exception e = (Exception) msg.object;
                    OnBitmapLoadListener listener = view.getOnBitmapLoadListener();
                    if (e != null && listener != null) {
                        listener.onBitmapLoadError(e);
                    }
                    return;
                case MSG_TILE_LOAD_SUCCESS:
                    LogUtil.info(TAG, "Process event tile load Success");
                    removeEvent(MSG_TILE_LOAD_SUCCESS);
                    view.onTileLoaded();
                    return;
                default:
                    break;
            }
        }
    }

    public EventHandler getmOriginalHandler() {
        return mOriginalHandler;
    }

    /*public static EventHandler mHandler;
        public void run_Runner(@NotNull HDImageView view) {
            EventRunner eventRunner = EventRunner.create();

            mHandler = new EventHandler(eventRunner.current()) {
                @Override
                public void processEvent(InnerEvent msg) {
                    switch (msg.eventId) {
                        case MSG_INIT_SUCCESS:
                            PacMap data = msg.getPacMap();
                            view.onTilesInitialized(data.getIntValue(SOURCE_WIDTH),
                                    data.getIntValue(SOURCE_Height),
                                    data.getIntValue(SOURCE_ORIENTATION));
                            return;
                        case MSG_INIT_FAILED:
                            Exception e = (Exception) msg.object;
                            OnBitmapLoadListener listener = view.getOnBitmapLoadListener();
                            if (e != null && listener != null) {
                                listener.onBitmapLoadError(e);
                            }
                            return;
                        case MSG_TILE_LOAD_SUCCESS:
                            removeEvent(MSG_TILE_LOAD_SUCCESS);
                            view.onTileLoaded();
                            return;
                        default:
                            break;
                    }
                }
            };

            eventRunner.run();
        }
    */
    public class ability extends Ability {
        @Override
        public void onRestoreAbilityState(PacMap inState) {
            if(inState.isEmpty()) {
                return;
            }

            Uri uri = Uri.parse(inState.getString("URI"));
            Float centerX = inState.getFloatValue("CenterX");
            Float centerY = inState.getFloatValue("CenterY");
            Float scale = inState.getFloatValue("Scale");
            int orientation = inState.getIntValue("Orientation");

            ImageViewOptions imageViewOptions = new ImageViewOptions(scale,
                    new PointF(centerX, centerY));

            setImageSource(ImageSourceBuilder.newBuilder().setUri(uri)
                    .setOrientation(orientation)
                    .setImageViewOptions(imageViewOptions)
                    .build());
            super.onRestoreAbilityState(inState);
        }

        @Override
        public void onSaveAbilityState(PacMap inState) {
            inState.putString("URI" , mUri.toString());
            PointF center = getCenter();
            inState.putFloatValue("CenterX" , center.x);
            inState.putFloatValue("CenterY" , center.y);
            inState.putFloatValue("Scale" , mScale);
            inState.putIntValue("Orientation" , mOrientation);
            super.onSaveAbilityState(inState);
        }
    }

    public final void setOrientation(@Orientation int orientation) {
        if (mOrientation == orientation) {
            return;
        }

        mOrientation = orientation;
        reset(false);
        postLayout();
    }

    public void setImageURI(Uri uri) {
        ImageSource imageSource = ImageSourceBuilder.newBuilder()
                .setUri(uri)
                .build();
        setImageSource(imageSource);
    }

    public void setImageURI(@Nullable String uriString) {
        Uri uri = uriString == null ? null : Uri.parse(uriString);
        setImageURI(uri);
    }

    public void setImageSource(@NotNull ImageSource imageSource) {
        reset(true);

        if (imageSource.getImageViewOptions() != null) {
            restoreState(imageSource.getImageViewOptions());
        }

        mUri = imageSource.getUri();
        mOrientation = imageSource.getOrientation();
        ImageSizeOptions sizeOptions = imageSource.getImageSizeOptions();

        if (sizeOptions != null) {
            mSourceWidth = sizeOptions.mWidth;
            mSourceHeight = sizeOptions.mHeight;
        }

        mImageSourceLoadListener = imageSource.getImageSourceLoadListener();
        mSourceRegion = imageSource.getImageSourceRegion();
        MappingsInit task = new MappingsInit(getContext(), mBitmapDataSource, getSourceRegion(), mUri);
        mOriginalExecutor.execute(task);
    }

    // FIGURE OUT WHY IT IS NOT BEING USED
    public void setScaleAnimationInterpolator(DecelerateInterpolator scaleAnimationInterpolator) {
        mScaleAnimationInterpolator = scaleAnimationInterpolator;
    }

    public DecelerateInterpolator getScaleAnimationInterpolator() {
        return mScaleAnimationInterpolator;
    }

    public void setTranslationAnimationInterpolator(DecelerateInterpolator translationAnimationInterpolator) {
        mTranslationAnimationInterpolator = translationAnimationInterpolator;
    }

    public DecelerateInterpolator getTranslationAnimationInterpolator() {
        return mTranslationAnimationInterpolator;
    }

    public void setAnimatorListener(AnimatorListener animatorListener) {
        mAnimatorListener = animatorListener;
    }

    public AnimatorListener getAnimatorListener() {
        return mAnimatorListener;
    }

    public void setAnimatorUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        mAnimatorUpdateListener = animatorUpdateListener;
    }

    public AnimatorUpdateListener getAnimatorUpdateListener() {
        return mAnimatorUpdateListener;
    }

    private void reset(boolean newImage) {
        mScale = 0f;
        mScaleStart = 0;
        mViewTranslate = null;
        mViewTranslateStart.set(0F, 0F);
        mPendingScale = -1f;
        mSourcePendingCenter = null;
        //mSourceRequestCenter = null;
        mIsZooming = false;
        mIsPanning = false;
        mMaxTouchCount = 0;
        mMaxSampleSize = 0;
        mLastViewCenter.set(0F, 0F);
        mLastViewDistance = 0;
        stopAnimator();
        mScaleAnimationInterpolator = HDImageViewFactory.getInstance().getScaleAnimationInterpolator();
        mTranslationAnimationInterpolator = HDImageViewFactory.getInstance().getTranslationAnimationInterpolator();
        mSatTemp.reset();
        mMatrix.reset();
        if (newImage) {
            mUri = null;
            if (mBitmapDataSource != null) {
                synchronized (mLock) {
                    mBitmapDataSource.recycle();
                }
            }
            mSourceWidth = 0;
            mSourceHeight = 0;
            mSourceOrientation = ORIENTATION_0;
            mSourceRegion = null;
            mReadySent = false;
            mImageLoadedSent = false;
        }
        if (mMappingMap != null) {
            for (int index = 0; index < mMappingMap.size(); index++) {
                List<Mapping> mappings = mMappingMap.valueAt(index);
                for (Mapping mapping : mappings) {
                    mapping.mVisible = false;
                    if (mapping.mBitmap != null) {
                        mapping.mBitmap.release();
                        mapping.mBitmap = null;
                    }
                }
            }
            mMappingMap = null;
        }
        invalidate();
    }

    private void stopAnimator() {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
    }

    private void setGestureDetector() {
        setLongPress();
        setClickGesture();
    }

    private void setLongPress() {
        mLongPressDetector = (component, touchEvent) -> {
            Date currentTime = Calendar.getInstance().getTime();
            if (currentTime.getTime() - touchEvent.getOccurredTime() <= 2000) {
                if (isLongClickOn() && mOnLongClickListener != null) {
                    mOnLongClickListener.onLongClicked(HDImageView.this);
                }
                return true;
            }

            return false;
        };

    }

    private void setClickGesture() {
        mTouchDetector = (TouchEventListener) (component, touchEvent) -> {
            if(touchEvent.getPointerCount() == 1) {
                simulateClick();
                return true;
            }

            if(touchEvent.getPointerCount() == 2) {
                if (mZoomEnabled && mQuickScaleEnabled && mReadySent && mViewTranslate != null) {
                    MmiPoint touchPoint = touchEvent.getPointerPosition(0);
                    PointF sourceCenter = viewToSourceCoordinate(new PointF(touchPoint.getX(), touchPoint.getY()));
                    PointF viewFocus = new PointF(touchPoint.getX(), touchPoint.getY());
                    startDoubleTapAnimator(sourceCenter, viewFocus);
                    return true;
                }
                return false;
            }

            return false;
        };
    }

    @Override
    public void onRefreshed(Component component) {
        LogUtil.info(TAG, "onRefreshed method called");
        PointF sCenter = getCenter();
        if (mReadySent && sCenter != null) {
            stopAnimator();
            mPendingScale = mScale;
            mSourcePendingCenter = sCenter;
        }
        invalidate();
    }

    /*protected void onSizeChanged() {
        PointF sCenter = getCenter();
        if (mReadySent && sCenter != null) {
            stopAnimator();
            mPendingScale = mScale;
            mSourcePendingCenter = sCenter;
        }
    }*/

    @Override
    public boolean onEstimateSize(int widthEstimateConfig, int heightEstimateConfig) {
        LogUtil.info(TAG, "onEstimateSize method called");

        int widthSpecMode = EstimateSpec.getMode(widthEstimateConfig);
        int heightSpecMode = EstimateSpec.getMode(heightEstimateConfig);
        int parentWidth = EstimateSpec.getSize(widthEstimateConfig);
        int parentHeight = EstimateSpec.getSize(heightEstimateConfig);
        boolean resizeWidth = widthSpecMode != EstimateSpec.PRECISE;
        boolean resizeHeight = heightSpecMode != EstimateSpec.PRECISE;
        int width = parentWidth;
        int height = parentHeight;
        if (mSourceWidth > 0 && mSourceHeight > 0) {
            if (resizeWidth && resizeHeight) {
                width = getShowWidth();
                height = getShowHeight();
            } else if (resizeHeight) {
                height = (int)((((double)getShowHeight()/(double)getShowWidth()) * width));
            } else if (resizeWidth) {
                width = (int)((((double)getShowWidth()/(double)getShowHeight()) * height));
            }
        }
        width = Math.max(width, getMinWidth());
        height = Math.max(height, getMinHeight());
        if (BuildConfig.DEBUG) {
            LogUtil.info(TAG, "SourceSize:(" + mSourceWidth + "," + mSourceHeight + ")");
            LogUtil.info(TAG, "(" + width + "," + height + ")");
        }
        setEstimatedSize(width, height);

        return false;
    }
  /*  protected void onMeasure() {
        int width = Math.max(getShowWidth() , getMinWidth());
        int height = Math.max(getShowHeight() , getMinHeight());
        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "SourceSize:(" + mSourceWidth + "," + mSourceHeight + ")");
//            Log.d(TAG, "(" + width + "," + height + ")");
        }
        ComponentContainer.LayoutConfig layoutConfig = getLayoutConfig();
        layoutConfig.width = width;
        layoutConfig.height = height;
        setLayoutConfig(layoutConfig);
    }*/

    @Override
    public boolean onTouchEvent(Component component, TouchEvent event) {
        if (mValueAnimator != null && !mValueAnimator.isInterrupted()) {
            return true;
        }

        stopAnimator();

        if (mViewTranslate == null) {
            return true;
        }

        if (mTouchDetector.onTouchEvent(component, event)) {
            mIsZooming = false;
            mIsPanning = false;
            mMaxTouchCount = 0;
            return true;
        }

        if (mLongPressDetector.onTouchEvent(component, event)) {
            mIsZooming = false;
            mIsPanning = false;
            mMaxTouchCount = 0;
            return true;
        }

        final int touchCount = event.getPointerCount();
        switch (event.getAction()) {
            case PRIMARY_POINT_DOWN:
            case OTHER_POINT_DOWN:
                down(event, touchCount);
                return true;
            case POINT_MOVE:
                boolean consumed = move(event, touchCount);
                if (consumed) {
                    invalidate();
                    return true;
                }
                break;
            case PRIMARY_POINT_UP:
            case OTHER_POINT_UP:
                if (up(event, touchCount)) return true;
                return true;
        }
        return true;
    }

    private boolean up(TouchEvent event, int touchCount) {
        if (mMaxTouchCount > 0 && (mIsZooming || mIsPanning)) {
            if (mIsZooming && touchCount == 2) {
                mIsPanning = true;
                mViewTranslateStart.set(mViewTranslate.x, mViewTranslate.y);
                if (event.getIndex() == 1) {
                    mLastViewCenter.set(event.getPointerPosition(0).getX(), event.getPointerPosition(0).getY());
                } else {
                    mLastViewCenter.set(event.getPointerPosition(1).getX(), event.getPointerPosition(1).getY());
                }
            }
            if (touchCount < 3) {
                mIsZooming = false;
            }
            if (touchCount < 2) {
                mIsPanning = false;
                mMaxTouchCount = 0;
            }

            refreshRequiredTiles(true);
            return true;
        }
        if (touchCount == 1) {
            mIsZooming = false;
            mIsPanning = false;
            mMaxTouchCount = 0;
        }
        return false;
    }

    private boolean move(TouchEvent event, int touchCount) {

        if (mMaxTouchCount == 0) {
            return false;
        }

        if (touchCount >= 2 && !mZoomEnabled) {
            return false;
        }

        if (touchCount >= 2) {
            float endDistance = distance(event.getPointerPosition(0).getX(), event.getPointerPosition(1).getX(), event.getPointerPosition(0).getY(), event.getPointerPosition(1).getY());
            float endCenterX = (event.getPointerPosition(0).getX() + event.getPointerPosition(1).getX()) / 2;
            float endCenterY = (event.getPointerPosition(0).getY() + event.getPointerPosition(1).getY()) / 2;

            float centerDistance = distance(mLastViewCenter.x, endCenterX, mLastViewCenter.y, endCenterY);

            if (centerDistance > 5 || Math.abs(endDistance - mLastViewDistance) > 5 || mIsPanning) {
                mIsZooming = true;
                mIsPanning = true;

                mScale = Math.min(mMaxScale, endDistance * mScaleStart / mLastViewDistance);

                if (mScale <= minScale()) {
                    mLastViewDistance = endDistance;
                    mScaleStart = minScale();
                    mLastViewCenter.set(endCenterX, endCenterY);
                    mViewTranslateStart.set(mViewTranslate);
                } else if (mTranslateEnabled) {
                    float vLeftStart = mLastViewCenter.x - mViewTranslateStart.x;
                    float vTopStart = mLastViewCenter.y - mViewTranslateStart.y;
                    float vLeftNow = vLeftStart * mScale / mScaleStart;
                    float vTopNow = vTopStart * mScale / mScaleStart;
                    mViewTranslate.x = endCenterX - vLeftNow;
                    mViewTranslate.y = endCenterY - vTopNow;
                } else {
                    mViewTranslate.x = (getWidth() - mScale * getShowWidth()) / 2;
                    mViewTranslate.y = (getHeight() - mScale * getShowHeight()) / 2;
                }

                fitToBounds(true);
                refreshRequiredTiles(false);
                return true;
            }

            return false;
        }

        if (mIsZooming) {
            return false;
        }

        float dx = Math.abs(event.getPointerPosition(0).getX() - mLastViewCenter.x);
        float dy = Math.abs(event.getPointerPosition(0).getY() - mLastViewCenter.y);

        final float offset = 5 * mDensity;
        if (dx > offset || dy > offset || mIsPanning) {

            mViewTranslate.x = mViewTranslateStart.x + event.getPointerPosition(0).getX() - mLastViewCenter.x;
            mViewTranslate.y = mViewTranslateStart.y + event.getPointerPosition(0).getY() - mLastViewCenter.y;

            float lastX = mViewTranslate.x;
            float lastY = mViewTranslate.y;
            fitToBounds(true);
            boolean atXEdge = lastX != mViewTranslate.x;
            boolean atYEdge = lastY != mViewTranslate.y;
            boolean edgeXSwipe = atXEdge && dx > dy && !mIsPanning;
            boolean edgeYSwipe = atYEdge && dy > dx && !mIsPanning;
            boolean translateY = lastY == mViewTranslate.y && dy > offset * 3;
            if (!edgeXSwipe && !edgeYSwipe &&
                    (!atXEdge || !atYEdge || translateY || mIsPanning)) {
                mIsPanning = true;
            } else if (dx > offset || dy > offset) {
                mMaxTouchCount = 0;
            }

            if (!mTranslateEnabled) {
                mViewTranslate.x = mViewTranslateStart.x;
                mViewTranslate.y = mViewTranslateStart.y;
            }

            refreshRequiredTiles(false);

            return true;
        }

        return false;
    }

    private void down(TouchEvent event, int touchCount) {
        stopAnimator();
//        getComponentParent().requestDisallowInterceptTouchEvent(true);
        mMaxTouchCount = Math.max(mMaxTouchCount, touchCount);

        if (touchCount < 2) {
            // 开始一个手指锅
            mViewTranslateStart.set(mViewTranslate.x, mViewTranslate.y);
            mLastViewCenter.set(event.getPointerPosition(0).getX(),event.getPointerPosition(0).getY());
            return;
        }

        if (mZoomEnabled) {
            // 开始捏捏缩放。 计算接触点与夹点中心点之间的距离。
            float startDistance = distance(event.getPointerPosition(0).getX(), event.getPointerPosition(1).getX(), event.getPointerPosition(0).getY(), event.getPointerPosition(1).getY());
            float startCenterX = (event.getPointerPosition(0).getX() + event.getPointerPosition(1).getX()) / 2;
            float startCenterY = (event.getPointerPosition(0).getY() + event.getPointerPosition(1).getY()) / 2;
            mScaleStart = mScale;
            mLastViewDistance = startDistance;
            mViewTranslateStart.set(mViewTranslate.x, mViewTranslate.y);
            mLastViewCenter.set(startCenterX, startCenterY);
        } else {
            //中止所有二次手势
            mMaxTouchCount = 0;
        }
    }

    private void startDoubleTapAnimator(PointF sCenter, PointF vFocus) {
        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "双击动画");
        }
        if (!mTranslateEnabled) {
            sCenter.x = getShowWidth() / 2.0F;
            sCenter.y = getShowHeight() / 2.0F;
        }
        float doubleTapZoomScale = Math.min(mMaxScale, mDoubleTapZoomScale);
        boolean zoomIn = mScale <= doubleTapZoomScale * 0.9f;
        float targetScale = zoomIn ? doubleTapZoomScale : minScale();
        if (mDoubleTapZoomStyle == ZOOM_FOCUS_CENTER_IMMEDIATE) {
            setScaleAndCenter(targetScale, sCenter);
        } else if (mDoubleTapZoomStyle == ZOOM_FOCUS_CENTER || !zoomIn || !mTranslateEnabled) {
            startZoomForCenter(sCenter, targetScale);
        } else if (mDoubleTapZoomStyle == ZOOM_FOCUS_FIXED) {
            startZoomForFixed(sCenter, vFocus, targetScale);
        }
        invalidate();
    }

    @Override
    public void onDraw(Component component , Canvas canvas) {
//        super.onDraw(canvas);
    //    onMeasure();
   //     onSizeChanged();
        LogUtil.info(TAG, "onDraw method called mSourceWidth: " + mSourceWidth + " mSourceHeight: " + mSourceHeight + " getWidth: " + getWidth() + " getHeight: " + getHeight());
        if (mSourceWidth == 0 || mSourceHeight == 0 || getWidth() == 0 || getHeight() == 0) {
            //If the area is 0, do not draw
            return;
        }

        if (mMappingMap == null && mBitmapDataSource != null) {
            //Initialize the texture
            initialiseBaseLayer(getMaxBitmapDimensions(canvas));
        }

        if (!checkReady()) {
            return;
        }

        preDraw();

        drawMappings(canvas);
    }

    private void drawMappings(Canvas canvas) {
        if (mMappingMap == null || !isBaseLayerReady()) {
            return;
        }

        int sampleSize = Math.min(mMaxSampleSize, calculateInSampleSize(mScale));

        boolean hasMissingTiles = false;
//        final List<Mapping> mappings = mMappingMap.get(sampleSize);
        final List<Mapping> mappings = mMappingMap.get(sampleSize, null);
        if (mappings != null) {
            for (Mapping mapping : mappings) {
                if (mapping.mVisible && (mapping.mLoading || mapping.mBitmap == null)) {
                    hasMissingTiles = true;
                    break;
                }
            }
        }

        if (hasMissingTiles) {
            for (int index = 0; index < mMappingMap.size(); index++) {
                final List<Mapping> list = mMappingMap.valueAt(index);
                drawMappings(canvas, list);
            }
        } else {
            if (mappings != null) {
                drawMappings(canvas, mappings);
            }
        }
    }

    private void drawMappings(Canvas canvas, List<Mapping> list) {
        for (Mapping mapping : list) {
            sourceToViewRect(mapping.mSourceRect, mapping.mViewRect);
            if (!mapping.mLoading && mapping.mBitmap != null) {
                if (mMappingBgPaint != null) {
                    canvas.drawRect(mapping.mViewRect, mMappingBgPaint);
                }
                mMatrix.reset();
                setMatrixArray(mSrcArray, 0, 0,
                        mapping.mBitmap.getImageInfo().size.width, 0, mapping.mBitmap.getImageInfo().size.width,
                        mapping.mBitmap.getImageInfo().size.height, 0, mapping.mBitmap.getImageInfo().size.height);
                int orientation = getRequiredRotation();
                switch (orientation) {
                    case ORIENTATION_0:
                        setMatrixArray(mDstArray, mapping.mViewRect.left, mapping.mViewRect.top, mapping.mViewRect.right,
                                mapping.mViewRect.top, mapping.mViewRect.right, mapping.mViewRect.bottom, mapping.mViewRect.left,
                                mapping.mViewRect.bottom);
                        break;
                    case ORIENTATION_90:
                        setMatrixArray(mDstArray, mapping.mViewRect.right, mapping.mViewRect.top, mapping.mViewRect.right,
                                mapping.mViewRect.bottom, mapping.mViewRect.left, mapping.mViewRect.bottom, mapping.mViewRect.left,
                                mapping.mViewRect.top);
                        break;
                    case ORIENTATION_180:
                        setMatrixArray(mDstArray, mapping.mViewRect.right, mapping.mViewRect.bottom, mapping.mViewRect.left,
                                mapping.mViewRect.bottom, mapping.mViewRect.left, mapping.mViewRect.top, mapping.mViewRect.right,
                                mapping.mViewRect.top);
                        break;
                    case ORIENTATION_270:
                        setMatrixArray(mDstArray, mapping.mViewRect.left, mapping.mViewRect.bottom, mapping.mViewRect.left,
                                mapping.mViewRect.top, mapping.mViewRect.right, mapping.mViewRect.top, mapping.mViewRect.right,
                                mapping.mViewRect.bottom);
                        break;
                    case ORIENTATION_EXIF:
                    default:
                        break;
                }
                mMatrix.setPolyToPoly(mSrcArray, 0, mDstArray, 0, 4);
                RectFloat r = new RectFloat();
                mMatrix.mapRect(r);
                PixelMapHolder pmh = new PixelMapHolder(mapping.mBitmap);
                canvas.drawPixelMapHolderRect(pmh, r, mBitmapPaint);
            }
        }
    }

    private void setMatrixArray(float[] array, float f0, float f1, float f2, float f3, float f4,
                                float f5, float f6, float f7) {
        array[0] = f0;
        array[1] = f1;
        array[2] = f2;
        array[3] = f3;
        array[4] = f4;
        array[5] = f5;
        array[6] = f6;
        array[7] = f7;
    }

    //Check if the base layer of the texture is ready
    private boolean isBaseLayerReady() {
        LogUtil.info(TAG, "isBaseLayerReady method called mMappingMap: " + mMappingMap);
        if (mMappingMap == null) {
            return false;
        }

        final List<Mapping> mappings = mMappingMap.get(mMaxSampleSize, null);
        if (mappings == null) {
            LogUtil.info(TAG, "isBaseLayerReady failed 2nd condition");
            return false;
        }

        for (Mapping mapping : mappings) {
            if (mapping.mLoading || mapping.mBitmap == null) {
                LogUtil.info(TAG, "isBaseLayerReady failed 3rd condition");
                return false;
            }
        }

        return true;
    }

    // Check that it is ready to be drawn
    private boolean checkReady() {
        LogUtil.info(TAG, "check Ready method called getWidth: " + getWidth() + " getHeight: " + getHeight() + " isBaseLayerReady: " + isBaseLayerReady());
        boolean ready = getWidth() > 0 && getHeight() > 0 &&
                mSourceWidth > 0 && mSourceHeight > 0 && isBaseLayerReady();
        if (!mReadySent && ready) {
            preDraw();
            mReadySent = true;
            if (mOnBitmapLoadListener != null) {
                mOnBitmapLoadListener.onBitmapLoadReady();
            }
        }
        return ready;
    }

    private boolean checkImageLoaded() {
        LogUtil.info(TAG, "checkImageLoaded method called");
        boolean imageLoaded = isBaseLayerReady();
        if (!mImageLoadedSent && imageLoaded) {
            preDraw();
            mImageLoadedSent = true;
        }
        return imageLoaded;
    }

    private void createPaints() {
        if (mBitmapPaint == null) {
            mBitmapPaint = new Paint();
            mBitmapPaint.setAntiAlias(true);
            mBitmapPaint.setFilterBitmap(true);
            mBitmapPaint.setDither(true);
        }
    }

    private synchronized void initialiseBaseLayer(Point maxTileDimensions) {

        LogUtil.info(TAG, "initialiseBaseLayer method called");
        fitToBounds(true, mSatTemp);

        mMaxSampleSize = calculateInSampleSize(mSatTemp.mScale);

        initialiseTileMap(maxTileDimensions);

        List<Mapping> baseGrid = mMappingMap.get(mMaxSampleSize, null);
        final int rotation = getRequiredRotation();
//        if (BuildConfig.DEBUG) Log.d(TAG, "initialiseBaseLayer");
        for (Mapping baseMapping : baseGrid) {
            MappingLoad task = new MappingLoad(mBitmapDataSource, baseMapping, mSourceRegion,
                    mSourceWidth, mSourceHeight, rotation);
            mOriginalExecutor.execute(task);
        }
        refreshRequiredTiles(true);
    }

    // Refresh map
    private void refreshRequiredTiles(boolean load) {
        if (mBitmapDataSource == null || mMappingMap == null) {
            return;
        }

        int sampleSize = Math.min(mMaxSampleSize, calculateInSampleSize(mScale));

        final int rotation = getRequiredRotation();
        for (int index = 0; index < mMappingMap.size(); index++) {
            final List<Mapping> mappings = mMappingMap.valueAt(index);
            for (Mapping mapping : mappings) {
                if (mapping.mSampleSize < sampleSize
                        || (mapping.mSampleSize > sampleSize && mapping.mSampleSize != mMaxSampleSize)) {
                    mapping.mVisible = false;
                    if (mapping.mBitmap != null) {
                        mapping.mBitmap.release();
                        mapping.mBitmap = null;
                    }
                }
                if (mapping.mSampleSize == sampleSize) {
                    if (mappingVisible(mapping)) {
                        mapping.mVisible = true;
                        if (!mapping.mLoading && mapping.mBitmap == null && load) {
//                            if (BuildConfig.DEBUG) Log.d(TAG, "refreshRequiredTiles");
                            MappingLoad task = new MappingLoad(mBitmapDataSource, mapping, mSourceRegion,
                                    mSourceWidth, mSourceHeight, rotation);
                            mOriginalExecutor.execute(task);
                        }
                    } else if (mapping.mSampleSize != mMaxSampleSize) {
                        mapping.mVisible = false;
                        if (mapping.mBitmap != null) {
                            mapping.mBitmap.release();
                            mapping.mBitmap = null;
                        }
                    }
                } else if (mapping.mSampleSize == mMaxSampleSize) {
                    mapping.mVisible = true;
                }
            }
        }
    }

    private void updateScaleAndTranslate() {
        if (mValueAnimator == null) return;

        mScale = mValueAnimator.getScale();
        PointF viewFocus = mValueAnimator.getViewFocus();
        mViewTranslate.x -= sourceToViewX(mValueAnimator.mSourceCenter.x) - viewFocus.x;
        mViewTranslate.y -= sourceToViewY(mValueAnimator.mSourceCenter.y) - viewFocus.y;

        fitToBounds(mValueAnimator.getAnimatedFraction() >= 1f || mValueAnimator.noChangeScale());
        refreshRequiredTiles(mValueAnimator.getAnimatedFraction() >= 1f);
        invalidate();
        if (mValueAnimator.isEnded()) {
            stopAnimator();
        }
    }

    private boolean mappingVisible(Mapping mapping) {
        float sVisLeft = viewToSourceX(0);
        float sVisRight = viewToSourceX(getWidth());
        float sVisTop = viewToSourceY(0);
        float sVisBottom = viewToSourceY(getHeight());
        return !(sVisLeft > mapping.mSourceRect.right ||
                mapping.mSourceRect.left > sVisRight ||
                sVisTop > mapping.mSourceRect.bottom ||
                mapping.mSourceRect.top > sVisBottom);
    }

    private void preDraw() {
        if (getWidth() == 0 || getHeight() == 0 || mSourceWidth <= 0 || mSourceHeight <= 0) {
            return;
        }

        if (mSourcePendingCenter != null && mPendingScale > -1f) {

            if (mViewTranslate == null) {
                mViewTranslate = new PointF();
            }

            mScale = mPendingScale;
            mViewTranslate.x = getWidth() / 2.0F - mScale * mSourcePendingCenter.x;
            mViewTranslate.y = getHeight() / 2.0F - mScale * mSourcePendingCenter.y;
            mSourcePendingCenter = null;
            mPendingScale = -1f;
            fitToBounds(true);
            refreshRequiredTiles(true);
        }

        fitToBounds(false);
    }

    private int calculateInSampleSize(float scale) {
        if (mMinimumMappingDpi > 0) {
            DisplayManager displayManager = DisplayManager.getInstance();
            Optional<Display> display = displayManager.getDefaultDisplay(mContext);
            DisplayAttributes metrics = display.get().getRealAttributes();
            float averageDpi = (metrics.xDpi + metrics.yDpi) / 2;
            scale = mMinimumMappingDpi * scale / averageDpi;
        }

        int reqWidth = (int) (getShowWidth() * scale);
        int reqHeight = (int) (getShowHeight() * scale);

        int inSampleSize = 1;
        if (reqWidth == 0 || reqHeight == 0) {
            return 32;
        }

        if (getShowHeight() > reqHeight || getShowWidth() > reqWidth) {
            final int heightRatio = Math.round(getShowHeight() / (float) reqHeight);
            final int widthRatio = Math.round(getShowWidth() / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        int power = 1;
        while (power * 2 < inSampleSize) {
            power *= 2;
        }
        return power;
    }

    //适应边界
    private void fitToBounds(boolean center) {
        boolean init = false;
        if (mViewTranslate == null) {
            init = true;
            mViewTranslate = new PointF(0, 0);
        }
//        sharedPreferences.flush();

        mSatTemp.mScale = mScale;
        mSatTemp.mViewTranslate.set(mViewTranslate);
        fitToBounds(center, mSatTemp);
        mScale = mSatTemp.mScale;
        mViewTranslate.set(mSatTemp.mViewTranslate);
        if (init) {
            mViewTranslate.set(getTranslateForSourceCenter(getShowWidth() / 2.0F, getShowHeight() / 2.0F, mScale));
        }
    }

    public void setCustomRange(RectF range) {
        this.mCustomRange.set(range);
    }

    private void fitToBounds(boolean center, ScaleAndTranslate sat) {
        if (mTranslateLimit == OUTSIDE && isReady()) {
            center = false;
        }


        // 计算padding的偏移效果
        final float xPaddingRatio = getPaddingLeft() > 0 || getPaddingRight() > 0
                ? getPaddingLeft() / (float) (getPaddingRight() + getPaddingLeft())
                : 0.5f;
        final float yPaddingRatio = getPaddingTop() > 0 || getPaddingBottom() > 0
                ? getPaddingTop() / (float) (getPaddingTop() + getPaddingBottom())
                : 0.5f;

        // 限制缩放的大小
        float scale = limitedScale(sat.mScale);
        sat.mScale = scale;
        // 获取缩放后的图片宽高
        float scaleWidth = scale * getShowWidth();
        float scaleHeight = scale * getShowHeight();

        if (mTranslateLimit == COUSTOM && isReady() && !mCustomRange.isEmpty()) {
            if (scaleWidth < mCustomRange.width()) {
                scale = (mCustomRange.width() * 1.0f) / (getShowWidth() * 1.0f);
                sat.mScale = scale;
                // 获取缩放后的图片宽高
                scaleWidth = scale * getShowWidth();
                scaleHeight = scale * getShowHeight();
            }

            if (scaleHeight < mCustomRange.height()) {
                scale = (mCustomRange.height() * 1.0f) / (getShowHeight() * 1.0f);
                sat.mScale = scale;
                // 获取缩放后的图片宽高
                scaleWidth = scale * getShowWidth();
                scaleHeight = scale * getShowHeight();
            }

            float translateX = limitTranslate(
                    mCustomRange.right - scaleWidth,
                    sat.mViewTranslate.x,
                    Math.max(0F, mCustomRange.left)
            );

            float translateY = limitTranslate(
                    mCustomRange.bottom - scaleHeight,
                    sat.mViewTranslate.y,
                    Math.max(0F, mCustomRange.top)
            );
            sat.mViewTranslate.x = translateX;
            sat.mViewTranslate.y = translateY;
            return;
        }

        if (mTranslateLimit == CENTER && isReady()) {
            float translateX = limitTranslate(getWidth() / 2.0F - scaleWidth,
                    sat.mViewTranslate.x,
                    Math.max(0F, getWidth() / 2.0F));
            float translateY = limitTranslate(getHeight() / 2.0F - scaleHeight,
                    sat.mViewTranslate.y,
                    Math.max(0F, getHeight() / 2.0F));
            sat.mViewTranslate.x = translateX;
            sat.mViewTranslate.y = translateY;
            return;
        }

        if (center) {
            float translateX = limitTranslate(getWidth() - scaleWidth,
                    sat.mViewTranslate.x,
                    Math.max(0F, (getWidth() - scaleWidth) * xPaddingRatio));
            float translateY = limitTranslate(getHeight() - scaleHeight,
                    sat.mViewTranslate.y,
                    Math.max(0F, (getHeight() - scaleHeight) * yPaddingRatio));
            sat.mViewTranslate.x = translateX;
            sat.mViewTranslate.y = translateY;
            return;
        }
        float translateX = limitTranslate(-scaleWidth,
                sat.mViewTranslate.x,
                Math.max(0F, getWidth()));
        float translateY = limitTranslate(-scaleHeight,
                sat.mViewTranslate.y,
                Math.max(0F, getHeight()));
        sat.mViewTranslate.x = translateX;
        sat.mViewTranslate.y = translateY;
    }

    private float limitTranslate(float min, float current, float max) {
        return Math.min(max, Math.max(min, current));
    }

    private void initialiseTileMap(Point maxTileDimensions) {
        LogUtil.info(TAG, "initialiseTileMap method called");
        mMappingMap = new PlainArray<>();
        int sampleSize = mMaxSampleSize;
        int xTiles = 1;
        int yTiles = 1;
        while (true) {
            int sTileWidth = getShowWidth() / xTiles;
            int sTileHeight = getShowHeight() / yTiles;
            int subTileWidth = sTileWidth / sampleSize;
            int subTileHeight = sTileHeight / sampleSize;

            while (subTileWidth + xTiles + 1 > maxTileDimensions.getPointXToInt()
                    || (subTileWidth > getWidth() * 1.25 && sampleSize < mMaxSampleSize)) {
                xTiles++;
                sTileWidth = getShowWidth() / xTiles;
                subTileWidth = sTileWidth / sampleSize;
            }

            while (subTileHeight + yTiles + 1 > maxTileDimensions.getPointYToInt()
                    || (subTileHeight > getHeight() * 1.25 && sampleSize < mMaxSampleSize)) {
                yTiles++;
                sTileHeight = getShowHeight() / yTiles;
                subTileHeight = sTileHeight / sampleSize;
            }
            List<Mapping> mappingGrid = new ArrayList<>(xTiles * yTiles);
            for (int x = 0; x < xTiles; x++) {
                for (int y = 0; y < yTiles; y++) {
                    Mapping mapping = new Mapping();
                    mapping.mSampleSize = sampleSize;
                    mapping.mVisible = sampleSize == mMaxSampleSize;
                    mapping.mSourceRect = new Rect(x * sTileWidth, y * sTileHeight,
                            x == xTiles - 1 ? getShowWidth() : (x + 1) * sTileWidth,
                            y == yTiles - 1 ? getShowHeight() : (y + 1) * sTileHeight);
                    mapping.mViewRect = new Rect(0, 0, 0, 0);
                    mapping.mFileSourceRect = new Rect(mapping.mSourceRect);
                    mappingGrid.add(mapping);
                }
            }
            mMappingMap.put(sampleSize, mappingGrid);
            if (sampleSize == 1) {
                break;
            } else {
                sampleSize /= 2;
            }
        }
        if (BuildConfig.DEBUG) {
            for (int index = 0; index < mMappingMap.size(); index++) {
//                Log.d(TAG, "[sampleSize]" + mMappingMap.keyAt(index) + " [tiles]" +
//                        mMappingMap.valueAt(index).size());
            }
        }
    }

    public synchronized void onTilesInitialized(int sWidth, int sHeight, int sOrientation) {
        LogUtil.info(TAG, "onTilesInitialized method called mSourceWidth: " + mSourceWidth + " mSourceHeight: " + mSourceHeight + " sWidth: " + sWidth + " sHeight: " + sHeight);
        if (mSourceWidth > 0 && mSourceHeight > 0 && (mSourceWidth != sWidth || mSourceHeight != sHeight)) {
            reset(false);
        }
        mSourceWidth = sWidth;
        mSourceHeight = sHeight;
        mSourceOrientation = sOrientation;

        if (mImageSourceLoadListener != null) {
            mImageSourceLoadListener.loadSuccess(mUri, new ImageSizeOptions(mSourceWidth, mSourceHeight));
        }

        if (mOnBitmapLoadListener != null) {
            mOnBitmapLoadListener.onBitmapLoaded(mSourceWidth, mSourceHeight);
        }

        checkReady();
        checkImageLoaded();
//        requestLayout();
        postLayout();
        invalidate();
//        if (BuildConfig.DEBUG) Log.d(TAG, "onTilesInitialized");
    }

    synchronized void onTileLoaded() {
        checkReady();
        checkImageLoaded();
        invalidate();
    }

    private void restoreState(ImageViewOptions state) {
        if (state != null && state.getCenter() != null) {
            mPendingScale = state.getScale();
            mSourcePendingCenter = state.getCenter();
            invalidate();
        }
    }

    private void setMaxTileSize(int maxPixels) {
        mMaxMappingWidth = maxPixels;
        mMaxMappingHeight = maxPixels;
    }

    private void setMaxTileSize(int maxPixelsX, int maxPixelsY) {
        mMaxMappingWidth = maxPixelsX;
        mMaxMappingHeight = maxPixelsY;
    }

    private Point getMaxBitmapDimensions(Canvas canvas) {
        int maxWidth = 2048;
        int maxHeight = 2048;

        return new Point(Math.min(maxWidth, mMaxMappingWidth), Math.min(maxHeight, mMaxMappingHeight));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private int getShowWidth() {
        int rotation = getRequiredRotation();
        if (rotation == ORIENTATION_90 || rotation == ORIENTATION_270) {
            return mSourceHeight;
        }
        return mSourceWidth;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private int getShowHeight() {
        int rotation = getRequiredRotation();
        if (rotation == ORIENTATION_90 || rotation == ORIENTATION_270) {
            return mSourceWidth;
        }
        return mSourceHeight;
    }

    @Orientation
    private int getRequiredRotation() {
        if (mOrientation == ORIENTATION_EXIF) {
            return mSourceOrientation;
        }
        return mOrientation;
    }

    private float distance(float x0, float x1, float y0, float y1) {
        float dx = x0 - x1;
        float dy = y0 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public void release() {
//        if (BuildConfig.DEBUG) Log.d(TAG, "recycle" + mUri + " id" + System.identityHashCode(this));
        reset(true);
        setOnBitmapLoadListener(null);
    }

    private float viewToSourceX(float vx) {
        if (mViewTranslate == null)
            return Float.NaN;
        return (vx - mViewTranslate.x) / mScale;
    }

    private float viewToSourceY(float vy) {
        if (mViewTranslate == null)
            return Float.NaN;
        return (vy - mViewTranslate.y) / mScale;
    }

    /**
     * 将屏幕坐标转换为资源坐标。
     *
     * @param viewCoordinate view的坐标
     * @return 返回图片的坐标
     */
    public final PointF viewToSourceCoordinate(@NotNull PointF viewCoordinate) {
        return viewToSourceCoordinate(viewCoordinate.x, viewCoordinate.y, new PointF());
    }

    /**
     * 将屏幕坐标转换为资源坐标。
     *
     * @param viewX view的x坐标
     * @param viewY view的y坐标
     * @return 返回图片的坐标
     */
    public final PointF viewToSourceCoordinate(float viewX, float viewY) {
        return viewToSourceCoordinate(viewX, viewY, new PointF());
    }

    /**
     * 将屏幕坐标转换为资源坐标。
     *
     * @param viewCoordinate view的坐标
     * @param sourceTarget   目标资源坐标
     * @return 返回图片的坐标
     */
    public final PointF viewToSourceCoordinate(@NotNull PointF viewCoordinate, @NotNull PointF sourceTarget) {
        return viewToSourceCoordinate(viewCoordinate.x, viewCoordinate.y, sourceTarget);
    }

    /**
     * 将屏幕坐标转换为资源坐标。
     *
     * @param viewX        view的x坐标
     * @param viewY        view的y坐标
     * @param sourceTarget 目标资源坐标
     * @return 返回图片的坐标
     */
    private PointF viewToSourceCoordinate(float viewX, float viewY, @NotNull PointF sourceTarget) {
        if (mViewTranslate == null)
            return null;

        sourceTarget.set(viewToSourceX(viewX), viewToSourceY(viewY));
        return sourceTarget;
    }

    private float sourceToViewX(float sx) {
        if (mViewTranslate == null) {
            return Float.NaN;
        }
        return (sx * mScale) + mViewTranslate.x;
    }

    private float sourceToViewY(float sy) {
        if (mViewTranslate == null) {
            return Float.NaN;
        }
        return (sy * mScale) + mViewTranslate.y;
    }

    /**
     * 将资源坐标转换为屏幕坐标。
     *
     * @param sourceCoordinate 资源坐标
     * @return 返回屏幕上的位置
     */
    public final PointF sourceToViewCoordinate(@NotNull PointF sourceCoordinate) {
        return sourceToViewCoordinate(sourceCoordinate.x, sourceCoordinate.y, new PointF());
    }

    public final PointF sourceToViewCoordinate(float sx, float sy) {
        return sourceToViewCoordinate(sx, sy, new PointF());
    }

    /**
     * 将资源坐标转换为屏幕坐标。
     *
     * @param sourceCoordinate 资源坐标
     * @param viewTarget       目标view坐标
     * @return 返回屏幕上的位置
     */
    public final PointF sourceToViewCoordinate(@NotNull PointF sourceCoordinate, @NotNull PointF viewTarget) {
        return sourceToViewCoordinate(sourceCoordinate.x, sourceCoordinate.y, viewTarget);
    }

    /**
     * 将资源坐标转换为屏幕坐标。
     *
     * @param sourceX    资源的x坐标
     * @param sourceY    资源的y坐标
     * @param viewTarget 目标view坐标
     * @return 返回屏幕上的位置
     */
    private PointF sourceToViewCoordinate(float sourceX, float sourceY, @NotNull PointF viewTarget) {
        if (mViewTranslate == null) {
            return null;
        }
        viewTarget.set(sourceToViewX(sourceX), sourceToViewY(sourceY));
        return viewTarget;
    }

    private Rect sourceToViewRect(Rect sourceRect, Rect viewTarget) {
        viewTarget.set((int) sourceToViewX(sourceRect.left),
                (int) sourceToViewY(sourceRect.top),
                (int) sourceToViewX(sourceRect.right),
                (int) sourceToViewY(sourceRect.bottom));
        return viewTarget;
    }

    private PointF getTranslateForSourceCenter(float sourceCenterX, float sourceCenterY, float scale) {
        int vxCenter = (getPaddingLeft() + getWidth() - getPaddingRight()) / 2;
        int vyCenter = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2;
        mSatTemp.mScale = scale;
        mSatTemp.mViewTranslate.set(vxCenter - sourceCenterX * scale, vyCenter - sourceCenterY * scale);
        fitToBounds(true, mSatTemp);
        return mSatTemp.mViewTranslate;
    }

    private PointF limitedSourceCenter(float sourceCenterX, float sourceCenterY, float scale, @NotNull PointF sourceTarget) {
        PointF vTranslate = getTranslateForSourceCenter(sourceCenterX, sourceCenterY, scale);
        int vxCenter = (getPaddingLeft() + getWidth() - getPaddingRight()) / 2;
        int vyCenter = (getPaddingTop() + getHeight() - getPaddingBottom()) / 2;
        float sx = (vxCenter - vTranslate.x) / scale;
        float sy = (vyCenter - vTranslate.y) / scale;
        sourceTarget.set(sx, sy);
        return sourceTarget;
    }

    private float minScale() {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (mScaleType == CENTER_CROP) {
            return Math.max(width / (float) getShowWidth(), height / (float) getShowHeight());
        }
        if (mScaleType == CUSTOM && mMinScale > 0) {
            return mMinScale;
        }
        return Math.min(width / (float) getShowWidth(), height / (float) getShowHeight());
    }

    private float limitedScale(float targetScale) {
        return Math.min(mMaxScale, Math.max(minScale(), targetScale));
    }

    public final void setBitmapDataSource(BitmapDataSource decoder) {
        mBitmapDataSource = decoder;
    }

    public final void setTranslateLimit(@Translation int translateLimit) {
        mTranslateLimit = translateLimit;
        if (isReady()) {
            fitToBounds(true);
            invalidate();
        }
    }

    public final void setScaleType(@ScaleType int scaleType) {
        if (mScaleType == scaleType) {
            return;
        }
        mScaleType = scaleType;
        if (isReady()) {
            fitToBounds(true);
            invalidate();
        }
    }

    public final void setMaxScale(float maxScale) {
        mMaxScale = maxScale;
    }

    public final void setMinScale(float minScale) {
        mMinScale = minScale;
    }

    public final void setMinimumDpi(int dpi) {
        DisplayManager displayManager = DisplayManager.getInstance();
        Optional<Display> display = displayManager.getDefaultDisplay(mContext);
        DisplayAttributes metrics = display.get().getRealAttributes();
        float averageDpi = (metrics.xDpi + metrics.yDpi) / 2;
        setMaxScale(averageDpi / dpi);
    }


    public float getMaxScale() {
        return mMaxScale;
    }

    public float getMinScale() {
        return mMinScale;
    }

    public final PointF getCenter() {
        int mX = getWidth() / 2;
        int mY = getHeight() / 2;
        return viewToSourceCoordinate(mX, mY);
    }

    public final void setScaleAndCenter(float scale, PointF sCenter) {
        stopAnimator();
        mPendingScale = scale;
        mSourcePendingCenter = sCenter;
        invalidate();
    }

    public final void resetScaleAndCenter() {

        LogUtil.info(TAG, "resetScaleAndCenter method called");
        stopAnimator();
        mPendingScale = limitedScale(0);
        if (isReady()) {
            mSourcePendingCenter = new PointF(getShowWidth() / 2.0F, getShowHeight() / 2.0F);
        } else {
            mSourcePendingCenter = new PointF(0, 0);
        }
        invalidate();
    }

    /**
     * Whether the image can be drawn in the next frame
     *
     * @return true Can already be displayed on View
     * false Still loading layers
     */
    public final boolean isReady() {
        return mReadySent;
    }

    public final boolean isImageLoaded() {
        return mImageLoadedSent;
    }

    public final int getSourceWidth() {
        return mSourceWidth;
    }

    public final int getSourceHeight() {
        return mSourceHeight;
    }

    @Orientation
    public final int getOrientation() {
        return mOrientation;
    }

    public ImageSource getImageSource() {
        if (mViewTranslate != null && mSourceWidth > 0 && mSourceHeight > 0) {
            return ImageSourceBuilder.newBuilder()
                    .setUri(mUri)
                    .setImageSourceRegion(mSourceRegion)
                    .setImageSourceLoadListener(mImageSourceLoadListener)
                    .setImageSizeOptions(new ImageSizeOptions(mSourceWidth, mSourceHeight))
                    .setOrientation(mOrientation)
                    .setImageViewOptions(new ImageViewOptions(mScale, getCenter()))
                    .build();
        }
        return null;
    }

    public final boolean isZoomEnabled() {
        return mZoomEnabled;
    }

    public final void setZoomEnabled(boolean zoomEnabled) {
        mZoomEnabled = zoomEnabled;
    }

    public final boolean isQuickScaleEnabled() {
        return mQuickScaleEnabled;
    }

    public final void setQuickScaleEnabled(boolean quickScaleEnabled) {
        mQuickScaleEnabled = quickScaleEnabled;
    }

    public final boolean isTranslateEnabled() {
        return mTranslateEnabled;
    }

    public final void setTranslateEnabled(boolean translateEnabled) {
        if (mTranslateEnabled == translateEnabled) {
            return;
        }
        mTranslateEnabled = translateEnabled;
        if (!translateEnabled && mViewTranslate != null) {
            mViewTranslate.x = (getWidth() - mScale * getShowWidth()) / 2;
            mViewTranslate.y = (getHeight() - mScale * getShowHeight()) / 2;
            if (isReady()) {
                refreshRequiredTiles(true);
                invalidate();
            }
        }
    }

    public final void setMappingBackgroundColor(Color color , int colorValue) {
        if (Color.alpha(colorValue) == 0) {
            mMappingBgPaint = null;
        } else {
            mMappingBgPaint = new Paint();
            mMappingBgPaint.setStyle(Paint.Style.FILL_STYLE);
            mMappingBgPaint.setColor(color);
            //have to change this
        }
        invalidate();
    }

    public final void setDoubleTapZoomScale(float doubleTapZoomScale) {
        mDoubleTapZoomScale = doubleTapZoomScale;
    }
    // We need to create Engine class to store xdpi and ydpi values
    public final void setDoubleTapZoomDpi(int dpi) {
        DisplayManager displayManager = DisplayManager.getInstance();
        Optional<Display> display = displayManager.getDefaultDisplay(mContext);
        DisplayAttributes metrics = display.get().getRealAttributes();
        float averageDpi = (metrics.xDpi + metrics.yDpi) / 2;
        setDoubleTapZoomScale(averageDpi / dpi);
    }

    public final void setDoubleTapZoomStyle(@Zoom int doubleTapZoomStyle) {
        mDoubleTapZoomStyle = doubleTapZoomStyle;
    }

    public final void setDuration(int duration) {
        mDuration = Math.max(0, duration);
    }

    public Rect getSourceRegion() {
        return mSourceRegion;
    }

    @Override
    public void setLongClickedListener(@Nullable LongClickedListener l) {
        if (!isLongClickOn()) {
            setLongClickable(true);
        }
        mOnLongClickListener = l;
    }

    public void setOnBitmapLoadListener(OnBitmapLoadListener listener) {
        mOnBitmapLoadListener = listener;
    }

    public OnBitmapLoadListener getOnBitmapLoadListener() {
        return mOnBitmapLoadListener;
    }

    private void startFilingAnimation(float sCenterXEnd, float sCenterYEnd) {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
        }

        final int vxCenter = (getWidth() + getPaddingLeft() - getPaddingRight()) / 2;
        final int vyCenter = (getHeight() + getPaddingTop() - getPaddingBottom()) / 2;

        final PointF sourceCenter = new PointF(sCenterXEnd, sCenterYEnd);
        mValueAnimator = new AnimationBuilder(sourceCenter)
                .setTarget(this)
                .setScaleStart(mScale)
                .setScaleEnd(mScale)
                .setViewFocusStart(sourceToViewCoordinate(sourceCenter))
                .setViewFocusEnd(new PointF(vxCenter, vyCenter))
                .setTranslateInterpolator(mTranslationAnimationInterpolator)
                .setScaleInterpolator(mScaleAnimationInterpolator)
                .setDuration(mDuration)
                .addAnimationListener(mAnimatorListener)
                .addAnimationListener(mDefaultAnimatorListener)
                .addAnimationUpdateListener(mAnimatorUpdateListener)
                .addAnimationUpdateListener(mDefaultAnimatorUpdateListener)
                .build();
        mValueAnimator.start();

        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "startFilingAnimation");
        }
    }

    private void startZoomForCenter(PointF sCenter, float scaleEnd) {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
        }
        final int vxCenter = (getWidth() + getPaddingLeft() - getPaddingRight()) / 2;
        final int vyCenter = (getHeight() + getPaddingTop() - getPaddingBottom()) / 2;

        final float limitScale = limitedScale(scaleEnd);

        final PointF limitSourceCenter = limitedSourceCenter(sCenter.x, sCenter.y, limitScale, new PointF());

        mValueAnimator = new AnimationBuilder(limitSourceCenter)
                .setTarget(this)
                .setScaleStart(mScale)
                .setScaleEnd(limitScale)
                .setViewFocusStart(sourceToViewCoordinate(limitSourceCenter))
                .setViewFocusEnd(new PointF(vxCenter, vyCenter))
                .setDuration(mDuration)
                .setInterrupt(false)
                .setTranslateInterpolator(mTranslationAnimationInterpolator)
                .setScaleInterpolator(mScaleAnimationInterpolator)
                .addAnimationListener(mAnimatorListener)
                .addAnimationListener(mDefaultAnimatorListener)
                .addAnimationUpdateListener(mAnimatorUpdateListener)
                .addAnimationUpdateListener(mDefaultAnimatorUpdateListener)
                .build();

        mValueAnimator.start();

        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "startZoomForCenter");
        }
    }



    private void startZoomForFixed(PointF sCenter, PointF vFocus, float scaleEnd) {

        if (mValueAnimator != null) {
            mValueAnimator.cancel();
        }

        final float limitScale = limitedScale(scaleEnd);

        final PointF limitSourceCenter = limitedSourceCenter(sCenter.x, sCenter.y, limitScale, new PointF());

        PointF focusEnd;
        if (vFocus != null) {
            PointF center = getCenter();
            float vTranslateXEnd = vFocus.x - (limitScale * center.x);
            float vTranslateYEnd = vFocus.y - (limitScale * center.y);
            ScaleAndTranslate satEnd = new ScaleAndTranslate(limitScale, new PointF(vTranslateXEnd, vTranslateYEnd));
            fitToBounds(true, satEnd);
            focusEnd = new PointF(
                    vFocus.x + (satEnd.mViewTranslate.x - vTranslateXEnd),
                    vFocus.y + (satEnd.mViewTranslate.y - vTranslateYEnd));
        } else {
            final int vxCenter = (getWidth() + getPaddingLeft() - getPaddingRight()) / 2;
            final int vyCenter = (getHeight() + getPaddingTop() - getPaddingBottom()) / 2;
            focusEnd = new PointF(vxCenter, vyCenter);
        }

        mValueAnimator = new AnimationBuilder(limitSourceCenter)
                .setTarget(this)
                .setScaleStart(mScale)
                .setScaleEnd(limitScale)
                .setViewFocusStart(sourceToViewCoordinate(limitSourceCenter))
                .setViewFocusEnd(focusEnd)
                .setDuration(mDuration)
                .setInterrupt(false)
                .setTranslateInterpolator(mTranslationAnimationInterpolator)
                .setScaleInterpolator(mScaleAnimationInterpolator)
                .addAnimationListener(mAnimatorListener)
                .addAnimationListener(mDefaultAnimatorListener)
                .addAnimationUpdateListener(mAnimatorUpdateListener)
                .addAnimationUpdateListener(mDefaultAnimatorUpdateListener)
                .build();

        mValueAnimator.start();

        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "startZoomForFixed");
        }
    }

    @Override
    public void onDragDown(Component component, DragInfo dragInfo) {

    }

    @Override
    public void onDragStart(Component component, DragInfo dragInfo) {

    }

    @Override
    public void onDragUpdate(Component component, DragInfo dragInfo) {

    }

    @Override
    public void onDragEnd(Component component, DragInfo dragInfo) {
        mIsZooming = false;
        mIsPanning = false;
        mMaxTouchCount = 0;

        Point e1 = new Point(dragInfo.startPoint);
        Point e2 = new Point(dragInfo.updatePoint);

        if (mTranslateEnabled && mReadySent && mViewTranslate != null && e1 != null && e2 != null
                && (Math.abs(e1.getPointX() - e2.getPointX()) > 50 || Math.abs(e1.getPointY() - e2.getPointY()) > 50)
                && (Math.abs(dragInfo.xVelocity) > 500 || Math.abs(dragInfo.yVelocity) > 500) && !mIsZooming) {
            PointF vTranslateEnd =
                    new PointF(mViewTranslate.x + ((float) dragInfo.xVelocity * 0.25f), mViewTranslate.y + ((float) dragInfo.yVelocity * 0.25f));
            float sCenterXEnd = ((getWidth() / 2.0F) - vTranslateEnd.x) / mScale;
            float sCenterYEnd = ((getHeight() / 2.0F) - vTranslateEnd.y) / mScale;
            startFilingAnimation(sCenterXEnd, sCenterYEnd);
//                    if (BuildConfig.DEBUG) Log.d(TAG, "onFling: 正在滑行");
//                    return true;
        }
    }

    @Override
    public void onDragCancel(Component component, DragInfo dragInfo) {

    }

    private class MappingsInit implements Runnable {

        private BitmapDataSource mDecoder;
        private Rect mRect;
        private Uri mUri;

        MappingsInit(@NotNull Context context,
                     @NotNull BitmapDataSource decoder,
                     Rect rect,
                     @NotNull Uri uri) {
            mContext = context;
            mDecoder = decoder;
            mRect = rect;
            mUri = uri;
            //Put image uri into shared preferences
//            sharedPreferences.putString("Uri", mUri.toString());
        }

        @Override
        public void run() {
            if (mContext == null || mDecoder == null || mUri == null) {
                return;
            }
            final Point dimensions = new Point(0 , 0);
            mDecoder.init(mContext, mUri, dimensions, new BitmapDataSource.OnInitListener() {
                @Override
                public void success() throws DataAbilityRemoteException {
                    int exifOrientation = mBitmapDataSource.getExifOrientation(mContext, mUri.toString());
                    PacMap bundle = new PacMap();
                    bundle.putIntValue(SOURCE_ORIENTATION, exifOrientation);
                    InnerEvent msg = InnerEvent.get();
                    msg.eventId = MSG_INIT_SUCCESS;
                    if (mRect != null) {

                        if (mRect.left > mRect.right) {
                            int tmp = mRect.left;
                            mRect.left = mRect.right;
                            mRect.right = tmp;
                        }

                        if (mRect.left < 0 || mRect.left >= dimensions.getPointXToInt()) {
                            mRect.left = 0;
                        }

                        if (mRect.right < 0 || mRect.right > dimensions.getPointYToInt()) {
                            mRect.right = dimensions.getPointXToInt();
                        }

                        if (mRect.top > mRect.bottom) {
                            int tmp = mRect.bottom;
                            mRect.bottom = mRect.top;
                            mRect.top = tmp;
                        }

                        if (mRect.top < 0 || mRect.top >= dimensions.getPointYToInt()) {
                            mRect.top = 0;
                        }

                        if (mRect.bottom < 0 || mRect.bottom > dimensions.getPointYToInt()) {
                            mRect.bottom = dimensions.getPointYToInt();
                        }

                        bundle.putIntValue(SOURCE_WIDTH, mRect.getWidth());
                        bundle.putIntValue(SOURCE_Height, mRect.getHeight());
                    } else {
                        bundle.putIntValue(SOURCE_WIDTH, dimensions.getPointXToInt());
                        bundle.putIntValue(SOURCE_Height, dimensions.getPointYToInt());
                    }
                    msg.setPacMap(bundle);
                    mOriginalHandler.sendEvent(msg);
                    LogUtil.info(TAG, "Mapping init runs and sent success message.");
                }

                @Override
                public void failed(Throwable throwable) {
                    InnerEvent msg = InnerEvent.get();
                    msg.eventId = MSG_INIT_FAILED;
                    msg.object = throwable;
                    mOriginalHandler.sendEvent(msg);
                    throwable.printStackTrace();
                }
            });
        }
    }

    private class MappingLoad implements Runnable {
        private BitmapDataSource mDecoder;
        private Mapping mMapping;
        private Rect mRegion;
        private int mSourceWidth;
        private int mSourceHeight;
        private int mRotation;

        public MappingLoad(BitmapDataSource decoder, Mapping mapping, Rect region,
                           int sourceWidth, int sourceHeight, int rotation) {
            mDecoder = decoder;
            mMapping = mapping;
            mRegion = region;
            mSourceWidth = sourceWidth;
            mSourceHeight = sourceHeight;
            mRotation = rotation;
        }

        @Override
        public void run() {
            if (mMapping == null || mDecoder == null) {
                return;
            }
            if (mDecoder.isReady() && mMapping.mVisible) {
                fileRect(mMapping.mSourceRect, mMapping.mFileSourceRect, mSourceWidth, mSourceHeight, mRotation);
                if (mRegion != null) {
                    mMapping.mFileSourceRect.offset(mRegion.left, mRegion.top);
                }

                PixelMap bitmap = mDecoder.decode(mMapping.mFileSourceRect, mMapping.mSampleSize);
                if (bitmap != null) {
                    mMapping.mBitmap = bitmap;
                    mMapping.mLoading = false;
                    if (!mOriginalHandler.hasInnerEvent(MSG_TILE_LOAD_SUCCESS)) {
                        mOriginalHandler.sendEvent(MSG_TILE_LOAD_SUCCESS);
                    } else if (BuildConfig.DEBUG) {
//                        Log.d(TAG, "已经有相同的消息了");
                    }
                }
            } else {
                mMapping.mLoading = false;
            }
        }
    }
}
