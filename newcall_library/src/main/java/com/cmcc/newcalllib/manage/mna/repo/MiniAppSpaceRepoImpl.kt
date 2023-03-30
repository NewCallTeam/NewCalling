package com.cmcc.newcalllib.manage.mna.repo

import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.PathManager
import com.cmcc.newcalllib.manage.support.storage.SpUtil
import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil
import java.io.File
import java.io.FileOutputStream

/**
 * allow mini-app check/delete/load files under specific
 * path, such as '$PARENT_DIR/$APP_ID/demo.txt'.
 * A mini-app can not touch files of another mini-app
 * @author jihongfei
 * @createTime 2023/1/9 10:20
 */
class MiniAppSpaceRepoImpl(
    private val sp: SpUtil,
): MiniAppSpaceRepo {

    private var mPathManager: PathManager? = null

    fun setPathManager(pathManager: PathManager) {
        mPathManager = pathManager
    }

    override fun checkFilesExists(appId: String, paths: List<String>): Map<String, Boolean> {
        val ret = mutableMapOf<String, Boolean>()
        paths.forEach {
            ret[it] = File(it).exists()
        }
        return ret
    }

    override fun deleteFiles(appId: String, paths: List<String>): Map<String, Boolean> {
        val ps = mPathManager?.getMiniAppPrivateSpace(appId)
        val ret = mutableMapOf<String, Boolean>()
        paths.forEach {
            if (ps == null) {
                ret[it] = false
            } else {
                val file = File(ps, it)
                val result = file.exists() && file.delete()
                ret[it] = result
            }
        }
        return ret
    }


    fun getFile(appId: String, path: String): File? {
        val ps = mPathManager?.getMiniAppPrivateSpace(appId)
        return if (ps == null) {
            null
        } else {
            File(ps, path)
        }
    }

    override fun saveFile(appId: String,
                          path: String?,
                          contentType: String,
                          byteArray: ByteArray,
                          callback: Callback<Results<String>>) {
        val ps = mPathManager?.getMiniAppPrivateSpace(appId)
        LogUtil.d("saveFile, path=$path, space=${ps?.path}")
        ThreadPoolUtil.runOnIOThread({
            val dest = File(ps, path ?: "")
            LogUtil.d("saveFile, dest=${dest.path}")
            FileOutputStream(dest).write(byteArray)
            dest.path
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(Results.success(it))
            }
        })
    }
}