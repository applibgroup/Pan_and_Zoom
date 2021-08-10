/*
 *
 *  * Copyright 2017 陈志鹏
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package xyz.zpayh.hdimageview.core;

//import android.content.Context;
//import android.graphics.Bitmap;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import android.view.animation.DecelerateInterpolator;
//import android.view.animation.Interpolator;
import ohos.agp.animation.Animator;
import ohos.app.Context;
//ohos.agp.animation	Animator.TimelineCurve	interface	getCurvedTime
import ohos.agp.animation.Animator.TimelineCurve;
import ohos.media.image.PixelMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.zpayh.hdimageview.datasource.Interceptor;
import xyz.zpayh.hdimageview.datasource.OrientationInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.AssetInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.AssetOrientationInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.ContentInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.ContentOrientationInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.FileInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.FileOrientationInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.Interceptors;
import xyz.zpayh.hdimageview.datasource.interceptor.NetworkInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.NetworkOrientationInterceptor;
import xyz.zpayh.hdimageview.datasource.interceptor.ResourceInterceptor;
import xyz.zpayh.hdimageview.util.Preconditions;
import xyz.zpayh.hdimageview.HDImageView;

import static xyz.zpayh.hdimageview.HDImageView.mFactor;
//import xyz.zpayh.hdimageview.Interpolator;

/**
 * 文 件 名: HDImageViewConfig
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/29 14:19
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class HDImageViewConfig {
    private static final String TAG = "HDImageViewConfig";
    private final DecelerateInterpolator mScaleAnimationInterpolator;
    private final DecelerateInterpolator mTranslationAnimationInterpolator;

    private final List<Interceptor> mInterceptors;
    private final List<OrientationInterceptor> mOrientationInterceptors;
    private final Config mBitmapConfig;

    public static Builder newBuilder(Context context){
        return new Builder(context);
    }

    private HDImageViewConfig(Builder builder){
        mBitmapConfig = builder.mBitmapConfig;
        mScaleAnimationInterpolator = builder.mScaleAnimationInterpolator == null ?
                v -> 0 : builder.mScaleAnimationInterpolator;
        mTranslationAnimationInterpolator = builder.mTranslationAnimationInterpolator == null ?
                v -> 0 : builder.mTranslationAnimationInterpolator;

        mInterceptors = new ArrayList<>();


        mInterceptors.add(new ResourceInterceptor(builder.mContext.getResourceManager()));
        mInterceptors.add(new AssetInterceptor(builder.mContext.getResourceManager()));
        mInterceptors.add(new ContentInterceptor(builder.mContext));
        mInterceptors.add(new FileInterceptor());

        Interceptor glideInterceptor = addGlideInterceptor(builder.mContext);
        if (glideInterceptor != null){
            mInterceptors.add(glideInterceptor);
        }

        Interceptor frescoInterceptor = addFrescoInterceptor();
        if (frescoInterceptor != null){
            mInterceptors.add(frescoInterceptor);
        }

        if (glideInterceptor == null && frescoInterceptor == null) {
            mInterceptors.add(new NetworkInterceptor(builder.mContext));
        }
        mInterceptors.addAll(builder.mInterceptors);

        mOrientationInterceptors = new ArrayList<>();
        mOrientationInterceptors.addAll(builder.mOrientationInterceptors);

        mOrientationInterceptors.add(new AssetOrientationInterceptor());
        mOrientationInterceptors.add(new ContentOrientationInterceptor());
        mOrientationInterceptors.add(new FileOrientationInterceptor());
        mOrientationInterceptors.add(new NetworkOrientationInterceptor());

        //init
        Interceptors.initDiskLruCache(builder.mContext);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Interceptor addGlideInterceptor(Context context){
        Interceptor interceptor = null;
        try {
            Class<Interceptor> clazz =
                    (Class<Interceptor>) Class.forName("xyz.zpayh.hdimage.datasource.interceptor.GlideInterceptor");
            Constructor<Interceptor> constructor = clazz.getConstructor(Context.class);
            interceptor = constructor.newInstance(context);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return interceptor;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Interceptor addFrescoInterceptor(){
        Interceptor interceptor = null;

        try {
            Class<Interceptor> clazz =
                    (Class<Interceptor>) Class.forName("xyz.zpayh.hdimage.datasource.interceptor.FrescoInterceptor");
            interceptor = clazz.newInstance();
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        return interceptor;
    }

    public DecelerateInterpolator getScaleAnimationInterpolator() {
        return mScaleAnimationInterpolator;
    }

    public DecelerateInterpolator getTranslationAnimationInterpolator() {
        return mTranslationAnimationInterpolator;
    }

    public List<Interceptor> getInterceptors() {
        return mInterceptors;
    }

    public List<OrientationInterceptor> getOrientationInterceptors() {
        return mOrientationInterceptors;
    }

    public Config getBitmapConfig() {
        return mBitmapConfig;
    }

    public static class Builder {
        private DecelerateInterpolator mScaleAnimationInterpolator;
        private DecelerateInterpolator mTranslationAnimationInterpolator;
        private Context mContext;
        private List<Interceptor> mInterceptors;
        private final List<OrientationInterceptor> mOrientationInterceptors;
        private Config mBitmapConfig;
        private Builder(Context context){
            mContext = Preconditions.checkNotNull(context);
            mInterceptors = new ArrayList<>();
            mOrientationInterceptors = new ArrayList<>();
            mBitmapConfig = Config.RGB_565;
        }

        public Builder setBitmapConfig(@NotNull Config config) {
            mBitmapConfig = Preconditions.checkNotNull(config);
            return this;
        }

        public Builder setScaleAnimationInterpolator(DecelerateInterpolator scaleAnimationInterpolator) {
            mScaleAnimationInterpolator = scaleAnimationInterpolator;
            return this;
        }

        public Builder setTranslationAnimationInterpolator(DecelerateInterpolator translationAnimationInterpolator) {
            mTranslationAnimationInterpolator = translationAnimationInterpolator;
            return this;
        }

        public Builder addInterceptor(Interceptor interceptor){
            mInterceptors.add(interceptor);
            return this;
        }

        public Builder addOrientationInterceptor(OrientationInterceptor interceptor){
            mOrientationInterceptors.add(interceptor);
            return this;
        }

        public HDImageViewConfig build(){
            return new HDImageViewConfig(this);
        }
    }
}
