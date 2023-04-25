package com.cmcc.newcalllib.manage.bussiness.interact

import com.cmcc.newcalllib.manage.ext.ExtensionManager
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.network.Origin
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProvider
import com.cmcc.newcalllib.bridge.CallBackFunction
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.bussiness.NewCallManager.MainThreadEventHandler
import com.cmcc.newcalllib.manage.mna.MiniAppManager
import com.cmcc.newcalllib.manage.entity.CallInfo
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.caller.req.*
import com.cmcc.newcalllib.manage.entity.handler.resp.RespNotifyDataChannel
import com.cmcc.newcalllib.manage.entity.handler.resp.ResponseData
import com.cmcc.newcalllib.manage.event.EventInternal
import com.cmcc.newcalllib.manage.event.EventRender
import com.cmcc.newcalllib.manage.support.storage.StorageManager
import com.cmcc.newcalllib.tool.*
import com.cmcc.newcalllib.tool.constant.EventType
import com.google.gson.JsonParseException
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * @author jihongfei
 * @createTime 2022/3/16 17:10
 */
class JsCommunicator(
    override val handler: MainThreadEventHandler,
    override val networkAdapter: NetworkAdapter,
    override val abilityProvider: NativeAbilityProvider,
    override val extensionManager: ExtensionManager,
    override val storageManager: StorageManager,
    val miniAppManager: MiniAppManager,
    val callInfo: CallInfo,
) : BaseJsCommunicator(handler, networkAdapter, abilityProvider, extensionManager, storageManager) {

    companion object {
        const val JS_HANDLER_DEFAULT = 0
        const val JS_HANDLER_BOOTSTRAP = 1
        const val JS_HANDLER_COMMON_MNA = 2

        const val JS_FUNC_NAME_DC_NOTIFY = "dataChannelNotify"
        const val JS_FUNC_NAME_MSG_NOTIFY = "messageNotify"
        const val JS_FUNC_NAME_CALLSTATE_NOTIFY = "callStateNotify"
        const val JS_FUNC_NAME_VISIBILITY_NOTIFY = "visibilityNotify"
        const val JS_FUNC_NAME_CALL_TYPE_NOTIFY = "callTypeNotify"
    }
    private var mBootstrapJsHandler: BootstrapJsHandler? = null
    private var mCommonJsHandler: CommonMiniAppJsHandler? = null
    var webViewVisibility: Int? = null
    var theCallType: Int? = null
//    var webViewWidth: Int? = null
//    var webViewHeight: Int? = null

    init {
        setupDataObserver()
        setupDataChannelCallback()
    }

    private fun setupDataObserver() {
        LogUtil.d("setupDataObserver")
        // set network data observer
        networkAdapter.setDataObserver(object : NetworkAdapter.DataObserver {
//            override fun onDataArrive(data: ByteBuffer, protocol: String) {
            override fun onDataArrive(label: String, data: ByteBuffer) {
                val data = data.toStr()
//                val cleanLabel = networkAdapter.getLabelDecorator().removeOrigin(label)
                LogUtil.v("onDataArrive, label:$label, data: $data")
                messageNotify(label, data, null)
            }
        })
    }

    private fun setupDataChannelCallback() {
        networkAdapter.setDataChannelCallback(object : NetworkAdapter.DataChannelCallback {
            override fun onBootstrapDataChannelCreated(success: Boolean) {
                LogUtil.d("onBootstrapDataChannelCreated, success=$success")
                handler.sendMessageType(
                    EventType.INTERNAL,
                    EventInternal(EventInternal.ET_BOOTSTRAP_CREATED)
                )
            }

            override fun onImsDataChannelSetupRequest(dcLabels: Array<String>, slotId: Int, callId: String) {
                LogUtil.d("onImsDataChannelSetupRequest, dcLabels=${dcLabels.joinToString()} slotId=$slotId")
//                val cleanDcLabels =
//                    dcLabels.map { networkAdapter.getLabelDecorator().removeOrigin(it) }
//                        .toTypedArray()
                // call dataChannelNotify to JS
                dataChannelNotify(dcLabels, object : CallBackFunction {
                    override fun onCallBack(data: String?) {
                        // check arguments
                        if (data.isNullOrEmpty()) {
                            LogUtil.e("dataChannelNotify callback with empty")
                            if (slotId >= 0) {
                                networkAdapter.respondDataChannelSetupRequest(
                                    dcLabels,
                                    emptyArray(),
                                    slotId,
                                    callId
                                )
                            }
                            return
                        }
                        var r: RespNotifyDataChannel? = null
                        try {
                            r = JsonUtil.fromJson(data, RespNotifyDataChannel::class.java)
                        } catch (e: JsonParseException) {
                            LogUtil.e("dataChannelNotify callback with bad argument, data=$data", e)
                            if (slotId >= 0) {
                                networkAdapter.respondDataChannelSetupRequest(
                                    dcLabels,
                                    emptyArray(),
                                    slotId,
                                    callId
                                )
                            }
                            return
                        }

                        // notify dc stack of chip
                        val acceptArr = dcLabels.copyOf().map { it in r.accepted }.toTypedArray()
                        // consider dcLabels belongs to one MiniApp.
                        // which means DC here is all from Local or Remote
                        val origin = if (dcLabels.any() { it.startsWith(Origin.LOCAL.getName()) }) Origin.LOCAL else Origin.REMOTE
                        LogUtil.d("dataChannelNotify callback, r=$r, dcLabels=${dcLabels.joinToString()}, " +
                                "acceptArr=$acceptArr, origin=${origin.getName()}")
                        if (slotId >= 0) {
                            networkAdapter.respondDataChannelSetupRequest(
                                dcLabels,
                                acceptArr,
                                slotId,
                                callId
                            )
                        }
                        // prepare app then auto launch
                        if (!r.url.isNullOrEmpty() && !r.appid.isNullOrEmpty() && !r.etag.isNullOrEmpty()) {
                            miniAppManager.prepareMiniApp(origin, r.url!!, r.appid!!, r.etag!!, object :
                                Callback<Results<String>> {
                                override fun onResult(t: Results<String>) {
                                    if (t.isSuccess() &&(t.getOrNull() != null)) {
                                        val path = t.value()
                                        if (FileUtil.exists(path, false)) {
                                            // pass origin by query
                                            handler.sendMessageType(EventType.RENDER, EventRender(path, query = "origin=${origin.getName()}"))
                                        }
                                    } else {
                                        LogUtil.e("Prepare mini-app on dataChannelNotify fail with: $r")
                                    }
                                }
                            })
                        } else {
                            LogUtil.w("dataChannelNotify callback, do not start mini-app")
                        }
                    }
                })
            }
        })
    }

    fun setBootstrapJsHandler(jsHandler: BootstrapJsHandler): Boolean {
        if (jsHandler.getJsHandlerType() != JS_HANDLER_BOOTSTRAP) {
            return false
        }
        jsHandler.init(this)
        mBootstrapJsHandler = jsHandler
        return true
    }

    fun setCommonJsHandler(jsHandler: CommonMiniAppJsHandler): Boolean {
        if (jsHandler.getJsHandlerType() != JS_HANDLER_COMMON_MNA) {
            return false
        }
        jsHandler.init(this)
        mCommonJsHandler = jsHandler
        return true
    }

    override fun getJsHandlerType(): Int {
        return JS_HANDLER_DEFAULT
    }

    override fun getRegisteredFunctionName(): MutableList<String> {
        // collect all function names
        val ret = mutableListOf<String>()
        mBootstrapJsHandler?.getRegisteredFunctionName()?.let { ret.addAll(it) }
        mCommonJsHandler?.getRegisteredFunctionName()?.let { ret.addAll(it) }
        return ret
    }

    override fun handleJsRequest(regFuncName: String?, dataFromJs: String, cbFromJs: CallBackFunction): Boolean {
        LogUtil.d("JS call native method:$regFuncName with param:$dataFromJs")
        if (regFuncName == null) {
            // no register handler, handle here
            LogUtil.d("JS call handle by default handler")
        } else {
            // register handler
            try {
                if (mBootstrapJsHandler?.handleJsRequest(regFuncName, dataFromJs, cbFromJs) != true
                    && mCommonJsHandler?.handleJsRequest(regFuncName, dataFromJs, cbFromJs) != true
                ) {
                    // no handle
                    LogUtil.w("JS call not handled")
                }
            } catch (e: Exception) {
                cbFromJs.onCallBack(JsonUtil.toJson(ResponseData<Any>(result = 0, message = e.message ?: "Unknown exception in JsHandler")))
            }
        }
        return true
    }

    /**
     * NOTIFY method below
     */

    fun dataChannelNotify(labels: Array<String>, cb: CallBackFunction?) {
        val dcNotify = DataChannelNotify(labels = labels.asList())
        callJsFunction(JS_FUNC_NAME_DC_NOTIFY, JsonUtil.toJson(dcNotify), cb)
    }

    fun messageNotify(label: String, msg: String, cb: CallBackFunction?) {
        val msgNotify = MsgNotify(label, msg)
        callJsFunction(JS_FUNC_NAME_MSG_NOTIFY, JsonUtil.toJson(msgNotify), cb)
    }

    fun callStateNotify(callState: Int, cb: CallBackFunction?) {
        val csNotify = CallStateNotify(callState)
        callJsFunction(JS_FUNC_NAME_CALLSTATE_NOTIFY, JsonUtil.toJson(csNotify), cb)
    }

    fun visibilityNotify(state: Int, cb: CallBackFunction?) {
        webViewVisibility = state
        val vNotify = VisibilityNotify(state)
        callJsFunction(JS_FUNC_NAME_VISIBILITY_NOTIFY, JsonUtil.toJson(vNotify), cb)
    }

    fun callTypeNotify(callType: Int, cb: CallBackFunction?) {
        theCallType = callType
        val callTypeNotify = CallTypeNotify(callType)
        callJsFunction(JS_FUNC_NAME_CALL_TYPE_NOTIFY, JsonUtil.toJson(callTypeNotify), cb)
    }

}