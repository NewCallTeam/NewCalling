package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For notifyApp
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
class ReqNotifyApp(
        theToken: String,
        val type: Int,
        val data: String,
): BaseReqBootstrap(theToken)
