package xyz.zpayh.hdimageview.datasource;

//import android.content.Context;
//import androidx.annotation.NonNull;

import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.app.Context;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.state.Orientation;

public interface OrientationInterceptor {
    @Orientation
    int getExifOrientation(@NotNull Context context, String sourceUri) throws DataAbilityRemoteException;
}
