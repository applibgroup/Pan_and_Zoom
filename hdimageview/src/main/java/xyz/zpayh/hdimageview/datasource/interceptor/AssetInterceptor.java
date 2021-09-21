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

package xyz.zpayh.hdimageview.datasource.interceptor;

//import android.content.res.AssetManager;
//import android.graphics.BitmapRegionDecoder;
//import android.net.Uri;
//import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.components.Component;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.global.resource.ResourceManager;
import ohos.media.image.ImageSource;
import ohos.utils.net.Uri;
import xyz.zpayh.hdimageview.BuildConfig;
import xyz.zpayh.hdimageview.datasource.Interceptor;
import xyz.zpayh.hdimageview.util.UriUtil;

/**
 * 文 件 名: AssetInterceptor
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/29 17:28
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class AssetInterceptor implements Interceptor{

    private static Context context;
    private final ResourceManager mAssetManager;

    public static void setContext(Context c) {
        context = c;
    }

    public Context getContext() {
        return context;
    }

    public AssetInterceptor(ResourceManager assetManager) {
        mAssetManager = assetManager;
    }

    @Override
    public ImageSource intercept(Chain chain) throws IOException, NotExistException, DataAbilityRemoteException {
        final Uri uri = chain.uri();
        ImageSource decoder = chain.chain(uri);
        if (decoder != null){
            return decoder;
        }

        if (UriUtil.isLocalAssetUri(uri)){
            if (BuildConfig.DEBUG) {
//                Log.d("AssetInterceptor", "从我这加载");
            }

            DataAbilityHelper dataAbilityHelper = DataAbilityHelper.creator(context, uri);
            try {
//                InputStream inputStream = mAssetManager.getRawFileEntry(getAssetName(uri));
                InputStream inputStream = dataAbilityHelper.obtainInputStream(uri);
                return ImageSource.create(inputStream, null);
            } catch (IOException e) {
                InputStream inputStream = dataAbilityHelper.obtainInputStream(uri);
                return Interceptors.fixJPEGDecoder(inputStream,uri,e);
            }
        }
        return null;
    }

    private static String getAssetName(Uri uri) {
        return uri.getDecodedPath();
    }
}
