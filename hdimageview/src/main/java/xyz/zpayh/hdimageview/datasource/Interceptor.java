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

//import android.graphics.BitmapRegionDecoder;
//import android.net.Uri;

import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.components.Component;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.media.image.ImageSource;
import ohos.utils.net.Uri;

import java.io.IOException;

/**
 * 文 件 名: Interceptor
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/7/29 16:31
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public interface Interceptor {
    ImageSource intercept(Chain chain) throws IOException, NotExistException, DataAbilityRemoteException;

    interface Chain {

        Uri uri();

        ImageSource chain(Uri uri) throws IOException, NotExistException, DataAbilityRemoteException;
    }
}
