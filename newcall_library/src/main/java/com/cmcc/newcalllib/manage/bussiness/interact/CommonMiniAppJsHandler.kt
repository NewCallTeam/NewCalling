package com.cmcc.newcalllib.manage.bussiness.interact

import android.net.Uri
import com.cmcc.newcalllib.BuildConfig
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.bridge.CallBackFunction
import com.cmcc.newcalllib.manage.entity.CallInfo
import com.cmcc.newcalllib.manage.entity.NewCallException
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.handler.req.*
import com.cmcc.newcalllib.manage.entity.handler.resp.*
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.storage.db.MiniApp
import com.cmcc.newcalllib.tool.*
import com.cmcc.newcalllib.tool.constant.ErrorCode
import java.io.File
import java.util.*

/**
 * JS handler for other mini-app use
 * @author jihongfei
 * @createTime 2022/3/24 15:15
 */
@Suppress("CascadeIf")
class CommonMiniAppJsHandler : BaseJsHandler() {
    companion object {
        const val ENABLE_ORIGIN_DECORATE = false

        const val GET_INFO = "getInfo"
        const val SAVE_DATA = "saveData"
        const val GET_DATA = "getData"
        const val START_APP = "startApp"
        const val START_BROWSER = "startBrowser"
        const val REQUEST_WEB_VIEW_CHANGE = "requestWebViewChange"
        const val CHOOSE_IMAGE = "chooseImage"
        const val CHOOSE_FILE = "chooseFile"
        const val CHOOSE_VIDEO = "chooseVideo"
        const val SET_AUDIO_ROUTE = "setAudioRoute"
        const val CREATE_APP_DATA_CHANNEL = "createAppDataChannel"
        const val CLOSE_APP_DATA_CHANNEL = "closeAppDataChannel"
        const val SEND_DATA = "sendData"
        const val SEND_HTTP = "sendHttp"
        const val SAVE_TRANSFER_FILE = "saveTransferFile"
        const val GET_TRANSFER_FILE_RECORDS = "getTransferFileRecords"
        const val SAVE_FILE_TO_LOCAL = "saveFileToLocal"
        const val ON_INIT = "onInit"
        const val ON_EXIT = "onExit"
        const val REQUEST_SCREEN_SHARE = "requestScreenShare"
        const val CONTROL_SCREEN_SHARE = "controlScreenShare"
        const val GET_LOCATION = "getLocation"
        const val REQUEST_AR_CALL = "requestARCall"
        const val CONTROL_AR_CALL = "controlARCall"
        const val CHECK_RESOURCE = "checkResource"
        const val DELETE_RESOURCE = "deleteResource"
        const val LOAD_AR_RESOURCE = "loadARResource"
        const val CONTROL_STT = "controlSTT"
        const val CONTROL_COMPRESS = "controlCompress"
    }

    private val mRegFuncNames = mutableListOf(
        GET_INFO,
        SAVE_DATA,
        GET_DATA,
        START_APP,
        START_BROWSER,
        REQUEST_WEB_VIEW_CHANGE,
        CHOOSE_IMAGE,
        CHOOSE_FILE,
        CHOOSE_VIDEO,
        SET_AUDIO_ROUTE,
        CREATE_APP_DATA_CHANNEL,
        CLOSE_APP_DATA_CHANNEL,
        SEND_DATA,
        SEND_HTTP,
        SAVE_TRANSFER_FILE,
        GET_TRANSFER_FILE_RECORDS,
        SAVE_FILE_TO_LOCAL,
        ON_INIT,
        ON_EXIT,
        REQUEST_SCREEN_SHARE,
        CONTROL_SCREEN_SHARE,
        GET_LOCATION,
        REQUEST_AR_CALL,
        CONTROL_AR_CALL,
        CHECK_RESOURCE,
        DELETE_RESOURCE,
        LOAD_AR_RESOURCE,
        CONTROL_STT,
        CONTROL_COMPRESS
    )

