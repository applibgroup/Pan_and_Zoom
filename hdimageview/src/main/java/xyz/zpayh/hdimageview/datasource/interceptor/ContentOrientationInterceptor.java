package xyz.zpayh.hdimageview.datasource.interceptor;

//import android.content.ContentResolver;
//import android.content.Context;
//import android.database.Cursor;
//import android.net.Uri;
//import android.provider.MediaStore;
//import androidx.annotation.NonNull;

import ohos.aafwk.ability.DataAbilityRemoteException;
import ohos.app.Context;
import ohos.data.resultset.ResultSet;
import ohos.utils.net.Uri;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.datasource.OrientationInterceptor;
import xyz.zpayh.hdimageview.state.Orientation;

import static ohos.aafwk.ability.DataAbilityHelper.creator;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_0;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_180;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_270;
import static xyz.zpayh.hdimageview.state.Orientation.ORIENTATION_90;

public class ContentOrientationInterceptor implements OrientationInterceptor {
    @Override
    public int getExifOrientation(@NotNull Context context, String sourceUri) throws DataAbilityRemoteException {
        if (sourceUri.startsWith("dataability")) {
            final String[] columns = {"orientation"};
            final ResultSet cursor = creator(context)
                    .query(Uri.parse(sourceUri),columns,null);
            if (cursor != null){
                if (cursor.goToFirstRow()){
                    int orientation = cursor.getInt(0);
                    if (orientation == ORIENTATION_0){
                        return ORIENTATION_0;
                    } else if (orientation == ORIENTATION_90){
                        return ORIENTATION_90;
                    } else if (orientation == ORIENTATION_180){
                        return ORIENTATION_180;
                    } else if (orientation == ORIENTATION_270){
                        return ORIENTATION_270;
                    }
                }
                cursor.close();
            }
        }
        return Orientation.ORIENTATION_EXIF;
    }
}
