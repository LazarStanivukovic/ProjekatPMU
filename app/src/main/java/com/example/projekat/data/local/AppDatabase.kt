package com.example.projekat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.Task

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new column imageUris (JSON string, default empty array)
        db.execSQL("ALTER TABLE notes ADD COLUMN imageUris TEXT NOT NULL DEFAULT '[]'")
        // Migrate existing imageUri data into imageUris as a JSON array
        db.execSQL("""
            UPDATE notes SET imageUris = '[\"' || imageUri || '\"]'
            WHERE imageUri IS NOT NULL AND imageUri != ''
        """)
        // We cannot drop the old column in SQLite easily, but Room will ignore it
        // since it's no longer in the entity. It will be cleaned up on next
        // destructive migration or can be left as-is (harmless).
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add colorIndex column to tasks table (default 0 = first color)
        db.execSQL("ALTER TABLE tasks ADD COLUMN colorIndex INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add priority column to tasks table (default MEDIUM)
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add checklistItems column to notes table (default empty JSON array)
        db.execSQL("ALTER TABLE notes ADD COLUMN checklistItems TEXT NOT NULL DEFAULT '[]'")
        // Add checklistItems column to tasks table (default empty JSON array)
        db.execSQL("ALTER TABLE tasks ADD COLUMN checklistItems TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add location fields to tasks table for geofencing feature
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationLat REAL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationLng REAL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationName TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN locationRadius INTEGER NOT NULL DEFAULT 100")
    }
}

@Database(
    entities = [Note::class, Task::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance for use outside of Hilt (e.g., BroadcastReceiver).
         * For normal dependency injection, use DatabaseModule.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projekat_database"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
