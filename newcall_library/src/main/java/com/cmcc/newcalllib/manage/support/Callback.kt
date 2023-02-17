package com.cmcc.newcalllib.manage.support

/**
 * @author jihongfei
 * @createTime 2022/3/25 14:24
 */
interface Callback<T> {
    fun onResult(t: T)
}