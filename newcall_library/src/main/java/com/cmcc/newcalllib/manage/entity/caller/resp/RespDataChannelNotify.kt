package com.cmcc.newcalllib.manage.entity.caller.resp

/**
 * For dataChannelNotify()
 * @author jihongfei
 * @createTime 2022/4/8 10:56
 */
data class RespDataChannelNotify(
    val acceptedApps: List<AcceptedApp>?
) {
    class AcceptedApp {
        var accepted: List<String>? = emptyList()
        var url: String? = ""
        var etag: String? = ""
        var appid: String? = ""
    }
}
