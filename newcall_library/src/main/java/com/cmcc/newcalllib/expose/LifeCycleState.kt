package com.cmcc.newcalllib.expose

/**
 * @author jihongfei
 * @createTime 2022/5/17 9:59
 */
enum class LifeCycleState(val value: Int) {
    ON_FOREGROUND_IN_VOICE_CALL(1),
    ON_BACKGROUND(2),//hide or background
    ON_FOREGROUND_IN_VIDEO_CALL(3)
}