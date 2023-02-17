package com.cmcc.newcalllib.manage.entity.handler.resp

/**
 * For checkResource
 * @author jihongfei
 * @createTime 2022/12/29 16:24
 */
data class RespCheckResource(val results: List<Result>) {
    data class Result(val path: String, val exists: Boolean)
}
