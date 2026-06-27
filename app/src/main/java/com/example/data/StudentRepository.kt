package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class StudentRepository(private val studentDao: StudentDao, private val context: Context) {

    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()

    suspend fun getStudentById(id: Int): Student? = withContext(Dispatchers.IO) {
        studentDao.getStudentById(id)
    }

    suspend fun insertStudent(student: Student) = withContext(Dispatchers.IO) {
        studentDao.insertStudent(student.copy(syncStatus = "Pending"))
    }

    suspend fun updateStudent(student: Student) = withContext(Dispatchers.IO) {
        studentDao.updateStudent(student.copy(syncStatus = "Pending"))
    }

    suspend fun softDeleteStudent(id: Int) = withContext(Dispatchers.IO) {
        studentDao.softDeleteStudent(id)
    }

    suspend fun hardDeleteStudent(id: Int) = withContext(Dispatchers.IO) {
        studentDao.hardDeleteStudent(id)
    }

    // Synchronize local data with Google Sheets Web App
    suspend fun syncWithGoogleSheets(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) {
            return@withContext Result.failure(Exception("Google Sheets Web App URL is not configured."))
        }

        try {
            // Get all raw students from the database, including soft-deleted ones
            val rawStudents = studentDao.getAllRawStudents()
            
            // Map to a list of maps for simple serialization
            val studentListPayload = rawStudents.map { student ->
                mapOf(
                    "id" to student.id.toString(),
                    "name" to student.name,
                    "classLevel" to student.classLevel,
                    "joiningDate" to student.joiningDate,
                    "monthlyFee" to student.monthlyFee.toString(),
                    "classType" to student.classType,
                    "paidStatus" to student.paidStatus,
                    "mobileNumber" to student.mobileNumber,
                    "notes" to student.notes,
                    "lastPaidMonth" to student.lastPaidMonth,
                    "isDeleted" to student.isDeleted.toString()
                )
            }

            val payload = mapOf(
                "action" to "sync",
                "students" to studentListPayload
            )

            // Setup Moshi for JSON serialization
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            val jsonPayload = adapter.toJson(payload)

            // Setup OkHttpClient with generous timeouts for sheets sync
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.d("SheetsSync", "Sync Response: $responseBody")

                // Update local status of synced students
                rawStudents.forEach { student ->
                    if (student.isDeleted) {
                        // Hard delete from local DB since it is successfully deleted from cloud
                        studentDao.hardDeleteStudent(student.id)
                    } else if (student.syncStatus == "Pending") {
                        // Mark as Synced
                        studentDao.updateStudent(student.copy(syncStatus = "Synced"))
                    }
                }
                Result.success("Sync completed successfully!")
            } else {
                Result.failure(Exception("Sync failed. Server responded with code: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("SheetsSync", "Sync exception: ", e)
            Result.failure(e)
        }
    }
}
