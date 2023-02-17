package com.cmcc.newcalllib.manage.entity.event

data class NotifyJsMessage(
    val contentType: String = "control",
    val controlType: String,
    val appId: String?
) {
    companion object {
        const val CONTROL_TYPE_EXIT = "exit"
    }
}
