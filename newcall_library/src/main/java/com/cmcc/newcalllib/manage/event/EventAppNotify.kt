package com.cmcc.newcalllib.manage.event

data class EventAppNotify<T>(val eventType: Int,
                             val data: T? = null
) {
    companion object {
        const val ET_REQUEST_WEB_VIEW_CHANGE = 1
        const val ET_NOTIFY_APP_BY_JS = 2
    }
}
