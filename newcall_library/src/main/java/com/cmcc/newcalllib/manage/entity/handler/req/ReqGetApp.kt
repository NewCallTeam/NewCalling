package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For getApplication
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
class ReqGetApp(
        val url: String,
        val appId: String,
        val eTag: String,
        theToken: String
) : BaseReqBootstrap(theToken) {
        override fun toString(): String {
                return "ReqGetApp(url='$url', appId='$appId', eTag='$eTag')"
        }
}
