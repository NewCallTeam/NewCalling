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