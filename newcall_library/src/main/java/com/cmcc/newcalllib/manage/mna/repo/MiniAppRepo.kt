package com.cmcc.newcalllib.manage.mna.repo

import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.storage.db.MiniApp

/**
 * @author jihongfei
 * @createTime 2022/3/25 11:20
 */
interface MiniAppRepo {
    /**
     * save mini-app entity into database.
     */
    fun saveMiniAppAsync(app: MiniApp, callback: Callback<Boolean>)

    /**
     * update mini-app entity in database
     */
    fun updateMiniAppAsync(app: MiniApp, callback: Callback<Boolean>)

    /**
     * query mini-app by appId in database
     */
    fun getLocalMiniAppAsync(appId: String, callback: Callback<MiniApp?>)

    /**
     * save value with key, in sharedPreference
     */
    fun saveStringInSp(key: String, value: String)

    /**
     * find value by key, in sharedPreference
     */
    fun getStringInSp(key: String): String
}