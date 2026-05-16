package com.aesthetic.tracker.di

import android.content.Context
import com.aesthetic.tracker.data.AestheticDao
import com.aesthetic.tracker.data.AestheticRepository
import com.aesthetic.tracker.data.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.create(context)

    @Provides
    fun provideAestheticDao(database: AppDatabase): AestheticDao = database.aestheticDao()

    @Provides
    @Singleton
    fun provideAestheticRepository(dao: AestheticDao): AestheticRepository = AestheticRepository(dao)
}
