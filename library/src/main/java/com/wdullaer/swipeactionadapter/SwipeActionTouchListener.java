/*
* Copyright 2013 Google Inc
* Copyright 2014 Wouter Dullaert
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.wdullaer.swipeactionadapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link View.OnTouchListener} that makes the list items in a {@link ListView}
 * dismissable. {@link ListView} is given special treatment because by default it handles touches
 * for its list items... i.e. it's in charge of drawing the pressed state (the list selector),
 * handling list item clicks, etc.
 *
 * <p>After creating the listener, the caller should also call
 * {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}, passing
 * in the scroll listener returned by {@link #makeScrollListener()}. If a scroll listener is
 * already assigned, the caller should still pass scroll changes through to this listener. This will
 * ensure that this {@link SwipeActionTouchListener} is paused during list view
 * scrolling.</p>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * SwipeActionTouchListener touchListener =
 * new SwipeActionTouchListener(
 * listView,
 * new SwipeActionTouchListener.OnDismissCallback() {
 * public void onDismiss(ListView listView, int[] reverseSortedPositions) {
 * for (int position : reverseSortedPositions) {
 * adapter.remove(adapter.getItem(position));
 * }
 * adapter.notifyDataSetChanged();
 * }
 * });
 * listView.setOnTouchListener(touchListener);
 * listView.setOnScrollListener(touchListener.makeScrollListener());
 * </pre>
 *
 * <p>This class Requires API level 12 or later due to use of {@link
 * ViewPropertyAnimator}.</p>
 */
