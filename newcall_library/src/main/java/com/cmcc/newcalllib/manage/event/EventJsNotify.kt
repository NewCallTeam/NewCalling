package com.cmcc.newcalllib.manage.event

data class EventJsNotify<T>(val eventType: Int,
                            val data: T? = null
) {
    companion object {
        const val ET_EXIT_APP = 1
    }
}
