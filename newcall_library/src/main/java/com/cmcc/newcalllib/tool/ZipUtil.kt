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

package com.cmcc.newcalllib.tool

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Utils for zip and unzip files
 */
@Suppress("MemberVisibilityCanBePrivate")
object ZipUtil {
    const val BUFFER_SIZE = 4096

    fun zip(files: List<File>, zipFilePath: String) {
        if (files.isEmpty()) return

        val zipFile = FileUtil.createFile(zipFilePath)
        val buffer = ByteArray(BUFFER_SIZE)
        var zipOutputStream: ZipOutputStream? = null
        var inputStream: FileInputStream? = null
        try {
            zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
            for (file in files) {
                if (!file.exists()) continue
                zipOutputStream.putNextEntry(ZipEntry(file.name))
                inputStream = FileInputStream(file)
                var len: Int
                while (inputStream.read(buffer).also { len = it } > 0) {
                    zipOutputStream.write(buffer, 0, len)
                }
                zipOutputStream.closeEntry()
            }
        } finally {
            inputStream?.close()
            zipOutputStream?.close()
        }
    }

    fun zipFilesInFolder(fileDir: String, zipFilePath: String) {
        val folder = File(fileDir)
        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles() ?: return
            val filesList: List<File> = files.toList()
            zip(filesList, zipFilePath)
        }
    }

    fun unzip(zipFile: String, descDir: String) {
        val buffer = ByteArray(BUFFER_SIZE)
        var outputStream: OutputStream? = null
        var inputStream: InputStream? = null
        try {
            val zf = ZipFile(zipFile)
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val zipEntry: ZipEntry = entries.nextElement() as ZipEntry
                val zipEntryName: String = zipEntry.name
                val descFilePath: String = descDir + File.separator + zipEntryName
                val descFile = File(descFilePath)
                if (zipEntry.isDirectory) {
                    descFile.mkdirs()
                } else {
                    FileUtil.createFile(descFilePath)
                    inputStream = zf.getInputStream(zipEntry)
                    outputStream = FileOutputStream(descFile)

                    var len: Int
                    while (inputStream.read(buffer).also { len = it } > 0) {
                        outputStream.write(buffer, 0, len)
                    }
                    inputStream.closeQuietly()
                    outputStream.closeQuietly()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.closeQuietly()
            outputStream?.closeQuietly()
        }
    }
}
