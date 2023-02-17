package com.cmcc.newcalllib.tool.constant

import android.telecom.Call

/**
 * phase of NewCall mini-app container
 * @author jihongfei
 * @createTime 2022/3/31 14:33
 */
enum class Phase(val phaseName: String) {
    NO_SHOW("noshow"),
    PRE_CALL("precall"),
    IN_CALL("incall"),
    HIDDEN("hidden"),//holding or after call
    UNKNOWN("unknown");

    companion object {
        fun fromCallState(state: Int): Phase {
            when (state) {
                Call.STATE_NEW -> {
                    return NO_SHOW
                }
                Call.STATE_CONNECTING, Call.STATE_DIALING, CustomizationCallState.VIVO_ALERTING.state -> {
                    return PRE_CALL
                }
                Call.STATE_RINGING -> {
                    return PRE_CALL
                }
                Call.STATE_ACTIVE -> {
                    return IN_CALL
                }
                Call.STATE_HOLDING -> {
                    return HIDDEN
                }
                Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> {
                    return HIDDEN
                }
            }
            return UNKNOWN
        }

        fun isPreCallOrInCall(state: Int): Boolean {
            val phase = fromCallState(state)
            return phase == PRE_CALL || phase == IN_CALL
        }
    }
}