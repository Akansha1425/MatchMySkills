package com.example.matchmyskills.data.local

import androidx.room.*
import com.example.matchmyskills.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE recruiterId = :recruiterId ORDER BY createdAt DESC")
    fun getJobsByRecruiter(recruiterId: String): Flow<List<Job>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<Job>)

    @Query("DELETE FROM jobs WHERE recruiterId = :recruiterId")
    suspend fun deleteJobsByRecruiter(recruiterId: String)
}

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    fun getApplicationsByJob(jobId: String): Flow<List<Application>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(applications: List<Application>)

    @Query("DELETE FROM applications WHERE jobId = :jobId")
    suspend fun deleteApplicationsByJob(jobId: String)
}

@Database(entities = [User::class, Job::class, Hackathon::class, Application::class], version = 6, exportSchema = false)
@TypeConverters(com.example.matchmyskills.util.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun applicationDao(): ApplicationDao
}
