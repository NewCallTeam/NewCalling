package com.cmcc.newcalllib.manage.entity.caller.req

/**
 * @author jihongfei
 * @createTime 2022/4/8 10:56
 */
data class VisibilityNotify(
    val state: Int
) {
    companion object {
        fun fromVisible(visible: Boolean): Int {
            return if (visible) 1 else 0
        }
    }
}
