/*
 * Copyright 2014 Wouter Dullaert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wdullaer.swipeactionadapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold a ListView item and the swipe backgrounds
 *
 * Created by wdullaer on 22.06.14.
 */
public class SwipeViewGroup extends FrameLayout implements Checkable {
    private View contentView = null;

    private SwipeDirection visibleView = SwipeDirection.DIRECTION_NEUTRAL;
    private HashMap<SwipeDirection, View> mBackgroundMap = new HashMap<>();
    private OnTouchListener swipeTouchListener;
    private boolean checked;

    public SwipeViewGroup(Context context) {
        super(context);
        initialize();
    }

    public SwipeViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SwipeViewGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Common code for all the constructors
     */
    private void initialize() {
        // Allows click events to reach the ListView in case the row has a clickable View like a Button
        // FIXME: probably messes with accessibility. Doesn't fix root cause (see onTouchEvent)
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
    }

    /**
     * Add a View to the background of the Layout. The background should have the same height
     * as the contentView
     *
     * @param background The View to be added to the Layout
     * @param direction The key to be used to find it again
     * @return A reference to the a layout so commands can be chained
     */
    public SwipeViewGroup addBackground(View background, SwipeDirection direction){
        if(mBackgroundMap.get(direction) != null) removeView(mBackgroundMap.get(direction));

        background.setVisibility(View.INVISIBLE);
        mBackgroundMap.put(direction, background);
        addView(background);
        return this;
    }

    /**
     * Show the View linked to a key. Don't do anything if the key is not found
     *
     * @param direction The key of the View to be shown
     * @param dimBackground Indicates whether the background should be dimmed
     */
    public void showBackground(SwipeDirection direction, boolean dimBackground){
        if(SwipeDirection.DIRECTION_NEUTRAL != direction && mBackgroundMap.get(direction) == null) return;

        if(SwipeDirection.DIRECTION_NEUTRAL != visibleView)
            mBackgroundMap.get(visibleView).setVisibility(View.INVISIBLE);
        if(SwipeDirection.DIRECTION_NEUTRAL != direction) {
            mBackgroundMap.get(direction).setVisibility(View.VISIBLE);
            mBackgroundMap.get(direction).setAlpha(dimBackground ? 0.4f : 1);
        }

        visibleView = direction;
    }

    /**
     * Add a contentView to the Layout
     *
     * @param contentView The View to be added
     * @return A reference to the layout so commands can be chained
     */
    public SwipeViewGroup setContentView(View contentView){
        if(this.contentView != null) removeView(contentView);
        addView(contentView);
        this.contentView = contentView;

        return this;
    }

    /**
     * Returns the current contentView of the Layout
     *
     * @return contentView of the Layout
     */
    public View getContentView(){
        return contentView;
    }

    /**
     * Move all backgrounds to the edge of the Layout so they can be swiped in
     */
    public void translateBackgrounds(){
        this.setClipChildren(false);
        for(Map.Entry<SwipeDirection, View> entry : mBackgroundMap.entrySet()) {
            int signum = entry.getKey().isLeft() ? 1 : -1;
            entry.getValue().setTranslationX(signum*entry.getValue().getWidth());
        }
    }

    /**
     * Set a touch listener the SwipeViewGroup will watch: once the OnTouchListener is interested in
     * events, the SwipeViewGroup will stop propagating touch events to its children
     *
     * @param swipeTouchListener The OnTouchListener to watch
     * @return A reference to the layout so commands can be chained
     */
    public SwipeViewGroup setSwipeTouchListener(OnTouchListener swipeTouchListener) {
        this.swipeTouchListener = swipeTouchListener;
        return this;
    }

    @Override
    public Object getTag() {
        if(contentView != null) return contentView.getTag();
        else return null;
    }

    @Override
    public void setTag(Object tag) {
        if(contentView != null) contentView.setTag(tag);
    }

    @Override
    public Object getTag(int key) {
        if(contentView != null) return contentView.getTag(key);
        else return null;
    }

    @Override
    public void setTag(int key, Object tag) {
        if(contentView != null) contentView.setTag(key, tag);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Start tracking the touch when a child is processing it
        return super.onInterceptTouchEvent(ev) || swipeTouchListener.onTouch(this, ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        // Finish the swipe gesture: our parent will no longer do it if this function is called
        return swipeTouchListener.onTouch(this, ev);
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        if (contentView != null && contentView instanceof Checkable) {
            ((Checkable)contentView).setChecked(checked);
        }
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        this.setChecked(!checked);
    }
}
