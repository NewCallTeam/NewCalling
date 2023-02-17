package com.cmcc.newcalllib.manage.bussiness.interact

import com.cmcc.newcalllib.bridge.CallBackFunction

/**
 * @author jihongfei
 * @createTime 2022/3/24 14:47
 */
interface JsHandler {
    /**
     * provide type of handler
     */
    fun getJsHandlerType(): Int

    /**
     * provide method names which want to be called in JavaScript
     */
    fun getRegisteredFunctionName(): MutableList<String>

    /**
     * handle function called in JavaScript
     */
    fun handleJsRequest(regFuncName: String?, dataFromJs: String, cbFromJs: CallBackFunction): Boolean
}