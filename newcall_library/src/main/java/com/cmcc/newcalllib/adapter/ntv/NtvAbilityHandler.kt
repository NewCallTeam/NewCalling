package com.cmcc.newcalllib.adapter.ntv

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

/**
 * @author jihongfei
 * @createTime 2022/4/26 10:43
 */
interface NtvAbilityHandler {
    fun onOpenFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean
}