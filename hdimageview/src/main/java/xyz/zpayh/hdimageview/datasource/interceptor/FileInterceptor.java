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

//import android.graphics.BitmapRegionDecoder;
//import android.net.Uri;
//import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.media.image.Image;
import ohos.media.image.ImageSource;
import ohos.utils.net.Uri;
//import xyz.zpayh.hdimageview.BuildConfig;
import xyz.zpayh.hdimageview.datasource.Interceptor;
import xyz.zpayh.hdimageview.util.UriUtil;

/**
 * 文 件 名: FileInterceptor
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/29 17:49
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class FileInterceptor implements Interceptor {

    private static Context fileInterceptor_context;

    public static void setFileInterceptor_context(Context context) {
        fileInterceptor_context = context;
    }

    private Context getFileInterceptor_context() {
        return fileInterceptor_context;
    }

    @Override
    public ImageSource intercept(Chain chain) throws IOException, NotExistException, DataAbilityRemoteException {
        final Uri uri = chain.uri();
        ImageSource decoder = chain.chain(uri);
        if (decoder != null){
            return decoder;
        }


        if (UriUtil.isLocalFileUri(uri)){
            File file = new File(uri.getDecodedPath());
//            if (BuildConfig.DEBUG) {
//                Log.d("FileInterceptor", "从我这加载");
//            }
            try {
//                return ImageSource.create(new FileInputStream(file.toString()),false);
//                return ImageSource.create()
                DataAbilityHelper dataAbilityHelper = DataAbilityHelper.creator(fileInterceptor_context , uri);
                InputStream inputStream = dataAbilityHelper.obtainInputStream(uri);
                return ImageSource.create(inputStream, null);
            } catch (IOException e) {
                return Interceptors.fixJPEGDecoder(file,e);
            }
        }
        return null;
    }
}
