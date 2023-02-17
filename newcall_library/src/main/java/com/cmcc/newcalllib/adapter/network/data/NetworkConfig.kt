package com.cmcc.newcalllib.adapter.network.data

/**
 * host, path config
 * @author jihongfei
 * @createTime 2022/2/22 18:14
 */
class NetworkConfig private constructor(
        val host: String?, val slotId: Int?, val callId: String?,
        val bufferAmount: Long?) {

    class Builder {
        private var bHost: String? = null
        private var bSlotId: Int? = null
        private var bCallId: String? = null
        private var bBufferAmount: Long? = null

        public fun setHost(host: String): Builder {
            this.bHost = host
            return this
        }
        public fun setSlotId(slotId: Int): Builder {
            this.bSlotId = slotId
            return this
        }
        public fun setCallId(callId: String): Builder {
            this.bCallId = callId
            return this
        }
        public fun setBufferAmount(ba: Long?): Builder {
            this.bBufferAmount = ba
            return this
        }
        public fun build() = NetworkConfig(bHost, bSlotId, bCallId, bBufferAmount)
    }
}