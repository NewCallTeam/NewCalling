package com.cmcc.newcalllib.adapter.network

import java.nio.ByteBuffer

class HttpSplitter : DataSplitter {

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
            // wrap byteBuffer
            callback(ByteBuffer.wrap(chunkContainer))
        } while (buffer.hasRemaining())
    }

}