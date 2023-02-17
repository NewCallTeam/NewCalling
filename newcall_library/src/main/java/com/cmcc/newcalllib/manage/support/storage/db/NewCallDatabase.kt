package com.cmcc.newcalllib.manage.support.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * @author jihongfei
 * @createTime 2022/3/18 11:21
 */
@Database(entities = [MiniApp::class, PersistentMessage::class], version = 1, exportSchema = false)
abstract class NewCallDatabase : RoomDatabase() {
    abstract fun miniAppDao(): MiniAppDao
    abstract fun persistentMessageDao(): PersistentMessageDao

    companion object {
        private const val DB_NAME = "newcall-db"

        fun initialize(context: Context): NewCallDatabase {
            val db = Room.databaseBuilder(
                    context,
                    NewCallDatabase::class.java,
                    DB_NAME
            )
//            .apply {
//                fallbackToDestructiveMigration()
//                addMigrations(MIGRATION_1_2)
//            }
            .build()
            return db
        }
//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL()
//            }
//        }
    }
}