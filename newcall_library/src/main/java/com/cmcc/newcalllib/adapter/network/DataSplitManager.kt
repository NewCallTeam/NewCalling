package com.cmcc.newcalllib.adapter.network

import com.cmcc.newcalllib.tool.LogUtil
import java.nio.ByteBuffer

class DataSplitManager : DataSplitter, DataAggregator {
    companion object {
        // enable if split&aggregate is needed
        const val ENABLE_SPLIT_AND_AGGREGATE = true
        // enable HTTP split and aggregate
        const val ENABLE_HTTP_SPLIT = true
        // enable Non-HTTP split and aggregate
        const val ENABLE_NON_HTTP_SPLIT = false

        const val SUB_PROTOCOL_HTTP = "HTTP"
        const val SUB_PROTOCOL_DEFAULT = ""
    }

    private val withHeaderSplitter = WithHeaderSplitter()
    private val withHeaderAggregator = WithHeaderAggregator()
    private val httpAggregator = HttpAggregator()
    private val httpSplitter = HttpSplitter()

    override fun split(
        subProtocol: String,
        label: String,
        buffer: ByteBuffer,
        limit: Long,
        callback: (buffer: ByteBuffer) -> Any
    ) {
        if (!ENABLE_SPLIT_AND_AGGREGATE) {
            LogUtil.d("DataSplitManager","split disabled, send directly")
            callback.invoke(buffer)
            return
        }
        LogUtil.d("DataSplitManager", "split on $label, subProtocol=$subProtocol, " +
                "bufferCapacity=${buffer.capacity()}, limit=$limit")
        if (subProtocol.contains(SUB_PROTOCOL_HTTP, true) && ENABLE_HTTP_SPLIT) {
            LogUtil.d("DataSplitManager","httpSplitter")
            // http response
            httpSplitter.split(subProtocol, label, buffer, limit, callback)
        } else if (ENABLE_NON_HTTP_SPLIT) {
            LogUtil.d("DataSplitManager","withHeaderSplitter")
            withHeaderSplitter.split(subProtocol, label, buffer, limit, callback)
        } else {
            LogUtil.d("DataSplitManager","no split, send directly")
            withHeaderSplitter.split(subProtocol, label, buffer, limit, callback)
        }
    }

    /**
     * 合并 http 分片数据
     */
    override fun aggregate(
        subProtocol: String,
        dcLabel: String,
        byteBuffer: ByteBuffer,
        callback: (buffer: ByteBuffer) -> Any
    ) {
        LogUtil.d("DataSplitManager", "aggregate. dcLabel=$dcLabel, subProtocol=$subProtocol")
        if (!ENABLE_SPLIT_AND_AGGREGATE) {
            LogUtil.d("DataSplitManager","disable aggregate, invoke directly")
            callback.invoke(byteBuffer)
            return
        }
        LogUtil.d("DataSplitManager","aggregate on $dcLabel, subProtocol=$subProtocol, bufferCapacity=${byteBuffer.capacity()}")
        if (subProtocol.contains(SUB_PROTOCOL_HTTP, true) && ENABLE_HTTP_SPLIT) {
            LogUtil.d("DataSplitManager","httpAggregator")
            // http response
            httpAggregator.aggregate(subProtocol, dcLabel, byteBuffer, callback)
        } else if (ENABLE_NON_HTTP_SPLIT) {
            LogUtil.d("DataSplitManager","withHeaderAggregator")
            withHeaderAggregator.aggregate(subProtocol, dcLabel, byteBuffer, callback)
        } else {
            LogUtil.d("DataSplitManager","no aggregate, invoke directly")
            callback.invoke(byteBuffer)
        }
    }

}