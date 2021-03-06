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

package xyz.zpayh.hdimageview;

//import android.graphics.PointF;
//import androidx.annotation.NonNull;
////import android.view.View;
//import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

import ohos.agp.animation.Animator;
import ohos.agp.components.Component;
import org.jetbrains.annotations.NotNull;
import xyz.zpayh.hdimageview.animation.AnimatorListener;
import xyz.zpayh.hdimageview.animation.AnimatorUpdateListener;
import xyz.zpayh.hdimageview.core.DecelerateInterpolator;
import xyz.zpayh.hdimageview.util.Preconditions;

/**
 * 文 件 名: AnimationBuilder
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/4/20 13:42
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class AnimationBuilder {
    private static final long DEFAULT_DURATION = 500L;

    private float mScaleEnd;
    private float mScaleStart;
    private final PointF mTargetSCenter;
    private long mDuration = DEFAULT_DURATION;
    private DecelerateInterpolator mScaleInterpolator;
    private DecelerateInterpolator mTranslateInterpolator;
    private boolean mInterrupt = true;

    private Component mTarget;
    private final List<AnimatorListener> mAnimatorListener = new ArrayList<>();
    private final List<AnimatorUpdateListener> mAnimatorUpdateListener =
            new ArrayList<>();

    private PointF mViewFocusStart;
    private PointF mViewFocusEnd;

    public AnimationBuilder(PointF sCenter) {
        mTargetSCenter = sCenter;
    }

    public AnimationBuilder setDuration(long duration) {
        mDuration = duration;
        return this;
    }

    public AnimationBuilder setInterrupt(boolean interrupt) {
        mInterrupt = interrupt;
        return this;
    }

    public AnimationBuilder setScaleInterpolator(DecelerateInterpolator scaleInterpolator) {
        mScaleInterpolator = scaleInterpolator;
        return this;
    }

    public AnimationBuilder setTranslateInterpolator(DecelerateInterpolator translateInterpolator) {
        mTranslateInterpolator = translateInterpolator;
        return this;
    }

    public AnimationBuilder addAnimationListener(AnimatorListener listener) {
        if (listener == null){
            return this;
        }
        mAnimatorListener.add(listener);
        return this;
    }

    public AnimationBuilder addAnimationListener(List<AnimatorListener> listeners) {
        if (listeners == null){
            return this;
        }
        mAnimatorListener.addAll(listeners);
        return this;
    }

    public AnimationBuilder addAnimationUpdateListener(AnimatorUpdateListener listener) {
        if (listener == null){
            return this;
        }
        mAnimatorUpdateListener.add(listener);
        return this;
    }

    public AnimationBuilder addAnimationUpdateListener(List<AnimatorUpdateListener> listeners) {
        if (listeners == null){
            return this;
        }
        mAnimatorUpdateListener.addAll(listeners);
        return this;
    }

    public AnimationBuilder setTarget(@NotNull Component target) {
        Preconditions.checkNotNull(target);
        mTarget = target;
        return this;
    }

    public AnimationBuilder setViewFocusStart(PointF viewFocusStart) {
        mViewFocusStart = viewFocusStart;
        return this;
    }

    public AnimationBuilder setViewFocusEnd(PointF viewFocusEnd) {
        mViewFocusEnd = viewFocusEnd;
        return this;
    }

    public AnimationBuilder setScaleStart(float scaleStart) {
        mScaleStart = scaleStart;
        return this;
    }

    public AnimationBuilder setScaleEnd(float scaleEnd) {
        mScaleEnd = scaleEnd;
        return this;
    }

    public SimpleValueAnimator build(){

        SimpleValueAnimator animator = new SimpleValueAnimator();
        animator.setTarget(mTarget);
        animator.setScaleStart(mScaleStart);
        animator.setScaleEnd(mScaleEnd);
        animator.setSourceCenter(mTargetSCenter);
        animator.setViewFocusStart(mViewFocusStart);
        animator.setViewFocusEnd(mViewFocusEnd);
        animator.setDuration(mDuration);
        animator.setInterrupted(mInterrupt);
        animator.setScaleInterpolator(mScaleInterpolator);
        animator.setTranslateInterpolator(mTranslateInterpolator);
        for (AnimatorUpdateListener listener : mAnimatorUpdateListener) {
            animator.addUpdateListener(listener);
        }
        for (AnimatorListener listener : mAnimatorListener) {
            animator.addListener(listener);
        }
        return animator;
    }
}
