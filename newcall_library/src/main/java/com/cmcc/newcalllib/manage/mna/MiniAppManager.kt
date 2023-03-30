package com.cmcc.newcalllib.manage.mna

import android.net.Uri
import com.cmcc.newcalllib.adapter.network.NetworkAdapter
import com.cmcc.newcalllib.adapter.network.Origin
import com.cmcc.newcalllib.adapter.ntv.NativeAbilityProvider
import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.PathManager
import com.cmcc.newcalllib.manage.support.storage.db.MiniApp
import com.cmcc.newcalllib.manage.entity.NewCallException
import com.cmcc.newcalllib.manage.entity.Results
import com.cmcc.newcalllib.manage.entity.handler.req.MiniAppEntity
import com.cmcc.newcalllib.manage.entity.handler.resp.RespGetTransferFileRecords
import com.cmcc.newcalllib.manage.mna.repo.MiniAppRepo
import com.cmcc.newcalllib.manage.mna.repo.MiniAppSpaceRepo
import com.cmcc.newcalllib.manage.mna.repo.MiniAppSpaceRepoImpl
import com.cmcc.newcalllib.manage.mna.repo.TransferFileRepo
import com.cmcc.newcalllib.manage.support.CallSessionManager
import com.cmcc.newcalllib.manage.support.ConfigManager
import com.cmcc.newcalllib.manage.support.storage.db.PersistentMessage
import com.cmcc.newcalllib.tool.*
import com.cmcc.newcalllib.tool.constant.Constants
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.min
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.jvm.Throws

/**
 * Manager of mini-app
 * @author jihongfei
 * @createTime 2022/2/22 15:54
 */
