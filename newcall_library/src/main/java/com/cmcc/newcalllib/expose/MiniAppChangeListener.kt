package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/3/23 15:32
 */
interface MiniAppChangeListener {
    /**
     * @param width required width of current webView. Null if no need to change.
     * @param height required height of current webView. Null if no need to change.
     */
    fun requestWebViewSizeChange(width: Int?, height: Int?)

    /**
     * @param visibility required visibility state of webView.
     * Visibility define:
     *  0: visible, 4: invisible, 8: gone, 16: removal
     */
    fun requestWebViewVisibilityChange(visibility: Int)

    /**
     * @param horizontalPos webView position in horizontal. 0:center 1:alignStart 2:alignEnd
     * @param verticalPos webView position in vertical. 0:center 1:alignTop 2:alignBottom
     */
    fun requestWebViewPositionChange(horizontalPos: Int?, verticalPos: Int?)

//    /**
//     * @param prevAppId appId of the mini-app before switching
//     * @param currAppId appId of the mini-app after switching
//     */
//    fun onMiniAppSwitch(prevAppId: String, currAppId: String)
}