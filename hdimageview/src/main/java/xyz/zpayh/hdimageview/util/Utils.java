/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zpayh.hdimageview.util;

import ohos.aafwk.ability.DataAbilityHelper;
import java.io.FileNotFoundException;
import java.io.InputStream;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.agp.utils.Rect;
import ohos.data.resultset.ResultSet;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.media.image.ImageSource;
import ohos.app.Context;
import ohos.utils.net.Uri;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.state.Orientation;

import static xyz.zpayh.hdimageview.datasource.BitmapDataSource.ASSET_SCHEME;
import static xyz.zpayh.hdimageview.datasource.BitmapDataSource.FILE_SCHEME;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_0;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_180;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_270;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_90;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_EXIF;

/**
 * Class containing some static utility methods.
 */
public class Utils {
    private static final HiLogLabel LABEL_LOG = new HiLogLabel(3, 0xD001100, "Utils");
    private static final String ORIENTATION = "orientation";

    private Utils() {}
    private static final String SCHEME_CONTENT = "content";
    @Orientation
    public static int getExifOrientation(@NotNull Context context, String sourceUri) throws DataAbilityRemoteException, FileNotFoundException {
        int exifOrientation = ORIENTATION_0;
        DataAbilityHelper dataAbilityHelper = DataAbilityHelper.creator(context, Uri.parse(sourceUri));
        InputStream inputStream = dataAbilityHelper.obtainInputStream(Uri.parse(sourceUri));

        if (sourceUri.startsWith(SCHEME_CONTENT)){
            final String[] columns = {ORIENTATION};

            final ResultSet cursor = dataAbilityHelper.query(Uri.parse(sourceUri), columns, null);
            if (cursor != null){
                if (cursor.goToFirstRow()){
                    int orientation = cursor.getInt(0);
                    if (orientation == ORIENTATION_0){
                        exifOrientation = ORIENTATION_0;
                    } else if (orientation == ORIENTATION_90){
                        exifOrientation = ORIENTATION_90;
                    } else if (orientation == ORIENTATION_180){
                        exifOrientation = ORIENTATION_180;
                    } else if (orientation == ORIENTATION_270){
                        exifOrientation = ORIENTATION_270;
                    } else{
                        HiLog.warn(LABEL_LOG, "Unsupported EXIF orientation" + orientation);
                    }
                }
                cursor.close();
            }
            return exifOrientation;
        }

        if (sourceUri.startsWith(FILE_SCHEME)){
            ImageSource exifInterface = ImageSource.create(inputStream, null);
            int orientationAttr = exifInterface.getImagePropertyInt("Orientation",
                    1);
            if (orientationAttr == 1 ||
                    orientationAttr == 0){
                exifOrientation = ORIENTATION_0;
            }else if (orientationAttr == 6){
                exifOrientation = ORIENTATION_90;
            }else if (orientationAttr == 3){
                exifOrientation = ORIENTATION_180;
            }else if (orientationAttr == 8){
                exifOrientation = ORIENTATION_270;
            }else{
                HiLog.warn(LABEL_LOG, "Unsupported EXIF orientation" + orientationAttr);
            }

            return exifOrientation;
        }

        if (sourceUri.startsWith(ASSET_SCHEME) && Build.SDK_INT >= 24){
            ImageSource exifInterface = ImageSource.create(inputStream , null);
            int orientationAttr = exifInterface.getImagePropertyInt("Orientation",
                    1);
            if (orientationAttr == 1 ||
                    orientationAttr == 0){
                exifOrientation = ORIENTATION_0;
            }else if (orientationAttr == 6){
                exifOrientation = ORIENTATION_90;
            }else if (orientationAttr == 3){
                exifOrientation = ORIENTATION_180;
            }else if (orientationAttr == 8){
                exifOrientation = ORIENTATION_270;
            }else{
                HiLog.warn(LABEL_LOG, "Unsupported EXIF orientation" + orientationAttr);
            }
        }

        return exifOrientation;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public static void fileRect(@NotNull Rect rect, @NotNull Rect target, int width,
                                int height, int rotation){
        switch (rotation){
            case ORIENTATION_0:
                target.set(rect.left, rect.top, rect.right, rect.bottom);
                return;
            case ORIENTATION_90:
                target.set(rect.top,height - rect.right,
                        rect.bottom,height - rect.left);
                return;
            case ORIENTATION_180:
                target.set(width - rect.right, height - rect.bottom,
                        width - rect.left, height - rect.top);
                return;
            case ORIENTATION_270:
            case ORIENTATION_EXIF:
            default:
                target.set(width-rect.bottom,rect.left,width-rect.top,
                        rect.right);
        }
    }

    public static boolean hasHoneycombMR1() {
        return Build.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasKitKat() {
        return Build.SDK_INT >= VERSION_CODES.KITKAT;
    }
}
