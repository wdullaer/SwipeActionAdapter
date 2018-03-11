SwipeActionAdapter - The Mailbox-like Swipe gesture library
===========================================================

![Maven Central](https://img.shields.io/maven-central/v/com.wdullaer/swipeactionadapter.svg)

SwipeActionAdapter is a library that provides a simple API to create Mailbox-like action when swiping in a ListView.
The idea is to make it simple enough to implement while still providing sufficient options to allow you to
integrate it with the design of your application.

Support for Android 4.0 and up. It might work on lower API versions, but I haven't tested it and I don't intend to
make any effort in that direction.

Feel free to fork or issue pull requests on github. Issues can be reported on the github issue tracker.

![Normal Swipe Gesture](https://raw.github.com/wdullaer/SwipeActionAdapter/gh-pages/images/swipe_1.png)

![Far Swipe Gesture](https://raw.github.com/wdullaer/SwipeActionAdapter/gh-pages/images/swipe_2.png)

Setup
=====
There are multiple options to add SwipeActionAdapter to your project:

* **Grade**  
Add it as a dependency to your `build.gradle`
```java
dependencies {
    compile 'com.wdullaer:swipeactionadapter:2.0.0'
}
```

* **Jar File**  
[Download the jar file](https://github.com/wdullaer/SwipeActionAdapter/releases/latest) and add it to your project

* **Build from source**  
If you would like to get the most recent code in a jar, clone the project and run ```./gradlew jar``` from
the root folder. This will build a `swipeactionadapter.jar` in ```library/build/libs/```.

You may also add the library as an Android Library to your project. All the library files live in ```library```.

 The library also uses some Java 8 features, which Android Studio will need to transpile. This requires the following stanza in your app's `build.gradle`.
 See https://developer.android.com/studio/write/java8-support.html for more information on Java 8 support in Android.
 ```groovy
 android {
   ...
   // Configure only for each module that uses Java 8
   // language features (either in its source code or
   // through dependencies).
   compileOptions {
     sourceCompatibility JavaVersion.VERSION_1_8
     targetCompatibility JavaVersion.VERSION_1_8
   }
 }
 ```

Creating your ListView with swipe actions
-----------------------------------------

If you'd rather just start with a working example, clone the project and take a look.

For a basic implementation, you'll need to

1. Wrap the Adapter of your ListView with a SwipeActionAdapter
2. Create a background layout for each swipe direction you wish to act upon.
3. Implement the SwipeActionAdapter

### Wrap the adapter of your ListView

The majority of this libraries functionality is provided through an adapter which wraps around the content ```Adapter```
of your ```ListView```. You will need to set the SwipeActionAdapter to your ListView and because
we need to set some properties of the ListView, you will also need to give a reference of the ```ListView```
to the ```SwipeActionAdapter```.
This is typically done in your ```Activity```'s or ```Fragments``` onCreate/onActivityCreated callbacks.

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
You should have a layout for at least ```SwipeDirection.DIRECTION_NORMAL_LEFT``` and ```SwipeDirection.DIRECTION_NORMAL_RIGHT```.
The other directions are optional.
It is important that the background layouts scale properly vertically.
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
    mAdapter.addBackground(SwipeDirection.DIRECTION_FAR_LEFT,R.layout.row_bg_left_far)
            .addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT,R.layout.row_bg_left)
            .addBackground(SwipeDirection.DIRECTION_FAR_RIGHT,R.layout.row_bg_right_far)
            .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT,R.layout.row_bg_right);
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

The final thing to do is listen to swipe gestures. This is done by implementing the ```SwipeActionListener```.
This interface has three methods:
* ```boolean hasActions(int position, SwipeDirection direction)```: return true if you want this item to be swipeable in this direction
* ```boolean shouldDismiss(int position, SwipeDirection direction)```: return true if you want the item to be dismissed,
return false if it should stay visible. This method runs on the interface thread so if you want to trigger any
heavy actions here, put them on an ```ASyncThread```
* ```void onSwipe(int[] position, SwipeDirection[] direction)```: triggered when all animations on the swiped items have finished.
You will receive an array of all swiped items, sorted in descending order with their corresponding directions.

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
    mAdapter.addBackground(SwipeDirection.DIRECTION_FAR_LEFT,R.layout.row_bg_left_far)
            .addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT,R.layout.row_bg_left)
            .addBackground(SwipeDirection.DIRECTION_FAR_RIGHT,R.layout.row_bg_right_far)
            .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT,R.layout.row_bg_right);

    // Listen to swipes
    mAdapter.setSwipeActionListener(new SwipeActionListener(){
        @Override
        public boolean hasActions(int position, SwipeDirection direction){
            if(direction.isLeft()) return true; // Change this to false to disable left swipes
            if(direction.isRight()) return true;
            return false;
        }

        @Override
        public boolean shouldDismiss(int position, SwipeDirection direction){
            // Only dismiss an item when swiping normal left
            return direction == SwipeDirection.DIRECTION_NORMAL_LEFT;
        }

        @Override
        public void onSwipe(int[] positionList, SwipeDirection[] directionList){
            for(int i=0;i<positionList.length;i++) {
                int direction = directionList[i];
                int position = positionList[i];
                String dir = "";

                switch (direction) {
                    case SwipeDirection.DIRECTION_FAR_LEFT:
                        dir = "Far left";
                        break;
                    case SwipeDirection.DIRECTION_NORMAL_LEFT:
                        dir = "Left";
                        break;
                    case SwipeDirection.DIRECTION_FAR_RIGHT:
                        dir = "Far right";
                        break;
                    case SwipeDirection.DIRECTION_NORMAL_RIGHT:
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Test Dialog").setMessage("You swiped right").create().show();
                        dir = "Right";
                        break;
                }
                Toast.makeText(
                        this,
                        dir + " swipe Action triggered on " + mAdapter.getItem(position),
                        Toast.LENGTH_SHORT
                ).show();
                mAdapter.notifyDataSetChanged();
            }
        }
        
        @Override
        public void onSwipeStarted(ListView listView, int position, SwipeDirection direction) {
            // User is swiping
        }
        
        @Override
        public void onSwipeEnded(ListView listView, int position, SwipeDirection direction) {
            // User stopped swiping (lifted finger from the screen)
        }
    });
}
```


Additional Options
==================

### setFadeOut(boolean fadeOut)
Setting this to true will cause the ListView item to slowly fade out as it is being swiped.

### setFixedBackgrounds(boolean fixedBackgrounds)
Setting this to true will make the backgrounds static behind the ListView item instead of sliding in from the side.

### setDimBackgrounds(boolean dimBackgrounds)
Setting this to true will make the backgrounds appear dimmed before the normal swipe threshold is reached.

### setNormalSwipeFraction(float normalSwipeFraction)
Allows you to set the fraction of the view width that must be swiped before it is counted as a normal swipe. The float must be between 0 and 1. 0 makes every swipe register, 1 effectively disables swipe.

### setFarSwipeFraction(float farSwipeFraction)
Allows you to set the fraction of the view width that must be swiped before it is counted as a far swipe. The float must be between 0 and 1. 0 makes every swipe a far swipe, 1 effectively disables a far swipe.


### onSwipeStarted and onSwipeEnded
You can use these events to execute code once the user starts or stops swiping in a listItem. This can be used to fix issues 
relating to other libraries hijacking touch events (for example; a SwipeRefreshLayout).
```
@Override
public void onSwipeStarted(ListView listView, int position, SwipeDirection direction) {
}

@Override
public void onSwipeEnded(ListView listView, int position, SwipeDirection direction) {
}
```
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
