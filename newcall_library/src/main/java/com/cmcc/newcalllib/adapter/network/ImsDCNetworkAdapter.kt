package com.cmcc.newcalllib.adapter.network

import android.content.Context
import android.os.Handler
import android.os.RemoteException
import com.cmcc.newcalllib.adapter.ar.aidl.ARAdapter
import com.cmcc.newcalllib.adapter.network.data.NetworkConfig
import com.cmcc.newcalllib.datachannel.*
import com.cmcc.newcalllib.dc.httpstack.HttpStack
import com.cmcc.newcalllib.dc.httpstack.decode.response.HttpStackHeaders
import com.cmcc.newcalllib.dc.httpstack.utils.HttpStackBufferUtil
import com.cmcc.newcalllib.dc.httpstack.utils.HttpStackUrlUtil
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.ConfigManager
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.constant.Constants
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*


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

    // keep DataChannel（key: dcLabel , value: Pair<ImsDCStatus, IImsDataChannel>）
    private val mDataChannelMap: MutableMap<String, Pair<ImsDCStatus, IImsDataChannel>> =
        mutableMapOf()
    // http 请求的request（key: dcLabel , value: callback）
    private val mHttpRequestCallbacks = mutableMapOf<String, HttpRequestCallback>()

    // keep callbacks of DC creation/close
    private val mCreateDCCallbacks: MutableMap<String, Callback<Results<Pair<String, Int>>>> =
        mutableMapOf()
    private val mCloseDCCallbacks: MutableMap<String, Callback<Results<Pair<String, Int>>>> =
        mutableMapOf()

    // 当前使用的sim卡
    private var mCurrSlotId = 0
    private var mCallId = ""


    // callback for arCall
    private var mARCallCallback: ARAdapter.ARCallback? = null

    private var mDataChannelSplitter: DataSplitter? = null
    private var mDataChannelAggregator: DataAggregator? = null

    companion object {
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
        LogUtil.d("ImsDCNetworkAdapter", "init.");
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
                LogUtil.d(
                    "ImsDCNetworkAdapter", "onServiceConnected, mCurrSlotId="
                            + mCurrSlotId + ", mCallId=" + mCallId
                )
                // "SDK进程向"向"IMS进程"设置监听：监听DC的建立、DC状态变化、对端建立DC的请求
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
        LogUtil.d("ImsDCNetworkAdapter", "release.");
        mDataChannelMap.clear()
        mCreateDCCallbacks.clear()
        mCloseDCCallbacks.clear()
        mDataInterceptors.clear()
        mImsDcManager.unbindImsDCService()
    }

    /**
     * SDK进程 通知 IMS进程：创建dc通道（一般用于创建App DC通道）
     */
    override fun createDataChannel(
        dcLabels: List<String>,
        dcDescription: String,
        slotId: Int,
        callId: String,
        callback: Callback<Results<Pair<String, Int>>>?
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "createDataChannel.")
        LogUtil.d("ImsDCNetworkAdapter", "dcLabels size: " + dcLabels?.size);
        val newDcLabels = mutableListOf<String>()
        dcLabels.forEach {
            if (mDataChannelMap.containsKey(it)) {
                LogUtil.d("ImsDCNetworkAdapter", "mDataChannelMap.containsKey(it)")
                if (mDataChannelMap[it]?.second?.state == ImsDCStatus.DC_STATE_CONNECTING) {
                    callback?.onResult(Results(Pair(it, CREATE_DC_ALREADY)))
                } else if (mDataChannelMap[it]?.second?.state == ImsDCStatus.DC_STATE_OPEN) {
                    callback?.onResult(Results(Pair(it, CREATE_DC_SUCCESS)))
                } else {
                    callback?.onResult(Results(Pair(it, CREATE_DC_FAIL)))
                }
            } else {
                LogUtil.d("ImsDCNetworkAdapter", "newDcLabels.add(it)")
                newDcLabels.add(it)
            }
        }
        if (newDcLabels.isEmpty()) {
            LogUtil.d("ImsDCNetworkAdapter", "newDcLabels isEmpty")
            return
        }
        // register listener for DC creation
        callback?.apply {
            newDcLabels.forEach {
                mCreateDCCallbacks[it] = this
            }
        }
        //TODO:fix later.liufeng
        // 创建dc通道
        mImsDcManager.createImsDc(newDcLabels.toTypedArray(), slotId, callId, null)
    }

    /**
     * SDK进程 通知 IMS进程：关闭dc通道（一般用于关闭App DC通道）
     */
    override fun closeDataChannel(
        dcLabels: List<String>,
        slotId: Int,
        callId: String,
        callback: Callback<Results<Pair<String, Int>>>?
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "closeDataChannel.")
        LogUtil.d("ImsDCNetworkAdapter", "closeDataChannel, before $mDataChannelMap")
        // register listener for DC close
        val newImsDataChannels = mutableListOf<String>()
        dcLabels.forEach {
            if (mDataChannelMap.containsKey(it)) {
                if (mDataChannelMap[it]?.second?.state == ImsDCStatus.DC_STATE_CLOSING) {
                    callback?.onResult(Results(Pair(it, CLOSE_DC_ALREADY)))
                    mDataChannelMap.remove(it)
                } else if (mDataChannelMap[it]?.second?.state == ImsDCStatus.DC_STATE_CLOSED) {
                    callback?.onResult(Results(Pair(it, CLOSE_DC_SUCCESS)))
                    mDataChannelMap.remove(it)
                } else {
                    newImsDataChannels.add(it)
                }
            } else {
                callback?.onResult(Results(Pair(it, CLOSE_DC_SUCCESS)))
            }
        }
        if (newImsDataChannels.isEmpty()) {
            LogUtil.d("ImsDCNetworkAdapter", "newImsDataChannels isEmpty")
            return
        }
        callback?.apply {
            newImsDataChannels.forEach {
                mCloseDCCallbacks[it] = this
            }
        }
        // 关闭dc通道
        val filterKeys = mDataChannelMap.filterKeys { it in newImsDataChannels }
        mImsDcManager.deleteImsDc(
            filterKeys.map { it.value.second.dcLabel }.toTypedArray(),
            slotId,
            callId
        )

        // update map
        filterKeys.keys.forEach(mDataChannelMap::remove)
        LogUtil.d("ImsDCNetworkAdapter", "closeDataChannel, after $mDataChannelMap")
    }

    /**
     * SDK进程 通知 IMS进程：同意建立DC通道
     */
    override fun respondDataChannelSetupRequest(
        dcLabels: Array<String>,
        accepted: Array<Boolean>,
        slotId: Int,
        callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "respondDataChannelSetupRequest. dcLabels: $dcLabels accepted: $accepted slotId: $slotId callId: $callId")
        // 调用IMS进程提供的接口，同意建立DC通道
        mImsDcManager.responseImsDcSetupRequest(
            dcLabels,
            accepted.toBooleanArray(), slotId, callId
        )
    }

    /**
     * SDK进程 通知 IMS进程：发起http请求（只适用于bootstrap dc使用http协议请求网络数据）
     */
    override fun sendHttpGet(
        label: String,
        url: String,
        headers: Map<String, String>,
        callback: HttpRequestCallback
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "sendHttpGet. label: $label url: $url headers: $headers")
        if (mHttpRequestCallbacks.containsKey(label)) {
            LogUtil.e("ImsDCNetworkAdapter", "Repeated request. label: $label url: $url", null)
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
        var channelPair = mDataChannelMap[label]
        val state = channelPair?.first
        val dc = channelPair?.second
        LogUtil.d("ImsDCNetworkAdapter", "mDataChannelMap：$mDataChannelMap")
        LogUtil.d("ImsDCNetworkAdapter", "dcInfo: dcLabel: ${dc?.dcLabel} " +
                "+ dcStreamId: ${dc?.streamId}+ dcType: ${dc?.dcType}+ dcState: ${dc?.state}" +
                "+ dcSubProtocol: ${dc?.subProtocol}+ dcCallId: ${dc?.callId}" +
                "+ dcBufferedAmount: ${dc?.bufferedAmount()}")
        /**
         * 获取http请求消息体
         */
        // 获取http请求 消息体
        val httpByteBuffer = HttpStack.encodeHttpGetRequest(url, headers)
        // 发送http请求
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
            doSendByteBuffer(dc, httpByteBuffer,callback)
        }
    }

    /**
     * 发送文本数据
     */
    override fun sendDataOverAppDc(label: String, data: String, callback: RequestCallback) {
        val byteBuffer = HttpStackBufferUtil.getByteBuffer(data)
        LogUtil.d("ImsDCNetworkAdapter", "sendDataOverAppDc. label：$label data: $data")
        /**
         * 获取当前的dc
         */
        var channelPair = mDataChannelMap[label]
        val dcStatus = channelPair?.first
        val dc = channelPair?.second
        LogUtil.d("ImsDCNetworkAdapter", "mDataChannelMap：$mDataChannelMap")
        LogUtil.d("ImsDCNetworkAdapter", "dcInfo: dcLabel: ${dc?.dcLabel} " +
                "+ dcStreamId: ${dc?.streamId}+ dcType: ${dc?.dcType}+ dcState: ${dc?.state}" +
                "+ dcSubProtocol: ${dc?.subProtocol}+ dcCallId: ${dc?.callId}" +
                "+ dcBufferedAmount: ${dc?.bufferedAmount()}")
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

    /**
     * 发送文件
     */
    @Deprecated("this function is deprecated：There is an error in the method implementation！")
    override fun sendFileOverAppDc(label: String, file: File, callback: RequestCallback) {
        LogUtil.d("ImsDCNetworkAdapter", "sendFile.")
        LogUtil.d("ImsDCNetworkAdapter", "label：$label file: ${file.path}");
    }

    private fun doSendByteBuffer(
        dc: IImsDataChannel?,
        byteBuffer: ByteBuffer,
        requestCallback: RequestCallback?
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "doSendByteBuffer")
        val data = byteBuffer.array()
        LogUtil.d("ImsDCNetworkAdapter", "buffer.size: ${data.size}")
        dc?.send(data, data.size, object : IDCSendDataCallback.Stub() {
            @Throws(RemoteException::class)
            override fun onSendDataResult(state: Int, errorcode: Int) {
                LogUtil.d("ImsDCNetworkAdapter", "onSendDataResult: state：$state errorcode：$errorcode")
                // 回调发送状态
                requestCallback?.onSendDataCallback(state, errorcode)
            }
        })
    }

    /**
     * SDK进程 通知 IMS进程：dc 可用状态查询
     */
    override fun isDataChannelAvailable(label: String): Boolean {
        LogUtil.d("ImsDCNetworkAdapter", "isDataChannelAvailable label：$label")
        /**
         * 获取当前的dc
         */
        var channelPair = mDataChannelMap[label]
        val dcType = channelPair?.first
        val dc = channelPair?.second
        LogUtil.d("ImsDCNetworkAdapter", "mDataChannelMap：$mDataChannelMap")
        LogUtil.d("ImsDCNetworkAdapter", "channelPair：$channelPair")
        LogUtil.d("ImsDCNetworkAdapter", "dcType：$dcType")
        LogUtil.d("ImsDCNetworkAdapter", "dc：$dc")
        if (dc?.state == ImsDCStatus.DC_STATE_OPEN) {
            return true;
        }
        return false
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 内部类
     *
     * "SDK进程向"向"IMS进程"设置监听：监听DC的建立、DC状态变化、对端建立DC的请求
     */
    inner class ImsDcCallback : IImsDataChannelCallback.Stub() {
        override fun onImsDataChannelSetupRequest(
            dcLabels: Array<String>,
            slotId: Int,
            callId: String
        ) {
            LogUtil.d("ImsDCNetworkAdapter", "onImsDataChannelSetupRequest. dcLabels: $dcLabels slotId: $slotId callId: $callId")
            // IMS进程 通知 SDK进程：对方要求建立dc通道的请求
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
            LogUtil.d("ImsDCNetworkAdapter", "onBoostrapDataChannelResponse. slotId: $slotId callId: $callId")
            LogUtil.d("ImsDCNetworkAdapter", "dcLabel: ${dc?.dcLabel} " +
                    "+ dcStreamId: ${dc?.streamId}+ dcType: ${dc?.dcType}+ dcState: ${dc?.state}" +
                    "+ dcSubProtocol: ${dc?.subProtocol}+ dcCallId: ${dc?.callId}" +
                    "+ dcBufferedAmount: ${dc?.bufferedAmount()}")
            // IMS进程 通知 SDK进程：Bootstrap DC的建立、DC状态变化
            if (dc != null) {
                this@ImsDCNetworkAdapter.onImsBootDcStatusCallback(dc,dc.state,slotId, callId)
            }
        }

        override fun onApplicationDataChannelResponse(
            dc: IImsDataChannel?,
            slotId: Int,
            callId: String
        ) {
            LogUtil.d("ImsDCNetworkAdapter", "onApplicationDataChannelResponse. slotId: $slotId callId: $callId")
            LogUtil.d("ImsDCNetworkAdapter", "dcLabel: ${dc?.dcLabel} " +
                    "+ dcStreamId: ${dc?.streamId}+ dcType: ${dc?.dcType}+ dcState: ${dc?.state}" +
                    "+ dcSubProtocol: ${dc?.subProtocol}+ dcCallId: ${dc?.callId}" +
                    "+ dcBufferedAmount: ${dc?.bufferedAmount()}")
            // IMS进程 通知 SDK进程：Application DC的建立、DC状态变化
            if (dc != null) {
                this@ImsDCNetworkAdapter.onImsAppDcStatusCallback(dc,dc.state,slotId,callId)
            }
        }
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * IMS进程 通知 SDK进程：对端要求建立DC数据通道
     */
    private fun onImsDcSetupRequest(
        dcLabels: Array<String>, slotId: Int, callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "onImsDcSetupRequest. dcLabels: $dcLabels slotId: $slotId callId: $callId ")
        if (dcLabels == null) {
            LogUtil.e("ImsDCNetworkAdapter", "dcLabels.isNullOrEmpty()", null)
            return
        }
        // 1、IMS进程 通知 SDK进程：呼叫方要求建立一个APP DC 数据通道；
        // 2、这里调用SDK进程 onImsDataChannelSetupRequest 咨询SDK进程，是否同意；
        // 3、若同意 SDK进程 调用 IMS进程方法 respondDataChannelSetupRequest
        // 4、IMS进程 创建APP DC，并回调DC
        mDataChannelCallback?.onImsDataChannelSetupRequest(dcLabels, slotId, callId)
    }


    /**
     * IMS进程 通知 SDK进程：Bootstrap DC的建立、DC状态变化
     * @param state CONNECTING = 0 , OPEN = 1, CLOSING = 2, CLOSED = 3;
     */
    private fun onImsBootDcStatusCallback(
        dc: IImsDataChannel, status: ImsDCStatus, slotId: Int, callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "onImsBootDcStatusCallback. status: $status slotId: $slotId callId: $callId")
        LogUtil.d("ImsDCNetworkAdapter", "dcLabel: ${dc?.dcLabel} " +
                "+ dcStreamId: ${dc?.streamId}+ dcType: ${dc?.dcType}+ dcState: ${dc?.state}" +
                "+ dcSubProtocol: ${dc?.subProtocol}+ dcCallId: ${dc?.callId}" +
                "+ dcBufferedAmount: ${dc?.bufferedAmount()}")
        // 回调DC状态
        when (status) {
            // CONNECTING
            ImsDCStatus.DC_STATE_CONNECTING -> {
                LogUtil.d("ImsDCNetworkAdapter", "BDC CONNECTING.")
                registerBdcMsgObserver(dc, status, slotId, callId)
            }
            // OPEN
            ImsDCStatus.DC_STATE_OPEN -> {
                LogUtil.d("ImsDCNetworkAdapter", "BDC OPEN.")
                // 展锐芯片MT端：一般不回调Creating，直接回调 Created（MO端仍然回调Creating）
                registerBdcMsgObserver(dc, status, slotId, callId)
                // notify bootstrap dc created. react only on local BDC
                if (dc.streamId == Constants.BOOTSTRAP_DATA_CHANNEL_STREAM_ID_LOCAL) {
                    mDataChannelCallback?.onBootstrapDataChannelCreated(true)
                }
            }
            // CLOSING
            ImsDCStatus.DC_STATE_CLOSING -> LogUtil.d("ImsDCNetworkAdapter", "BDC CLOSING")
            // CLOSED
            ImsDCStatus.DC_STATE_CLOSED -> {
                LogUtil.d("ImsDCNetworkAdapter", "BDC CLOSED.")
                // notify bootstrap dc Closed
                val label = Constants.getBdcLableByStreamId(dc.streamId)
                unregisterDcObserver(label)
            }
            else -> { // 异常情况
                LogUtil.e("ImsDCNetworkAdapter", "onImsBootDcStatusCallback state error：$status", null)
            }
        }
    }


    /**
     * IMS进程 通知 SDK进程：Application DC的建立、DC状态变化
     * @param state CONNECTING = 0 , OPEN = 1, CLOSING = 2, CLOSED = 3;
     */
    private fun onImsAppDcStatusCallback(
        dc: IImsDataChannel, status: ImsDCStatus, slotId: Int, callId: String
    ) {
        LogUtil.d("ImsDCNetworkAdapter", "onImsAppDcStatusCallback. status: $status slotId: $slotId callId: $callId")
        LogUtil.d("ImsDCNetworkAdapter", "dcLabel: ${dc?.dcLabel} " +
                "+ dcStreamId: ${dc?.streamId}+ dcType: ${dc?.dcType}+ dcState: ${dc?.state}" +
                "+ dcSubProtocol: ${dc?.subProtocol}+ dcCallId: ${dc?.callId}" +
                "+ dcBufferedAmount: ${dc?.bufferedAmount()}")
        // 回调DC状态
        when (status) {
            // CONNECTING
            ImsDCStatus.DC_STATE_CONNECTING -> {
                LogUtil.d("ImsDCNetworkAdapter", "AppDc CONNECTING.")
                registerAppDcMsgObserver(dc,status, slotId, callId)
            }
            // OPEN
            ImsDCStatus.DC_STATE_OPEN -> {
                LogUtil.d("ImsDCNetworkAdapter", "AppDc OPEN.")
                // 展锐芯片MT端：一般不回调Creating，直接回调 Created（MO端仍然回调Creating）
                registerAppDcMsgObserver(dc,status, slotId, callId)
                // change by xiaxl：主动创建的app dc
                if(mCreateDCCallbacks.containsKey(dc.dcLabel)){
                    mCreateDCCallbacks[dc.dcLabel]?.onResult(Results(Pair(dc.dcLabel, CREATE_DC_SUCCESS)))
                    mCreateDCCallbacks.remove(dc.dcLabel)
                }
                // change by xiaxl：被动由芯片创建的 app dc (或者说是远端创建的dc，比如：屏幕共享)
                else{
                    val labelList: MutableList<String> = ArrayList()
                    labelList.add(dc.dcLabel)
                    mDataChannelCallback?.onImsDataChannelSetupRequest(labelList.toTypedArray(),-1,"")
                }
            }
            // CLOSING
            ImsDCStatus.DC_STATE_CLOSING -> LogUtil.d("ImsDCNetworkAdapter", "AppDc CLOSING")
            // CLOSED
            ImsDCStatus.DC_STATE_CLOSED -> {
                LogUtil.d("ImsDCNetworkAdapter", "AppDc CLOSED.")
                // notify bootstrap dc Closed
                unregisterDcObserver(dc.dcLabel)
                // invoke dc creating callback
                mCloseDCCallbacks[dc.dcLabel]?.onResult(Results(Pair(dc.dcLabel, CLOSE_DC_SUCCESS)))
                mCloseDCCallbacks.remove(dc.dcLabel)
            }
            else -> { // 异常情况
                LogUtil.e("ImsDCNetworkAdapter", "onImsAppDcStatusCallback state error：$status", null)
            }
        }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 注册 Bdc 数据回调
     */
    private fun registerBdcMsgObserver(dc: IImsDataChannel, imsDCStatus: ImsDCStatus, slotId: Int, callId: String){
        LogUtil.d("ImsDCNetworkAdapter", "registerBdcMsgObserver.")
        val label = Constants.getBdcLableByStreamId(dc.streamId)
        LogUtil.d("ImsDCNetworkAdapter", "getBdcLableByStreamId label=$label")
        if (!mDataChannelMap.containsKey(label)) {
            // 保存 BDC
            mDataChannelMap[label] = Pair(imsDCStatus, dc)
            // 注册 BDC 数据、状态变化的监听方法
            dc.registerObserver(object : IImsDCObserver.Stub() {
                override fun onDataChannelStateChange(status: ImsDCStatus) {
                    // bdc状态变化
                    LogUtil.d("ImsDCNetworkAdapter", "BootDc ImsDCObserver.onDataChannelStateChange. status：$status status：${status.toInteger()}")
                    onImsBootDcStatusCallback(dc, status, slotId, callId)
                }

                override fun onMessage(data: ByteArray?, length: Int) {
                    // bdc返回的消息数据
                    LogUtil.d("ImsDCNetworkAdapter", "BootDc ImsDCObserver.onMessage. data：$data ,length：$length")
                    LogUtil.d("ImsDCNetworkAdapter", "data.size：${data?.size}")
                    LogUtil.d("ImsDCNetworkAdapter", "dcLabel: ${dc.dcLabel} " +
                            "+ streamId: ${dc.streamId}+ dcType: ${dc.dcType}+ state: ${dc.state}" +
                            "+ subProtocol: ${dc.subProtocol}+ callId: ${dc.callId}" +
                            "+ bufferedAmount: ${dc.bufferedAmount()}")
                    //
                    val byteBuffer = ByteBuffer.wrap(data)
                    val callback: (buffer: ByteBuffer) -> Any = { aggregated ->
                        // 回调Response数据
                        onHttpGetResponse(label, aggregated)
                    }
                    if (mDataChannelAggregator != null) {
                        mDataChannelAggregator!!.aggregate(
                            dc.subProtocol,
                            dc.dcLabel,
                            byteBuffer,
                            callback
                        )
                    } else {
                        callback.invoke(byteBuffer)
                    }
                }
            })
        }
    }

    /**
     * 注册 Adc 数据回调
     */
    private fun registerAppDcMsgObserver(dc: IImsDataChannel, imsDCStatus: ImsDCStatus, slotId: Int, callId: String){
        LogUtil.d("ImsDCNetworkAdapter", "registerAppDcMsgObserver.")
        // application dc
        val label = dc.getDcLabel()
        if (!mDataChannelMap.containsKey(label)) {
            // 保存 ADC
            mDataChannelMap[label] = Pair(imsDCStatus, dc)
            // 注册 BDC 数据、状态变化的监听方法
            dc.registerObserver(object : IImsDCObserver.Stub() {
                override fun onDataChannelStateChange(status: ImsDCStatus) {
                    // dc状态变化
                    LogUtil.d("ImsDCNetworkAdapter", "AppDc onDataChannelStateChange. state：$status state：${status.toInteger()}")
                    onImsAppDcStatusCallback(dc, status, slotId, callId)
                }

                override fun onMessage(data: ByteArray?, length: Int) {
                    LogUtil.d("ImsDCNetworkAdapter", "AppDc ImsDCObserver.onMessage. data：$data length：$length")
                    LogUtil.d("ImsDCNetworkAdapter", "dcLabel: ${dc.dcLabel} " +
                            "+ streamId: ${dc.streamId}+ dcType: ${dc.dcType}+ state: ${dc.state}" +
                            "+ subProtocol: ${dc.subProtocol}+ callId: ${dc.callId}" +
                            "+ bufferedAmount: ${dc.bufferedAmount()}")
                    val byteBuffer = ByteBuffer.wrap(data)
                    var handled = false
                    val callback: (buffer: ByteBuffer) -> Any = { aggregated ->
                        // handled by interceptor
                        mDataInterceptors
                            .filter { it.provideDataChannelLabel() == dc.dcLabel }
                            .forEach {
                                if (!handled) {
                                    handled = it.onDataArrive(aggregated)
                                }
                            }
                        // 回调请求数据
                        if (dc.subProtocol.contains(DataSplitManager.SUB_PROTOCOL_HTTP, true)) {
                            // 回调Response数据
                            onHttpGetResponse(dc.dcLabel, aggregated)
                        } else {
                            if (!handled) {
                                mDataObserver?.onDataArrive(dc.dcLabel, aggregated)
                            }
                        }
                    }
                    if (mDataChannelAggregator != null) {
                        mDataChannelAggregator!!.aggregate(
                            dc.subProtocol,
                            dc.dcLabel,
                            byteBuffer,
                            callback
                        )
                    } else {
                        callback.invoke(byteBuffer)
                    }
                }
            })
        }
    }

    /**
     * 回调 http1.1 Get Response 请求数据
     */
    private fun onHttpGetResponse(label: String, buffer: ByteBuffer) {
        LogUtil.d("ImsDCNetworkAdapter", "onHttpGetResponse label：$label")
        /**
         * 从二进制字节流中解析 Http Response 消息数据
         */
        var response = HttpStack.decodeHttpResponse(buffer);
        LogUtil.d("ImsDCNetworkAdapter", "response：$response ")
        // header 其他header信息
        val responseHeaders: HttpStackHeaders = response.headers()
        val headerMap = HttpStackUrlUtil.getHeaderMap(responseHeaders)
        LogUtil.d("ImsDCNetworkAdapter", "responseHeaders：$responseHeaders ")
        LogUtil.d("ImsDCNetworkAdapter", "headerMap：$headerMap ")
        val responseBody = response.body()
        val responseBodyBytes = responseBody.bytes()
        LogUtil.d("ImsDCNetworkAdapter", "responseBody：${responseBody} ")
        LogUtil.d("ImsDCNetworkAdapter", "responseBodyBytes：${responseBodyBytes} ")
        LogUtil.d("ImsDCNetworkAdapter", "responseBodyBytes.size：${responseBodyBytes.size} ")
        try {
            // 回调 http 请求数据
            mHttpRequestCallbacks[label]?.onMessageCallback(
                response.code(),
                response.message(),
                headerMap, responseBodyBytes
            )
            // 移除请求request
            mHttpRequestCallbacks.remove(label)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LogUtil.d("ImsDCNetworkAdapter", "mHttpRequestCallbacks remove dcLabel：$label ")
    }


    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * 取消dc数据的回调
     */
    private fun unregisterDcObserver(dcLabel: String) {
        // 查找对应的dc
        var channelPair = mDataChannelMap[dcLabel]
        val dc = channelPair?.second
        // notify bootstrap dc Closed
        dc?.unregisterObserver()
    }

    override fun startARAbility(slotId: Int, callId: String, callback: Callback<Results<Int>>?) {
        LogUtil.d("ImsDCNetworkAdapter", "startARAbility.")
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
        LogUtil.d("ImsDCNetworkAdapter", "stopARAbility.")
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
        LogUtil.d("ImsDCNetworkAdapter", "setARCallback.")
        mARCallCallback = callback
    }
}