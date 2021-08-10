package xyz.zpayh.hdimageview.datasource.interceptor;

//import android.content.Context;
//import androidx.annotation.NonNull;

import ohos.app.Context;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.datasource.OrientationInterceptor;

public class NetworkOrientationInterceptor implements OrientationInterceptor {
    @Override
    public int getExifOrientation(@NotNull Context context, String sourceUri) {
        return Interceptors.getExifOrientation(sourceUri);
    }
}
