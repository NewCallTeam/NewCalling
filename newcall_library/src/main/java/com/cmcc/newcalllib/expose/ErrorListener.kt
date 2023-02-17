package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/2/22 11:45
 */
interface ErrorListener {
    /**
     * @param errCode int error code
     * @param msg error message, can be empty
     */
    fun onFail(errCode: Int, msg: String = "")
}