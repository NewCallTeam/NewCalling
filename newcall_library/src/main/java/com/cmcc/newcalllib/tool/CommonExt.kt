package com.cmcc.newcalllib.tool

import java.io.Closeable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.StandardCharsets


/**
 * extension functions
 * @author jihongfei
 * @createTime 2022/3/4 17:50
 */

fun String.toByteBuffer(): ByteBuffer {
    return ByteBuffer.wrap(this.toByteArray())
}

fun ByteBuffer.toStr(encoding: String = StandardCharsets.UTF_8.name()): String {
    val charset: Charset = Charset.forName(encoding)
    val decoder: CharsetDecoder = charset.newDecoder()//only once
    val charBuffer: CharBuffer = decoder.decode(this)
    return charBuffer.toString()
}

fun Closeable.closeQuietly() {
    try {
        this.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun Boolean?.toResult(): Int {
    return if (this == null) {
        0
    } else {
        if (this) 1 else 0
    }
}