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

import java.util.Arrays;
import java.util.List;

/**
 * Class containing a set of constant directions used throughout the package
 *
 * Created by wdullaer on 02.07.14.
 */
public class SwipeDirections {
    // Constants
    public static final int DIRECTION_NORMAL_LEFT = -1;
    public static final int DIRECTION_FAR_LEFT = -2;
    public static final int DIRECTION_NORMAL_RIGHT = 1;
    public static final int DIRECTION_FAR_RIGHT = 2;
    public static final int DIRECTION_NEUTRAL = 0;

    static List<Integer> getAllDirections(){
        return Arrays.asList(
                DIRECTION_FAR_LEFT,
                DIRECTION_FAR_RIGHT,
                DIRECTION_NEUTRAL,
                DIRECTION_NORMAL_LEFT,
                DIRECTION_NORMAL_RIGHT
        );
    }
}
