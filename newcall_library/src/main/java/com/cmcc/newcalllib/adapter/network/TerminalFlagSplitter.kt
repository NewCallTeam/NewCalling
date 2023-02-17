package com.cmcc.newcalllib.adapter.network

import com.cmcc.newcalllib.tool.LogUtil
import java.nio.ByteBuffer

class TerminalFlagSplitter : DataSplitter {
    companion object {
        val TERM_FLAG = "@END\$".toByteArray()
    }

    override fun split(subProtocol: String, label: String, buffer: ByteBuffer, limit: Long, callback: (buffer: ByteBuffer) -> Any) {
        LogUtil.d("split, dcLabel=$label")
        val allByteBuffer = ByteBuffer.allocate(buffer.capacity() + TERM_FLAG.size)
        System.arraycopy(buffer.array(), 0, allByteBuffer.array(), 0, buffer.capacity())
        System.arraycopy(
            TERM_FLAG, 0, allByteBuffer.array(), buffer.capacity(), TERM_FLAG.size
        )

        var bytes: ByteArray
        val byteArray = ByteArray(limit.toInt())
        do {
            bytes = if (limit > allByteBuffer.remaining()) {
                ByteArray(allByteBuffer.remaining())
            } else {
                byteArray
            }
            allByteBuffer[bytes, 0, bytes.size]
            LogUtil.d("split, callback")
            callback(ByteBuffer.wrap(bytes))
        } while (allByteBuffer.hasRemaining())
    }

}