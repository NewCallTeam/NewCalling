package com.cmcc.newcalllib.manage.entity.handler.resp

import com.cmcc.newcalllib.adapter.network.NetworkAdapter

/**
 * Response Data for common use
 * @author jihongfei
 * @createTime 2022/3/23 17:54
 */
data class ResponseData<T>(
    /**
     * 1: success, 0: failure
     */
    val result: Int = 1,
    /**
     * 200: success
     */
    val statuscode: Int = NetworkAdapter.STATUS_CODE_OK,
    /**
     * response message
     */
    val message: String = "",
    /**
     * response data
     */
    val data: T? = null
) {
}
