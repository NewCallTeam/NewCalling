package com.cmcc.newcalllib.adapter.network

import java.nio.ByteBuffer

class HttpSplitter : DataSplitter {

    override fun split(subProtocol: String, label: String, buffer: ByteBuffer,
                       limit: Long, callback: (buffer: ByteBuffer) -> Any) {
        // 倒回到缓冲区的初始为止
        buffer.rewind()
        // 创建分块数组
        var chunkContainer: ByteArray
        // 创建最大长度限制数组
        val limitSizeArr = ByteArray(limit.toInt())
        do {
            // 创建分块数组（创建一个长度小于等于 limit 的空数组）
            chunkContainer = if (limit > buffer.remaining()) {
                // consider last chunk
                ByteArray(buffer.remaining())
            } else {
                limitSizeArr
            }
            // 从buffer中截取对应长度的字节数据
            buffer[chunkContainer, 0, chunkContainer.size]
            // wrap byteBuffer
            callback(ByteBuffer.wrap(chunkContainer))
        } while (buffer.hasRemaining())
    }

}