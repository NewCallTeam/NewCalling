package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For requestWebViewChange
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
data class ReqRequestWebViewChange(
        val appId: String,
        val w: Int?,
        val h: Int?,
        val visibility: Int?,
        val horizontalPos: Int?,
        val verticalPos: Int?,
)
