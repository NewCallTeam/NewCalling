package com.cmcc.newcalllib.manage.support.storage

import android.content.Context
import android.content.SharedPreferences
import com.cmcc.newcalllib.tool.constant.Constants
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SpUtil constructor(context: Context, id: String?) {

    companion object {
        const val SP_PREFIX = Constants.APP_NAME
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences("${SP_PREFIX}_${id?:""}", Context.MODE_PRIVATE)

    fun put(key: String, value: String?) {
        preferences.edit()
            .putString(key, value)
            .apply()
    }

    fun remove(key: String) {
        preferences.edit()
            .remove(key)
            .apply()
    }

    fun clear() {
        preferences.edit()
            .clear()
            .apply()
    }

    fun get(key: String): String {
        return preferences.getString(key, "") ?: ""
    }

    //can define keys with type
    var appListJson: String? by SharedPreferenceDelegates.string("")

    private object SharedPreferenceDelegates {

        fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SpUtil, Int> {

            override fun getValue(thisRef: SpUtil, property: KProperty<*>): Int {
                return thisRef.preferences.getInt(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpUtil, property: KProperty<*>, value: Int) {
                thisRef.preferences.edit().putInt(property.name, value).apply()
            }
        }

        fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SpUtil, Long> {

            override fun getValue(thisRef: SpUtil, property: KProperty<*>): Long {
                return thisRef.preferences.getLong(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpUtil, property: KProperty<*>, value: Long) {
                thisRef.preferences.edit().putLong(property.name, value).apply()
            }
        }

        fun boolean(defaultValue: Boolean = false) = object : ReadWriteProperty<SpUtil, Boolean> {
            override fun getValue(thisRef: SpUtil, property: KProperty<*>): Boolean {
                return thisRef.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpUtil, property: KProperty<*>, value: Boolean) {
                thisRef.preferences.edit().putBoolean(property.name, value).apply()
            }
        }

        fun float(defaultValue: Float = 0.0f) = object : ReadWriteProperty<SpUtil, Float> {
            override fun getValue(thisRef: SpUtil, property: KProperty<*>): Float {
                return thisRef.preferences.getFloat(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpUtil, property: KProperty<*>, value: Float) {
                thisRef.preferences.edit().putFloat(property.name, value).apply()
            }
        }

        fun string(defaultValue: String? = null) = object : ReadWriteProperty<SpUtil, String?> {
            override fun getValue(thisRef: SpUtil, property: KProperty<*>): String? {
                return thisRef.preferences.getString(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpUtil, property: KProperty<*>, value: String?) {
                thisRef.preferences.edit().putString(property.name, value).apply()
            }
        }

        fun setString(defaultValue: Set<String>? = null) = object : ReadWriteProperty<SpUtil, Set<String>?> {
            override fun getValue(thisRef: SpUtil, property: KProperty<*>): Set<String>? {
                return thisRef.preferences.getStringSet(property.name, defaultValue)
            }

            override fun setValue(thisRef: SpUtil, property: KProperty<*>, value: Set<String>?) {
                thisRef.preferences.edit().putStringSet(property.name, value).apply()
            }
        }
    }
}