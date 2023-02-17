package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For startBrowser/startApp, getApplist/getApplication
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
class ReqWithUrl(
        val url: String,
        theToken: String
) : BaseReqBootstrap(theToken) {
        override fun toString(): String {
                return "ReqWithUrl(url='$url')"
        }
}
