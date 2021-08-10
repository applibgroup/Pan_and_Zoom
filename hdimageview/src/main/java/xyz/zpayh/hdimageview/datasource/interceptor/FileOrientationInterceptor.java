package xyz.zpayh.hdimageview.datasource.interceptor;

//import android.content.Context;
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

import static xyz.zpayh.hdimageview.datasource.BitmapDataSource.FILE_SCHEME;

public class FileOrientationInterceptor implements OrientationInterceptor {
    @Override
    public int getExifOrientation(@NotNull Context context, String sourceUri) {
        if (sourceUri.startsWith(FILE_SCHEME)) {
            try {
                Uri uri = Uri.parse(sourceUri);
                String path = uri.getDecodedPath();
                DataAbilityHelper dataAbilityHelper = DataAbilityHelper.creator(context, uri);
                InputStream inputStream = dataAbilityHelper.obtainInputStream(uri);
                ImageSource exifInterface = ImageSource.create(inputStream, null);
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
