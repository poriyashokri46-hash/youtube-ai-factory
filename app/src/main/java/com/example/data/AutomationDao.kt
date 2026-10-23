package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<AutomationTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AutomationTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<AutomationTask>)

    @Delete
    suspend fun deleteTask(task: AutomationTask)

    @Query("DELETE FROM automation_tasks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM automation_tasks")
    suspend fun getCount(): Int
}
