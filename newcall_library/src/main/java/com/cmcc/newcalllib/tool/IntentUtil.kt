package com.cmcc.newcalllib.tool

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

/**
 * @author jihongfei
 * @createTime 2022/4/26 15:09
 */
object IntentUtil {
    /**
     * handle choose image intent result on devices above 4.4
     */
    fun handleImageOnKitKat(data: Intent, context: Context?): String? {
        val uri = data.data
        return handleImageOnKitKat(uri, context)
    }

    fun handleImageOnKitKat(uri: Uri?, context: Context?): String? {
        var imagePath: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.providers.media.documents" == uri!!.authority) {
                val id = docId.split(":").toTypedArray()[1]
                val selection = MediaStore.Images.Media._ID + "=" + id
                LogUtil.d("id=$id,selection=$selection")
                imagePath =
                    getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, context)
            } else if ("com.android.providers.downloads.documents" == uri.authority) {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(docId)
                )
                imagePath = getImagePath(contentUri, null, context)
            }
        } else if ("content".equals(uri!!.scheme, ignoreCase = true)) {
            imagePath = getImagePath(uri, null, context)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            imagePath = uri.path
        }
        return imagePath
    }

    /**
     * handle choose image intent result on devices below 4.4
     */
    fun handleImageBeforeKitKat(data: Intent, context: Context?): String? {
        val uri = data.data
        return getImagePath(uri, null, context)
    }

    /**
     * find path in given uri and selection
     */
    private fun getImagePath(uri: Uri?, selection: String?, context: Context?): String? {
        var path: String? = null
        val cursor: Cursor? = context?.contentResolver?.query(uri!!, null, selection, null, null)
        if (cursor != null) {
            var i = 0
            while (i < cursor.columnCount) {
                var ss = cursor.getColumnName(i)
                i++
            }
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                LogUtil.d("get path $path")
            }
            cursor.close()
        }
        return path
    }

    fun getFileResultPath(uri: Uri, context: Context): String? {
        var chooseFilePath: String? = null
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            chooseFilePath = uri.path
            return chooseFilePath
        }
        return getPath(uri, context)
    }

    private fun getPath(uri: Uri, context: Context): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val documentId = DocumentsContract.getDocumentId(uri)
                val split = documentId.split(":")
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory()}${File.separator}${split[1]}"
                }
            } else if (isDownloadsDocument(uri)) {
                // DownloadsProvider
                val documentId = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                    documentId.toLong())
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                // MediaProvider
                val documentId = DocumentsContract.getDocumentId(uri)
                val split = documentId.split(":")
                val type = split[0]

                var mediaContentUri: Uri? = null
                if ("image".equals(type)) {
                    mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("audio".equals(type)) {
                    mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                } else if ("video".equals(type)) {
                    mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, mediaContentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // MediaStore (and general)
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            // File
            return uri.path
        }
        return null
    }

    /**
     * *
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = uri?.let { context.contentResolver.query(it, projection, selection, selectionArgs, null) }
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            LogUtil.e("getDataColumn Failed to obtain the file path: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}