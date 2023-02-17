package com.cmcc.newcalllib.manage.entity.handler.resp

/**
 * For dataChannelNotify
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
@Suppress("ArrayInDataClass")
data class RespNotifyDataChannel(
        val accepted: Array<String>,
        val url: String?,
        val etag: String?,
        val appid: String?,
)