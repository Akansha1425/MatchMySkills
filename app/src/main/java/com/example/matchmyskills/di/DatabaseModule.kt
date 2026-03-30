package com.example.matchmyskills.di

import android.content.Context
import androidx.room.Room
import com.example.matchmyskills.data.local.*
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
            "match_my_skills_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideJobDao(database: AppDatabase): JobDao = database.jobDao()

    @Provides
    fun provideApplicationDao(database: AppDatabase): ApplicationDao = database.applicationDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("match_my_skills_prefs", Context.MODE_PRIVATE)
    }
}
