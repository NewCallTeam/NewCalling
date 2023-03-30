package com.cmcc.newcalllib.manage.bussiness.interact

import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.network.Origin
import com.cmcc.newcalllib.bridge.CallBackFunction
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.event.NotifyAppMessage
import com.cmcc.newcalllib.manage.entity.handler.req.*
import com.cmcc.newcalllib.manage.entity.handler.resp.ResponseData
import com.cmcc.newcalllib.manage.event.EventAppNotify
import com.cmcc.newcalllib.manage.support.storage.db.MiniApp
import com.cmcc.newcalllib.tool.FileUtil
import com.cmcc.newcalllib.tool.JsonUtil
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.TokenUtil
import com.cmcc.newcalllib.tool.constant.Constants
import com.cmcc.newcalllib.tool.constant.ErrorCode
import com.cmcc.newcalllib.tool.constant.EventType
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * JS handler for bootstrap mini app
 * @author jihongfei
 * @createTime 2022/3/24 15:15
 */
class BootstrapJsHandler : BaseJsHandler() {
    companion object {
        const val START_MNA = "startMiniApp"
        const val NOTIFY_APP = "notifyApp"
        const val GET_APP_LIST = "getAppList"
        const val GET_APPLICATION = "getApplication"
    }

    private val mRegFuncNames = mutableListOf(
        START_MNA,
        NOTIFY_APP,
        GET_APP_LIST,
        GET_APPLICATION
    )

    override fun getJsHandlerType(): Int {
        return JsCommunicator.JS_HANDLER_BOOTSTRAP
    }

    override fun getRegisteredFunctionName(): MutableList<String> {
        return mRegFuncNames
    }

    override fun handleJsRequest(
        regFuncName: String?,
        dataFromJs: String,
        cbFromJs: CallBackFunction
    ): Boolean {
        if (!mRegFuncNames.contains(regFuncName)) {
            LogUtil.d("BootstrapJsHandler ignore func:$regFuncName")
            return false
        }
        var handled = true
        val miniAppManager = getJsCommunicator().miniAppManager
        val topMiniApp = miniAppManager.getTopMiniApp()
        if (topMiniApp == null) {
            invokeCallback(cbFromJs, ErrorCode.MINI_APP_STACK_WRONG)
            return handled
        }
        val currAppId = topMiniApp.appId
        when (regFuncName) {
            START_MNA -> {
                var r: ReqStartMiniApp? = null
                r = parseRequest(dataFromJs, ReqStartMiniApp::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (!verifyToken(r, currAppId)) {
                    invokeCallback(cbFromJs, ErrorCode.BS_TOKEN_INVALID)
                } else {
                    val appId = r.appId
                    if (appId.isEmpty()) {
                        LogUtil.e("appId is empty")
                        invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                    } else {
                        // query local db
                        miniAppManager.findMiniApp(appId, object : Callback<Results<MiniApp>> {
                            override fun onResult(t: Results<MiniApp>) {
                                if (t.value == null || t.value.path.isEmpty()
                                    || !(FileUtil.exists(t.value.path, false)
                                    && FileUtil.checkExtension(t.value.path, "html"))) {
                                    invokeCallback(
                                        cbFromJs,
                                        ResponseData<Any>(0, message = "start app fail")
                                    )
                                } else {
                                    sendRenderEvent(path = t.value.path, query = "origin=${t.value.origin}")
                                    invokeCallback(cbFromJs, ResponseData<Any>(1))
                                }
                            }
                        })
                    }
                }
            }
            NOTIFY_APP -> {
                var r: ReqNotifyApp? = null
                r = parseRequest(dataFromJs, ReqNotifyApp::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (!verifyToken(r, currAppId)) {
                    invokeCallback(cbFromJs, ErrorCode.BS_TOKEN_INVALID)
                } else {
                    val msg = NotifyAppMessage(r.type, r.data)
                    getJsCommunicator().handler.sendMessageType(
                        EventType.APP_NOTIFY,
                        EventAppNotify(EventAppNotify.ET_NOTIFY_APP_BY_JS, msg)
                    )
                    invokeCallback(cbFromJs, ResponseData(result = 1, data = ""))
                }
            }
            GET_APP_LIST -> {
                var r: ReqWithUrl? = null
                r = parseRequest(dataFromJs, ReqWithUrl::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (!verifyToken(r, currAppId)) {
                    invokeCallback(cbFromJs, ErrorCode.BS_TOKEN_INVALID)
                } else {
                    if (!checkDcConnection(Constants.BOOTSTRAP_DATA_CHANNEL_LABEL_LOCAL)) {
                        invokeCallback(cbFromJs, ResponseData<Any>(result = 0, message = ErrorCode.DATA_CHANNEL_NOT_AVAILABLE.reason()))
                    } else {
                        // get application list through bootstrap DC
                        miniAppManager.getApplicationList(r.url, object :
                            Callback<Results<String>> {
                            override fun onResult(t: Results<String>) {
                                if (t.isSuccess() && t.getOrNull() != null) {
                                    val json = t.getOrNull()!!
                                    invokeCallback(
                                        cbFromJs, ResponseData(
                                            result = 1,
                                            statuscode = NetworkAdapter.STATUS_CODE_OK,
                                            data = JsonUtil.strToJsonObject(json) ?: JsonUtil.strToJsonArray(json)
                                        )
                                    )
                                } else {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 0, message = "get app list fail"))
                                }
                            }
                        })
                    }
                }
            }
            GET_APPLICATION -> {
                var r: ReqGetApp? = null
                r = parseRequest(dataFromJs, ReqGetApp::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (!verifyToken(r, currAppId)) {
                    invokeCallback(cbFromJs, ErrorCode.BS_TOKEN_INVALID)
                } else {
                    if (!checkDcConnection(Constants.BOOTSTRAP_DATA_CHANNEL_LABEL_LOCAL)) {
                        invokeCallback(cbFromJs, ResponseData<Any>(result = 0, message = ErrorCode.DATA_CHANNEL_NOT_AVAILABLE.reason()))
                    } else {
                        miniAppManager.prepareMiniApp(Origin.LOCAL, r.url, r.appId, r.eTag, object :
                            Callback<Results<String>> {
                            override fun onResult(t: Results<String>) {
                                if (t.isSuccess() &&(t.getOrNull() != null)) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 1, statuscode = NetworkAdapter.STATUS_CODE_OK))
                                } else {
                                    LogUtil.e("Prepare mini-app fail with: $r")
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 0, message = "get app fail"))
                                }
                            }
                        })
                    }
                }
            }
            else -> {
                handled = false
            }
        }
        LogUtil.i("BootstrapJsHandler handle func=$regFuncName with result=$handled")
        return handled
    }

    /**
     * verify token
     * @return token validity
     */
    private fun verifyToken(requestData: Any, appId: String): Boolean {
        // decode and check token first
        var reqBootstrap: BaseReqBootstrap? = null
        try {
            reqBootstrap = requestData as BaseReqBootstrap
        } catch (e: Exception) {
            LogUtil.e("BootstrapJsHandler meet cast exception")
            return false
        }
        if (reqBootstrap.token == null) {
            LogUtil.e("BootstrapJsHandler meet token null")
            return false
        }
        val token = URLDecoder.decode(reqBootstrap.token, StandardCharsets.UTF_8.name())
        if (!TokenUtil.verify(token, appId).isSuccess) {
            LogUtil.e("BootstrapJsHandler meet token invalid")
            return false
        }
        return true
    }
}