package xyz.zpayh.hdimageview.datasource.interceptor;
//
//import android.content.Context;
//import android.content.res.AssetManager;
//import androidx.annotation.NonNull;
//import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

import ohos.aafwk.ability.DataAbilityHelper;
import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.app.Context;
import ohos.media.image.ImageSource;
import ohos.utils.net.Uri;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.datasource.OrientationInterceptor;
import xyz.zpayh.hdimageview.state.Orientation;

import static xyz.zpayh.hdimageview.datasource.BitmapDataSource.ASSET_SCHEME;

public class AssetOrientationInterceptor implements OrientationInterceptor {
    @Override
    public int getExifOrientation(@NotNull Context context, String sourceUri) {
        if (sourceUri.startsWith(ASSET_SCHEME)) {
            try {
//                String assetName = sourceUri.substring(ASSET_SCHEME.length());
                Uri uri = Uri.parse(sourceUri);
                DataAbilityHelper dataAbilityHelper = DataAbilityHelper.creator(context, uri);
                InputStream inputStream = dataAbilityHelper.obtainInputStream(uri);
                ImageSource exifInterface = ImageSource.create(inputStream, null);
//                ImageSource exifInterface = ImageSource.create(uri.getDecodedPath() , null);
                int orientationAttr = exifInterface.getImagePropertyInt("Orientation",1);
                switch (orientationAttr) {
                    case 1:
                    case 0:
                        return Orientation.ORIENTATION_0;
                    case 6:
                        return Orientation.ORIENTATION_90;
                    case 3:
                        return Orientation.ORIENTATION_180;
                    case 8:
                        return Orientation.ORIENTATION_270;
                }
            } catch (IOException | DataAbilityRemoteException e) {
                e.printStackTrace();
            }
        }
        return Orientation.ORIENTATION_EXIF;
    }
}

//C:\Users\91935\DevEcoStudioProjects\HDImageView\entry\src\main\resources\rawfile\girl2.jpg