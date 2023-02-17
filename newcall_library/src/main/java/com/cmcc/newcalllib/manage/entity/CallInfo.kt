package com.cmcc.newcalllib.manage.entity

import android.telecom.Call

/**
 * @author jihongfei
 * @createTime 2022/3/31 15:20
 */
data class CallInfo(
    val localNumber: String,//MSISDN TODO DEL
    val remoteNumber: String,//MSISDN
    val slotId: Int,
    val callId: String,
    val direction: Int,//0: incoming, 1: outgoing
) {
    companion object {
        const val DIRECTION_INCOMING = 0
        const val DIRECTION_OUTGOING = 1

        fun fromCall(call: Call?): CallInfo? {
            // TODO
            if (call == null) {
                return null
            }
            return CallInfo("", "", 0, "0", 0)
        }
    }
    @Transient
    var callStatus: Int = Call.STATE_NEW
}
