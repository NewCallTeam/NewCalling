package com.cmcc.newcalllib.manage.entity.handler.req

/**
 * For saveTransferFile
 */
data class ReqSaveTransferFile(
    val base64: String,
    val mime: String,
    val direction: Int,// 0:incoming, 1:outgoing
    val type: Int,// 0:text, 1:image, 2:video, 3:audio, 4:files, 5:location, 6:vcard, 7:cards
    val fileName: String,
    val fileSize: Long
)
