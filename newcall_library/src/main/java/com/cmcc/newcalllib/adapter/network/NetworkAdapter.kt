package com.cmcc.newcalllib.adapter.network

import android.content.Context
import android.os.Handler
import android.os.Message
import com.cmcc.newcalllib.adapter.network.data.NetworkConfig
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.tool.constant.Constants
import com.cmcc.newcalllib.tool.constant.ErrorCode
import com.cmcc.newcalllib.tool.constant.EventType
import java.io.File
import java.nio.ByteBuffer

/**
 * manage DC, use thread pool to send/retrieve data on net
 * @author jihongfei
 * @createTime 2022/2/22 15:01
 */
abstract class NetworkAdapter(
    protected val mHandler: Handler,
    protected val mNetworkConfig: NetworkConfig,
    protected val mLabelDecorator: LabelDecorator
) {
    companion object {
        const val STATUS_CODE_OK = 200
        const val STATUS_CODE_NO_MODIFY = 304
        const val STATUS_CODE_BAD_REQUEST = 400
    }

    protected var mDataObserver: DataObserver? = null
    protected var mDataChannelCallback: DataChannelCallback? = null
    protected var mDataInterceptors: MutableList<DataInterceptor> = mutableListOf()

    /**
     * create necessary http engine, data channels
     */
    abstract fun init(context: Context)

    /**
     * check specified data channel is connected
     */
    abstract fun isDataChannelAvailable(label: String): Boolean

    /**
     * release and unbind data channel service
     */
    abstract fun release()

    /**
     * create DC
     *
     * @param slotId 默认是0；卡1  slotid=0；卡2 slotid=1;
     */
    abstract fun createDataChannel(
        dcLabels: List<String>,
        dcDescription: String,
        slotId: Int,
        callId: String,
        callback: Callback<Results<Map<String, Int?>>>?
    )

    /**
     * close DC
     *
     * @param slotId 默认是0；卡1  slotid=0；卡2 slotid=1;
     */
    abstract fun closeDataChannel(
        dcLabels: List<String>,
        slotId: Int,
        callId: String,
        callback: Callback<Results<Map<String, Int>>>?
    )

    /**
     * respond to dc setup request from remote
     */
    abstract fun respondDataChannelSetupRequest(
        dcLabels: Array<String>,
        accepted: Array<Boolean>,
        slotId: Int,
        callId: String
    )

    /**
     * send http get request through BootStrap DataChannel
     *
     * GET /?Terminal_Vendor=UNISOC&Terminal_Model=ums9620_2h10_native HTTP/1.1
     * If-None-Match: 8790279ffba16de0a7ea673729be2ae6
     */
    fun sendHttpOnBootDC(
        origin: Origin,
        url: String,
        headerMap: Map<String, String>?,
        callback: HttpRequestCallback
    ) {
        // 获取 bdc 请求的 label
        val label = Constants.getBdcLableByOrigin(origin)
        // 通过 bdc 发起http请求
        sendHttpOnDC(label, url, headerMap, callback)
    }

    /**
     * send data through APP DataChannel
     */
    abstract fun sendDataOnAppDC(label: String, data: String, callback: RequestCallback)

    /**
     * send http get request through DataChannel
     *
     * GET /?Terminal_Vendor=UNISOC&Terminal_Model=ums9620_2h10_native HTTP/1.1
     * If-None-Match: 8790279ffba16de0a7ea673729be2ae6
     */
    fun sendHttpOnDC(
        label: String,
        url: String,
        headerMap: Map<String, String>?,
        callback: HttpRequestCallback
    ) {
        sendHttpOnDC(label, url, headerMap, null, null, callback)
    }

    /**
     * send http post request through DataChannel
     *
     * POST /?Terminal_Vendor=UNISOC&Terminal_Model=ums9620_2h10_native HTTP/1.1
     * If-None-Match: 8790279ffba16de0a7ea673729be2ae6
     * Content-Type: application/x-www-form-urlencoded
     * Content-Length: 43
     *
     * formInfo02=jsonData02&formInfo01=jsonData01
     */
    fun sendHttpOnDC(
        label: String,
        url: String,
        headerMap: Map<String, String>?,
        formBodyMap: Map<String, String>?,
        callback: HttpRequestCallback
    ) {
        sendHttpOnDC(label, url, headerMap, formBodyMap, null, callback)
    }

    /**
     * send http post request through DataChannel
     *
     * POST /?Terminal_Vendor=UNISOC&Terminal_Model=ums9620_2h10_native HTTP/1.1
     * If-None-Match: 8790279ffba16de0a7ea673729be2ae6
     * Content-Type: multipart/form-data; boundary=9b459543-901f-415f-a2c6-4ebe962aafe5
     * Content-Length: 205
     *
     * --9b459543-901f-415f-a2c6-4ebe962aafe5
     * Content-Disposition: form-data; name="field1"; filename="123.txt"
     * Content-Type: text/plain
     * Content-Length: 7
     *
     * 123.txt
     * --9b459543-901f-415f-a2c6-4ebe962aafe5--
     */
    fun sendHttpOnDC(
        label: String,
        url: String,
        headerMap: Map<String, String>?,
        formFileList: List<File?>?,
        callback: HttpRequestCallback
    ) {
        sendHttpOnDC(label, url, headerMap, null, formFileList, callback)
    }

    /**
     * send http post request through DataChannel
     *
     * POST /?Terminal_Vendor=UNISOC&Terminal_Model=ums9620_2h10_native HTTP/1.1
     * If-None-Match: 8790279ffba16de0a7ea673729be2ae6
     * Content-Type: multipart/form-data; boundary=869157a6-e8a7-496f-9bf2-5d1d84bf35f6
     * Content-Length: 455
     *
     * --869157a6-e8a7-496f-9bf2-5d1d84bf35f6
     * Content-Disposition: form-data; name="formInfo02"
     * Content-Length: 10
     *
     * jsonData02
     * --869157a6-e8a7-496f-9bf2-5d1d84bf35f6
     * Content-Disposition: form-data; name="formInfo01"
     * Content-Length: 10
     *
     * jsonData01
     * --869157a6-e8a7-496f-9bf2-5d1d84bf35f6
     * Content-Disposition: form-data; name="file1"; filename="123.txt"
     * Content-Type: text/plain
     * Content-Length: 7
     *
     * 123.txt
     * --869157a6-e8a7-496f-9bf2-5d1d84bf35f6--
     */
    abstract fun sendHttpOnDC(
        label: String,
        url: String,
        headerMap: Map<String, String>?,
        formBodyMap: Map<String, String>?,
        formFileList: List<File?>?,
        callback: HttpRequestCallback
    )


    fun getNetworkConfig(): NetworkConfig {
        return mNetworkConfig
    }

    fun getLabelDecorator(): LabelDecorator {
        return mLabelDecorator
    }

    /**
     * send event to handler, including exception/stateChange
     */
    protected fun sendMessageToHandler(msg: Message) {
        mHandler.sendMessage(msg)
    }

    /**
     * send error event
     */
    protected fun sendErrorCode(ec: ErrorCode) {
        sendMessageType(EventType.EXCEPTION, ec)
    }

    /**
     * send message with eventType
     */
    protected fun sendMessageType(mt: EventType, arg: Any?) {
        val msg = Message.obtain()
        msg.what = mt.code()
        msg.obj = arg
        mHandler.sendMessage(msg)
    }

    fun setDataObserver(dataObserver: DataObserver) {
        mDataObserver = dataObserver
    }

    fun setDataChannelCallback(dcCallback: DataChannelCallback) {
        mDataChannelCallback = dcCallback
    }


    /**
     * register data interceptor to handle data in specific DC, and provide the data handle callback
     */
    fun registerDataInterceptor(dataInterceptor: DataInterceptor) {
        if (mDataInterceptors.find { it.provideDataChannelLabel() == dataInterceptor.provideDataChannelLabel() } == null) {
            mDataInterceptors.add(dataInterceptor)
        }
    }

    /**
     * send status callback for basic DC request
     */
    interface RequestCallback {
        fun onSendDataCallback(statusCode: Int, errorCode: Int)
    }

    /**
     * send status and message arrival callback for HTTP request over DC
     */
    interface HttpRequestCallback : RequestCallback {
        fun onMessageCallback(
            status: Int,
            msg: String,
            headers: MutableMap<String, String>?,
            body: ByteArray?
        )
    }

    /**
     * message arrival callback for basic DC request
     */
    interface DataObserver {
        //        fun onDataArrive(data: ByteBuffer, protocol: String);
        fun onDataArrive(label: String, data: ByteBuffer)
    }

    /**
     * Callback to notify DC request or state change
     */
    interface DataChannelCallback {
        fun onBootstrapDataChannelCreated(success: Boolean)

        // create AppDc by remote
        fun onImsDataChannelSetupRequest(dcLabels: Array<String>, slotId: Int, callId: String)
    }

    /**
     *  handle data from DC before deliver to JS.
     *  do not support data in HTTP format
     */
    interface DataInterceptor {
        /**
         * DC label
         */
        fun provideDataChannelLabel(): String

        /**
         * @param data received data
         * @return true if data handled by this interceptor
         */
        fun onDataArrive(data: ByteBuffer): Boolean
    }
}