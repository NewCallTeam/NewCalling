package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For startARCall/stopARCall
 */
data class ReqARCall(val enable: Boolean, val dcLabels: List<String>) {
}
