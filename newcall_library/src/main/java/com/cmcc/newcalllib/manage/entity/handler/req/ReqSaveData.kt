package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For saveData
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
class ReqSaveData(
        val lifeCycle: Int? = LIFE_CYCLE_CROSS_SESSION,
        val allowPublicAccess: Boolean? = false,
        val key: String,
        val value: String,
) {
        companion object {
                const val LIFE_CYCLE_CROSS_SESSION = 0
                const val LIFE_CYCLE_ONLY_THIS_SESSION = 1
        }
}
