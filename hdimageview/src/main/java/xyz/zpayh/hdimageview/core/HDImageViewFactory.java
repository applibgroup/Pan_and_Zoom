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
//import android.view.animation.Interpolator;

import ohos.agp.animation.Animator;
import ohos.app.Context;
import ohos.media.image.PixelMap;
import java.util.List;

//import xyz.zpayh.hdimageview.Interpolator;
import xyz.zpayh.hdimageview.datasource.Interceptor;
import xyz.zpayh.hdimageview.datasource.OrientationInterceptor;
import xyz.zpayh.hdimageview.util.Preconditions;

/**
 * 文 件 名: HDImageViewFactory
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/29 14:08
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class HDImageViewFactory {
    private static HDImageViewFactory sInstance = null;

    private static HDImageViewFactory sDefaultInstance = null;

    public static HDImageViewFactory getInstance(){
        if (sInstance == null){
            return Preconditions.checkNotNull(sDefaultInstance, "Default HDImageViewFactory was not initialized!");
        }
        return sInstance;
    }

    public static void initializeDefault(Context context){
        if (sDefaultInstance == null){
            synchronized (HDImageViewFactory.class){
                if (sDefaultInstance == null){
                    sDefaultInstance = new HDImageViewFactory(HDImageViewConfig.newBuilder(context.getApplicationContext()).build());
                }
            }
        }
    }

    public static void initialize(Context context){
        initialize(HDImageViewConfig.newBuilder(context.getApplicationContext()).build());
    }

    public static void initialize(HDImageViewConfig config){
        sInstance = new HDImageViewFactory(config);
    }

    private final HDImageViewConfig mConfig;

    public HDImageViewFactory(HDImageViewConfig config){
        mConfig = Preconditions.checkNotNull(config);
    }

    public DecelerateInterpolator getScaleAnimationInterpolator() {
        return mConfig.getScaleAnimationInterpolator();
    }

    public DecelerateInterpolator getTranslationAnimationInterpolator() {
        return mConfig.getTranslationAnimationInterpolator();
    }

    public List<Interceptor> getDataSourceInterceptor(){
        return mConfig.getInterceptors();
    }

    public List<OrientationInterceptor> getOrientationInterceptor() {
        return mConfig.getOrientationInterceptors();
    }

    public Config getBitmapConfig() {
        return mConfig.getBitmapConfig();
    }
}