    override fun getJsHandlerType(): Int {
        return JsCommunicator.JS_HANDLER_COMMON_MNA
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
            LogUtil.d("CommonMiniAppJsHandler ignore func:$regFuncName")
            return false
        }
        var handled = true

        when (regFuncName) {
            GET_INFO -> {
                val r = parseRequest(dataFromJs, ReqGetInfo::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    when (r.type) {
                        ReqGetInfo.TYPE_GET_MINI_APP_INFO -> {
                            val appId = getJsCommunicator().miniAppManager.getTopMiniApp()?.appId
                            var appETag: String? = null
                            if (appId != null && appId.isNotEmpty()) {
                                getJsCommunicator().miniAppManager.findMiniAppVer(appId, object :
                                    Callback<Results<String>> {
                                    override fun onResult(t: Results<String>) {
                                        if (t.isSuccess()) {
                                            appETag = t.getOrNull()!!
                                            invokeCallback(
                                                cbFromJs,
                                                ResponseData(
                                                    result = 1,
                                                    data = RespInfo(
                                                        appetag = appETag,
                                                        appid = appId
                                                    )
                                                )
                                            )
                                        } else {
                                            invokeCallback(cbFromJs, ResponseData<Any>(0,
                                                message = "app not found"))
                                        }
                                    }
                                })
                            } else {
                                invokeCallback(cbFromJs, ResponseData<Any>(0,
                                    message = "app not found"))
                            }
                        }
                        ReqGetInfo.TYPE_GET_CALL_INFO -> {
                            val info = getJsCommunicator().callInfo
                            invokeCallback(
                                cbFromJs,
                                ResponseData(
                                    result = 1,
                                    data = RespInfo(
                                        localnumber = info.localNumber,
                                        remotenumber = info.remoteNumber,
                                        callstatus = info.callStatus,
                                        ismo = info.direction == CallInfo.DIRECTION_OUTGOING,
                                        callType = getJsCommunicator().theCallType,
                                    )
                                )
                            )
                        }
                        ReqGetInfo.TYPE_GET_TERMINAL_CONFIG_INFO -> {
                            invokeCallback(
                                cbFromJs,
                                ResponseData(
                                    result = 1,
                                    data = RespInfo(
                                        //host = ConfigManager.host,
                                        screenWidth = DisplayHelper.getWidth(),
                                        screenHeight = DisplayHelper.getHeight(),
//                                        webViewWidth = getJsCommunicator().webViewWidth,
//                                        webViewHeight = getJsCommunicator().webViewHeight,
                                        webViewVisibility = getJsCommunicator().webViewVisibility,
                                        sdkVersion = BuildConfig.VERSION_NAME
                                    )
                                )
                            )
                        }
                        else -> {
                            invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                        }
                    }
                }
            }
            SAVE_DATA -> {
                var r: ReqSaveData? = null
                val currAppId = getCurrentAppId()
                r = parseRequest(dataFromJs, ReqSaveData::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (currAppId.isNullOrEmpty()) {
                    invokeCallback(cbFromJs, ErrorCode.MINI_APP_STACK_WRONG)
                } else {
                    if (r.key.isNotEmpty()) {
                        val sp = if (r.lifeCycle == ReqSaveData.LIFE_CYCLE_CROSS_SESSION) {
                            getJsCommunicator().storageManager.getSp()
                        } else {
                            getJsCommunicator().storageManager.getSpInSession()
                        }
                        sp.put("$currAppId-${r.key}", r.value)
                        invokeCallback(cbFromJs, ResponseData<Any>(result = 1))
                    } else {
                        invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                    }
                }
            }
            GET_DATA -> {
                var r: ReqGetData? = null
                val currAppId = getCurrentAppId()
                r = parseRequest(dataFromJs, ReqGetData::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (currAppId.isNullOrEmpty()) {
                    invokeCallback(cbFromJs, ErrorCode.MINI_APP_STACK_WRONG)
                } else {
                    if (r.key.isNotEmpty()) {
                        val sp = if (r.lifeCycle == ReqSaveData.LIFE_CYCLE_CROSS_SESSION) {
                            getJsCommunicator().storageManager.getSp()
                        } else {
                            getJsCommunicator().storageManager.getSpInSession()
                        }
                        val value = sp.get("$currAppId-${r.key}")
                        invokeCallback(cbFromJs, ResponseData<Any>(result = 1, data = value))
                    } else {
                        invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                    }
                }
            }
            START_APP -> {
                val r = parseRequest(dataFromJs, ReqStartApp::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    getJsCommunicator().abilityProvider.startApp(r.url, object :
                        Callback<Results<String>> {
                        override fun onResult(t: Results<String>) {
                            if (t.isSuccess()) {
                                invokeCallback(cbFromJs, ResponseData(result = 1, data = t.value))
                            } else {
                                invokeCallback(cbFromJs, ResponseData(result = 0, data = t.exception()?.message))
                            }
                        }
                    })
                }
            }
            START_BROWSER -> {
                val r = parseRequest(dataFromJs, ReqWithUrl::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    sendRenderEvent(url = r.url)
                    invokeCallback(cbFromJs, ResponseData<Any>(result = 1))
                }
            }
            REQUEST_WEB_VIEW_CHANGE -> {
                val r = parseRequest(dataFromJs, ReqRequestWebViewChange::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val appId = r.appId
                    if (appId.isEmpty()) {
                        LogUtil.e("appId is empty")
                        invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                    } else {
                        sendRequestSizeEvent(r.w, r.h, r.visibility, r.horizontalPos, r.verticalPos)
                        invokeCallback(cbFromJs, ResponseData<Any>(result = 1))
                    }
                }
            }
            CHOOSE_IMAGE -> {
                val r = parseRequest(dataFromJs, ReqChooseImageOrVideo::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    if (r.sourceType == "camera") {
                        getJsCommunicator().abilityProvider.openPicByCamera(object :
                            Callback<Results<Array<Uri?>>> {
                            override fun onResult(t: Results<Array<Uri?>>) {
                                if (t.isSuccess()) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 1, data = t.value))
                                } else {
                                    invokeCallback(cbFromJs, ResponseData<Any>(0, message = "Get image path failure"))
                                }
                            }
                        })
                    } else {
                        getJsCommunicator().abilityProvider.openPicByGallery(r.count, r.type, object :
                            Callback<Results<Array<Uri?>>> {
                            override fun onResult(t: Results<Array<Uri?>>) {
                                if (t.isSuccess()) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 1, data = t.value))
                                } else {
                                    invokeCallback(cbFromJs, ResponseData<Any>(0, message = "Get image path failure"))
                                }
                            }
                        })
                    }
                }
            }
            CHOOSE_FILE -> {
                val r = parseRequest(dataFromJs, ReqChooseFile::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    getJsCommunicator().abilityProvider.chooseFile(r.count, r.extension, object :
                        Callback<Results<Array<Uri?>>> {
                        override fun onResult(t: Results<Array<Uri?>>) {
                            if (t.isSuccess()) {
                                invokeCallback(cbFromJs, ResponseData<Any>(result = 1, data = t.value))
                            } else {
                                invokeCallback(cbFromJs, ResponseData<Any>(0, message = "Get file path failure"))
                            }
                        }
                    })
                }
            }
            CHOOSE_VIDEO -> {
                val r = parseRequest(dataFromJs, ReqChooseImageOrVideo::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    if (r.sourceType == "camera") {
                        getJsCommunicator().abilityProvider.openVideoByCamera(object :
                            Callback<Results<Array<Uri?>>> {
                            override fun onResult(t: Results<Array<Uri?>>) {
                                if (t.isSuccess()) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 1, data = t.value))
                                } else {
                                    invokeCallback(cbFromJs, ResponseData<Any>(0, message = "Get video path failure"))
                                }
                            }
                        })
                    } else {
                        getJsCommunicator().abilityProvider.openVideoByGallery(r.count, r.type, object :
                            Callback<Results<Array<Uri?>>> {
                            override fun onResult(t: Results<Array<Uri?>>) {
                                if (t.isSuccess()) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = 1, data = t.value))
                                } else {
                                    invokeCallback(cbFromJs, ResponseData<Any>(0, message = "Get video path failure"))
                                }
                            }
                        })
                    }
                }
            }
            SET_AUDIO_ROUTE -> {

            }
            CREATE_APP_DATA_CHANNEL -> {
                val r = parseRequest(dataFromJs, ReqCreateDC::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val callInfo = getJsCommunicator().callInfo
                    decorateLabels(r.dclabels) { newDcLabels ->
                        LogUtil.d("createDC newDcLabels=${Arrays.toString(newDcLabels.toTypedArray())}")
                        getJsCommunicator().networkAdapter.createDataChannel(
                            newDcLabels,
                            r.description,
                            callInfo.slotId,
                            callInfo.callId,
                            object : Callback<Results<Map<String, Int?>>> {
                                override fun onResult(t: Results<Map<String, Int?>>) {
                                    if (t.isSuccess()) {
                                        val labelDecorator =
                                            getJsCommunicator().networkAdapter.getLabelDecorator()
//                                        val cleanLabel = labelDecorator.removeOrigin(t.value().first)
                                        invokeCallback(
                                            cbFromJs,
                                            ResponseData<Any>(
                                                result = 1,
                                                data = RespCreateOrCloseDC(t.value?.map {
                                                    RespCreateOrCloseDC.CreateOrCloseDCDetail(
                                                        label = it.key,
                                                        result = it.value!!
                                                    )
                                                }!!)
                                            )
                                        )
                                    } else {
                                        invokeCallback(cbFromJs, ResponseData<Any>(result = 0, message = "DC create fail"))
                                    }
                                }
                            }
                        )
                    }
                }
            }
            CLOSE_APP_DATA_CHANNEL -> {
                val r = parseRequest(dataFromJs, ReqCloseDC::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val callInfo = getJsCommunicator().callInfo
                    decorateLabels(r.dclabels) { newDcLabels ->
                        getJsCommunicator().networkAdapter.closeDataChannel(
                            newDcLabels,
                            callInfo.slotId,
                            callInfo.callId,
                            object : Callback<Results<Map<String, Int>>> {
                                override fun onResult(t: Results<Map<String, Int>>) {
                                    if (t.isSuccess()) {
                                        val labelDecorator =
                                            getJsCommunicator().networkAdapter.getLabelDecorator()
//                                        val cleanLabel = labelDecorator.removeOrigin(t.value().first)
                                        invokeCallback(
                                            cbFromJs,
                                            ResponseData<Any>(
                                                result = 1,
                                                data = RespCreateOrCloseDC(t.value?.map {
                                                    RespCreateOrCloseDC.CreateOrCloseDCDetail(
                                                        label = it.key,
                                                        result = it.value
                                                    )
                                                }!!)
                                            )
                                        )
                                    } else {
                                        invokeCallback(cbFromJs, ResponseData<Any>(result = 0, message = "DC close fail"))
                                    }
                                }
                            }
                        )
                    }

                }
            }
            SEND_DATA -> {
                val r = parseRequest(dataFromJs, ReqSendData::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    decorateLabels(Collections.singletonList(r.dclabel)) { newDcLabels ->
                        val newLabel = newDcLabels.single()
                        LogUtil.d("sendData newDcLabels=${newLabel}")
                        getJsCommunicator().networkAdapter.sendDataOnAppDC(newLabel, r.data,
                            object : NetworkAdapter.RequestCallback {
                                override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                                    LogUtil.d("SendData statusCode=$statusCode, errCode=$errorCode")
                                    invokeCallback(cbFromJs, ResponseData<Any>(statuscode = statusCode, result = statusCode))
                                }
                            })
                    }

                }
            }
            SEND_HTTP -> {
                val r = parseRequest(dataFromJs, ReqSendHttp::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    decorateLabels(Collections.singletonList(r.dclabel)) { newDcLabels ->
                        val newLabel = newDcLabels.single()
                        LogUtil.d("sendHttp newDcLabels=${newLabel}")
                        LogUtil.d("sendHttp headers=${r.headers?.entries?.joinToString()}")
                        getJsCommunicator().networkAdapter.sendHttpOnDC(newLabel, r.url, r.headers,
                            object : NetworkAdapter.HttpRequestCallback {
                                override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                                    LogUtil.d("sendHttp statusCode=$statusCode, errCode=$errorCode")
                                }

                                override fun onMessageCallback(
                                    status: Int,
                                    msg: String,
                                    headers: MutableMap<String, String>?,
                                    body: ByteArray?
                                ) {
                                    LogUtil.d("sendHttp onMessageCallback status=$status")
                                    val currAppId = getCurrentAppId()
                                    val ct = headers?.filterKeys {
                                        it.equals("Content-Type", ignoreCase = true)
                                    }?.values?.firstOrNull()
                                    val cd = headers?.filterKeys {
                                        it.equals("Content-Disposition", ignoreCase = true)
                                    }?.values?.firstOrNull()
                                    val isFile = FileUtil.isFileContentType(ct)
                                    var fileName =
                                        FileUtil.extractFileNameInContentPosition(cd)
                                    if (isFile && fileName == null) {
                                        val ext = FileUtil.getFileExt(ct!!)
                                        fileName = FileUtil.genRandomFileName(ext)
                                    }
                                    LogUtil.d("sendHttp onMessageCallback, contentType=$ct, contentDisposition=$cd, isFile=$isFile")
                                    if (currAppId != null && ct != null && isFile) {
                                        LogUtil.d("sendHttp onMessageCallback, saveFile")
                                        getJsCommunicator().miniAppManager
                                            .saveFile(currAppId, fileName, ct, body!!, object: Callback<Results<String>> {
                                                override fun onResult(t: Results<String>) {
                                                    LogUtil.d("sendHttp onMessageCallback, saveFile res=${t.getOrNull()}")
                                                    invokeCallback(
                                                        cbFromJs, ResponseData<Any>(statuscode = status, result = 1,
                                                            data = RespSendHttp(
                                                                status,
                                                                msg,
                                                                headers,
                                                                isFile,
                                                                t.getOrNull()
                                                            )
                                                        )
                                                    )
                                                }
                                            })
                                    } else {
                                        LogUtil.d("sendHttp onMessageCallback, invoke with no file")
                                        invokeCallback(
                                            cbFromJs, ResponseData<Any>(statuscode = status, result = 1,
                                                data = RespSendHttp(
                                                    status,
                                                    msg,
                                                    headers,
                                                    isFile,
                                                    null
                                                )
                                            )
                                        )
                                    }
                                }

                            })
                    }

                }
            }
            SAVE_TRANSFER_FILE -> {
                val r = parseRequest(dataFromJs, ReqSaveTransferFile::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val callInfo = getJsCommunicator().callInfo
                    val miniAppManager = getJsCommunicator().miniAppManager
                    miniAppManager.saveTransferFile(callInfo.remoteNumber, r.base64, r.mime, r.fileName, r.fileSize, object :
                        Callback<Results<File>> {
                        override fun onResult(t: Results<File>) {
                            if (t.isSuccess()) {
                                miniAppManager.saveTransferFileInfo(r.type, r.direction, r.mime, t.value(),
                                    object : Callback<Results<RespGetTransferFileRecords>> {
                                        override fun onResult(t: Results<RespGetTransferFileRecords>) {
                                            LogUtil.i("save transfer file success!")
                                            invokeCallback(cbFromJs, ResponseData(result = 1, data = t.value()))
                                        }
                                    })
                            } else {
                                invokeCallback(cbFromJs, ResponseData(result = 0, data = t.exception()?.message))
                            }
                        }
                    })
                }
            }
            GET_TRANSFER_FILE_RECORDS -> {
                val r = parseRequest(dataFromJs, ReqGetTransferFileRecords::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    getJsCommunicator().miniAppManager.findTransferFileRecords(
                        object : Callback<Results<List<RespGetTransferFileRecords>>> {
                            override fun onResult(t: Results<List<RespGetTransferFileRecords>>) {
                                LogUtil.d("get transfer files: $t")
                                invokeCallback(cbFromJs, ResponseData(result = 1, data = t.value))
                            }
                        }
                    )
                }
            }
            SAVE_FILE_TO_LOCAL -> {
                val r = parseRequest(dataFromJs, ReqSaveTransferFile::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val callInfo = getJsCommunicator().callInfo
                    val miniAppManager = getJsCommunicator().miniAppManager
                    miniAppManager.saveFileToLocal(callInfo.remoteNumber, r.base64, r.type, r.fileName, r.mime, object:
                        Callback<Results<Boolean>> {
                        override fun onResult(t: Results<Boolean>) {
                            if (t.isSuccess()) {
                                LogUtil.i("save local file success!")
                                invokeCallback(cbFromJs, ResponseData(result = 1, data = t.value()))
                            } else {
                                invokeCallback(cbFromJs, ResponseData(result = 0, data = t.exception()?.message))
                            }
                        }
                    })
                }
            }
            ON_INIT -> {
                val r = parseRequest(dataFromJs, MiniAppEntity::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val pushResult = getJsCommunicator().miniAppManager.pushMiniApp(r)
                    val result = if (pushResult) 1 else 0
                    invokeCallback(cbFromJs, ResponseData<Any>(result = result))
                }
            }
            ON_EXIT -> {
                val r = parseRequest(dataFromJs, MiniAppEntity::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val popResult = getJsCommunicator().miniAppManager.popMiniApp(r)
                    val result = if (popResult) 1 else 0
                    invokeCallback(cbFromJs, ResponseData<Any>(result = result))
                }
            }
            // bridge 接口：查询双方是否可以发起屏幕共享
            REQUEST_SCREEN_SHARE -> {
                val screenShareController =
                    getJsCommunicator().extensionManager.getScreenShareManager()
                // 查询屏幕共享是否可用
                val available = screenShareController.isScreenShareAvailable()
                // 回调可用状态
                invokeCallback(
                    cbFromJs,
                    ResponseData(result = 1, data = RespRequestAbility(available))
                )
            }
            // bridge 接口：控制屏幕共享的开启或关闭
            CONTROL_SCREEN_SHARE -> {
                // 前端请求开启屏幕共享
                val r = parseRequest(dataFromJs, ReqControlScreenShare::class.java)
                // 数据为空，回调错误
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val screenShareController =
                        getJsCommunicator().extensionManager.getScreenShareManager()
                    // 开启屏幕共享
                    if (r.enable) {
                        decorateLabels(Collections.singletonList(r.dcLabel)) { newDcLabels ->
                            screenShareController.enableScreenShare(r.role, newDcLabels.single(), object : Callback<Results<Boolean>> {
                                override fun onResult(t: Results<Boolean>) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(result = t.value.toResult()))
                                }
                            })
                        }
                    }
                    // 关闭屏幕共享
                    else {
                        screenShareController.disableScreenShare()
                        invokeCallback(cbFromJs, ResponseData<Any>(result = 1))
                    }
                }
            }
            GET_LOCATION -> {
                getJsCommunicator().abilityProvider.getLocation(object : Callback<Results<Pair<Double, Double>>> {
                    override fun onResult(t: Results<Pair<Double, Double>>) {
                        getJsCommunicator().abilityProvider.stopGetLocation()
                        if (t.isSuccess()) {
                            invokeCallback(cbFromJs, ResponseData(result = 1, data = RespGetLocation(t.value!!.first, t.value.second)))
                        } else {
                            invokeCallback(cbFromJs, ResponseData(result = 0, data = t.exception()?.message))
                        }
                    }
                })
            }
            REQUEST_AR_CALL -> {
                val arManager =
                    getJsCommunicator().extensionManager.getARManager()
                arManager.isARCallAvailable(object: Callback<Results<Boolean>> {
                    override fun onResult(t: Results<Boolean>) {
                        invokeCallback(
                            cbFromJs,
                            ResponseData(
                                result = 1,
                                data = RespRequestAbility(if (t.isSuccess()) t.value() else false)
                            )
                        )
                    }
                })
            }
            CONTROL_AR_CALL -> {
                val r = parseRequest(dataFromJs, ReqARCall::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val arController =
                        getJsCommunicator().extensionManager.getARManager()
                    val callInfo = getJsCommunicator().callInfo
                    if (r.enable) {
                        decorateLabels(r.dcLabels) { newDcLabels ->
                            arController.startARCall(newDcLabels,
                                callInfo.slotId,
                                callInfo.callId,
                                object : Callback<Results<Boolean>> {
                                    override fun onResult(t: Results<Boolean>) {
                                        invokeCallback(cbFromJs, ResponseData<Any>(result = t.value.toResult()))
                                    }
                                })
                        }
                    } else {
                        arController.stopARCall(
                            callInfo.slotId,
                            callInfo.callId,
                            object : Callback<Results<Boolean>> {
                            override fun onResult(t: Results<Boolean>) {
                                invokeCallback(cbFromJs, ResponseData<Any>(result = t.value.toResult()))
                            }
                        })
                    }
                }
            }
            CHECK_RESOURCE -> {
                val currAppId = getCurrentAppId()
                val r = parseRequest(dataFromJs, ReqCheckResource::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (currAppId != r.appId) {
                    LogUtil.e("passed appId not match, currAppId=$currAppId")
                    invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                } else {
                    val results = mutableListOf<RespCheckResource.Result>()
                    getJsCommunicator().miniAppManager
                        .checkFilesExists(r.appId, r.paths)
                        .entries.forEach {
                            results.add(RespCheckResource.Result(it.key, it.value))
                        }
                    LogUtil.d("checkResource, results=$results")
                    invokeCallback(cbFromJs, ResponseData<Any>(
                        result = 1,
                        data = RespCheckResource(results)
                    ))
                }
            }
            DELETE_RESOURCE -> {
                val currAppId = getCurrentAppId()
                val r = parseRequest(dataFromJs, ReqDeleteResource::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (currAppId != r.appId) {
                    LogUtil.e("passed appId not match, currAppId=$currAppId")
                    invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                } else {
                    val results = mutableListOf<RespDeleteResource.Result>()
                    getJsCommunicator().miniAppManager
                        .deleteFiles(r.appId, r.paths)
                        .entries.forEach {
                            results.add(RespDeleteResource.Result(it.key, it.value))
                        }
                    LogUtil.d("deleteResource, results=$results")
                    invokeCallback(cbFromJs, ResponseData<Any>(
                        result = 1,
                        data = RespDeleteResource(results)
                    ))
                }
            }
            LOAD_AR_RESOURCE -> {
                val currAppId = getCurrentAppId()
                val r = parseRequest(dataFromJs, ReqLoadARResource::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else if (currAppId != r.appId) {
                    LogUtil.e("passed appId not match, currAppId=$currAppId")
                    invokeCallback(cbFromJs, ErrorCode.ARGUMENTS_ILLEGAL)
                } else {
                    // TODO call ar sdk?
                    invokeCallback(cbFromJs, ResponseData<Any>(
                        result = 1
                    ))
                }
            }
            CONTROL_STT -> {
                val r = parseRequest(dataFromJs, ReqControlSTT::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    val sttManager = getJsCommunicator().extensionManager.getSTTManager()
                    if (r.enable) {
                        decorateLabels(Collections.singletonList(r.dcLabel)) { newDcLabels ->
                            sttManager.enableSTT((r.textSize ?: 0), newDcLabels.single(), object: Callback<Results<Boolean>> {
                                override fun onResult(t: Results<Boolean>) {
                                    invokeCallback(cbFromJs, ResponseData<Any>(
                                        result = t.value.toResult()
                                    ))
                                }
                            })
                        }
                    } else {
                        sttManager.disableSTT(object: Callback<Results<Boolean>> {
                            override fun onResult(t: Results<Boolean>) {
                                invokeCallback(cbFromJs, ResponseData<Any>(
                                    result = t.value.toResult()
                                ))
                            }
                        })
                    }
                }
            }
            CONTROL_COMPRESS -> {
                val r = parseRequest(dataFromJs, ReqControlCompress::class.java)
                if (r == null) {
                    invokeCallback(cbFromJs, ErrorCode.JSON_PARSE_WRONG)
                } else {
                    if (!File(r.path).exists()) {
                        invokeCallback(cbFromJs, ErrorCode.FILE_OR_FOLDER_NOT_EXIST)
                        return true
                    }
                    if (!r.compress) {
                        if (!r.path.endsWith(".zip", true)) {
                            invokeCallback(cbFromJs, ErrorCode.DECOMPRESS_FILE_FAILED)
                        } else {
                            val unZipFilePath =
                                r.path.substring(0, r.path.lastIndexOf(".zip", r.path.length, true))
                            val deleteRecursivelyResult = File(unZipFilePath).deleteRecursively()
                            LogUtil.d("controlCompress, decompress deleteRecursivelyResult = $deleteRecursivelyResult")
                            ZipUtil.unzip(r.path, unZipFilePath)
                            invokeCallback(cbFromJs, ResponseData<Any>(
                                    result = 1,
                                    data = RespControlCompress(unZipFilePath)
                                )
                            )
                        }
                    } else {
                        val file = File(r.path)
                        val zipFilePath: String
                        if (file.isDirectory) {
                            if (file.listFiles().isNullOrEmpty()) {
                                invokeCallback(cbFromJs, ErrorCode.FOLDER_IS_EMPTY)
                                return true
                            }
                            zipFilePath = "${r.path}.zip"
                            ZipUtil.zipFilesInFolder(r.path, zipFilePath)
                        } else {
                            zipFilePath =
                                "${r.path.substring(0, r.path.lastIndexOf('.', r.path.length))}.zip"
                            ZipUtil.zip(listOf(file), zipFilePath)
                        }
                        invokeCallback(cbFromJs, ResponseData<Any>(
                                result = 1,
                                data = RespControlCompress(zipFilePath)
                            )
                        )
                    }
                }
            }
            else -> {
                handled = false
            }
        }
        LogUtil.i("CommonMiniAppJsHandler handle func=$regFuncName with result=$handled")
        return handled
    }

    /**
     * append origin in start of the given dc label if origin is absent
     */
    private fun decorateLabels(oldLabels: List<String>,
                               callback: (List<String>) -> Any) {
        if (!ENABLE_ORIGIN_DECORATE) {
            callback.invoke(oldLabels)
            return
        }
        LogUtil.d("decorateLabels")
        val labelDecorator = getJsCommunicator().networkAdapter.getLabelDecorator()
        val appIds = oldLabels.map { labelDecorator.parseAppId(it) ?: "" }
        if (appIds.any { it.isEmpty() }) {
            throw NewCallException("decorate label err on parseAppId")
        }
        appIds.let { ids ->
            getJsCommunicator().miniAppManager.findMiniApp(
                ids,
                object : Callback<Results<List<MiniApp>>> {
                    override fun onResult(t: Results<List<MiniApp>>) {
                        if (t.isSuccess()) {
                            val miniApps = t.value()
                            val newLabels = labelDecorator.addOrigins(miniApps, oldLabels)
                            callback.invoke(newLabels)
                        } else {
                            throw NewCallException("decorate label err on query MNA")
                        }
                    }
                })
        }
    }
}