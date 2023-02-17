package com.cmcc.newcalllib.manage.mna.repo

import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.storage.db.MiniApp
import com.cmcc.newcalllib.manage.support.storage.db.MiniAppDao
import com.cmcc.newcalllib.manage.support.storage.SpUtil
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil
import java.io.File

/**
 * Manage data in local storage, including thread handling
 * @author jihongfei
 * @createTime 2022/3/24 17:38
 */
class MiniAppRepoImpl(
    private val sp: SpUtil,
    private val dao: MiniAppDao
): MiniAppRepo {

    override fun saveMiniAppAsync(app: MiniApp, callback: Callback<Boolean>) {
        // insert on io thread, then callback on main thread
        ThreadPoolUtil.runOnIOThread({
            dao.insert(app)
            true
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(it)
            }
        })
    }

    override fun updateMiniAppAsync(app: MiniApp, callback: Callback<Boolean>) {
        ThreadPoolUtil.runOnIOThread({
            dao.update(app)
            true
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(it)
            }
        })
    }

    override fun getLocalMiniAppAsync(appId: String, callback: Callback<MiniApp?>) {
        ThreadPoolUtil.runOnIOThread({
            dao.getOne(appId)
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(it)
            }
        })
    }

    override fun saveStringInSp(key: String, value: String) {
        sp.put(key, value)
    }

    override fun getStringInSp(key: String): String {
        return sp.get(key)
    }

}