public class SwipeActionTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private ListView mListView;
    private ActionCallbacks mCallbacks;
    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero
    private boolean mFadeOut = false;
    private boolean mFixedBackgrounds = false;
    private boolean mDimBackgrounds = false;
    private float mNormalSwipeFraction = 0.25f;
    private float mFarSwipeFraction = 0.5f;

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<>();
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private SwipeViewGroup mDownViewGroup;
    private boolean mPaused;
    private int mDirection;
    private boolean mFar;

    /**
     * The callback interface used by {@link SwipeActionTouchListener} to inform its client
     * about a successful dismissal of one or more list item positions.
     */
    public interface ActionCallbacks {
        /**
         * Called to determine whether the given position can be dismissed.
         *
         * @param position the position of the item that was swiped
         * @return boolean indicating whether the item has actions
         */
        boolean hasActions(int position);

        /**
         * Called when the user has swiped a list item position.
         * The listener will wait for this method to return before starting the dismiss animation
         * or the reappear animation. Please perform any heavy computations in an ASyncTask to avoid
         * blocking the interface.
         *
         * @param listView The originating {@link ListView}.
         * @param position The position to perform the action on, sorted in descending  order
         *                 for convenience.
         * @param direction The type of swipe that triggered the action
         * @return boolean that indicates whether the list item should be dismissed or shown again.
         */
        boolean onPreAction(ListView listView, int position, int direction);

        /**
         * Called after the dismiss or reappear animation of a swiped item has finished.
         *
         * @param listView The originating {@link ListView}.
         * @param position The position to perform the action on, sorted in descending  order
         *                 for convenience.
         * @param direction The type of swipe that triggered the action
         */
        void onAction(ListView listView, int[] position, int[] direction);
    }

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given list view.
     *
     * @param listView The list view whose items should be dismissable.
     * @param callbacks The callback to trigger when the user has indicated that she would like to
     * dismiss one or more list items.
     */
    public SwipeActionTouchListener(ListView listView, ActionCallbacks callbacks) {
        ViewConfiguration vc = ViewConfiguration.get(listView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = listView.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mListView = listView;
        mCallbacks = callbacks;
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    /**
     * Returns an {@link AbsListView.OnScrollListener} to be added to the {@link
     * ListView} using {@link ListView#setOnScrollListener(AbsListView.OnScrollListener)}.
     * If a scroll listener is already assigned, the caller should still pass scroll changes through
     * to this listener. This will ensure that this {@link SwipeActionTouchListener} is
     * paused during list view scrolling.</p>
     *
     * @see SwipeActionTouchListener
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };
    }

    /**
     * Set whether the list item should fade out when swiping or not.
     * The default value for this property is false
     *
     * @param fadeOut true for a fade out, false for no fade out.
     */
    protected void setFadeOut(boolean fadeOut){
        mFadeOut = fadeOut;
    }

    /**
     * Set whether the backgrounds should be fixed or swipe in from the side
     * The default value for this property is false: backgrounds will swipe in
     *
     * @param fixedBackgrounds true for fixed backgrounds, false for swipe in
     */
    protected void setFixedBackgrounds(boolean fixedBackgrounds){
        mFixedBackgrounds = fixedBackgrounds;
    }

    /**
     * Set whether the backgrounds should be dimmed while in no-trigger zone
     * The default value for this property is false: backgrounds will not dim
     *
     * @param dimBackgrounds true for fixed backgrounds, false for swipe in
     */
    protected void setDimBackgrounds(boolean dimBackgrounds){
        mDimBackgrounds = dimBackgrounds;
    }

    /**
     * Set the fraction of the View Width that needs to be swiped before it is counted as a far swipe
     *
     * @param farSwipeFraction float between 0 and 1, should be equal to or greater than normalSwipeFraction
     */
    protected void setFarSwipeFraction(float farSwipeFraction) {
        mFarSwipeFraction = farSwipeFraction;
    }

    /**
     * Set the fraction of the View Width that needs to be swiped before it is counted as a normal swipe
     *
     * @param normalSwipeFraction float between 0 and 1, should be equal to or less than farSwipeFraction
     */
    protected void setNormalSwipeFraction(float normalSwipeFraction) {
        mNormalSwipeFraction = normalSwipeFraction;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mViewWidth < 2) {
            mViewWidth = mListView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mListView.getChildCount();
                int[] listViewCoords = new int[2];
                mListView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = mListView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        try {
                            mDownViewGroup = (SwipeViewGroup) child;
                            mDownView = mFixedBackgrounds ? mDownViewGroup.getContentView() : child;
                            if(!mFixedBackgrounds) mDownViewGroup.translateBackgrounds();
                        }
                        catch(Exception e) {
                            mDownView = child;
                        }
                        break;
                    }
                }

                if (mDownView != null) {
                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();
                    mDownPosition = mListView.getPositionForView(mDownView);
                    if (mCallbacks.hasActions(mDownPosition)) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                    } else {
                        mDownView = null;
                    }
                }
                return false;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mDownView != null && mSwiping) {
                    // cancel
                    final int downPosition = mDownPosition;
                    mDownView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    resetBackgrounds(downPosition);
                                }
                            });
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                mDirection = SwipeDirections.DIRECTION_NEUTRAL;
                mFar = false;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;
                if (Math.abs(deltaX) > (mViewWidth * mNormalSwipeFraction) && mSwiping) {
                    dismiss = true;
                    dismissRight = deltaX > 0;
                } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                        && absVelocityY < absVelocityX && mSwiping) {
                    // dismiss only if flinging in the same direction as dragging
                    dismiss = (velocityX < 0) == (deltaX < 0);
                    dismissRight = mVelocityTracker.getXVelocity() > 0;
                }
                if (dismiss && mDownPosition != ListView.INVALID_POSITION) {
                    // dismiss
                    final View downView = mDownView; // mDownView gets null'd before animation ends
                    final int downPosition = mDownPosition;
                    final int direction = mDirection;
                    ++mDismissAnimationRefCount;
                    mDownView.animate()
                            .translationX(dismissRight ? mViewWidth : -mViewWidth)
                            .alpha(mFadeOut ? 0 : 1)
                            .setDuration(mAnimationTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    boolean performDismiss = mCallbacks.onPreAction(
                                            mListView,
                                            downPosition,
                                            direction
                                    );
                                    if(performDismiss) performDismiss(downView,downPosition,direction);
                                    else slideBack(downView, downPosition, direction);
                                }
                            });
                } else {
                    // cancel
                    final int downPosition = mDownPosition;
                    mDownView.animate()
                            .translationX(0)
                            .alpha(1)
                            .setDuration(mAnimationTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    resetBackgrounds(downPosition);
                                }
                            });
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                mDirection = SwipeDirections.DIRECTION_NEUTRAL;
                mFar = false;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (!mSwiping && Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                    mSwiping = true;
                    mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);

                    mListView.requestDisallowInterceptTouchEvent(true);

                    // Cancel ListView's touch (un-highlighting the item)
                    MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (motionEvent.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    mListView.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (mSwiping) {
                    if(mDirection*deltaX < 0) mFar = false;
                    if(!mFar && Math.abs(deltaX) > mViewWidth*mFarSwipeFraction) mFar = true;
                    if(!mFar) mDirection = (deltaX > 0 ? SwipeDirections.DIRECTION_NORMAL_RIGHT : SwipeDirections.DIRECTION_NORMAL_LEFT);
                    else mDirection = (deltaX > 0 ? SwipeDirections.DIRECTION_FAR_RIGHT : SwipeDirections.DIRECTION_FAR_LEFT);
                    mDownViewGroup.showBackground(mDirection, mDimBackgrounds && (Math.abs(deltaX) < mViewWidth*mNormalSwipeFraction));

                    mDownView.setTranslationX(deltaX - mSwipingSlop);
                    if(mFadeOut) mDownView.setAlpha(Math.max(0f, Math.min(1f,
                                1f - 2f * Math.abs(deltaX) / mViewWidth)));
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private void resetBackgrounds(int position) {
        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int lastVisiblePosition = mListView.getLastVisiblePosition();
        if (position >= firstVisiblePosition && position <= lastVisiblePosition) {
            // We need to prevent the swipe background from showing through once swipe is complete
            SwipeViewGroup swipeViewGroup = (SwipeViewGroup) mListView.getChildAt(position - firstVisiblePosition);
            swipeViewGroup.hideBackgrounds();
        }
    }

    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public int direction;
        public View view;

        public PendingDismissData(int position, int direction, View view) {
            this.position = position;
            this.direction = direction;
            this.view = view;
        }

        @Override
        public int compareTo(@NonNull PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    private void slideBack(final View slideInView, final int downPosition, final int direction){
        mPendingDismisses.add(new PendingDismissData(downPosition,direction,slideInView));
        slideInView.setTranslationX(slideInView.getTranslationX());
        slideInView.animate()
                .translationX(0)
                .alpha(1)
                .setDuration(mAnimationTime)
                .setListener(createAnimatorListener(slideInView.getHeight()));
    }

    private void performDismiss(final View dismissView, final int dismissPosition, final int direction) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

        animator.addListener(createAnimatorListener(originalHeight));

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, direction, dismissView));
        animator.start();
    }

    private AnimatorListenerAdapter createAnimatorListener(final int originalHeight){
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --mDismissAnimationRefCount;
                if (mDismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    // Sort by descending position
                    Collections.sort(mPendingDismisses);

                    int[] dismissPositions = new int[mPendingDismisses.size()];
                    int[] dismissDirections = new int [mPendingDismisses.size()];
                    for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                        dismissPositions[i] = mPendingDismisses.get(i).position;
                        dismissDirections[i] = mPendingDismisses.get(i).direction;
                    }
                    mCallbacks.onAction(mListView, dismissPositions, dismissDirections);

                    // Reset mDownPosition to avoid MotionEvent.ACTION_UP trying to start a dismiss
                    // animation with a stale position
                    mDownPosition = ListView.INVALID_POSITION;

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : mPendingDismisses) {
                        // Reset view presentation
                        pendingDismiss.view.setAlpha(1f);
                        pendingDismiss.view.setTranslationX(0);
                        lp = pendingDismiss.view.getLayoutParams();
                        lp.height = originalHeight;
                        pendingDismiss.view.setLayoutParams(lp);
                    }

                    // Send a cancel event
                    long time = SystemClock.uptimeMillis();
                    MotionEvent cancelEvent = MotionEvent.obtain(time, time,
                            MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    mListView.dispatchTouchEvent(cancelEvent);

                    mPendingDismisses.clear();
                    for (int position: dismissPositions) {
                        resetBackgrounds(position);
                    }
                }
            }
        };
    }
}
