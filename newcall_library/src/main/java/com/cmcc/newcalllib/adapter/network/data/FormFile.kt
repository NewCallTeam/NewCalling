package com.cmcc.newcalllib.adapter.network.data

import java.io.File

/**
 * @author jihongfei
 * @createTime 2022/3/4 11:26
 */
data class FormFile(
        val fileName: String,
        val contentType: String,
        val file: File
)
