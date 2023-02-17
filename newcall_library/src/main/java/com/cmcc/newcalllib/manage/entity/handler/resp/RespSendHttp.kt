package com.cmcc.newcalllib.manage.entity.handler.resp

/**
 * For sendHttp
 * @author jihongfei
 * @createTime 2023/01/06 11:24
 */
data class RespSendHttp(
    val status: Int,
    val msg: String,
    val headers: Map<String, String>?,
    val isFile: Boolean,
    val filePath: String?
) {
}
