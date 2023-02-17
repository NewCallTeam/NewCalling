package com.cmcc.newcalllib.manage.entity.handler.resp

/**
 * For getInfo
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
data class RespInfo(
        val appetag: String? = null,
        val localnumber: String? = null,// to del?
        val remotenumber: String? = null,
        val callstatus: Int? = null,
        val appid: String? = null,
        val ismo: Boolean? = null,
        val host: String? = null,
        val screenWidth: Int? = null,
        val screenHeight: Int? = null,
        val webViewWidth: Int? = null,
        val webViewHeight: Int? = null,
        val webViewLifeCycleState: Int? = null
)
