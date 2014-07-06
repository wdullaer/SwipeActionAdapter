SwipeActionAdapter - The Mailbox-like Swipe gesture library
===========================================================

SwipeActionAdapter is a library that provides a simple API to create Mailbox-like action when swiping in a ListView. The idea is to make it simple enough to implement while still providing sufficient options to allow you to integrate it with the design of your application.

Support for Android 4.0 and up. It might work on lower API versions, but I haven't tested it and I don't intend to make any effort in that direction.

Feel free to fork or issue pull requests on github. Issues can be reported on the github issue tracker.

<!-- ![Focused TokenAutoCompleteTextView example](https://raw.github.com/splitwise/TokenAutoComplete/gh-pages/images/focused.png) -->

<!-- ![Unfocused TokenAutoCompleteTextView example](https://raw.github.com/splitwise/TokenAutoComplete/gh-pages/images/not_focused.png) -->

Setup
=====

* [Download the jar file](https://github.com/wdullaer/SwipeActionAdapter/releases) and add it to your project

If you would like to get the most recent code in a jar, clone the project and run ```./gradlew jar``` from the root folder. This will build a swipeactionadapter.jar in ```library/build/libs/```.

You may also add the library as an Android Library to your project. All the library files live in ```library```.

Creating your listview with swipe actions
-----------------------------------------

If you'd rather just start with a working example, clone the project and take a look.

For a basic implementation, you'll need to

1. Wrap the adapter of your ListView with a SwipeActionAdapter
2. Create a background layout for each swipe direction you wish to act upon.
3. Implement the SwipeActionAdapter

### Wrap the adapter of you ListView

The majority of this libraries functionality is provided through an adapter which wraps around the content ```Adapter``` of your ```ListView```. You will need to set the SwipeActionAdapter to your ListView and because we need to set some properties of the ListView, you will also need to give a reference of the ```ListView``` to the ```SwipeActionAdapter```. This is typically done in your ```Activity```'s or ```Fragments``` onCreate/onActivityCreated callbacks.

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create an Adapter for your content
    String[] content = new String[20];
    for (int i=0;i<20;i++) content[i] = "Row "+(i+1);
    ArrayAdapter<String> stringAdapter = new ArrayAdapter<String>(
            this,
            R.layout.row_bg,
            R.id.text,
            new ArrayList<String>(Arrays.asList(content))
    );
    
    // Wrap your content in a SwipeActionAdapter
    mAdapter = new SwipeActionAdapter(stringAdapter);
    
    // Pass a reference of your ListView to the SwipeActionAdapter
    mAdapter.setListView(getListView());
    
    // Set the SwipeActionAdapter as the Adapter for your ListView
    setListAdapter(mAdapter);
}
```

### Create a  background layout for each swipe direction

I'm just supplying an empty layout with a background for each direction.
You should have a layout for at least ```SwipeDirections.DIRECTION_NORMAL_LEFT``` and ```SwipeDirections.DIRECTION_NORMAL_RIGHT```. The other directions are optional.
It is important that the background layouts have the same height as the normal row layout.
The ```onCreate``` callback from the previous section now becomes:

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create an Adapter for your content
    String[] content = new String[20];
    for (int i=0;i<20;i++) content[i] = "Row "+(i+1);
    ArrayAdapter<String> stringAdapter = new ArrayAdapter<String>(
            this,
            R.layout.row_bg,
            R.id.text,
            new ArrayList<String>(Arrays.asList(content))
    );
    
    // Wrap your content in a SwipeActionAdapter
    mAdapter = new SwipeActionAdapter(stringAdapter);
    
    // Pass a reference of your ListView to the SwipeActionAdapter
    mAdapter.setListView(getListView());

    // Set the SwipeActionAdapter as the Adapter for your ListView
    setListAdapter(mAdapter);
    
    // Set backgrounds for the swipe directions
    mAdapter.addBackground(SwipeDirections.DIRECTION_FAR_LEFT,R.layout.row_bg_left_far)
            .addBackground(SwipeDirections.DIRECTION_NORMAL_LEFT,R.layout.row_bg_left)
            .addBackground(SwipeDirections.DIRECTION_FAR_RIGHT,R.layout.row_bg_right_far)
            .addBackground(SwipeDirections.DIRECTION_NORMAL_RIGHT,R.layout.row_bg_right);
}
```

Layout code

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical" android:layout_width="match_parent"
    android:layout_height="?android:listPreferredItemHeight"
    android:background="@android:color/holo_blue_bright">

</LinearLayout>
```

### Implement the SwipeActionListener

The final thing to do is listen to swipe gestures. This is done by implementing the ```SwipeActionListener```. This interface has two methods:
* ```boolean hasActions(int position)```: return true if you want this item to be swipeable
* ```boolean onSwipe(int position, int direction)```: triggered when an item is swiped. Return true if you want the item to be dismissed, return false if it should stay visible. This method runs on the interface thread so perform any heavy logic in an ASyncThread
You should pass a reference of your ```SwipeActionListener``` to the ```SwipeActionAdapter```

Here's the final implementation of our example's ```onCreate``` method:
```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Create an Adapter for your content
    String[] content = new String[20];
    for (int i=0;i<20;i++) content[i] = "Row "+(i+1);
    ArrayAdapter<String> stringAdapter = new ArrayAdapter<String>(
            this,
            R.layout.row_bg,
            R.id.text,
            new ArrayList<String>(Arrays.asList(content))
    );
    
    // Wrap your content in a SwipeActionAdapter
    mAdapter = new SwipeActionAdapter(stringAdapter);
    
    // Pass a reference of your ListView to the SwipeActionAdapter
    mAdapter.setListView(getListView());

    // Set the SwipeActionAdapter as the Adapter for your ListView
    setListAdapter(mAdapter);
    
    // Set backgrounds for the swipe directions
    mAdapter.addBackground(SwipeDirections.DIRECTION_FAR_LEFT,R.layout.row_bg_left_far)
            .addBackground(SwipeDirections.DIRECTION_NORMAL_LEFT,R.layout.row_bg_left)
            .addBackground(SwipeDirections.DIRECTION_FAR_RIGHT,R.layout.row_bg_right_far)
            .addBackground(SwipeDirections.DIRECTION_NORMAL_RIGHT,R.layout.row_bg_right);
    
    // Listen to swipes
    mAdapter.setSwipeActionListener(new SwipeActionListener(){
        @Override
        public boolean hasActions(int position){
            // All items can be swiped
            return true;
        }

        @Override
        public boolean onSwipe(int position, int direction){
            String dir = "";
            boolean output = false;
            switch(direction) {
                case SwipeDirections.DIRECTION_FAR_LEFT:
                    dir = "Far left";
                    break;
                case SwipeDirections.DIRECTION_NORMAL_LEFT:
                    dir = "Left";
                    output = true;
                    break;
                case SwipeDirections.DIRECTION_FAR_RIGHT:
                    dir = "Far right";
                    break;
                case SwipeDirections.DIRECTION_NORMAL_RIGHT:
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Test Dialog").setMessage("You swiped right").create().show();
                    dir = "Right";
                    break;
            }
            Toast.makeText(
                    this,
                    dir + " swipe Action triggered on "+mAdapter.getItem(position),
                    Toast.LENGTH_SHORT
            ).show();
            mAdapter.notifyDataSetChanged();

            return output;
        }    
    });
}
```


Additional Options
==================

### setFadeOut(boolean fadeOut)
Setting this to true will cause the ListView item to slowly fade out as it is being swiped

### setFixedBackground(boolean setFixedBackground)
Setting this to true will make the backgrounds static behind the ListView item instead of sliding in from the side.


License
=======

    Copyright (c) 2014 Wouter Dullaert

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
