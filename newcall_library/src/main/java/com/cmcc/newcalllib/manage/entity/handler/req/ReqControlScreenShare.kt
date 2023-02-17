package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For controlScreenShare
 */
data class ReqControlScreenShare(val enable: Boolean, val role: Int, val dcLabel: String) {
    companion object {
        const val ROLE_POSITIVE = 0
        const val ROLE_NEGATIVE = 1
    }
}
