package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val classLevel: String,
    val joiningDate: String, // YYYY-MM-DD
    val monthlyFee: Double,
    val classType: String, // "Online" or "Offline"
    val paidStatus: String, // "Paid" or "Unpaid"
    val mobileNumber: String,
    val notes: String = "",
    val lastPaidMonth: String = "", // e.g. "2026-06"
    val syncStatus: String = "Pending", // "Synced" or "Pending"
    val isDeleted: Boolean = false
) : Serializable
