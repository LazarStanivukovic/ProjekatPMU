package com.example.projekat.di

import android.content.Context
import androidx.room.Room
import com.example.projekat.data.local.AppDatabase
import com.example.projekat.data.local.MIGRATION_1_2
import com.example.projekat.data.local.MIGRATION_2_3
import com.example.projekat.data.local.MIGRATION_3_4
import com.example.projekat.data.local.MIGRATION_4_5
import com.example.projekat.data.local.MIGRATION_5_6
import com.example.projekat.data.local.NoteDao
import com.example.projekat.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "projekat_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }
}
