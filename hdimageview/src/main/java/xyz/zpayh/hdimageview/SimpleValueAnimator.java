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
//import android.view.View;
import ohos.agp.animation.Animator;
import ohos.agp.components.*;
import ohos.agp.window.service.*;
//import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

import ohos.eventhandler.InnerEvent;
import xyz.zpayh.hdimageview.animation.AnimatorListener;
import xyz.zpayh.hdimageview.animation.AnimatorUpdateListener;
import xyz.zpayh.hdimageview.animation.ValueAnimator;
import xyz.zpayh.hdimageview.core.DecelerateInterpolator;


/**
 * 文 件 名: SimpleValueAnimator
 * 创 建 人: 陈志鹏
 * 创建日期: 2017/4/20 14:43
 * 邮   箱: ch_zh_p@qq.com
 * 修改时间:
 * 修改备注:
 */

public class SimpleValueAnimator implements ValueAnimator {
    private List<AnimatorListener> mListeners = new ArrayList<>();
    private List<AnimatorUpdateListener> mUpdateListeners
            = new ArrayList<>();
    private Component mTarget;
    private long mStartTime;
    private long mDuration = 500;
    private float mFraction = 0f;

    private boolean mStarted = false;
    private boolean mEnded = false;

    private float mScaleStart;
    private float mScaleEnd;
    public PointF mSourceCenter;

    private PointF mViewFocusStart;
    private PointF mViewFocusEnd;
    private boolean mInterrupted = true;
    private DecelerateInterpolator mScaleInterpolator;
    private DecelerateInterpolator mTranslateInterpolator;

    public SimpleValueAnimator() {
    }
//
//



    public void setScaleStart(float scaleStart) {
        mScaleStart = scaleStart;
    }

    public void setScaleEnd(float scaleEnd) {
        mScaleEnd = scaleEnd;
    }

    public void setSourceCenter(PointF sourceCenter) {
        mSourceCenter = sourceCenter;
    }

    public void setViewFocusStart(PointF viewFocusStart) {
        mViewFocusStart = viewFocusStart;
    }

    public void setViewFocusEnd(PointF viewFocusEnd) {
        mViewFocusEnd = viewFocusEnd;
    }

    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }

    public boolean isInterrupted() {
        return mInterrupted;
    }

    public void setScaleInterpolator(DecelerateInterpolator scaleInterpolator) {
        mScaleInterpolator = scaleInterpolator;
    }

    public void setTranslateInterpolator(DecelerateInterpolator translateInterpolator) {
        mTranslateInterpolator = translateInterpolator;
    }

    public float getScale(){
        return mScaleStart + mScaleInterpolator.getCurvedTime(mFraction) * (mScaleEnd - mScaleStart);
    }

    public PointF getViewFocus(){
        float focusX = mViewFocusStart.x + mTranslateInterpolator.getCurvedTime(mFraction) *
                (mViewFocusEnd.x - mViewFocusStart.x);
        float focusY = mViewFocusStart.y + mTranslateInterpolator.getCurvedTime(mFraction) *
                (mViewFocusEnd.y - mViewFocusStart.y);
        return new PointF(focusX,focusY);
    }

    public boolean noChangeScale(){
        return mScaleStart == mScaleEnd;
    }

    private Runnable mLoopRunnable = new Runnable() {
        @Override
        public void run() {
            long dt = getTime() - mStartTime;
            float fraction = dt * 1f / mDuration;
            if (fraction > 1f || mTarget.getComponentParent() == null) {
                fraction = 1f;
            }
            mFraction = fraction;
            notifyUpdateListeners();
            if (mFraction >= 1f) {
                mEnded = true;
                dispatchEnd();
            } else {
//                HandlerActionQueue handlerActionQueue = new HandlerActionQueue();
//                handlerActionQueue.postDelayed(mLoopRunnable, 16);
                HDImageView.mHandler.postTask(mLoopRunnable , 16);
            }
        }
    };

    private void notifyUpdateListeners() {
        for (int i = mUpdateListeners.size() - 1; i >= 0; i--) {
            mUpdateListeners.get(i).onAnimationUpdate(this);
        }
    }

    private void dispatchStart() {
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            mListeners.get(i).onAnimationStart(this);
        }
    }

    private void dispatchEnd() {
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            mListeners.get(i).onAnimationEnd(this);
        }
    }

    private void dispatchCancel() {
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            mListeners.get(i).onAnimationCancel(this);
        }
    }

    private long getTime() {
        return System.currentTimeMillis();
    }

    @Override
    public void setTarget(Component view) {
        mTarget = view;
    }

    @Override
    public void addListener(AnimatorListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void setDuration(long duration) {
        if (!mStarted){
            mDuration = duration;
        }
    }

    public boolean isEnded() {
        return mEnded;
    }

    public boolean isStarted() {
        return mStarted;
    }

    @Override
    public void start() {
        if (mStarted){
            return;
        }
        mStarted = true;
        dispatchStart();
        mFraction = 0f;
        mStartTime = getTime();
        HDImageView.mHandler.postTask(mLoopRunnable , 16);
//        InnerEvent ie = new InnerEvent();
//        ie.callback
    }

    @Override
    public void cancel() {
        if (mEnded){
            return;
        }
        mEnded = true;
        if (mStarted){
            dispatchCancel();
        }
        dispatchEnd();
    }

    @Override
    public void addUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        mUpdateListeners.add(animatorUpdateListener);
    }

    @Override
    public float getAnimatedFraction() {
        return mFraction;
    }
}
