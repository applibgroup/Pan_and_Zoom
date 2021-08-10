package xyz.zpayh.hdimageview.datasource;

//import android.content.Context;
//import androidx.annotation.NonNull;

import java.util.List;

import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.app.Context;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.state.Orientation;

public class RealOrientationInterceptor implements OrientationInterceptor{

    private final List<OrientationInterceptor> mInterceptors;

    RealOrientationInterceptor(List<OrientationInterceptor> interceptors) {
        mInterceptors = interceptors;
    }

    @Override
    public int getExifOrientation(@NotNull Context context, String sourceUri) throws DataAbilityRemoteException {
        for (OrientationInterceptor interceptor : mInterceptors) {
            int orientation = interceptor.getExifOrientation(context, sourceUri);
            if (orientation != Orientation.ORIENTATION_EXIF) {
                return orientation;
            }
        }
        return Orientation.ORIENTATION_0;
    }
}
