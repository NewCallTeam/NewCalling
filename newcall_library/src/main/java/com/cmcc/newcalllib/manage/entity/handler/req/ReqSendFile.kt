package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For sendFile
 */
data class ReqSendFile(
    val dclabel: String,
    val name: String,
    val size: String,
    val ifneedresponse: String?,
    val report: String?,
)
