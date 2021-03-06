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

//import android.content.ContentResolver;
//import android.content.Context;
//import android.graphics.BitmapRegionDecoder;
//import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.utils.net.Uri;
import xyz.zpayh.hdimageview.datasource.Interceptor;
import xyz.zpayh.hdimageview.util.Preconditions;
import xyz.zpayh.hdimageview.util.UriUtil;

import static ohos.aafwk.ability.DataAbilityHelper.creator;

/**
 * 文 件 名: ContentInterceptor
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/30 20:07
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class ContentInterceptor implements Interceptor {

    private final Context mContext;

    public ContentInterceptor(Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    @Override
    public ImageSource intercept(Chain chain) throws IOException, DataAbilityRemoteException, NotExistException {
        final Uri uri = chain.uri();
        ImageSource decoder = chain.chain(uri);
        if (decoder != null){
            return decoder;
        }

        if (UriUtil.isLocalContentUri(uri) || UriUtil.isQualifiedResourceUri(uri)){
            InputStream inputStream = null;
            try {
                DataAbilityHelper resolver = creator(mContext);
                try {
                    inputStream = resolver.obtainInputStream(uri);
//                    resolver.openFile().
                    decoder = ImageSource.create(inputStream,null);
                } catch (IOException e) {
                    //e.printStackTrace();
                    inputStream = resolver.obtainInputStream(uri);
                    return Interceptors.fixJPEGDecoder(inputStream,uri,e);
                }
            } finally {
                if (inputStream != null){
                    try { inputStream.close(); }
                    catch (Exception e){e.printStackTrace();}
                }
            }
        }

        return decoder;
    }
}
