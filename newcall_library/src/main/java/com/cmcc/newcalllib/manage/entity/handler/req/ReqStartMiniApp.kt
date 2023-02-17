package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For startMiniApp
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
class ReqStartMiniApp(
        theToken: String,
        val appId: String,
        val route: String,
        val description: String
): BaseReqBootstrap(theToken)