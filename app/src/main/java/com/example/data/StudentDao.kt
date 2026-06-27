package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Int): Student?

    @Query("SELECT * FROM students WHERE syncStatus = 'Pending'")
    suspend fun getUnsyncedStudents(): List<Student>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Query("UPDATE students SET isDeleted = 1, syncStatus = 'Pending' WHERE id = :id")
    suspend fun softDeleteStudent(id: Int)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun hardDeleteStudent(id: Int)

    @Query("SELECT * FROM students")
    suspend fun getAllRawStudents(): List<Student>
}
