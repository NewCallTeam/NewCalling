package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For getTransferFileHistory
 */
data class ReqGetTransferFileRecords(
    val order: Int = 0,//0: asc by time, 1: desc by time
    val limit: Int? = Int.MAX_VALUE,
    val page: Int?,
    val pageCnt: Int?
)
