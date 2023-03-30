package com.cmcc.newcalllib.adapter.network

import android.content.Context
import android.os.Handler
import android.os.RemoteException
import com.cmcc.newcalllib.adapter.ar.aidl.ARAdapter
import com.cmcc.newcalllib.adapter.network.data.NetworkConfig
import com.cmcc.newcalllib.datachannel.*
import com.cmcc.newcalllib.dc.httpstack.HttpStack
import com.cmcc.newcalllib.dc.httpstack.utils.HttpStackBufferUtil
import com.cmcc.newcalllib.dc.httpstack.utils.HttpStackUrlUtil
import com.cmcc.newcalllib.dc.httpstack.utils.HttpStackUtil
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.ConfigManager
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.constant.Constants
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap


/**
 * @author jihongfei
 * @createTime 2022/2/22 17:39
 */
class ImsDCNetworkAdapter(
    handler: Handler,
    networkConfig: NetworkConfig,
    labelDecorator: LabelDecorator
) :
    NetworkAdapter(handler, networkConfig, labelDecorator), ARAdapter {

    private lateinit var mContext: Context

    private val mImsDcManager: ImsDataChannelManagerImpl = ImsDataChannelManagerImpl.getInstance()

    // DC Map：keep DataChannel（key: dcLabel , value: Pair<ImsDCStatus, IImsDataChannel>）
    private val mDataChannelMap: MutableMap<String, Pair<ImsDCStatus, IImsDataChannel>> =
        mutableMapOf()

    // DC http请求去重（key: dcLabel , value: callback）：每一个DC通道，同一时刻只能有一个 http 请求
    private val mHttpRequestCallbacks = mutableMapOf<String, HttpRequestCallback>()

    // 当前使用的sim卡
    private var mCurrSlotId = 0
    private var mCallId = ""


    // callback for arCall
    private var mARCallCallback: ARAdapter.ARCallback? = null

    private var mDataChannelSplitter: DataSplitter? = null
    private var mDataChannelAggregator: DataAggregator? = null

    // callback for create dc
    private var mCreateDcCallback: Callback<Results<Map<String, Int?>>>? = null
    // create dc result map list. map of "label-result"
    private var mCreateDCResultMaps: MutableList<MutableMap<String, Int>> = mutableListOf()
    // callback for close dc
    private var mCloseDcCallback: Callback<Results<Map<String, Int>>>? = null
    // close dc result map list. map of "label-result"
    private var mCloseDCResultMaps: MutableList<MutableMap<String, Int>> = mutableListOf()

    companion object {
        const val DEFAULT_STATE = -100

        const val CREATE_DC_FAIL = 0     // 操作失败
        const val CREATE_DC_SUCCESS = 1  // 操作成功
        const val CREATE_DC_ALREADY = -1 // DC创建中，勿重复创建

        const val CLOSE_DC_FAIL = 0     // 关闭失败
        const val CLOSE_DC_SUCCESS = 1  // 关闭成功
        const val CLOSE_DC_ALREADY = -1 // DC关闭中，勿重复关闭
    }

    /**
     * init
     */
    override fun init(context: Context) {
        LogUtil.d("ImsDCNetworkAdapter: init.");
        mContext = context
        getNetworkConfig().slotId?.apply {
            mCurrSlotId = this
        }
        getNetworkConfig().callId?.apply {
            mCallId = this
        }
        // init
        mImsDcManager.init(context)
        // bind service
        mImsDcManager.bindImsDcService(
            ConfigManager.dcServiceAction,
            ConfigManager.dcServicePackage
        )
        // 连接状态回调
        mImsDcManager.setImsDcServiceConnectionCallback(object :
            ImsDcServiceConnectionCallback {
            override fun onServiceConnected() {
                LogUtil.d("ImsDCNetworkAdapter: onServiceConnected. mCurrSlotId=$mCurrSlotId  mCallId=$mCallId")
                // "SDK进程向"向"IMS进程"设置监听: 监听DC的建立、DC状态变化、对端建立DC的请求
                mImsDcManager.setImsDcStatusCallback(ImsDcCallback(), mCurrSlotId, mCallId)
            }

            override fun onServiceDisconnected() {}
        })

        /// disable splitter&aggregator
        mDataChannelSplitter = DataSplitManager()
        mDataChannelAggregator = DataSplitManager()
    }

    /**
     * unbind service
     */
    override fun release() {
        LogUtil.d("ImsDCNetworkAdapter: release.");
        mDataChannelMap.clear()
        mCreateDCResultMaps.clear()
        mCloseDCResultMaps.clear()
        mDataInterceptors.clear()
        mImsDcManager.unbindImsDCService()
        mHttpRequestCallbacks.clear()
    }

    /**
     * SDK进程 通知 IMS进程: 创建dc通道（一般用于创建App DC通道）
     */
    override fun createDataChannel(
        dcLabels: List<String>,
        dcDescription: String,
        slotId: Int,
        callId: String,
        callback: Callback<Results<Map<String, Int?>>>?
    ) {
        LogUtil.d("ImsDCNetworkAdapter: createDataChannel.")
        LogUtil.d("ImsDCNetworkAdapter: dcLabels size: ${dcLabels.size}")
        // check cached dc map first
        val newDcLabels = mutableListOf<String>()
        val createDCResultMap = ConcurrentHashMap<String, Int>()
        dcLabels.forEach {
            createDCResultMap[it] = DEFAULT_STATE
            if (isDcMapContainsLabel(it, mDataChannelMap)) {
                LogUtil.d("ImsDCNetworkAdapter: label:$it in mDataChannelMap")
                val pair = getFromDcMap(it, mDataChannelMap)
                if (pair?.second?.state == ImsDCStatus.DC_STATE_CONNECTING) {
                    createDCResultMap[it] = CREATE_DC_ALREADY
                } else if (pair?.second?.state == ImsDCStatus.DC_STATE_OPEN) {
                    createDCResultMap[it] = CREATE_DC_SUCCESS
                } else {
                    createDCResultMap[it] = CREATE_DC_FAIL
                }
            } else {
                LogUtil.d("ImsDCNetworkAdapter: newDcLabels.add(it)")
                newDcLabels.add(it)
            }
        }
        if (newDcLabels.isEmpty()) {
            LogUtil.w("ImsDCNetworkAdapter: newDcLabels isEmpty")
            callback?.onResult(Results(createDCResultMap))
            return
        }

        mCreateDcCallback = callback
        mCreateDCResultMaps.add(createDCResultMap)
        // 创建dc通道
        mImsDcManager.createImsDc(newDcLabels.toTypedArray(), slotId, callId, dcDescription)
    }

    /**
     * SDK进程 通知 IMS进程: 关闭dc通道（一般用于关闭App DC通道）
     */
    override fun closeDataChannel(
        dcLabels: List<String>,
        slotId: Int,
        callId: String,
        callback: Callback<Results<Map<String, Int>>>?
    ) {
        LogUtil.d("ImsDCNetworkAdapter: closeDataChannel.")
        LogUtil.d("ImsDCNetworkAdapter: closeDataChannel, before $mDataChannelMap")
        // register listener for DC close
        val newImsDataChannels = mutableListOf<String>()
        val closeDCResultMap = ConcurrentHashMap<String, Int>()
        dcLabels.forEach {
            closeDCResultMap[it] = DEFAULT_STATE
            if (isDcMapContainsLabel(it, mDataChannelMap)) {
                val pair = getFromDcMap(it, mDataChannelMap)
                if (pair?.second?.state == ImsDCStatus.DC_STATE_CLOSING) {
                    closeDCResultMap[it] = CLOSE_DC_ALREADY
                    mDataChannelMap.remove(it)
                } else if (pair?.second?.state == ImsDCStatus.DC_STATE_CLOSED) {
                    closeDCResultMap[it] = CLOSE_DC_SUCCESS
                    mDataChannelMap.remove(it)
                } else {
                    newImsDataChannels.add(it)
                }
            } else {
                closeDCResultMap[it] = CLOSE_DC_SUCCESS
            }
        }
        if (newImsDataChannels.isEmpty()) {
            LogUtil.d("ImsDCNetworkAdapter: newImsDataChannels isEmpty")
            callback?.onResult(Results(closeDCResultMap))
            return
        }
        mCloseDcCallback = callback
        mCloseDCResultMaps.add(closeDCResultMap)
        // 关闭dc通道
        val filterKeys = mDataChannelMap.filterKeys {
            // TODO need consider origin? only 'local-' label in parameter?
            it in newImsDataChannels
        }
        mImsDcManager.deleteImsDc(
            filterKeys.map { it.value.second.dcLabel }.toTypedArray(),
            slotId,
            callId
        )

        // update map
        filterKeys.keys.forEach(mDataChannelMap::remove)
        LogUtil.d("ImsDCNetworkAdapter: closeDataChannel, after $mDataChannelMap")
    }

    /**
     * SDK进程 通知 IMS进程: 同意建立DC通道
     */
    override fun respondDataChannelSetupRequest(
        dcLabels: Array<String>,
        accepted: Array<Boolean>,
        slotId: Int,
        callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter: respondDataChannelSetupRequest. dcLabels: $dcLabels accepted: $accepted slotId: $slotId callId: $callId")
        // 调用IMS进程提供的接口，同意建立DC通道
        mImsDcManager.responseImsDcSetupRequest(
            dcLabels,
            accepted.toBooleanArray(), slotId, callId
        )
    }

    /**
     * 发送文件
     */
    override fun sendHttpOnDC(
        label: String,
        url: String,
        headerMap: Map<String, String>?,
        formBodyMap: Map<String, String>?,
        formFileList: List<File?>?,
        callback: HttpRequestCallback
    ) {
        LogUtil.d("ImsDCNetworkAdapter: sendHttpGet. label: $label url: $url headerMap: $headerMap formBodyMap: $formBodyMap formFileList: $formFileList")
        if (isDcMapContainsLabel(label, mHttpRequestCallbacks)) {
            LogUtil.e("ImsDCNetworkAdapter: Repeated request!!! label: $label url: $url", null)
            // 回调错误信息
            callback.onSendDataCallback(-1, -1)
            callback.onMessageCallback(
                -1,
                "Only one request can be made in the same time period. label: $label url: $url",
                null,
                null
            )
            return;
        }
        mHttpRequestCallbacks[label] = callback
        /**
         * 获取当前的dc
         */
        var channelPair = getFromDcMap(label, mDataChannelMap)
        val state = channelPair?.first
        val dc = channelPair?.second
        LogUtil.d("ImsDCNetworkAdapter: mDataChannelMap: $mDataChannelMap")
        LogUtil.d(
            "ImsDCNetworkAdapter: dcInfo: dcLabel: ${dc?.dcLabel} " +
                    " dcStreamId: ${dc?.streamId}  dcType: ${dc?.dcType}  dcState: ${dc?.state}" +
                    " dcSubProtocol: ${dc?.subProtocol}  dcCallId: ${dc?.callId}" +
                    " dcBufferedAmount: ${dc?.bufferedAmount()}"
        )
        if (dc == null) {
            LogUtil.d("ImsDCNetworkAdapter: Error: failed to sendHttp. dc was not found!!!  label=$label url=$url")
            return
        }
        /**
         * 获取http请求消息体
         */
        // 获取http请求 消息体
        val httpByteBuffer = HttpStack.encodeHttpRequest(url, headerMap, formBodyMap, formFileList);
        // 分片发送
        val bufferAmount = getNetworkConfig().bufferAmount
        if (mDataChannelSplitter != null && bufferAmount != null) {
            mDataChannelSplitter!!.split(
                DataSplitManager.SUB_PROTOCOL_HTTP,
                label,
                httpByteBuffer,
                bufferAmount
            ) {
                doSendByteBuffer(dc, it, callback)
            }
        } else {
            doSendByteBuffer(dc, httpByteBuffer, callback)
        }
    }

    /**
     * 发送文本数据
     */
    override fun sendDataOnAppDC(label: String, data: String, callback: RequestCallback) {
        val byteBuffer = HttpStackBufferUtil.getByteBuffer(data)
        LogUtil.d("ImsDCNetworkAdapter: sendDataOverAppDc. label: $label data: $data")
        /**
         * 获取当前的dc
         */
        var channelPair = getFromDcMap(label, mDataChannelMap)
        val dcStatus = channelPair?.first
        val dc = channelPair?.second
        LogUtil.d("ImsDCNetworkAdapter: mDataChannelMap: $mDataChannelMap")
        LogUtil.d(
            "ImsDCNetworkAdapter: dcInfo: dcLabel: ${dc?.dcLabel} " +
                    " dcStreamId: ${dc?.streamId} dcType: ${dc?.dcType} dcState: ${dc?.state}" +
                    " dcSubProtocol: ${dc?.subProtocol} dcCallId: ${dc?.callId}" +
                    " dcBufferedAmount: ${dc?.bufferedAmount()}"
        )
        if (dc == null) {
            LogUtil.d("ImsDCNetworkAdapter: Error: failed to sendData. dc was not found!!!  label=$label data=$data")
            return
        }
        // 发送websocket请求
        val bufferAmount = getNetworkConfig().bufferAmount
        if (mDataChannelSplitter != null && bufferAmount != null) {
            mDataChannelSplitter!!.split(
                DataSplitManager.SUB_PROTOCOL_DEFAULT,
                label,
                byteBuffer,
                bufferAmount
            ) {
                doSendByteBuffer(dc, it, callback)
            }
        } else {
            doSendByteBuffer(dc, byteBuffer, callback)
        }
    }



    private fun doSendByteBuffer(
        dc: IImsDataChannel?,
        byteBuffer: ByteBuffer,
        requestCallback: RequestCallback?
    ) {
        LogUtil.d("ImsDCNetworkAdapter: doSendByteBuffer")
        val data = byteBuffer.array()
        LogUtil.d("ImsDCNetworkAdapter: buffer.size: ${data.size}")
        dc?.send(data, data.size, object : IDCSendDataCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onSendDataResult(state: Int, errorcode: Int) {
                LogUtil.d("ImsDCNetworkAdapter:onSendDataResult: state: $state errorcode: $errorcode")
                // 回调发送状态
                requestCallback?.onSendDataCallback(state, errorcode)
            }
        })
    }

    /**
     * SDK进程 通知 IMS进程: dc 可用状态查询
     */
    override fun isDataChannelAvailable(label: String): Boolean {
        LogUtil.d("ImsDCNetworkAdapter: isDataChannelAvailable label: $label")
        /**
         * 获取当前的dc
         */
        var channelPair = getFromDcMap(label, mDataChannelMap)
        val dcType = channelPair?.first
        val dc = channelPair?.second
        LogUtil.d("ImsDCNetworkAdapter: mDataChannelMap: $mDataChannelMap")
        LogUtil.d("ImsDCNetworkAdapter: channelPair: $channelPair")
        LogUtil.d("ImsDCNetworkAdapter: dcType: $dcType")
        LogUtil.d("ImsDCNetworkAdapter: dc: $dc")
        if (dc?.state == ImsDCStatus.DC_STATE_OPEN) {
            return true;
        }
        return false
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 内部类
     *
     * "SDK进程向"向"IMS进程"设置监听: 监听DC的建立、DC状态变化、对端建立DC的请求
     */
    inner class ImsDcCallback : IImsDataChannelCallback.Stub() {
        override fun onImsDataChannelSetupRequest(
            dcLabels: Array<String>,
            slotId: Int,
            callId: String
        ) {
            LogUtil.d("ImsDCNetworkAdapter: onImsDataChannelSetupRequest. dcLabels: $dcLabels slotId: $slotId callId: $callId")
            // IMS进程 通知 SDK进程: 对方要求建立dc通道的请求
            this@ImsDCNetworkAdapter.onImsDcSetupRequest(dcLabels, slotId, callId)
        }

//        override fun onGetSurface(sf: Surface?, slotId: Int, callId: String?) {
//            if (sf == null || callId == null) {
//                LogUtil.e(
//                    "ImsDCNetworkAdapter",
//                    "onGetSurface $sf, $callId",
//                    NullPointerException()
//                )
//            } else {
//                mARCallCallback?.onGetSurface(sf, slotId, callId)
//            }
//        }

        override fun onBoostrapDataChannelResponse(
            dc: IImsDataChannel?,
            slotId: Int,
            callId: String
        ) {
            LogUtil.d("ImsDCNetworkAdapter: onBoostrapDataChannelResponse. slotId: $slotId callId: $callId")
            // IMS进程 通知 SDK进程: Bootstrap DC的建立、DC状态变化
            if (dc != null) {
                this@ImsDCNetworkAdapter.onImsDcStatusCallback(dc, dc.state, slotId, callId)
            }
        }

        override fun onApplicationDataChannelResponse(
            dc: IImsDataChannel?,
            slotId: Int,
            callId: String
        ) {
            LogUtil.d("ImsDCNetworkAdapter: onApplicationDataChannelResponse. slotId: $slotId callId: $callId")
            // IMS进程 通知 SDK进程: Application DC的建立、DC状态变化
            if (dc != null) {
                this@ImsDCNetworkAdapter.onImsDcStatusCallback(dc, dc.state, slotId, callId)
            }
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * IMS进程 通知 SDK进程: 对端要求建立DC数据通道
     */
    private fun onImsDcSetupRequest(
        dcLabels: Array<String>, slotId: Int, callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter: onImsDcSetupRequest. dcLabels: $dcLabels slotId: $slotId callId: $callId ")
        // 1、IMS进程 通知 SDK进程: 呼叫方要求建立一个APP DC 数据通道；
        // 2、这里调用SDK进程 onImsDataChannelSetupRequest 咨询SDK进程，是否同意；
        // 3、若同意 SDK进程 调用 IMS进程方法 respondDataChannelSetupRequest
        // 4、IMS进程 创建APP DC，并回调DC
        mDataChannelCallback?.onImsDataChannelSetupRequest(dcLabels, slotId, callId)
    }



    /**
     * IMS进程 通知 SDK进程: DC的建立、DC状态变化
     * @param state CONNECTING = 0 , OPEN = 1, CLOSING = 2, CLOSED = 3;
     */
    private fun onImsDcStatusCallback(
        dc: IImsDataChannel, status: ImsDCStatus, slotId: Int, callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter: onImsDcStatusCallback. status: $status slotId: $slotId callId: $callId")
        LogUtil.d(
            "ImsDCNetworkAdapter: dcLabel: ${dc?.dcLabel} " +
                    " dcStreamId: ${dc?.streamId} dcType: ${dc?.dcType} dcState: ${dc?.state}" +
                    " dcSubProtocol: ${dc?.subProtocol} dcCallId: ${dc?.callId}" +
                    " dcBufferedAmount: ${dc?.bufferedAmount()}"
        )
        // 获取dc label
        var dcLabel = Constants.getDcLable(dc);
        LogUtil.d("ImsDCNetworkAdapter: dcLabel: $dcLabel")
        // 回调DC状态
        when (status) {
            // CONNECTING
            ImsDCStatus.DC_STATE_CONNECTING -> {
                LogUtil.d("ImsDCNetworkAdapter: DC CONNECTING.")
                registerDcMsgObserver(dc, status, slotId, callId)
            }
            // OPEN
            ImsDCStatus.DC_STATE_OPEN -> {
                LogUtil.d("ImsDCNetworkAdapter: DC OPEN.")
                registerDcMsgObserver(dc, status, slotId, callId)
                // 获取 记录的 state状态
                val mapDcState = getFromDcMap(dcLabel, mDataChannelMap)?.first
                LogUtil.d("ImsDCNetworkAdapter: mapDcState $mapDcState")
                // 已经OPEN过，则不再OPEN (三星存在多次OPEN的情况)
                //if (mapDcState?.toInteger() != ImsDCStatus.DC_STATE_OPEN.toInteger()) {
                // boot dc: notify bootstrap dc created. react only on local BDC
                if (Constants.isBootDc(dc)) {
                    if (dc.streamId == Constants.BOOTSTRAP_DATA_CHANNEL_STREAM_ID_LOCAL) {
                        LogUtil.d("ImsDCNetworkAdapter: onBootstrapDataChannelCreated")
                        mDataChannelCallback?.onBootstrapDataChannelCreated(true)
                    }
                }
                // app dc：
                else {
                    // 主动创建的app dc
                    if (mCreateDcCallback != null) {
                        LogUtil.d("ImsDCNetworkAdapter: AppDc OPEN, callback dc success")
                        mCreateDCResultMaps.forEach {
                            if (it.containsKey(dc.dcLabel)) {
                                it[dc.dcLabel] = CREATE_DC_SUCCESS
                            }
                            // 全部默认值被替换代表本组全部返回结果
                            if (!it.containsValue(DEFAULT_STATE)) {
                                mCreateDcCallback?.onResult(Results(it))
                                mCreateDcCallback = null
                                mCreateDCResultMaps.remove(it)
                            }
                        }
                    }
                    // 被动由芯片创建的 app dc (或者说是远端创建的dc，比如: 屏幕共享)
                    else {
                        LogUtil.d("ImsDCNetworkAdapter: AppDc OPEN, callback dc setup request")
                        val labelList: MutableList<String> = ArrayList()
                        labelList.add(dc.dcLabel)
                        mDataChannelCallback?.onImsDataChannelSetupRequest(labelList.toTypedArray(), slotId, callId)
                    }
                }
            }
            // CLOSING
            ImsDCStatus.DC_STATE_CLOSING -> {
                LogUtil.d("ImsDCNetworkAdapter: DC CLOSING")
            }
            // CLOSED
            ImsDCStatus.DC_STATE_CLOSED -> {
                LogUtil.d("ImsDCNetworkAdapter: DC CLOSED.")
                unregisterDcObserver(dc)
            }
        }
        // 更新 Map 状态
        updateDcMapStatus(dc, status)
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private fun registerDcMsgObserver(
        dc: IImsDataChannel,
        imsDCStatus: ImsDCStatus,
        slotId: Int,
        callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter: registerDcMsgObserver.")
        LogUtil.d(
            "ImsDCNetworkAdapter: dcLabel: ${dc?.dcLabel} " +
                    " dcStreamId: ${dc?.streamId} dcType: ${dc?.dcType} dcState: ${dc?.state}" +
                    " dcSubProtocol: ${dc?.subProtocol} dcCallId: ${dc?.callId}" +
                    " dcBufferedAmount: ${dc?.bufferedAmount()}"
        )
        val dcLabel = Constants.getDcLable(dc)
        LogUtil.d("ImsDCNetworkAdapter: dcLabel: $dcLabel")
        // 若已经注册过，则不再注册；
        if (isDcMapContainsLabel(dcLabel, mDataChannelMap)) {
            LogUtil.d("ImsDCNetworkAdapter: DC observer have been registered!!!  dcLabel: $dcLabel")
            return;
        }
        // 如果没有注册过，先保存DC，再注册消息回调；
        mDataChannelMap[dcLabel] = Pair(imsDCStatus, dc)
        // 注册消息回调
        dc.registerObserver(object : IImsDCObserver.Stub() {
            override fun onDataChannelStateChange(status: ImsDCStatus) {
                // bdc状态变化
                LogUtil.d("ImsDCNetworkAdapter: ImsDCObserver.onDataChannelStateChange. status: $status status: ${status.toInteger()}")
                onImsDcStatusCallback(dc, status, slotId, callId)
            }

            override fun onMessage(data: ByteArray?, length: Int) {
                // bdc返回的消息数据
                LogUtil.d("ImsDCNetworkAdapter: ImsDCObserver.onMessage. data: $data ,length: $length")
                LogUtil.d("ImsDCNetworkAdapter: data.size: ${data?.size}")
                LogUtil.d(
                    "ImsDCNetworkAdapter: dcLabel: ${dc.dcLabel} " +
                            "+ streamId: ${dc.streamId}+ dcType: ${dc.dcType}+ state: ${dc.state}" +
                            "+ subProtocol: ${dc.subProtocol}+ callId: ${dc.callId}" +
                            "+ bufferedAmount: ${dc.bufferedAmount()}"
                )
                //
                val byteBuffer = ByteBuffer.wrap(data)
                var handled = false
                val callback: (buffer: ByteBuffer) -> Any = { aggregated ->
                    LogUtil.d("ImsDCNetworkAdapter: callback.dcLabel: $dcLabel")
                    // handled by interceptor
                    mDataInterceptors
                        .filter {
                            LogUtil.d("ImsDCNetworkAdapter: filter.provideDcLabel: ${it.provideDataChannelLabel()}")
                            it.provideDataChannelLabel() == dcLabel
                        }
                        .forEach {
                            if (!handled) {
                                handled = it.onDataArrive(aggregated)
                            }
                            LogUtil.d("ImsDCNetworkAdapter: forEach.provideDcLabel: ${it.provideDataChannelLabel()}")
                            LogUtil.d("ImsDCNetworkAdapter: forEach.onDataArrive: $handled")
                        }
                    LogUtil.d("ImsDCNetworkAdapter: handled: $handled")
                    // un handle
                    if (!handled) {
                        // 回调请求数据
                        if (dc.subProtocol.contains(DataSplitManager.SUB_PROTOCOL_HTTP, true)) {
                            // 回调Response数据
                            onHttpGetResponse(dcLabel, aggregated)
                        } else {
                            mDataObserver?.onDataArrive(dcLabel, aggregated)
                        }
                    }
                }
                if (mDataChannelAggregator != null) {
                    mDataChannelAggregator!!.aggregate(
                        dc.subProtocol,
                        dcLabel,
                        byteBuffer,
                        callback
                    )
                } else {
                    callback.invoke(byteBuffer)
                }
            }
        })
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    /**
     * 回调 http1.1 Get Response 请求数据
     */
    private fun onHttpGetResponse(label: String, buffer: ByteBuffer) {
        LogUtil.d("ImsDCNetworkAdapter: onHttpGetResponse label: $label")
        try {
            // 从二进制字节流中解析 Http Response 消息数据
            var response = HttpStack.decodeHttpResponse(buffer);
            LogUtil.d("ImsDCNetworkAdapter: response: $response ")
            // 打印http网络请求数据
            val code = response.code()
            var msg = response.message()
            val headers = HttpStackUrlUtil.getHeaderMap(response.headers())
            val responseBodyBytes = response.body().bytes()
            // 打印需要验证的http协议
            LogUtil.d("ImsDCNetworkAdapter: HttpResponse-StatusLine: code=$code  msg=$msg ")
            LogUtil.d("ImsDCNetworkAdapter: HttpResponse-Headers: $headers ")
            LogUtil.d("ImsDCNetworkAdapter: HttpResponse-BodyByteLength: ${responseBodyBytes.size} ")
            HttpStackUtil.printHttpByteArray(response?.headers()?.get("Content-Type"), responseBodyBytes)
            //
            LogUtil.d("ImsDCNetworkAdapter: mHttpRequestCallbacks: $mHttpRequestCallbacks")
            var callback = getFromDcMap(label, mHttpRequestCallbacks)
            if (callback == null) {
                LogUtil.d("ImsDCNetworkAdapter: Error: callback was not found!!! dcLabel: $label mHttpRequestCallbacks: $mHttpRequestCallbacks")
                return;
            }
            // 回调 http 请求数据
            callback.onMessageCallback(
                response.code(),
                response.message(),
                headers, responseBodyBytes
            )
            // 移除请求request
            mHttpRequestCallbacks.remove(label)
            LogUtil.d("ImsDCNetworkAdapter: mHttpRequestCallbacks.remove dcLabel: $label ")
        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.e("ImsDCNetworkAdapter: Exception", e)
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * 更新 Map 中记录的 DC 状态
     */
    private fun updateDcMapStatus(dc: IImsDataChannel, imsDCStatus: ImsDCStatus) {
        LogUtil.d("ImsDCNetworkAdapter: updateDcMapStatus.")
        // 获取dc label
        var dcLabel = Constants.getDcLable(dc);
        /**
         * 获取当前的dc
         */
        var channelPair = getFromDcMap(dcLabel, mDataChannelMap)
        // 更新 imsDCStatus
        if (channelPair != null) {
            val valueState = channelPair.first
            val valueDc = channelPair.second
            if (valueState.toInteger() != imsDCStatus?.toInteger()) {
                mDataChannelMap[dcLabel] = Pair(imsDCStatus, valueDc)
            }
        }
    }

    /**
     * 取消dc数据的回调
     */
    private fun unregisterDcObserver(dc: IImsDataChannel) {
        LogUtil.d("ImsDCNetworkAdapter: unregisterDcObserver.")
        val dcLabel = Constants.getDcLable(dc)
        // 查找对应的dc
        var channelPair = getFromDcMap(dcLabel, mDataChannelMap)
        if (channelPair != null) {
            val dc = channelPair.second
            dc.unregisterObserver()
        }
        // app dc
        if (!Constants.isBootDc(dc)) {
            // invoke dc closing callback
            if (mCloseDcCallback != null) {
                mCloseDCResultMaps.forEach {
                    if (it.containsKey(dc.dcLabel)) {
                        it[dc.dcLabel] = CLOSE_DC_SUCCESS
                    }
                    // 全部默认值被替换代表本组全部返回结果
                    if (!it.containsValue(DEFAULT_STATE)) {
                        mCloseDcCallback?.onResult(Results(it))
                        mCloseDcCallback = null
                        mCloseDCResultMaps.remove(it)
                    }
                }
            }
        }
    }

    override fun startARAbility(slotId: Int, callId: String, callback: Callback<Results<Int>>?) {
        LogUtil.d("ImsDCNetworkAdapter: startARAbility.")
//        mImsDcManager.startARCall(object : IImsARCallCallback.Stub() {
//            override fun onStartCallback(status: Int) {
//                callback?.onResult(Results.success(status))
//            }
//
//            override fun onStopCallback(status: Int) {
//            }
//        }, slotId, callId)
    }

    override fun stopARAbility(slotId: Int, callId: String, callback: Callback<Results<Int>>?) {
        LogUtil.d("ImsDCNetworkAdapter: stopARAbility.")
//        mImsDcManager.stopARCall(object : IImsARCallCallback.Stub() {
//            override fun onStartCallback(status: Int) {
//            }
//
//            override fun onStopCallback(status: Int) {
//                callback?.onResult(Results.success(status))
//            }
//        }, slotId, callId)
    }

    override fun setARCallback(callback: ARAdapter.ARCallback) {
        LogUtil.d("ImsDCNetworkAdapter: setARCallback.")
        mARCallCallback = callback
    }

    /**
     * check given label in cached dc map.
     * @param label orig_appId_type_name
     * @param map cached map with dc label as key
     * @param strict check origin or not. default set to true
     */
    private fun isDcMapContainsLabel(label: String, map: Map<String, Any>, strict: Boolean = true): Boolean {
        return if (strict) {
            map.containsKey(label)
        } else {
            val cleanLabel = getLabelDecorator().removeOrigin(label)
            map.keys.any { it.endsWith(cleanLabel) }
        }
    }

    /**
     * retrieve dc(or callback) from cached dc map
     * @param label orig_appId_type_name
     * @param map cached map with dc label as key
     * @param strict check origin or not. default set to true
     */
    private fun <T> getFromDcMap(label: String, map: Map<String, T>, strict: Boolean = true): T? {
        val ret = if (strict) {
            map[label]
        } else {
            val cleanLabel = getLabelDecorator().removeOrigin(label)
            map.entries.singleOrNull { it.key.endsWith(cleanLabel) }?.value
        }
        LogUtil.v("ImsDCNetworkAdapter: getFromDcMap. label=$label, ret=$ret")
        return ret
    }
}