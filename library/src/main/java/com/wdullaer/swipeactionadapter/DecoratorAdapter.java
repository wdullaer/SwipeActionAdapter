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

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

/**
 * Helper class that provides functionality for adapters that need to wrap other adapters
 * Created by wdullaer on 04.06.14.
 */
public class DecoratorAdapter extends BaseAdapter {
    protected final BaseAdapter mBaseAdapter;

    public DecoratorAdapter(BaseAdapter baseAdapter){
        mBaseAdapter = baseAdapter;
    }

    @SuppressWarnings("unused")
    public BaseAdapter getAdapter(){
        return mBaseAdapter;
    }

    @Override
    public int getCount(){
        return mBaseAdapter.getCount();
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent){
        return mBaseAdapter.getView(position,convertView,parent);
    }

    @Override
    public Object getItem(final int position){
        return mBaseAdapter.getItem(position);
    }

    @Override
    public long getItemId(final int position){
        return mBaseAdapter.getItemId(position);
    }

    @Override
    public boolean areAllItemsEnabled(){
        return mBaseAdapter.areAllItemsEnabled();
    }

    @Override
    public View getDropDownView(final int position, final View convertView, final ViewGroup parent){
        return mBaseAdapter.getDropDownView(position,convertView,parent);
    }

    @Override
    public int getItemViewType(final int position){
        return mBaseAdapter.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount(){
        return mBaseAdapter.getViewTypeCount();
    }

    @Override
    public boolean hasStableIds(){
        return mBaseAdapter.hasStableIds();
    }

    @Override
    public boolean isEmpty(){
        return mBaseAdapter.isEmpty();
    }

    @Override
    public boolean isEnabled(final int position){
        return mBaseAdapter.isEnabled(position);
    }

    @Override
    public void notifyDataSetChanged(){
        // ArrayAdapter does this himself
        if(!(mBaseAdapter instanceof ArrayAdapter<?>)) mBaseAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated(){
        mBaseAdapter.notifyDataSetInvalidated();
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer){
        mBaseAdapter.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(final DataSetObserver observer){
        mBaseAdapter.unregisterDataSetObserver(observer);
    }
}
