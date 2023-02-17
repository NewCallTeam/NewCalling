package com.cmcc.newcalllib.manage.mna.repo

import android.content.Context
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.storage.db.PersistentMessage
import com.cmcc.newcalllib.manage.support.storage.db.PersistentMessageDao
import com.cmcc.newcalllib.tool.StringUtil
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil

/**
 * Manage data in local storage, including thread handling
 * @author jihongfei
 * @createTime 2022/3/24 17:38
 */
class TransferFileRepoImpl(
    private val context: Context,
    private val dao: PersistentMessageDao
): TransferFileRepo {
    override fun saveBase64ToFile(base64: String, path: String, callback: Callback<Boolean>) {
        ThreadPoolUtil.runOnIOThread {
            val result: Boolean =
                StringUtil.base64ToFile(StringUtil.extractPureBase64(base64), path)
            if (result) {
                callback.onResult(true)
            } else {
                callback.onResult(false)
            }
        }
    }

    override fun saveInfoToDB(transferFile: PersistentMessage, callback: Callback<Boolean>) {
        ThreadPoolUtil.runOnIOThread({
            dao.insert(transferFile)
            true
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(it)
            }
        })
    }

    override fun findByRemoteNum(remoteNum: String, callback: Callback<List<PersistentMessage>>) {
        ThreadPoolUtil.runOnIOThread({
            dao.getByRemoteNum(remoteNum)
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(it)
            }
        })
    }

    override fun saveBase64ToLocal(base64: String, fileName: String, type: Int, mime: String, callback: Callback<Boolean>) {
        ThreadPoolUtil.runOnIOThread {
            val result: Boolean =
                StringUtil.base64ToLocal(context.applicationContext, StringUtil.extractPureBase64(base64), fileName, type, mime)
            callback.onResult(result)
        }
    }
}