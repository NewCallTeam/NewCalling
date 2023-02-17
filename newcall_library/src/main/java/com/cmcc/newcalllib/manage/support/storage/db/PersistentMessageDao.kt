package com.cmcc.newcalllib.manage.support.storage.db

import androidx.room.*

@Dao
interface PersistentMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg msg: PersistentMessage)

    @Update
    fun update(vararg msg: PersistentMessage)

    @Delete
    fun delete(msg: PersistentMessage)

    @Query("SELECT * FROM PersistentMessage WHERE _id = :id")
    fun getOne(id: Long): PersistentMessage?

    @Query("SELECT * FROM PersistentMessage WHERE sessionId = :sessionId")
    fun getBySessionId(sessionId: String): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE sessionId = :sessionId AND type in (:types)")
    fun getBySessionId(sessionId: String, types: List<Int>): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE sessionId = :sessionId AND direction = :direction")
    fun getBySessionId(sessionId: String, direction: Int): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE sessionId = :sessionId AND type in (:types) AND direction = :direction")
    fun getBySessionId(sessionId: String, types: List<Int>, direction: Int): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE remoteNum = :remoteNum")
    fun getByRemoteNum(remoteNum: String): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE remoteNum = :remoteNum AND type in (:types)")
    fun getByRemoteNum(remoteNum: String, types: List<Int>): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE remoteNum = :remoteNum AND direction = :direction")
    fun getByRemoteNum(remoteNum: String, direction: Int): List<PersistentMessage>

    @Query("SELECT * FROM PersistentMessage WHERE remoteNum = :remoteNum AND type in (:types) AND direction = :direction")
    fun getByRemoteNum(remoteNum: String, types: List<Int>, direction: Int): List<PersistentMessage>
}