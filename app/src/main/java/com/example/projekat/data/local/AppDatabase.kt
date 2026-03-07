package com.example.projekat.data.local

import androidx.room.Database
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

@Database(
    entities = [Note::class, Task::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
}
