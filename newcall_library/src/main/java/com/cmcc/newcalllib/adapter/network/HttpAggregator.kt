package com.cmcc.newcalllib.adapter.network

import com.cmcc.newcalllib.dc.httpstack.HttpStack
import com.cmcc.newcalllib.tool.LogUtil
import java.nio.ByteBuffer

/**
 * Http 1.1 Get Response 字节流分组数据组装
 *
 * @author xiaxueliang@chinamobile.com
 * @since 2023/1/3
 */
class HttpAggregator : DataAggregator {

    // buffer缓存数据
    private val allByteBuffers = mutableMapOf<String, ByteBuffer>()

    /**
     * 合并 http 分片数据
     */
    override fun aggregate(
        subProtocol: String,
        dcLabel: String,
        byteBuffer: ByteBuffer,
        callback: (buffer: ByteBuffer) -> Any
    ) {
        LogUtil.d("HttpAggregator", "aggregate. subProtocol: $subProtocol dcLabel: $dcLabel byteBuffer: $byteBuffer")
        // 未添加过该dc的返回数据，直接添加数据
        if (!allByteBuffers.containsKey(dcLabel)) {
            // 分配buffer内存空间
            allByteBuffers[dcLabel] = ByteBuffer.allocate(byteBuffer.capacity())
            // 将数据拷贝到 allByteBuffers 中
            System.arraycopy(
                byteBuffer.array(), 0, allByteBuffers[dcLabel]!!.array(), 0, byteBuffer.capacity()
            )
        } else {
            // 不完整数据的一部分
            val capacity = allByteBuffers[dcLabel]!!.capacity()
            val array = allByteBuffers[dcLabel]!!.array()
            allByteBuffers[dcLabel] = ByteBuffer.allocate(capacity + byteBuffer.capacity())
            System.arraycopy(array, 0, allByteBuffers[dcLabel]!!.array(), 0, array.size)
            System.arraycopy(
                byteBuffer.array(),
                0,
                allByteBuffers[dcLabel]!!.array(),
                array.size,
                byteBuffer.capacity()
            )
        }
        // 数据是否完整
        var complete = HttpStack.verifyHttp1ResponseCompleted(allByteBuffers[dcLabel])
        LogUtil.d("HttpAggregator", "complete: $complete")
        if (complete) {
            allByteBuffers[dcLabel]?.let {
                // 回调数据
                callback(it)
                // 移除缓存数据
                allByteBuffers.remove(dcLabel)
            }
        } else {
            // 数据不完整，等待下次数据到来
        }

    }

}