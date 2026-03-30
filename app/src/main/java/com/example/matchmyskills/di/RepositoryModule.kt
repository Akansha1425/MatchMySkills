package com.example.matchmyskills.di

import android.content.SharedPreferences
import com.example.matchmyskills.data.local.*
import com.example.matchmyskills.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        prefs: SharedPreferences
    ): AuthRepository = AuthRepository(auth, firestore, prefs)

    @Provides
    @Singleton
    fun provideJobRepository(
        firestore: FirebaseFirestore,
        jobDao: JobDao
    ): JobRepository = JobRepository(firestore, jobDao)

    @Provides
    @Singleton
    fun provideApplicationRepository(
        firestore: FirebaseFirestore,
        applicationDao: ApplicationDao
    ): ApplicationRepository = ApplicationRepository(firestore, applicationDao)
}
