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

package xyz.zpayh.hdimageview.datasource;

//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.BitmapRegionDecoder;
//import android.graphics.Point;
//import android.graphics.Rect;
//import android.net.Uri;
//import androidx.annotation.NonNull;

import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.utils.Point;
import ohos.agp.utils.Rect;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.media.image.Image;
import ohos.media.image.PixelMap;
import ohos.media.image.ImageSource;
import ohos.utils.net.Uri;
import org.jetbrains.annotations.NotNull;
//import xyz.zpayh.hdimageview.Point;



import java.io.IOException;
import xyz.zpayh.hdimageview.core.HDImageViewFactory;

/**
 * 文 件 名: DefaultBitmapDataSource
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/29 13:26
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class DefaultBitmapDataSource implements BitmapDataSource{

    private ImageSource mDecoder;
    private final Object mDecoderLock = new Object();
    private OrientationInterceptor mOrientationInterceptor;

//    @Override
//    public void init(Context context, Uri uri, Point dimensions, OnInitListener listener) {
//
//
//    }

    @Override
    public void init(Context context, Uri uri, Point dimensions, OnInitListener listener) {
        try {
            mDecoder = getDecoderWithInterceptorChain(uri);
            mDecoder = getDecoderWithInterceptorChain(uri);
            if (mDecoder != null) {
                if (dimensions != null){
                    dimensions.modify(mDecoder.getImageInfo().size.width, mDecoder.getImageInfo().size.height);
                }
                if (listener != null){
                    listener.success();
                }
            }else{
                if (listener != null){
                    listener.failed(new IOException("init failed"));
                }
            }
        } catch (IOException | NotExistException | DataAbilityRemoteException e) {
            e.printStackTrace();
            if (listener != null){
                listener.failed(e);
            }
        }
    }

    @Override
    public PixelMap decode(Rect sRect, int sampleSize) {
        if (mDecoder == null){
            return null;
        }
        synchronized (mDecoderLock) {
//            ImageSource.SourceOptions options = new ImageSource.SourceOptions();
//            options. = sampleSize;
//            // Default RGB_565
//            options.inPreferredConfig = HDImageViewFactory.getInstance().
//            return mDecoder.c;
            ImageSource.DecodingOptions decodingOptions = new ImageSource.DecodingOptions();
            decodingOptions.sampleSize = sampleSize;
            ohos.media.image.common.Rect nRect = new ohos.media.image.common.Rect();
            nRect.minX = sRect.left;
            nRect.width = sRect.right - sRect.left;
            nRect.minY = sRect.bottom;
            nRect.height = sRect.top - sRect.bottom;
            decodingOptions.desiredRegion = nRect;
            return mDecoder.createPixelmap(decodingOptions);
        }
    }

    @Override
    public boolean isReady() {
        return mDecoder != null && !mDecoder.isReleased();
    }

    @Override
    public void recycle() {
        if (mDecoder != null) {
            mDecoder.release();
        }
    }

    @Override
    public int getExifOrientation(@NotNull Context context, String sourceUri) throws DataAbilityRemoteException {
        if (mOrientationInterceptor == null) {
            mOrientationInterceptor = new RealOrientationInterceptor(HDImageViewFactory.getInstance().getOrientationInterceptor());
        }
        return mOrientationInterceptor.getExifOrientation(context, sourceUri);
    }

    private ImageSource getDecoderWithInterceptorChain(Uri uri) throws IOException, NotExistException, DataAbilityRemoteException {
        Interceptor.Chain chain = new RealInterceptorChain(HDImageViewFactory.getInstance().getDataSourceInterceptor(),0,uri);
        return chain.chain(uri);
    }
}
