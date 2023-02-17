package com.cmcc.newcalllib.manage.support.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PersistentMessage(
    @PrimaryKey(autoGenerate = true) val _id: Long = 0L,
    val sessionId: String, // 会话 id
    val remoteNum: String, // 对端号码
    val direction: Int, // 消息传输方向：0 为发送，1 为接收
    val type: Int, // 消息类型：0 文本，1 图片，2 视频，3 音频，4 其他文件，5 位置，6 VCard，7 卡片类
    val mimeType: String, // MIME 类型
    val fileName: String, // 文件名，包含扩展名
    val filePath: String, // 文件的存储路径
    val thumbnailMimeType: String, // 缩略图 MIME 类型
    val thumbnailPath: String, // 缩略图存储路径
    val body: String, // 文本消息内容
    val extBody: String, // 扩展内容
    val createTime: Long // 创建时间
)
