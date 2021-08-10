package xyz.zpayh.hdimageview.datasource;

import ohos.agp.animation.Animator;
import xyz.zpayh.hdimageview.core.DecelerateInterpolator;

public class AccelerateDecelerateInterpolator implements DecelerateInterpolator {

    @Override
    public float getCurvedTime(float input) {
        return (float)(Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }
}
