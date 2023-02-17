package com.cmcc.newcalllib.manage.mna.repo

import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil
import java.io.File

/**
 * @author jihongfei
 * @createTime 2023/1/9 10:20
 */
interface MiniAppSpaceRepo {

    fun checkFilesExists(appId: String, paths: List<String>): Map<String, Boolean>

    fun deleteFiles(appId: String, paths: List<String>): Map<String, Boolean>

    fun saveFile(appId: String,
                 path: String?,
                 contentType: String,
                 byteArray: ByteArray,
                 callback: Callback<Result<String>>)
}