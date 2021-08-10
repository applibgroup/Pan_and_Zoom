package xyz.zpayh.hdimage.slice;

import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import xyz.zpayh.hdimage.ResourceTable;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import xyz.zpayh.hdimageview.*;
import xyz.zpayh.hdimageview.datasource.AccelerateDecelerateInterpolator;

public class MainAbilitySlice extends AbilitySlice {
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);
        final HiLogLabel LABEL_LOG = new HiLogLabel(3, 0xD001100, "MainAbilitySlice");
        String IMAGE = "https://pic1.zhimg.com/80/v2-4514d92451b190a0239a438a0736ec78_hd.jpg";

        HiLog.debug(LABEL_LOG , "We are in the main ability slice");

        HDImageView mImageView = (HDImageView) findComponentById(ResourceTable.Id_library);
        mImageView.setMaxScale(20f);
        mImageView.setMinScale(1f);
        mImageView.setDoubleTapZoomScale(10F);
        mImageView.setScaleType(3);

        HiLog.debug(LABEL_LOG , "We are in the main ability slice");

        ImageSource imageSource = ImageSourceBuilder.newBuilder()
                .setUri(IMAGE)
                .setImageSourceLoadListener((uri, options) -> {
                    float scaleW = mImageView.getWidth() / options.mWidth;
                    float scaleH = mImageView.getHeight() / options.mHeight;
                    mImageView.setMinScale(Math.min(1.0f , Math.min(scaleW , scaleH)));
                    mImageView.resetScaleAndCenter();
                })
                .build();
        mImageView.setImageSource(imageSource);
        mImageView.setScaleAnimationInterpolator(new AccelerateDecelerateInterpolator());
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }
}
