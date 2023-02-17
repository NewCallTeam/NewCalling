/*
 * Copyright (c) 2023 China Mobile Communications Group Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("VideoExt")

package com.cmcc.newcalllib.tool

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "VideoExt"

// 保存位置，这里使用 Picures，也可以改为 DCIM
private val ALBUM_DIR = Environment.DIRECTORY_PICTURES

/**
 * 用于 Q 以下系统获取视频文件大小来更新 [MediaStore.Video.Media.SIZE]
 */
private class OutputVideoFileTaker(var file: File? = null)

/**
 * 保存视频 Stream 到相册的 Pictures 文件夹
 *
 * @param context 上下文
 * @param fileName 文件名。需要携带后缀
 * @param relativePath 相对于 Pictures 的路径
 */
fun InputStream.saveVideoToAlbum(context: Context, fileName: String, relativePath: String?, mime: String): Uri? {
    val resolver = context.contentResolver
    val outputFile = OutputVideoFileTaker()
    val videoUri = resolver.insertMediaVideo(fileName, relativePath, outputFile, mime)
    if (videoUri == null) {
        Log.w(TAG, "insert: error: uri == null")
        return null
    }

    (videoUri.outputStream(resolver) ?: return null).use { output ->
        this.use { input ->
            input.copyTo(output)
            videoUri.finishPending(context, resolver, outputFile.file)
        }
    }
    return videoUri
}

private fun Uri.outputStream(resolver: ContentResolver): OutputStream? {
    return try {
        resolver.openOutputStream(this)
    } catch (e: FileNotFoundException) {
        Log.e(TAG, "save: open stream error: $e")
        null
    }
}

private fun Uri.finishPending(
    context: Context,
    resolver: ContentResolver,
    outputFile: File?,
) {
    val videoValues = ContentValues()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (outputFile != null) {
            videoValues.put(MediaStore.Video.Media.SIZE, outputFile.length())
        }
        resolver.update(this, videoValues, null, null)
        // 通知媒体库更新
        val intent = Intent(@Suppress("DEPRECATION") Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, this)
        context.sendBroadcast(intent)
    } else {
        // Android Q 添加了 IS_PENDING 状态，为 0 时其他应用才可见
        videoValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(this, videoValues, null, null)
    }
}

/**
 * 插入视频到媒体库
 */
private fun ContentResolver.insertMediaVideo(
    fileName: String,
    relativePath: String?,
    outputVideoFileTaker: OutputVideoFileTaker? = null,
    mime: String
): Uri? {
    // 视频信息
    val videoValues = ContentValues().apply {
        put(MediaStore.Video.Media.MIME_TYPE, mime)
        val date = System.currentTimeMillis() / 1000
        put(MediaStore.Video.Media.DATE_ADDED, date)
        put(MediaStore.Video.Media.DATE_MODIFIED, date)
    }
    // 保存的位置
    val collection: Uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val path = if (relativePath != null) "${ALBUM_DIR}/${relativePath}" else ALBUM_DIR
        videoValues.apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.RELATIVE_PATH, path)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        // 高版本不用查重直接插入，会自动重命名
    } else {
        // 老版本
        val pictures =
            @Suppress("DEPRECATION") Environment.getExternalStoragePublicDirectory(ALBUM_DIR)
        val saveDir = if (relativePath != null) File(pictures, relativePath) else pictures

        if (!saveDir.exists() && !saveDir.mkdirs()) {
            Log.e(TAG, "save: error: can't create Pictures directory")
            return null
        }

        // 文件路径查重，重复的话在文件名后拼接数字
        var videoFile = File(saveDir, fileName)
        val fileNameWithoutExtension = videoFile.nameWithoutExtension
        val fileExtension = videoFile.extension

        // 查询文件是否已经存在
        var queryUri = this.queryMediaVideo28(videoFile.absolutePath)
        var suffix = 1
        while (queryUri != null) {
            // 存在的话重命名，路径后面拼接 fileNameWithoutExtension(数字).mp4
            val newName = fileNameWithoutExtension + "(${suffix++})." + fileExtension
            videoFile = File(saveDir, newName)
            queryUri = this.queryMediaVideo28(videoFile.absolutePath)
        }

        videoValues.apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
            // 保存路径
            val videoPath = videoFile.absolutePath
            Log.v(TAG, "save file: $videoPath")
            put(@Suppress("DEPRECATION") MediaStore.Video.Media.DATA, videoPath)
        }
        outputVideoFileTaker?.file = videoFile // 回传文件路径，用于设置文件大小
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
    // 插入视频信息
    return this.insert(collection, videoValues)
}

/**
 * Android Q以下版本，查询媒体库中当前路径是否存在
 * @return Uri 返回null时说明不存在，可以进行视频插入逻辑
 */
private fun ContentResolver.queryMediaVideo28(videoPath: String): Uri? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null

    val videoFile = File(videoPath)
    if (videoFile.canRead() && videoFile.exists()) {
        Log.v(TAG, "query: path: $videoFile exists")
        // 文件已存在，返回一个file://xxx的uri
        return Uri.fromFile(videoFile)
    }
    // 保存的位置
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    // 查询是否已经存在相同视频
    val query = this.query(
        collection,
        arrayOf(MediaStore.Video.Media._ID, @Suppress("DEPRECATION") MediaStore.Video.Media.DATA),
        "${@Suppress("DEPRECATION") MediaStore.Video.Media.DATA} == ?",
        arrayOf(videoPath), null
    )

    query?.use {
        while (it.moveToNext()) {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val id = it.getLong(idColumn)
            val existsUri = ContentUris.withAppendedId(collection, id)
            Log.v(TAG, "query: path: $videoPath exists uri: $existsUri")
            return existsUri
        }
    }
    return null
}