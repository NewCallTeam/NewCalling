/*
 * Copyright (c) 2022 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the XXXX License, Version X.X (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://xxxxxxx/licenses/LICENSE-X.X
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib.adapter.screenshare.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

public class SketchCtrlLayout extends LinearLayout {


    private OnScreenChangedListener mScreenChangedListener;
    private OnSketchCtrlKeyListener mCtrlLayoutKeyListener;

    public SketchCtrlLayout(Context context) {
        super(context);
    }

    public SketchCtrlLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SketchCtrlLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("xiaxl: ", "---onConfigurationChanged---");
        Log.d("xiaxl: ", "newConfig: " + newConfig);
        if (mScreenChangedListener != null) {
            mScreenChangedListener.onScreenChanged(newConfig);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mCtrlLayoutKeyListener != null) {
            mCtrlLayoutKeyListener.onKeyEvent(this, event.getKeyCode(), event);
            return false;
        }
        return super.dispatchKeyEvent(event);
    }


    public void setOnScreenChangedListener(OnScreenChangedListener listener) {
        this.mScreenChangedListener = listener;
    }

    public void setOnSketchCtrlKeyListener(OnSketchCtrlKeyListener listener) {
        this.mCtrlLayoutKeyListener = listener;
    }

    public interface OnScreenChangedListener {
        void onScreenChanged(Configuration newConfig);
    }

    public interface OnSketchCtrlKeyListener {
        void onKeyEvent(View v, int keyCode, KeyEvent event);
    }

}
