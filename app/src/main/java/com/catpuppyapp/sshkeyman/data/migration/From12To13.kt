package com.catpuppyapp.sshkeyman.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//手动迁移数据库示例代码
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        //在这写sql就行了
        database.execSQL("ALTER TABLE `repo` ADD COLUMN `parentRepoId` TEXT NOT NULL DEFAULT ''")
    }
}