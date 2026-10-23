package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_tasks")
data class AutomationTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val format: String = "SHORTS",
    val language: String = "fa",
    val scheduledTimeStr: String,
    val status: String = "QUEUED", // QUEUED, IN_PROGRESS, COMPLETED, FAILED, PENDING_APPROVAL
    val logMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)
