package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CourseDao
import com.example.data.local.dao.DailyLogDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.WorkspaceDao
import com.example.data.model.CourseEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.WorkspaceEntity

@Database(
    entities = [
        WorkspaceEntity::class,
        TaskEntity::class,
        ProjectEntity::class,
        CourseEntity::class,
        DailyLogEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun courseDao(): CourseDao
    abstract fun dailyLogDao(): DailyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun migrateToV7(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `calendar_events`")

            // Check existing columns in `tasks`
            val existingColumns = mutableSetOf<String>()
            db.query("PRAGMA table_info(`tasks`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0) {
                        existingColumns.add(cursor.getString(nameIndex))
                    }
                }
            }

            if (existingColumns.isNotEmpty()) {
                if ("scheduledDate" !in existingColumns) {
                    db.execSQL("ALTER TABLE `tasks` ADD COLUMN `scheduledDate` TEXT")
                }

                // If `googleCalendarEventId` exists from v6, rebuild `tasks` table to match v7 schema exactly
                if ("googleCalendarEventId" in existingColumns) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `tasks_new` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `isRecurring` INTEGER NOT NULL,
                            `repeatDays` TEXT NOT NULL,
                            `scheduledDate` TEXT,
                            `time` TEXT,
                            `reminderId` INTEGER NOT NULL,
                            `durationMins` INTEGER NOT NULL,
                            `loggedMins` INTEGER NOT NULL,
                            `subtasks` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `priority` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `completedAt` INTEGER,
                            `lastCompletedDate` TEXT,
                            `preCompletionLoggedMins` INTEGER NOT NULL,
                            `workspaceId` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `tasks_new` (
                            `id`, `title`, `description`, `isRecurring`, `repeatDays`,
                            `scheduledDate`, `time`, `reminderId`, `durationMins`, `loggedMins`,
                            `subtasks`, `isCompleted`, `priority`, `createdAt`, `completedAt`,
                            `lastCompletedDate`, `preCompletionLoggedMins`, `workspaceId`
                        )
                        SELECT
                            `id`, `title`, `description`, `isRecurring`, `repeatDays`,
                            `scheduledDate`, `time`, `reminderId`, `durationMins`, `loggedMins`,
                            `subtasks`, `isCompleted`, `priority`, `createdAt`, `completedAt`,
                            `lastCompletedDate`, `preCompletionLoggedMins`, `workspaceId`
                        FROM `tasks`
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE `tasks`")
                    db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")
                }
            }
        }

        private val MIGRATION_4_7 = object : Migration(4, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToV7(db)
        }

        private val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToV7(db)
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToV7(db)
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskflow_clean_v3.db"
                )
                    .addMigrations(MIGRATION_4_7, MIGRATION_5_7, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
