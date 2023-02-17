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

@file:JvmName("ImageExt")
@file:Suppress("unused")

package com.cmcc.newcalllib.tool

import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream


private const val TAG = "ImageExt"

// 保存位置，这里使用 Picures，也可以改为 DCIM
private val ALBUM_DIR = Environment.DIRECTORY_PICTURES

/**
 * 用于 Q 以下系统获取图片文件大小来更新 [MediaStore.Images.Media.SIZE]
 */
private class OutputFileTaker(var file: File? = null)

/**
 * 复制图片文件到相册的 Pictures 文件夹
 *
 * @param context 上下文
 * @param fileName 文件名。需要携带后缀
 * @param relativePath 相对于 Pictures 的路径
 */
fun File.copyToAlbum(context: Context, fileName: String, relativePath: String?, mime: String): Uri? {
    if (!this.canRead() || !this.exists()) {
        Log.w(TAG, "check: read file error: $this")
        return null
    }
    return this.inputStream().use {
        it.saveImageToAlbum(context, fileName, relativePath, mime)
    }
}

/**
 * 保存图片 Stream 到相册的 Pictures 文件夹
 *
 * @param context 上下文
 * @param fileName 文件名。需要携带后缀
 * @param relativePath 相对于 Pictures 的路径
 */
fun InputStream.saveImageToAlbum(context: Context, fileName: String, relativePath: String?, mime: String): Uri? {
    val resolver = context.contentResolver
    val outputFile = OutputFileTaker()
    val imageUri = resolver.insertMediaImage(fileName, relativePath, outputFile, mime)
    if (imageUri == null) {
        Log.w(TAG, "insert: error: uri == null")
        return null
    }

    (imageUri.outputStream(resolver) ?: return null).use { output ->
        this.use { input ->
            input.copyTo(output)
            imageUri.finishPending(context, resolver, outputFile.file)
        }
    }
    return imageUri
}

/**
 * 保存 Bitmap 到相册的 Pictures 文件夹
 *
 * https://developer.android.google.cn/training/data-storage/shared/media
 *
 * @param context 上下文
 * @param fileName 文件名。需要携带后缀
 * @param relativePath 相对于 Pictures 的路径
 * @param quality 质量
 */
fun Bitmap.saveToAlbum(
    context: Context,
    fileName: String,
    relativePath: String? = null,
    quality: Int = 75,
    mime: String
): Uri? {
    // 插入图片信息
    val resolver = context.contentResolver
    val outputFile = OutputFileTaker()
    val imageUri = resolver.insertMediaImage(fileName, relativePath, outputFile, mime)
    if (imageUri == null) {
        Log.w(TAG, "insert: error: uri == null")
        return null
    }

    // 保存图片
    (imageUri.outputStream(resolver) ?: return null).use {
        val format = fileName.getBitmapFormat()
        this@saveToAlbum.compress(format, quality, it)
        imageUri.finishPending(context, resolver, outputFile.file)
    }
    return imageUri
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
    val imageValues = ContentValues()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (outputFile != null) {
            imageValues.put(MediaStore.Images.Media.SIZE, outputFile.length())
        }
        resolver.update(this, imageValues, null, null)
        // 通知媒体库更新
        val intent = Intent(@Suppress("DEPRECATION") Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, this)
        context.sendBroadcast(intent)
    } else {
        // Android Q 添加了 IS_PENDING 状态，为 0 时其他应用才可见
        imageValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(this, imageValues, null, null)
    }
}

private fun String.getBitmapFormat(): Bitmap.CompressFormat {
    val fileName = this.lowercase()
    return when {
        fileName.endsWith(".png") -> Bitmap.CompressFormat.PNG
        fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> Bitmap.CompressFormat.JPEG
        fileName.endsWith(".webp") -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.PNG
    }
}

/**
 * 插入图片到媒体库
 */
private fun ContentResolver.insertMediaImage(
    fileName: String,
    relativePath: String?,
    outputFileTaker: OutputFileTaker? = null,
    mime: String
): Uri? {
    // 图片信息
    val imageValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        val date = System.currentTimeMillis() / 1000
        put(MediaStore.Images.Media.DATE_ADDED, date)
        put(MediaStore.Images.Media.DATE_MODIFIED, date)
    }
    // 保存的位置
    val collection: Uri
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val path = if (relativePath != null) "${ALBUM_DIR}/${relativePath}" else ALBUM_DIR
        imageValues.apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.RELATIVE_PATH, path)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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
        var imageFile = File(saveDir, fileName)
        val fileNameWithoutExtension = imageFile.nameWithoutExtension
        val fileExtension = imageFile.extension

        // 查询文件是否已经存在
        var queryUri = this.queryMediaImage28(imageFile.absolutePath)
        var suffix = 1
        while (queryUri != null) {
            // 存在的话重命名，路径后面拼接 fileNameWithoutExtension(数字).png
            val newName = fileNameWithoutExtension + "(${suffix++})." + fileExtension
            imageFile = File(saveDir, newName)
            queryUri = this.queryMediaImage28(imageFile.absolutePath)
        }

        imageValues.apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
            // 保存路径
            val imagePath = imageFile.absolutePath
            Log.v(TAG, "save file: $imagePath")
            put(@Suppress("DEPRECATION") MediaStore.Images.Media.DATA, imagePath)
        }
        outputFileTaker?.file = imageFile // 回传文件路径，用于设置文件大小
        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    // 插入图片信息
    return this.insert(collection, imageValues)
}

/**
 * Android Q以下版本，查询媒体库中当前路径是否存在
 * @return Uri 返回null时说明不存在，可以进行图片插入逻辑
 */
private fun ContentResolver.queryMediaImage28(imagePath: String): Uri? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null

    val imageFile = File(imagePath)
    if (imageFile.canRead() && imageFile.exists()) {
        Log.v(TAG, "query: path: $imagePath exists")
        // 文件已存在，返回一个file://xxx的uri
        return Uri.fromFile(imageFile)
    }
    // 保存的位置
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    // 查询是否已经存在相同图片
    val query = this.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID, @Suppress("DEPRECATION") MediaStore.Images.Media.DATA),
        "${@Suppress("DEPRECATION") MediaStore.Images.Media.DATA} == ?",
        arrayOf(imagePath), null
    )
    query?.use {
        while (it.moveToNext()) {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = it.getLong(idColumn)
            val existsUri = ContentUris.withAppendedId(collection, id)
            Log.v(TAG, "query: path: $imagePath exists uri: $existsUri")
            return existsUri
        }
    }
    return null
}
