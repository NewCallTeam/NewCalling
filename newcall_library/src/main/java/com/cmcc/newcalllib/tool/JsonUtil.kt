package com.cmcc.newcalllib.tool

import com.google.gson.*
import java.lang.Exception

/**
 * @author jihongfei
 * @createTime 2022/3/16 15:29
 */
object JsonUtil {
    private val mGson: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun getDefaultGson(): Gson {
        return mGson
    }

    fun <T> fromJson(json: String, classOfT: Class<T>): T {
        return getDefaultGson().fromJson(json, classOfT)
    }

    fun toJson(obj: Any): String {
        return getDefaultGson().toJson(obj)
    }

    fun strToJsonObject(str: String): JsonObject? {
        try {
            return mGson.fromJson(str, JsonObject::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun strToJsonArray(str: String): JsonArray? {
        try {
            return mGson.fromJson(str, JsonArray::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}