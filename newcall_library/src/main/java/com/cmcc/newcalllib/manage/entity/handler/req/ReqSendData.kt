package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For sendData
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
data class ReqSendData(
    val dclabel: String,
    val data: String,
    val ifneedresponse: String?,
    val report: String?,
)
