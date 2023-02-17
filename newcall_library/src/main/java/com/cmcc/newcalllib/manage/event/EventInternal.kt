package com.cmcc.newcalllib.manage.event

/**
 * @author jihongfei
 * @createTime 2022/3/25 15:25
 */
data class EventInternal(
    val eventType: Int
) {
    companion object {
        const val ET_BOOTSTRAP_CREATED = 1
    }
}