package com.cmcc.newcalllib.manage.event

/**
 * @author jihongfei
 * @createTime 2022/3/25 15:25
 */
data class EventRender(
    val url: String
) {
    constructor(
        path: String,
        query: String
    ) : this("file://$path${if (query.isNotEmpty()) "?$query" else ""}")
}