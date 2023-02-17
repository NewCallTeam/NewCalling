package com.cmcc.newcalllib.manage.bussiness.interact

import com.cmcc.newcalllib.bridge.CallBackFunction

/**
 * @author jihongfei
 * @createTime 2022/3/24 14:47
 */
interface JsCaller {
    /**
     * call js function if needed
     */
    fun callJsFunction(jsFuncName: String, data: String, cb: CallBackFunction?)
}