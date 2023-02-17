package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/3/23 15:32
 */
interface MiniAppChangeListener {
    /**
     * @param width required width of current webView
     * @param height required height of current webView
     */
    fun requestWebViewSizeChange(width: Int, height: Int)

    /**
     * @param visibility required visibility state of webView.
     * Visibility define:
     *  0: visible, 4: invisible, 8: gone, 16: removal
     */
    fun requestWebViewVisibilityChange(visibility: Int)

//    /**
//     * @param prevAppId appId of the mini-app before switching
//     * @param currAppId appId of the mini-app after switching
//     */
//    fun onMiniAppSwitch(prevAppId: String, currAppId: String)
}