class MiniAppManager(
    private val netAdapter: NetworkAdapter,
    private val ntvProvider: NativeAbilityProvider,
    private val miniAppRepo: MiniAppRepo,
    private val transferFileRepo: TransferFileRepo,
    private val miniAppSpaceRepo: MiniAppSpaceRepo,
    private val pathManager: PathManager
)
{
    companion object {
        private const val TOKEN_SEED = "CMCC_BOOTSTRAP"
        private const val HTTP_HEADER_IF_NONE_MATCH = "If-None-Match"
        private const val QUERY_TERMINAL_VENDOR = "Terminal_Vendor"
        private const val QUERY_TERMINAL_MODEL = "Terminal_Model"

        fun buildBsAppQuery(phase: String?, display: String?): String {
            val token = TokenUtil.createTokenNeverTimeout(
                TOKEN_SEED,
                Constants.BOOTSTRAP_DATA_CHANNEL_LABEL_LOCAL
            )
            // need url-encode
            val encodeToken = URLEncoder.encode(token, StandardCharsets.UTF_8.name())
            var queryStr = "token=$encodeToken"
            if (phase != null) {
                queryStr += "&phase=$phase"
            }
            if (display != null) {
                queryStr += "&display=$display"
            }
            LogUtil.d("buildBsAppQuery, query=$queryStr")
            return queryStr
        }
    }

    interface BootstrapMiniAppPrepareListener {
        fun onPrepared()
    }

    @Volatile
    private var mBootStrapMiniAppPrepared = false
    private var mBootStrapMiniAppListener: BootstrapMiniAppPrepareListener? = null
    private var mAppIdStack: LinkedList<MiniAppEntity> = LinkedList()


    fun initMiniAppManager() {
        LogUtil.d("init miniAppManager. alwaysDown=${ConfigManager.alwaysDownloadMiniApp}")
        (miniAppSpaceRepo as MiniAppSpaceRepoImpl).setPathManager(pathManager)
    }

    fun getBsAppPrepared(): Boolean {
        return mBootStrapMiniAppPrepared
    }

    fun markBsAppPrepared() {
        LogUtil.d("mark bs app prepared")
        mBootStrapMiniAppPrepared = true
        mBootStrapMiniAppListener?.onPrepared()
    }

    fun setBootstrapMiniAppPrepareListener(listener: BootstrapMiniAppPrepareListener) {
        mBootStrapMiniAppListener = listener
    }

    fun pushMiniApp(app: MiniAppEntity): Boolean {
        return if (app.appId != mAppIdStack.peek()?.appId) {
            mAppIdStack.push(app)
            LogUtil.d("push mna, stack now: ${mAppIdStack}, appId: ${app.appId}")
            true
        } else {
            LogUtil.d("push mna, no need to push")
            true
        }
    }

    fun popMiniApp(expectTop: MiniAppEntity?): Boolean {
        if (mAppIdStack.isEmpty()) {
            LogUtil.w("pop mna, stack empty")
            return false
        }
        val pop = mAppIdStack.pop()
        LogUtil.d("pop mna, stack now: $mAppIdStack, appId:${expectTop?.appId}, top: ${pop.appId}")
        return if (expectTop == null) true else (pop.appId == expectTop.appId)
    }

    fun clearMiniAppStack() {
        LogUtil.d("clear mna stack")
        mAppIdStack.clear()
    }

    fun getTopMiniApp(): MiniAppEntity? {
        return mAppIdStack.peek()
    }

    private fun getBootstrapMiniAppUrl(): String {
        val ti = ntvProvider.getTerminalInfo()
        val requestParameters: MutableMap<String, String> = HashMap()
        requestParameters[QUERY_TERMINAL_VENDOR] = ti.vendor
        requestParameters[QUERY_TERMINAL_MODEL] = ti.model

        val host = netAdapter.getNetworkConfig().host
        return "$host/?${QUERY_TERMINAL_VENDOR}=${ti.vendor}&${QUERY_TERMINAL_MODEL}=${ti.model}"
    }

    /**
     * request bootstrap mini-app, then update db
     * build url first. involved in request construction and parsing
     * @param callback result with absolute path of newest bootstrap mini-app
     */
    @Throws(java.lang.Exception::class)
    fun prepareBootstrapMiniApp(callback: Callback<Results<String>>) {
        // prepare path
        var htmlFile: File?
        val baseUrl = getBootstrapMiniAppUrl()
        // find cached version
        val appId = Constants.BOOTSTRAP_MINI_APP_ID
        miniAppRepo.getLocalMiniAppAsync(appId, object : Callback<MiniApp?> {
            override fun onResult(t: MiniApp?) {
                var localVer = ""
                val appInDb = t
                if (appInDb != null) {
                    localVer = appInDb.version
//                    htmlFile = pathManager.getBootstrapMiniAppHtml()
//                    if (!ConfigManager.alwaysDownloadMiniApp && htmlFile?.exists() == true) {
//                        LogUtil.d("Get local cached bs app")
//                        callback.onResult(Results.success(htmlFile!!.absolutePath.toString()))
//                        markBsAppPrepared()
//                        return
//                    }
                }
                // url
                val urlWithVer = baseUrl
                LogUtil.d("Get bs app, build url=$urlWithVer")
                // header
                val headers: MutableMap<String, String> = HashMap()
                headers[HTTP_HEADER_IF_NONE_MATCH] = localVer
                // download
                netAdapter.sendHttpOnBootDC(Origin.LOCAL, urlWithVer, headers, object : NetworkAdapter.HttpRequestCallback {
                    override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                        LogUtil.d(
                            "Get bs app onSendDataCallback, " +
                                    "statusCode=$statusCode, errCode=$errorCode"
                        )
                    }

                    override fun onMessageCallback(
                        status: Int,
                        msg: String,
                        headers: MutableMap<String, String>?,
                        body: ByteArray?
                    ) {
                        LogUtil.i("Get bs app onMessageCallback, status=$status")
                        when (status) {
                            NetworkAdapter.STATUS_CODE_OK -> {
                                var fos: FileOutputStream? = null
                                var zipFile: File? = null
                                val unzipDir: File? = pathManager.getFrameworkDir()
                                try {
                                    zipFile = pathManager.createCacheFile(fileName = "${System.currentTimeMillis()}.zip")
                                    fos = FileOutputStream(zipFile.absolutePath)
                                    fos.write(body)

                                    // unzip
                                    val delResult = unzipDir!!.deleteRecursively()
                                    ZipUtil.unzip(zipFile.absolutePath, unzipDir.absolutePath)
                                    htmlFile = pathManager.getBootstrapMiniAppHtml()
                                    val lastModified = htmlFile?.lastModified() ?: 0L
                                    LogUtil.d("Get bs mini-app, delResult:$delResult, lastModified:$lastModified")

                                    // TODO verify every time before render MNA

                                    // update db
                                    val newestVer = headers?.get("ETag") ?: ""
                                    val now = System.currentTimeMillis()
                                    val toSave = if (appInDb == null) {
                                        MiniApp(
                                            appId, htmlFile!!.absolutePath.toString(),
                                            newestVer, "", now, now, Origin.LOCAL.getName()
                                        )
                                    } else {
                                        appInDb.copy (version = newestVer, updateTime = now)
                                    }
                                    LogUtil.d("bs mini-app to save: $toSave")
                                    miniAppRepo.saveMiniAppAsync(toSave, object : Callback<Boolean> {
                                        override fun onResult(t: Boolean) {
                                            LogUtil.i("Save bs mini-app result=$t")
                                            markBsAppPrepared()
                                            callback.onResult(Results.success(htmlFile!!.absolutePath.toString()))
                                        }
                                    })
                                } catch (e: Exception) {
                                    LogUtil.e("Get bs app, write with err", e)
                                    callback.onResult(Results.failure(e))
                                    unzipDir?.deleteRecursively()
                                } finally {
                                    fos?.closeQuietly()
                                    zipFile?.delete()
                                }
                            }
                            NetworkAdapter.STATUS_CODE_NO_MODIFY -> {
                                LogUtil.d("bs mini-app no need to update")
                                htmlFile = pathManager.getBootstrapMiniAppHtml()
                                markBsAppPrepared()
                                callback.onResult(Results.success(htmlFile!!.absolutePath.toString()))
                            }
                            else -> {
                                markBsAppPrepared()
                                LogUtil.d("bs mini-app status fail, status=$status")
                                callback.onResult(Results.failure("Get bs app status=$status"))
                            }
                        }
                    }
                })
            }
        })
    }

    /**
     * request application list
     * @param url request url
     * @param callback result with absolute path of newest bootstrap mini-app
     */
    fun getApplicationList(
        url: String,
        callback: Callback<Results<String>>
    ) {
        LogUtil.d("GetApplicationList, url=$url")
        netAdapter.sendHttpOnBootDC(Origin.LOCAL, url, emptyMap(),
            object : NetworkAdapter.HttpRequestCallback {
                override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                    LogUtil.d("Get application list onSendDataCallback, status=$statusCode, err=$errorCode")
                }

                override fun onMessageCallback(
                    status: Int,
                    msg: String,
                    headers: MutableMap<String, String>?,
                    body: ByteArray?
                ) {
                    LogUtil.i("Get application list onMessageCallback, status=$status, bodyLen=${body?.size}")
                    if (status == NetworkAdapter.STATUS_CODE_OK) {
                        if(body != null){
                            val strBody = String(body)
                            LogUtil.d(
                                "Get application list, message back " +
                                        "status=$status, msg=$msg, headers=$headers, " +
                                        "strBody(part)=${strBody.subSequence(0, min(strBody.length, 1024))}"
                            )
                            callback.onResult(Results.success(strBody))
                        }
                    } else {
                        LogUtil.e("Get application list err, status=$status")
                        callback.onResult(Results.failure("status=$status"))
                    }
                }
            })
    }

    /**
     * request bootstrap mini-app, then update db
     * @param appId request appId
     * @param newestAppVer version of mini-app from applist.json
     * @param callback result with absolute path of newest mini-app
     */
    fun prepareMiniApp(
        origin: Origin,
        url: String,
        appId: String,
        newestAppVer: String,
        callback: Callback<Results<String>>
    ) {
        LogUtil.d("prepareMiniApp, appId=$appId, origin=${origin.getName()}")
        // prepare path
        var htmlFile: File? = pathManager.getMiniAppHtml(appId)
        // find cached version
        miniAppRepo.getLocalMiniAppAsync(appId, object : Callback<MiniApp?> {
            override fun onResult(t: MiniApp?) {
                var localVer = ""
                val appInDb = t
                if (appInDb != null) {
                    if (ConfigManager.alwaysDownloadMiniApp) {
                        LogUtil.i("Force download mna, appId=$appId")
                        localVer = appInDb.version
                    } else if (appInDb.version == newestAppVer && htmlFile != null && htmlFile!!.exists()) {
                        // no need update
                        LogUtil.i("No need to download mna, appId=$appId")
                        val toSave = appInDb.copy(
                            updateTime = System.currentTimeMillis(),
                            origin = origin.getName()
                        )
                        LogUtil.d("mini-app to save: $toSave")
                        miniAppRepo.saveMiniAppAsync(toSave, object : Callback<Boolean> {
                            override fun onResult(t: Boolean) {
                                LogUtil.i("Save mini-app result=$t")
                                callback.onResult(Results.success(htmlFile!!.absolutePath.toString()))
                            }
                        })
                        return
                    } else {
                        LogUtil.i("Try download mna, appId=$appId")
                        localVer = appInDb.version
                    }
                }
                // url
                val urlWithVer = url
                LogUtil.d("Get application, url=$urlWithVer")
                // header
                val headers: MutableMap<String, String> = HashMap()
                headers[HTTP_HEADER_IF_NONE_MATCH] = localVer
                // download
                netAdapter.sendHttpOnBootDC(origin, urlWithVer, headers, object : NetworkAdapter.HttpRequestCallback {
                    override fun onSendDataCallback(statusCode: Int, errorCode: Int) {
                        LogUtil.d(
                            "Get application, appId=$appId, " +
                                    "statusCode=$statusCode, errCode=$errorCode"
                        )
                    }

                    override fun onMessageCallback(
                        status: Int,
                        msg: String,
                        headers: MutableMap<String, String>?,
                        body: ByteArray?
                    ) {
                        LogUtil.i("Get application, appId=$appId, status=$status")
                        when (status) {
                            NetworkAdapter.STATUS_CODE_OK -> {
                                var fos: FileOutputStream? = null
                                var zipFile: File? = null
                                val unzipDir: File? = pathManager.getMiniAppDir(appId)
                                try {
                                    zipFile = pathManager.createCacheFile(fileName = "${appId}_${System.currentTimeMillis()}.zip")
                                    fos = FileOutputStream(zipFile.absolutePath)
                                    fos.write(body)

                                    // unzip
                                    val delResult = unzipDir!!.deleteRecursively()
                                    ZipUtil.unzip(zipFile.absolutePath, unzipDir.absolutePath)
                                    htmlFile = pathManager.getMiniAppHtml(appId)
                                    val lastModified = htmlFile?.lastModified() ?: 0L
                                    LogUtil.d("Get application, delResult:$delResult, lastModified:$lastModified")

                                    // TODO verify every time before render MNA

                                    // update db
                                    val now = System.currentTimeMillis()
                                    val toSave = if (appInDb == null) {
                                        MiniApp(
                                            appId, htmlFile!!.absolutePath.toString(),
                                            newestAppVer, "", now, now, origin.getName()
                                        )
                                    } else {
                                        appInDb.copy(
                                            version = newestAppVer,
                                            updateTime = now,
                                            origin = origin.getName()
                                        )
                                    }
                                    LogUtil.d("mini-app to save: $toSave")
                                    miniAppRepo.saveMiniAppAsync(toSave, object : Callback<Boolean> {
                                        override fun onResult(t: Boolean) {
                                            LogUtil.i("Save mini-app result=$t")
                                            callback.onResult(Results.success(htmlFile!!.absolutePath.toString()))
                                        }
                                    })
                                } catch (e: Exception) {
                                    LogUtil.e("Get application, write with err", e)
                                    callback.onResult(Results.failure(e))
                                    unzipDir?.deleteRecursively()
                                } finally {
                                    fos?.closeQuietly()
                                    zipFile?.delete()
                                }
                            }
                            NetworkAdapter.STATUS_CODE_NO_MODIFY -> {
                                htmlFile = pathManager.getMiniAppHtml(appId)
                                callback.onResult(Results.success(htmlFile!!.absolutePath.toString()))
                            }
                            else -> {
                                callback.onResult(Results.failure("Get application status=$status"))
                            }
                        }
                    }
                })
            }
        })
    }

    /**
     * find the local path of index.html of mini-app by appId.
     * @param appId of desired mini-app
     * @param callback result with absolute path
     */
    fun findMiniAppPath(appId: String, callback: Callback<Results<String>>) {
        miniAppRepo.getLocalMiniAppAsync(appId, object : Callback<MiniApp?> {
            override fun onResult(t: MiniApp?) {
                LogUtil.d("findMiniAppPath, appId=$appId, app=${t?.toString()}")
                if (t == null || t.path.isEmpty()
                    || !(FileUtil.exists(t.path, false)
                            && FileUtil.checkExtension(t.path, "html"))) {
                    callback.onResult(Results.failure(NewCallException("mini app not found")))
                } else {
                    callback.onResult(Results.success(t.path))
                }
            }
        })
    }

    /**
     * find the local path of index.html of bootstrap mini-app.
     * @param callback result with absolute path
     */
    fun findBootstrapAppPath(callback: Callback<Results<String>>) {
        findMiniAppPath(Constants.BOOTSTRAP_MINI_APP_ID, callback)
    }

    /**
     * find the version of stored mini-app by appId.
     * @param appId of desired mini-app
     * @param callback result with version
     */
    fun findMiniAppVer(appId: String, callback: Callback<Results<String>>) {
        miniAppRepo.getLocalMiniAppAsync(appId, object : Callback<MiniApp?> {
            override fun onResult(t: MiniApp?) {
                if (t == null) {
                    callback.onResult(Results.failure(NewCallException("mini app not found")))
                } else {
                    callback.onResult(Results.success(t.version))
                }
            }
        })
    }

    /**
     * find the stored mini-app by appId.
     * @param appId of desired mini-app
     * @param callback result with mini-app
     */
    fun findMiniApp(appId: String, callback: Callback<Results<MiniApp>>) {
        miniAppRepo.getLocalMiniAppAsync(appId, object : Callback<MiniApp?> {
            override fun onResult(t: MiniApp?) {
                if (t == null) {
                    callback.onResult(Results.failure(NewCallException("mini app not found")))
                } else {
                    callback.onResult(Results.success(t))
                }
            }
        })
    }

    /**
     * find the stored mini-app by appId list.
     * @param appIds of desired mini-apps
     * @param callback result with mini-apps
     */
    fun findMiniApp(appIds: List<String>, callback: Callback<Results<List<MiniApp>>>) {
        miniAppRepo.getLocalMiniAppsAsync(appIds, object : Callback<List<MiniApp>> {
            override fun onResult(t: List<MiniApp>) {
                if (t.isEmpty()) {
                    callback.onResult(Results.failure(NewCallException("mini app not found")))
                } else {
                    callback.onResult(Results.success(t))
                }
            }
        })
    }

    /**
     * save transfer file to file system.
     * @param remoteNum remote side phone number
     * @param base64 base64 of file
     * @param mime MIME type of file
     * @param callback result with file path
     */
    fun saveTransferFile(remoteNum: String, base64: String, mime: String,
                         fileName: String, fileSize: Long, callback: Callback<Results<File>>) {
//        val fileName: String = pathManager.genTransferFileName(mime)
        if (fileSize > TransferFileRepo.MAX_FILE_SIZE_BYTES) {
            callback.onResult(Results.failure("file exceeds threshold"))
            return
        }
        val file: File = pathManager.getTransferFile(remoteNum, fileName)
        if (file.exists()) {
            transferFileRepo.saveBase64ToFile(base64, file.path, object : Callback<Boolean> {
                override fun onResult(t: Boolean) {
                    if (t) {
                        callback.onResult(Results.success(file))
                    } else {
                        callback.onResult(Results.failure(NewCallException("file save fail")))
                    }
                }
            })
        } else {
            callback.onResult(Results.failure(NewCallException("file not exist")))
        }
    }

    /**
     * save transfer file to database.
     * @param file file to save
     * @param callback result with file path
     */
    fun saveTransferFileInfo(type: Int, direction: Int, mime: String, file: File, callback: Callback<Results<RespGetTransferFileRecords>>) {
        if (!file.exists() || !file.isFile) {
            callback.onResult(Results.failure(NewCallException("file save db not ready")))
            return
        }
        val remoteNum = CallSessionManager.getCallInfo().remoteNumber
        val sessionId = CallSessionManager.getSessionId()
        val msg = PersistentMessage(
            sessionId = sessionId,
            remoteNum = remoteNum,
            direction = direction,
            type = type,
            mimeType = mime,
            fileName = file.name,
            filePath = file.path,
            thumbnailMimeType = "",
            thumbnailPath = "",
            body = "",
            extBody = "",
            createTime = System.currentTimeMillis()
        )
        transferFileRepo.saveInfoToDB(msg, object : Callback<Boolean> {
            override fun onResult(t: Boolean) {
                if (t) {
                    val transferFileRecords = RespGetTransferFileRecords(
                        direction = msg.direction,
                        type = msg.type,
                        fileUri = Uri.fromFile(File(msg.filePath)).toString(),
                        timestamp = msg.createTime
                    )
                    callback.onResult(Results.success(transferFileRecords))
                } else {
                    callback.onResult(Results.failure(NewCallException("file save db fail")))
                }
            }
        })
    }

    /**
     * query transfer file records by remoteNumber from database.
     * @param callback result with file path
     */
    fun findTransferFileRecords(callback: Callback<Results<List<RespGetTransferFileRecords>>>) {
        val remoteNum = CallSessionManager.getCallInfo().remoteNumber
        val sessionId = CallSessionManager.getSessionId()
        transferFileRepo.findByRemoteNum(remoteNum, object : Callback<List<PersistentMessage>> {
            override fun onResult(t: List<PersistentMessage>) {
                if (t.isNullOrEmpty()) {
                    callback.onResult(Results.success(emptyList()))
                } else {
                    val records = t.map {
                        RespGetTransferFileRecords(
                            direction = it.direction,
                            type = it.type,
                            fileUri = Uri.fromFile(File(it.filePath)).toString(),
                            timestamp = it.createTime
                        )
                    }
                    callback.onResult(Results.success(records))
                }
            }
        })
    }

    /**
     * save file to local.
     * @param remoteNum remote side phone number
     * @param base64 base64 of file
     * @param type type of file
     * @param callback Callback<Results<Boolean>>
     */
    fun saveFileToLocal(remoteNum: String, base64: String, type: Int,
                        fileName: String, mime: String, callback: Callback<Results<Boolean>>) {
        if (type == TransferFileRepo.TYPE_IMAGE || type == TransferFileRepo.TYPE_VIDEO) {
            transferFileRepo.saveBase64ToLocal(base64, fileName, type, mime, object : Callback<Boolean> {
                override fun onResult(t: Boolean) {
                    if (t) {
                        callback.onResult(Results.success(t))
                    } else {
                        callback.onResult(Results.failure(NewCallException("file save album fail")))
                    }
                }
            })
        } else {
            val file = pathManager.getManuallySaveFile(remoteNum, fileName)
            if (file.exists()) {
                transferFileRepo.saveBase64ToFile(base64, file.path, object : Callback<Boolean> {
                    override fun onResult(t: Boolean) {
                        if (t) {
                            callback.onResult(Results.success(t))
                        } else {
                            callback.onResult(Results.failure(NewCallException("file save sdcard fail")))
                        }
                    }
                })
            } else {
                callback.onResult(Results.failure(NewCallException("file not exist")))
            }
        }
    }

    /**
     * check resources exists under app private space with given appId
     * @param appId app ID
     * @param paths path list of resources to check
     */
    fun checkFilesExists(appId: String, paths: List<String>): Map<String, Boolean> {
        return miniAppSpaceRepo.checkFilesExists(appId, paths)
    }

    /**
     * delete resources under app private space with given appId
     * @param appId app ID
     * @param paths path list of resources to delete
     */
    fun deleteFiles(appId: String, paths: List<String>): Map<String, Boolean> {
        return miniAppSpaceRepo.deleteFiles(appId, paths)
    }

    /**
     * save bytes as file in app private space with given appId
     * @param appId app ID
     * @param path path of file to save. if null, repo will generate a random path
     * @param contentType content-type
     * @param byteArray bytes of file
     * @param callback save file callback
     */
    fun saveFile(appId: String,
                 path: String?,
                 contentType: String,
                 byteArray: ByteArray,
                 callback: Callback<Results<String>>) {
        miniAppSpaceRepo.saveFile(appId, path, contentType, byteArray, callback)
    }
}