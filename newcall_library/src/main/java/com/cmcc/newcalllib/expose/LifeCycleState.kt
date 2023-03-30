package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/5/17 9:59
 */
enum class LifeCycleState(val value: Int) {
    ON_CREATE(1),
    ON_START(2),
    ON_RESUME(3),
    ON_PAUSE(4),
    ON_STOP(5),
    ON_DESTROY(6),
}