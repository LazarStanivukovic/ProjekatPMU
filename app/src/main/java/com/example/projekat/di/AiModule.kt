package com.example.projekat.di

import com.example.projekat.data.ai.AiScheduleService
import com.example.projekat.data.ai.PollinationsAiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for AI scheduling dependencies.
 * Replaces the old NetworkModule which provided Retrofit.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun providePollinationsAiClient(): PollinationsAiClient {
        return PollinationsAiClient()
    }

    @Provides
    @Singleton
    fun provideAiScheduleService(
        aiClient: PollinationsAiClient
    ): AiScheduleService {
        return AiScheduleService(aiClient)
    }
}
