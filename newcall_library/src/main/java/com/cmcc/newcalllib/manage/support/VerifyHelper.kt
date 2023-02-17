package com.cmcc.newcalllib.manage.support

import com.cmcc.newcalllib.tool.LogUtil
import com.cmcc.newcalllib.tool.StringUtil
import com.cmcc.newcalllib.tool.thread.ThreadPoolUtil
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object VerifyHelper {

    private val stringBuilder: StringBuilder = StringBuilder()

    fun calculateHash(file: File, callback: Callback<String>) {
        stringBuilder.clear()
        ThreadPoolUtil.runOnIOThread({
            putAllFiles(file)
            StringUtil.md5(stringBuilder.toString())
        }, {
            ThreadPoolUtil.runOnUiThread {
                callback.onResult(it)
            }
        })
    }

    private fun putAllFiles(file: File) {
        if (file.exists()) {
            file.listFiles().forEach {
                if (it.isDirectory) {
                    putAllFiles(it)
                } else {
                    if (!it.name.endsWith(".sf")) {
                        stringBuilder.append(getFileMD5(it))
                    }
                }
            }
        } else {
            stringBuilder.append("")
        }
    }

    private fun getFileMD5(file: File): String? {
        if (!file.isFile) {
            return null
        }
        val digest: MessageDigest
        val fileInputStream: FileInputStream
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance("MD5")
            fileInputStream = FileInputStream(file)
            fileInputStream.use {
                while (it.read(buffer, 0, 1024).also { len = it } != -1) {
                    digest.update(buffer, 0, len)
                }
            }
        } catch (e: Exception) {
            LogUtil.e("VerifyHelper getFileMD5 fail: ${e.message}")
            return null
        }
        return bytesToHexString(digest.digest())
    }

    private fun bytesToHexString(byteArray: ByteArray?): String? {
        if (byteArray == null || byteArray.isEmpty()) {
            return null
        }
        return with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length < 2) {
                    append(0)
                }
                append(hexStr)
            }
            toString()
        }
    }

}