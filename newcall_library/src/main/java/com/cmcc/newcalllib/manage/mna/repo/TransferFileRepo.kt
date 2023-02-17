package com.cmcc.newcalllib.manage.mna.repo

import com.cmcc.newcalllib.manage.support.Callback
import com.cmcc.newcalllib.manage.support.storage.db.PersistentMessage

/**
 * @author jihongfei
 * @createTime 2022/3/25 11:20
 */
interface TransferFileRepo {
    companion object {
        const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 //10M

        const val DIRECTION_INCOMING = 0
        const val DIRECTION_OUTGOING = 1

        const val TYPE_TEXT = 0
        const val TYPE_IMAGE = 1
        const val TYPE_VIDEO = 2
        const val TYPE_AUDIO = 3
        const val TYPE_FILES = 4
        const val TYPE_LOCATION = 5
        const val TYPE_VCARD = 6
        const val TYPE_CARDS = 7
    }

    fun saveBase64ToFile(base64: String, path: String, callback: Callback<Boolean>)

//    fun generateThumbnail(type: Int, file: File, callback: Callback<Boolean>)

    fun saveInfoToDB(transferFile: PersistentMessage, callback: Callback<Boolean>)

    fun findByRemoteNum(remoteNum: String, callback: Callback<List<PersistentMessage>>)

    fun saveBase64ToLocal(base64: String, fileName: String, type: Int, mime: String, callback: Callback<Boolean>)
}