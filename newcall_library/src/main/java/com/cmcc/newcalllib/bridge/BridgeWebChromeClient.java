/*
 * Copyright (c) 2023 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cmcc.newcalllib.bridge;

import android.net.Uri;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.cmcc.newcalllib.adapter.ntv.NtvAbilityHandler;
import com.cmcc.newcalllib.tool.LogUtil;

public class BridgeWebChromeClient extends WebChromeClient {

    private static final String TAG = "BridgeWebChromeClient";
    private NtvAbilityHandler mNtvAbilityHandler;

    public void setNtvAbilityHandler(NtvAbilityHandler ntvAbilityHandler) {
        mNtvAbilityHandler = ntvAbilityHandler;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        return mNtvAbilityHandler.onOpenFileChooser(filePathCallback, fileChooserParams);
    }
}