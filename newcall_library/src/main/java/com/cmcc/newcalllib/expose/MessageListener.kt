package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/8/17 14:35
 */
interface MessageListener {
    /**
     * @param type msg type. 1: report parsed business type and status.
     * @param data message data, detail format depends on type.
     */
    fun onMessage(type: Int, data: String)
}