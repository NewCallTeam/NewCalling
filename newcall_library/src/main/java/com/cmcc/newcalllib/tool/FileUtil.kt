package com.cmcc.newcalllib.tool

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.cmcc.newcalllib.tool.lang.RandomStringUtils
import java.io.*
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.regex.Pattern

object FileUtil {
    const val FILE_PROVIDER_AUTHORITY = "com.cmcc.newcalllib.fileProvider"

    fun getFileDirByPath(cxt: Context, path: String, mkdir: Boolean = true): File? {
        val dir = File(cxt.filesDir, path)
        if (mkdir && !dir.exists()) {
            LogUtil.v("mkdir: ${dir.path}")
            dir.mkdirs()
        }
        if (dir.exists() && dir.isDirectory) {
            return dir
        }
        return null
    }

    fun getFileByPath(cxt: Context, path: String, mkdir: Boolean = true, createFile: Boolean): File? {
        val file = File(cxt.filesDir, path)
        val parentFile = file.parentFile
        if (mkdir && (parentFile != null && !parentFile.exists())) {
            LogUtil.v("mkdir: ${parentFile.path}")
            parentFile.mkdirs()
        }
        if (!file.exists() && createFile && file.createNewFile()) {
            LogUtil.v("createNewFile: ${file.path}")
            return file
        } else if (file.exists() && file.isFile) {
            return file
        }
        return null // return null if file not exists
    }

    fun getInternalFile(cxt: Context, path: String, mkdir: Boolean = true, createFile: Boolean): File {
        val file = File(cxt.filesDir, path)
        val parentFile = file.parentFile
        if (mkdir && (parentFile != null && !parentFile.exists())) {
            LogUtil.v("mkdir: ${parentFile.path}")
            parentFile.mkdirs()
        }
        if (!file.exists() && createFile) {
            LogUtil.v("createNewFile: ${file.path}")
            file.createNewFile()
        }
        return file
    }

    fun getExternalFile(cxt: Context, path: String, mkdir: Boolean = true, createFile: Boolean): File {
        val file: File
        // try external dir, use filesDir if no external dir available
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
            || !Environment.isExternalStorageRemovable()) {
            LogUtil.i("externalFilesDir available")
            file = File(cxt.getExternalFilesDir(null), path)
        } else {
            LogUtil.i("externalFilesDir not available, use filesDir")
            file = File(cxt.filesDir, path)
        }
        val parentFile = file.parentFile
        if (mkdir && (parentFile != null && !parentFile.exists())) {
            LogUtil.v("mkdir: ${parentFile.path}")
            parentFile.mkdirs()
        }
        if (!file.exists() && createFile) {
            LogUtil.v("createNewFile: ${file.path}")
            file.createNewFile()
        }
        return file
    }

    fun exists(path: String, isDir: Boolean): Boolean {
        val file = File(path)
        if (file.exists() && (isDir == file.isDirectory)) {
            return true
        }
        return false
    }

    fun checkExtension(path: String, expExt: String): Boolean {
        val file = File(path)
        if (file.exists() && file.extension == expExt) {
            return true
        }
        return false
    }

    fun createFile(filePath: String): File {
        val file = File(filePath)
        val parentFile = file.parentFile!!
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    @Suppress("UnnecessaryVariable")
    @SuppressLint("ObsoleteSdkInt")
    fun getUri(context: Context?, authority: String? = FILE_PROVIDER_AUTHORITY, file: File?): Uri {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context!!, authority!!, file!!)
        } else {
            Uri.fromFile(file)
        }
        return uri
    }

    /**
     * 往文件中写入内容
     *
     * @param fullPath 文件路径
     * @param content  内容
     * @param append   如果为 true，则内容将被写入文件的末尾而不是文件的开头
     * @return {@code true}: success<br>{@code false}: fail
     */
    fun writeFileFromString(fullPath: String, content: String, append: Boolean): Boolean {
        var file = File(fullPath)
        if (!exists(fullPath, false)) {
            file = createFile(fullPath)
        }
        var bw: BufferedWriter? = null
        try {
            bw = BufferedWriter(FileWriter(file, append))
            bw.write(content)
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                bw?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun readFileToByteBuffer(file: File): ByteBuffer? {
        val inputStream: InputStream
        var baos: ByteArrayOutputStream? = null
        try {
            inputStream = FileInputStream(file)
            baos = ByteArrayOutputStream()
            inputStream.use { it.copyTo(baos) }
            return ByteBuffer.wrap(baos.toByteArray())
        } catch (e: Exception) {
            LogUtil.e("readFileToByteBuffer fail", e)
        } finally {
            baos?.closeQuietly()
        }
        return null
    }

    /**
     * support image, video, audio, zip, pdf, doc, ppt, excel
     */
    fun isFileContentType(contentType: String?): Boolean {
        if (contentType == null) {
            return false
        }
        if (contentType in setOf(
                "application/octet-stream",
                "application/zip",
                "application/x-gzip",
                "application/pdf",
                "application/msword",
                "application/vnd.ms-excel",
                "application/vnd.ms-powerpoint",
            )
            || contentType.contains("image/")
            || contentType.contains("video/")
            || contentType.contains("audio/")) {
            return true
        }
        return false
    }

    fun extractFileNameInContentPosition(contentDisposition: String?): String? {
        if (contentDisposition != null) {
            val pattern: Pattern = Pattern.compile("filename=\"([\\S]+)\"")
            val matcher = pattern.matcher(contentDisposition)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun genRandomFileName(ext: String? = null): String {
        return if (ext == null) {
            RandomStringUtils.randomAlphanumeric(16)
        } else {
            "${RandomStringUtils.randomAlphanumeric(16)}.$ext"
        }
    }

    /**
     * do not suggest use
     */
    fun getFileExt(contentType: String): String? {
        if (contentType in setOf(
                "application/zip",
                "application/x-gzip",
            )) {
            return "zip"
        } else if (contentType.contains("image/")
            || contentType.contains("video/")
            || contentType.contains("audio/")) {
            return contentType.substring(contentType.indexOf("/") + 1)
        }
        return null
    }

}