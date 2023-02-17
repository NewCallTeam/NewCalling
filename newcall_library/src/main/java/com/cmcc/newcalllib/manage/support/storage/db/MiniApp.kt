package com.cmcc.newcalllib.manage.support.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author jihongfei
 * @createTime 2022/3/18 11:10
 */
@Entity
data class MiniApp(
        @PrimaryKey val appId: String,
        val path: String,//local path of index.html
        val version: String,//version of mini-app
        val number: String,//no use now
        val createTime: Long,//millis
        val updateTime: Long,//millis
        val origin: String//local or remote
)
