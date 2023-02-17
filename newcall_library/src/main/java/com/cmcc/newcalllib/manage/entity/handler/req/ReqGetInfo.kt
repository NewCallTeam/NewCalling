package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For getInfo
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
data class ReqGetInfo(
        val type: Int,//0: mini-app info, 1: call info, 2: terminal config
) {
        companion object {
                const val TYPE_GET_MINI_APP_INFO = 0
                const val TYPE_GET_CALL_INFO = 1
                const val TYPE_GET_TERMINAL_CONFIG_INFO = 2
        }
}
