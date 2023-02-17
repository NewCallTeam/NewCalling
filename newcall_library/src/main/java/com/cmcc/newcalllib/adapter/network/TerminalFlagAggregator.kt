package com.cmcc.newcalllib.adapter.network

import com.cmcc.newcalllib.tool.LogUtil
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

class TerminalFlagAggregator : DataAggregator {


//    private val lastBytes = ByteArray(TerminalFlagSplitter.TERM_FLAG.size)
    private val lastBytesKeeper = mutableMapOf<String, ByteArray>()
    private val allByteBuffers = mutableMapOf<String, ByteBuffer>()

    override fun aggregate(
        subProtocol: String,
        dcLabel: String,
        byteBuffer: ByteBuffer,
        callback: (buffer: ByteBuffer) -> Any
    ) {
        // TODO check
        LogUtil.d("aggregate, dcLabel=$dcLabel")
        if (!allByteBuffers.containsKey(dcLabel)) {
            allByteBuffers[dcLabel] = ByteBuffer.allocate(byteBuffer.capacity())
            System.arraycopy(
                byteBuffer.array(), 0, allByteBuffers[dcLabel]!!.array(), 0, byteBuffer.capacity()
            )
            lastBytesKeeper[dcLabel] = ByteArray(TerminalFlagSplitter.TERM_FLAG.size)
        } else {
            val capacity = allByteBuffers[dcLabel]!!.capacity()
            val array = allByteBuffers[dcLabel]!!.array()
            allByteBuffers[dcLabel] = ByteBuffer.allocate(capacity + byteBuffer.capacity())
            System.arraycopy(array, 0, allByteBuffers[dcLabel]!!.array(), 0, array.size)
            System.arraycopy(
                byteBuffer.array(), 0, allByteBuffers[dcLabel]!!.array(), array.size, byteBuffer.capacity()
            )
        }
        val lastBytes: ByteArray = lastBytesKeeper[dcLabel]!!
        allByteBuffers[dcLabel]?.let {
            LogUtil.v("aggregate, lastBytes=${lastBytes.toString(Charset.defaultCharset())}")
            if (it.capacity() >= TerminalFlagSplitter.TERM_FLAG.size) {
                it.position(it.capacity() - TerminalFlagSplitter.TERM_FLAG.size)
                it.get(lastBytes, 0, lastBytes.size)
                if (Arrays.equals(TerminalFlagSplitter.TERM_FLAG, lastBytes)) {
                    LogUtil.d("aggregate callback, dcLabel=$dcLabel")
                    // collect whole data
                    it.rewind()
                    val ret = ByteArray(it.capacity() - TerminalFlagSplitter.TERM_FLAG.size)
                    it.get(ret, 0, ret.size)
                    callback(ByteBuffer.wrap(ret))
                    // clear map
                    allByteBuffers.remove(dcLabel)
                    lastBytesKeeper.remove(dcLabel)
                }
            } else {
                LogUtil.e("aggregate, capacity only ${it.capacity()}")
            }
        }
    }

}