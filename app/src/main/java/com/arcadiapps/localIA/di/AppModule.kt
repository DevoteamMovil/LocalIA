package com.arcadiapps.localIA.di

import android.content.Context
import androidx.room.Room
import com.arcadiapps.localIA.data.dao.AIModelDao
import com.arcadiapps.localIA.data.dao.ChatMessageDao
import com.arcadiapps.localIA.data.dao.ChatSessionDao
import com.arcadiapps.localIA.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "localIA.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideModelDao(db: AppDatabase): AIModelDao = db.aiModelDao()
    @Provides fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()
    @Provides fun provideChatSessionDao(db: AppDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // sin timeout para descargas grandes
        .build()
}
