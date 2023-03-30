package com.cmcc.newcalllib.adapter.network

import com.cmcc.newcalllib.tool.LogUtil
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

class WithHeaderAggregator : DataAggregator {
    override fun aggregate(
        subProtocol: String,
        dcLabel: String,
        byteBuffer: ByteBuffer,
        callback: (buffer: ByteBuffer) -> Any
    ) {
        // TODO("WithHeaderAggregator not yet implemented")
    }

}