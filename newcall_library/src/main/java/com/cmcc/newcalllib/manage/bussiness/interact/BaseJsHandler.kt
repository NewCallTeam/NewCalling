package com.cmcc.newcalllib.manage.bussiness.interact

import com.cmcc.newcalllib.bridge.CallBackFunction
import com.cmcc.newcalllib.manage.entity.handler.resp.ResponseData
import com.cmcc.newcalllib.manage.event.EventRender
import com.cmcc.newcalllib.manage.event.EventAppNotify
import com.cmcc.newcalllib.manage.entity.event.RequestWebViewChangeData
import com.cmcc.newcalllib.tool.JsonUtil
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.constant.ErrorCode
import com.cmcc.newcalllib.tool.constant.EventType
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil
import com.google.gson.JsonParseException

/**
 * @author jihongfei
 * @createTime 2022/4/15 14:08
 */
abstract class BaseJsHandler : JsHandler {

    private lateinit var mJsCommunicator: JsCommunicator

    fun init(jsCommunicator: JsCommunicator) {
        mJsCommunicator = jsCommunicator
    }

    protected fun getJsCommunicator() : JsCommunicator {
        return mJsCommunicator
    }

    protected fun sendRenderEvent(path: String, query: String = "") {
        val eventRender = EventRender(path, query)
        mJsCommunicator.handler.sendMessageType(EventType.RENDER, eventRender)
    }

    protected fun sendRenderEvent(url: String) {
        val eventRender = EventRender(url)
        mJsCommunicator.handler.sendMessageType(EventType.RENDER, eventRender)
    }

    protected fun sendExceptionEvent(err: ErrorCode) {
        mJsCommunicator.handler.sendErrorCode(err)
    }

    protected fun sendRequestSizeEvent(width: String?, height: String?, visibility: Int?) {
        val eventAppNotify = EventAppNotify(EventAppNotify.ET_REQUEST_WEB_VIEW_CHANGE,
            RequestWebViewChangeData(width?.toInt(), height?.toInt(), visibility))
        mJsCommunicator.handler.sendMessageType(EventType.APP_NOTIFY, eventAppNotify)
    }

    protected fun invokeCallback(cbFromJs: CallBackFunction, responseData: ResponseData<*>) {
        val toJson = JsonUtil.toJson(responseData)
        LogUtil.d("invokeCallback, resp json: $toJson")
        ThreadPoolUtil.runOnUiThread {
            cbFromJs.onCallBack(toJson)
        }
    }

    protected fun invokeCallback(cbFromJs: CallBackFunction, err: ErrorCode) {
        LogUtil.e("invokeCallback with err:${err.reason()}")
        ThreadPoolUtil.runOnUiThread {
            invokeCallback(
                cbFromJs, ResponseData<Any>(
                    result = 0,
                    message = err.reason()
                )
            )
        }
    }

    /**
     * parse and get entity
     */
    protected fun <T> parseRequest(dataFromJs: String, classOfT: Class<T>): T? {
        var requestData: T? = null
        try {
            LogUtil.v("parseRequest, dataFromJs: $dataFromJs")
            requestData = JsonUtil.getDefaultGson()
                .fromJson(dataFromJs, classOfT)
        } catch (e: JsonParseException) {
            LogUtil.e("parseRequest, exception:${ErrorCode.JSON_PARSE_WRONG.reason()}", e)
            return null
        }
        LogUtil.v("parseRequest, parsed:$requestData")
        return requestData
    }

    protected fun checkDcConnection(label: String): Boolean {
        return mJsCommunicator.networkAdapter.isDataChannelAvailable(label)
    }


    protected fun getCurrentAppId(): String? {
        val miniAppManager = getJsCommunicator().miniAppManager
        val topMiniApp = miniAppManager.getTopMiniApp()
        return topMiniApp?.appId
    }
}