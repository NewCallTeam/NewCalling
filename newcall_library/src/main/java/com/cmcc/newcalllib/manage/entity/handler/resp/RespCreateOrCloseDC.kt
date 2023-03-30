package com.cmcc.newcalllib.manage.entity.handler.resp

/**
 * For createAppDataChannel/closeAppDataChannel
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
data class RespCreateOrCloseDC(val details: List<CreateOrCloseDCDetail>) {
    class CreateOrCloseDCDetail (val label: String, val result: Int)
}
