package com.cmcc.newcalllib.manage.support

import android.content.Context
import com.cmcc.newcalllib.tool.constant.Constants
import com.cmcc.newcalllib.tool.FileUtil
import com.cmcc.newcalllib.tool.LogUtil
import java.io.File

/**
 * Path manager of FML
 */
class PathManager(private val context: Context, private val id: String) {
    /**
     * get mini app FWK path
     * @return file of FWK, null if not exists
     */
    fun getFrameworkDir(): File? {
        return FileUtil.getFileDirByPath(context, Constants.FRAME_ROOT_PATH, true)
    }

    /**
     * get bootstrap mini app path
     * @return file of mini app
     */
    fun getBootstrapMiniAppHtml(): File? {
        return FileUtil.getFileByPath(context,
            Constants.FRAME_ROOT_PATH +
                    File.separator + Constants.GUIDE_FILE_NAME, mkdir = true, createFile = false
        )
    }

    /**
     * get mini app path by appId
     * @param appId app id
     * @return file of mini app
     */
    fun getMiniAppHtml(appId: String): File? {
        return FileUtil.getFileByPath(context,
                Constants.MINI_APP_ROOT_PATH + appId + File.separator +
                        Constants.INDEX_FILE_NAME, mkdir = true, createFile = false)
    }

    /**
     * get mini app path
     * @param appId app id
     * @return dir of mini app
     */
    fun getMiniAppDir(appId: String): File? {
        return FileUtil.getFileDirByPath(context,
                Constants.MINI_APP_ROOT_PATH + appId, true)
    }

    /**
     * create file with given pathName in cached dir
     * @param dirName
     * @param fileName
     * @return cache file
     */
    fun createCacheFile(dirName: String = "", fileName: String): File {
        val child = if (dirName.isNotEmpty()) {
            "$dirName${File.separator}$fileName"
        } else {
            fileName
        }
        val f = File(context.cacheDir, child)
        return FileUtil.createFile(f.absolutePath)
    }

    /**
     * create file for file-transfer use with given id
     */
    fun getTransferFile(id: String, fileName: String): File {
        return FileUtil.getInternalFile(
            context,
            Constants.TRANSFER_FILE_ROOT_PATH + id + File.separator + fileName,
            mkdir = true,
            createFile = true
        )
    }

    /**
     * return file name for file-transfer use with given mime type
     */
    fun genTransferFileName(mime: String?): String {
        val mimeParser = MimeParser(mime ?: "")
        val ret = "${System.currentTimeMillis()}.${mimeParser.getExtension()}"
        LogUtil.d("genTransferFileName: $ret")
        return ret
    }

    /**
     * create file for save with given id
     */
    fun getManuallySaveFile(id: String, fileName: String): File {
        return FileUtil.getExternalFile(
            context,
            Constants.MANUALLY_SAVE_FILE_ROOT_PATH + id + File.separator + fileName,
            mkdir = true,
            createFile = true
        )
    }

    /**
     * get file for mini app private use
     * @param appId app id
     * @return dir of mini app
     */
    fun getMiniAppPrivateSpace(appId: String): File? {
        return FileUtil.getFileDirByPath(context,
            Constants.MINI_APP_PRIVATE_SPACE_PATH + appId, true)
    }
}