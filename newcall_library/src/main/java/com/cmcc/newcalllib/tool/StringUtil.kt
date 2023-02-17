package com.cmcc.newcalllib.tool

import android.content.Context
import android.util.Base64
import com.cmcc.newcalllib.manage.mna.repo.TransferFileRepo
import java.io.*
import java.lang.Exception
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * @author jihongfei
 * @createTime 2022/3/24 10:01
 */
object StringUtil {
    const val BASE64_FLAG = "base64,"
    const val ALGO_MD5 = "MD5"

    fun base64Encode(byteArray: ByteArray): String? {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun base64Decode(base64Str: String): ByteArray? {
        return Base64.decode(base64Str, Base64.NO_WRAP)
    }

    fun equals(str1: String?, str2: String?): Boolean {
        if (str1 == str2) {
            return true
        }
        if (str1 == null || str2 == null) {
            return false
        }
        if (str1.length != str2.length) {
            return false
        }
        return str1 == str2
    }

    fun bytesToBase64(bytes: ByteArray): String {
        return org.apache.commons.codec.binary.Base64.encodeBase64String(bytes)
    }

    fun base64ToBytes(base64: String): ByteArray? {
        return org.apache.commons.codec.binary.Base64.decodeBase64(base64)
    }

    fun extractMimeType(base64: String): String? {
        if (base64.contains(BASE64_FLAG)) {
            val substring = base64.substring(0, base64.indexOf(BASE64_FLAG))
            val pattern: Pattern = Pattern.compile("data:([\\w\\d+-.]+/[\\w\\d+-.]+)")
            val matcher = pattern.matcher(substring)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun extractPureBase64(base64: String): String {
        if (base64.contains(BASE64_FLAG)) {
            return base64.substring(base64.indexOf(BASE64_FLAG) + BASE64_FLAG.length)
        } else {
            return base64
        }
    }

    /**
     * decode base64 string to file with given file path
     * @param base64 base64 data of file
     * @param path   file path
     * @return
     */
    fun base64ToFile(base64: String, path: String): Boolean {
        val file = File(path)
        LogUtil.v("try base64 to file, path=$path, exists=${file.exists()}, isFile=${file.isFile}")
        if (base64.isEmpty()) {
            LogUtil.e("input base64 empty")
            return false
        }
        try {
            val out: OutputStream = FileOutputStream(path)
            try {
                // decode to bytes
                val bytes: ByteArray = org.apache.commons.codec.binary.Base64.decodeBase64(base64)
                out.write(bytes)
                out.flush()
                return true
            } catch (e: Exception) {
                LogUtil.e("base64 to file fail", e)
            } finally {
                out.closeQuietly()
            }
        } catch (e: FileNotFoundException) {
            LogUtil.e("base64 to file fail", e)
        }
        return false
    }

    private fun byteArrToHex(byteArray: ByteArray): String {
        return with(StringBuilder()) {
            byteArray.forEach {
                val hex = it.toInt() and (0xFF)
                val hexStr = Integer.toHexString(hex)
                if (hexStr.length == 1) {
                    append("0").append(hexStr)
                } else {
                    append(hexStr)
                }
            }
            toString()
        }
    }

    fun md5(str: String): String {
        val digest = MessageDigest.getInstance(ALGO_MD5)
        val result = digest.digest(str.toByteArray())
        return byteArrToHex(result)
    }

    /**
     * decode base64 string into local album files
     * @param context Context
     * @param fileName String
     * @param base64 String
     * @return Boolean
     */
    fun base64ToLocal(context: Context, base64: String, fileName: String, type: Int, mime: String): Boolean {
        if (base64.isEmpty()) {
            LogUtil.e("input base64 empty")
            return false
        }
        val bytes: ByteArray = org.apache.commons.codec.binary.Base64.decodeBase64(base64)
        val inputStream: InputStream = ByteArrayInputStream(bytes)

        return when (type) {
            TransferFileRepo.TYPE_IMAGE -> {
                inputStream.saveImageToAlbum(context, fileName, null, mime) != null
            }
            TransferFileRepo.TYPE_VIDEO -> {
                inputStream.saveVideoToAlbum(context, fileName, null, mime) != null
            }
            else -> false
        }
    }

}