package com.cmcc.newcalllib.manage.mna.repo

import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.support.Callback

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
                 callback: Callback<Results<String>>)
}