package com.cmcc.newcalllib.manage.support

import com.cmcc.newcalllib.BuildConfig
import com.cmcc.newcalllib.manage.entity.WebViewSize

/**
 * Configuration store and management
 */
object ConfigManager {
    var alwaysDownloadMiniApp = true
    var webViewSize = WebViewSize(0, 0)

    var dcServiceAction = BuildConfig.ACTION_NAME;
    var dcServicePackage = BuildConfig.PACKAGE_NAME;
    var dcBufferAmountSize: Long = BuildConfig.CHUNK_SIZE
}