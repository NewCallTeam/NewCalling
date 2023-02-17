package com.cmcc.newcalllib.tool.constant
/**
 * @author jihongfei
 * @createTime 2022/2/22 14:26
 */
enum class EventType(private val code: Int) {
    EXCEPTION(100),
    RENDER(101),
    INTERNAL(102),
    JS_NOTIFY(103),
    APP_NOTIFY(104);

    fun code(): Int {
        return code
    }
}

