/**
 * Copyright (c) 2022 China Mobile Communications Group Co.,Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cmcc.newcalllib.tool;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

public class DisplayHelper {
    private static int mDisplayWidth;
    private static int mDisplayHeight;
    private static int mDisplayDpi;

    public static void initDisplaySize(Activity activity) {
        if (activity != null) {
            DisplayMetrics metrics = new DisplayMetrics();
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getMetrics(metrics);
            mDisplayWidth = metrics.widthPixels;
            mDisplayHeight = metrics.heightPixels;
            mDisplayDpi = metrics.densityDpi;
        }
    }

    public static int getWidth() {
        return mDisplayWidth;
    }

    public static int getHeight() {
        return mDisplayHeight;
    }

    public static int getDpi() {
        return mDisplayDpi;
    }


    /**
     * px转换为dp
     *
     * @param px
     * @return
     */
    public static int px2dp(Context context, float px) {
        if (context != null) {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (px / scale + 0.5f);
        }
        return (int) px;
    }

    /**
     * dp值转换为px
     *
     * @param dp
     * @return
     */
    public static int dp2px(Context context, float dp) {
        if (context != null) {
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }
        return (int) dp;
    }
}
