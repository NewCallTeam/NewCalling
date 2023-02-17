package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For getData
 * @author jihongfei
 * @createTime 2022/3/18 17:31
 */
class ReqGetData(
        val lifeCycle: Int? = ReqSaveData.LIFE_CYCLE_CROSS_SESSION,
        val key: String,
)
