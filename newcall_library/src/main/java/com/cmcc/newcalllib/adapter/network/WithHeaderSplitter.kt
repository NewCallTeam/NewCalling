package com.cmcc.newcalllib.adapter.network

import com.cmcc.newcalllib.tool.LogUtil
import java.nio.ByteBuffer

/**
 * wrap original data with headers. ref RFC-4975 MSRP?
 *
 */
class WithHeaderSplitter : DataSplitter {

    override fun split(subProtocol: String, label: String, buffer: ByteBuffer,
                       limit: Long, callback: (buffer: ByteBuffer) -> Any) {
        buffer.rewind()
        var chunkContainer: ByteArray
        val limitSizeArr = ByteArray(limit.toInt())
        do {
            chunkContainer = if (limit > buffer.remaining()) {
                // consider last chunk
                ByteArray(buffer.remaining())
            } else {
                limitSizeArr
            }
            // split
            buffer[chunkContainer, 0, chunkContainer.size]
            // wrap to byteBuffer
            val stringBuilder = StringBuilder()

            // TODO request line and headers
//            stringBuilder.append(String(chunkContainer))
            // body
            stringBuilder.append(String(chunkContainer))

            callback(ByteBuffer.wrap(stringBuilder.toString().toByteArray()))
        } while (buffer.hasRemaining())
    }

}