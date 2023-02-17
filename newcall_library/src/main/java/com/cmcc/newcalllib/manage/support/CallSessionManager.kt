package com.cmcc.newcalllib.manage.support

import com.cmcc.newcalllib.manage.entity.CallInfo

/**
 * @author jihongfei
 * @createTime 2022/8/3 11:23
 */
object CallSessionManager {
    private lateinit var mCallInfo: CallInfo
    private lateinit var mSessionId: String

    fun init(sessionId: String, callInfo: CallInfo) {
        mSessionId = sessionId
        mCallInfo = callInfo
    }

    fun getSessionId(): String {
        return mSessionId
    }

    fun getCallInfo(): CallInfo {
        return mCallInfo
    }
}