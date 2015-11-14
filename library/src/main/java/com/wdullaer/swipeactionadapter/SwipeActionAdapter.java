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

import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter that adds support for multiple swipe actions to your ListView
 *
 * Created by wdullaer on 04.06.14.
 */
public class SwipeActionAdapter extends DecoratorAdapter implements
        SwipeActionTouchListener.ActionCallbacks
{
    private ListView mListView;
    private SwipeActionTouchListener mTouchListener;
    protected SwipeActionListener mSwipeActionListener;
    private boolean mFadeOut = false;
    private boolean mFixedBackgrounds = false;
    private boolean mDimBackgrounds = false;
    private float mFarSwipeFraction = 0.5f;
    private float mNormalSwipeFraction = 0.25f;

    protected HashMap<SwipeDirection, Integer> mBackgroundResIds = new HashMap<>();

    public SwipeActionAdapter(BaseAdapter baseAdapter){
        super(baseAdapter);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent){
        SwipeViewGroup output = (SwipeViewGroup)convertView;

        if(output == null) {
            output = new SwipeViewGroup(parent.getContext());
            for(Map.Entry<SwipeDirection, Integer> entry : mBackgroundResIds.entrySet()) {
                output.addBackground(View.inflate(parent.getContext(), entry.getValue(), null), entry.getKey());
            }
            output.setSwipeTouchListener(mTouchListener);
        }

        output.setContentView(super.getView(position,output.getContentView(),output));

        return output;
    }

    /**
     * SwipeActionTouchListener.ActionCallbacks callback
     * We just link it through to our own interface
     *
     * @param position the position of the item that was swiped
     * @param direction the direction in which the swipe has happened
     * @return boolean indicating whether the item has actions
     */
    @Override
    public boolean hasActions(int position, SwipeDirection direction){
        return mSwipeActionListener != null && mSwipeActionListener.hasActions(position, direction);
    }

    /**
     * SwipeActionTouchListener.ActionCallbacks callback
     * We just link it through to our own interface
     *
     * @param listView The originating {@link ListView}.
     * @param position The position to perform the action on, sorted in descending  order
     *                 for convenience.
     * @param direction The type of swipe that triggered the action
     * @return boolean that indicates whether the list item should be dismissed or shown again.
     */
    @Override
    public boolean onPreAction(ListView listView, int position, SwipeDirection direction){
        return mSwipeActionListener != null && mSwipeActionListener.shouldDismiss(position, direction);
    }

    /**
     * SwipeActionTouchListener.ActionCallbacks callback
     * We just link it through to our own interface
     *
     * @param listView The originating {@link ListView}.
     * @param position The positions to perform the action on, sorted in descending  order
     *                 for convenience.
     * @param direction The type of swipe that triggered the action.
     */
    @Override
    public void onAction(ListView listView, int[] position, SwipeDirection[] direction){
        if(mSwipeActionListener != null) mSwipeActionListener.onSwipe(position, direction);
    }

    /**
     * Set whether items should have a fadeOut animation
     *
     * @param mFadeOut true makes items fade out with a swipe (opacity -> 0)
     * @return A reference to the current instance so that commands can be chained
     */
    @SuppressWarnings("unused")
    public SwipeActionAdapter setFadeOut(boolean mFadeOut){
        this.mFadeOut = mFadeOut;
        if(mListView != null) mTouchListener.setFadeOut(mFadeOut);
        return this;
    }

    /**
     * Set whether the backgrounds should be fixed or swipe in from the side
     * The default value for this property is false: backgrounds will swipe in
     *
     * @param fixedBackgrounds true for fixed backgrounds, false for swipe in
     */
    @SuppressWarnings("unused")
    public SwipeActionAdapter setFixedBackgrounds(boolean fixedBackgrounds){
        this.mFixedBackgrounds = fixedBackgrounds;
        if(mListView != null) mTouchListener.setFixedBackgrounds(fixedBackgrounds);
        return this;
    }

    /**
     * Set whether the backgrounds should be dimmed when in no-trigger zone
     * The default value for this property is false: backgrounds will not dim
     *
     * @param dimBackgrounds true for dimmed backgrounds, false for no opacity change
     */
    @SuppressWarnings("unused")
    public SwipeActionAdapter setDimBackgrounds(boolean dimBackgrounds){
        this.mDimBackgrounds = dimBackgrounds;
        if(mListView != null) mTouchListener.setDimBackgrounds(dimBackgrounds);
        return this;
    }

    /**
     * Set the fraction of the View Width that needs to be swiped before it is counted as a far swipe
     *
     * @param farSwipeFraction float between 0 and 1
     */
    @SuppressWarnings("unused")
    public SwipeActionAdapter setFarSwipeFraction(float farSwipeFraction) {
        if(farSwipeFraction < 0 || farSwipeFraction > 1) {
            throw new IllegalArgumentException("Must be a float between 0 and 1");
        }
        this.mFarSwipeFraction = farSwipeFraction;
        if(mListView != null) mTouchListener.setFarSwipeFraction(farSwipeFraction);
        return this;
    }

    /**
     * Set the fraction of the View Width that needs to be swiped before it is counted as a normal swipe
     *
     * @param normalSwipeFraction float between 0 and 1
     */
    @SuppressWarnings("unused")
    public SwipeActionAdapter setNormalSwipeFraction(float normalSwipeFraction) {
        if(normalSwipeFraction < 0 || normalSwipeFraction > 1) {
            throw new IllegalArgumentException("Must be a float between 0 and 1");
        }
        this.mNormalSwipeFraction = normalSwipeFraction;
        if(mListView != null) mTouchListener.setNormalSwipeFraction(normalSwipeFraction);
        return this;
    }
    
    /**
     * We need the ListView to be able to modify it's OnTouchListener
     *
     * @param listView the ListView to which the adapter will be attached
     * @return A reference to the current instance so that commands can be chained
     */
    public SwipeActionAdapter setListView(ListView listView){
        this.mListView = listView;
        mTouchListener = new SwipeActionTouchListener(listView,this);
        this.mListView.setOnTouchListener(mTouchListener);
        this.mListView.setOnScrollListener(mTouchListener.makeScrollListener());
        this.mListView.setClipChildren(false);
        mTouchListener.setFadeOut(mFadeOut);
        mTouchListener.setDimBackgrounds(mDimBackgrounds);
        mTouchListener.setFixedBackgrounds(mFixedBackgrounds);
        mTouchListener.setNormalSwipeFraction(mNormalSwipeFraction);
        mTouchListener.setFarSwipeFraction(mFarSwipeFraction);
        return this;
    }

    /**
     * Getter that is just here for completeness
     *
     * @return the current ListView
     */
    @SuppressWarnings("unused")
    public AbsListView getListView(){
        return mListView;
    }

    /**
     * Add a background image for a certain callback. The key for the background must be one of the
     * directions from the SwipeDirections class.
     *
     * @param key the identifier of the callback for which this resource should be shown
     * @param resId the resource Id of the background to add
     * @return A reference to the current instance so that commands can be chained
     */
    public SwipeActionAdapter addBackground(SwipeDirection key, int resId){
        if(SwipeDirection.getAllDirections().contains(key)) mBackgroundResIds.put(key,resId);
        return this;
    }

    /**
     * Set the listener for swipe events
     *
     * @param mSwipeActionListener class listening to swipe events
     * @return A reference to the current instance so that commands can be chained
     */
    public SwipeActionAdapter setSwipeActionListener(SwipeActionListener mSwipeActionListener){
        this.mSwipeActionListener = mSwipeActionListener;
        return this;
    }

    /**
     * Interface that listeners of swipe events should implement
     */
    public interface SwipeActionListener{
        boolean hasActions(int position, SwipeDirection direction);
        boolean shouldDismiss(int position, SwipeDirection direction);
        void onSwipe(int[] position, SwipeDirection[] direction);
    }
}
