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

import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

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
    private float mFarSwipeFraction = 0.5f;

    protected SparseIntArray mBackgroundResIds = new SparseIntArray();

    public SwipeActionAdapter(BaseAdapter baseAdapter){
        super(baseAdapter);
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent){
        SwipeViewGroup output = (SwipeViewGroup)convertView;

        if(output == null) {
            output = new SwipeViewGroup(parent.getContext());
            for (int i = 0; i < mBackgroundResIds.size(); i++) {
                output.addBackground(View.inflate(parent.getContext(),mBackgroundResIds.valueAt(i), null),mBackgroundResIds.keyAt(i));
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
     * @return boolean indicating whether the item has actions
     */
    @Override
    public boolean hasActions(int position){
        return mSwipeActionListener != null && mSwipeActionListener.hasActions(position);
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
    public boolean onPreAction(ListView listView, int position, int direction){
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
    public void onAction(ListView listView, int[] position, int[] direction){
        if(mSwipeActionListener != null) mSwipeActionListener.onSwipe(position,direction);
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
     * @param mFixedBackgrounds true for fixed backgrounds, false for swipe in
     */
    @SuppressWarnings("unused")
    public SwipeActionAdapter setFixedBackgrounds(boolean mFixedBackgrounds){
        this.mFixedBackgrounds = mFixedBackgrounds;
        if(mListView != null) mTouchListener.setFixedBackgrounds(mFixedBackgrounds);
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
        mFarSwipeFraction = farSwipeFraction;
        if(mListView != null) mTouchListener.setFarSwipeFraction(mFarSwipeFraction);
        return this;
    }

    /**
     * We need the ListView to be able to modify it's OnTouchListener
     *
     * @param mListView the ListView to which the adapter will be attached
     * @return A reference to the current instance so that commands can be chained
     */
    public SwipeActionAdapter setListView(ListView mListView){
        this.mListView = mListView;
        mTouchListener = new SwipeActionTouchListener(mListView,this);
        this.mListView.setOnTouchListener(mTouchListener);
        this.mListView.setOnScrollListener(mTouchListener.makeScrollListener());
        this.mListView.setClipChildren(false);
        mTouchListener.setFadeOut(mFadeOut);
        mTouchListener.setFixedBackgrounds(mFixedBackgrounds);
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
    public SwipeActionAdapter addBackground(int key, int resId){
        if(SwipeDirections.getAllDirections().contains(key)) mBackgroundResIds.put(key,resId);
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
        public boolean hasActions(int position);
        public boolean shouldDismiss(int position, int direction);
        public void onSwipe(int[] position, int[] direction);
    }
}
