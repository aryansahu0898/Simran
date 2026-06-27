package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.Student
import com.example.data.StudentDatabase
import com.example.data.StudentRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TuitionViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudentDatabase.getDatabase(application)
    private val repository = StudentRepository(database.studentDao(), application)
    private val prefs = PreferencesManager(application)

    // Teacher authentication & session state
    val isLoggedIn = MutableStateFlow(prefs.isLoggedIn)
    val teacherEmailOrMobile = MutableStateFlow(prefs.teacherEmailOrMobile)
    val sheetsWebAppUrl = MutableStateFlow(prefs.sheetsWebAppUrl)

    // User inputs & filter states
    val searchQuery = MutableStateFlow("")
    val classFilter = MutableStateFlow("All")
    val paidFilter = MutableStateFlow("All")
    val classTypeFilter = MutableStateFlow("All")

    // Sync state
    val isSyncing = MutableStateFlow(false)
    private val _syncEvent = MutableSharedFlow<String>()
    val syncEvent: SharedFlow<String> = _syncEvent.asSharedFlow()

    // Retrieve all active students flow
    private val rawStudentsList: StateFlow<List<Student>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined filtered students list
    val studentsList: StateFlow<List<Student>> = combine(
        rawStudentsList,
        searchQuery,
        classFilter,
        paidFilter,
        classTypeFilter
    ) { students, query, cls, paid, type ->
        students.filter { student ->
            val matchesQuery = student.name.contains(query, ignoreCase = true) || 
                               student.classLevel.contains(query, ignoreCase = true)
            val matchesClass = cls == "All" || student.classLevel.equals(cls, ignoreCase = true)
            val matchesPaid = paid == "All" || student.paidStatus.equals(paid, ignoreCase = true)
            val matchesType = type == "All" || student.classType.equals(type, ignoreCase = true)

            matchesQuery && matchesClass && matchesPaid && matchesType
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactively computed statistics for the Dashboard
    val dashboardStats: StateFlow<DashboardStats> = rawStudentsList
        .combine(studentsList) { allActive, _ ->
            // Dashboard stats should reflect all active student aggregates
            val totalCount = allActive.size
            val totalCollection = allActive.sumOf { it.monthlyFee }
            
            val totalPaid = allActive.filter { it.paidStatus.equals("Paid", ignoreCase = true) }.sumOf { it.monthlyFee }
            val totalPending = allActive.filter { it.paidStatus.equals("Unpaid", ignoreCase = true) }.sumOf { it.monthlyFee }
            
            val onlineCount = allActive.count { it.classType.equals("Online", ignoreCase = true) }
            val offlineCount = allActive.count { it.classType.equals("Offline", ignoreCase = true) }

            DashboardStats(
                totalStudents = totalCount,
                totalMonthlyCollection = totalCollection,
                totalPaidAmount = totalPaid,
                totalPendingAmount = totalPending,
                onlineStudentsCount = onlineCount,
                offlineStudentsCount = offlineCount
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    // Automatically generated active reminders based on joining date and calendar month
    val activeReminders: StateFlow<List<FeeReminder>> = rawStudentsList
        .combine(MutableStateFlow(getCurrentMonthString())) { students, currentMonth ->
            val remindersList = mutableListOf<FeeReminder>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentYearMonthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())

            for (student in students) {
                try {
                    val joinDate: Date = sdf.parse(student.joiningDate) ?: continue
                    val joinCal = Calendar.getInstance().apply { time = joinDate }
                    val currentCal = Calendar.getInstance()

                    // Check if student joined in or before current calendar month
                    val joinYearMonthStr = currentYearMonthSdf.format(joinDate)
                    val isJoinedAlready = joinYearMonthStr <= currentMonth

                    // Reminder criteria:
                    // 1. Is joined
                    // 2. Paid status is Unpaid, or the lastPaidMonth is older than currentMonth
                    val isUnpaid = student.paidStatus.equals("Unpaid", ignoreCase = true) ||
                            student.lastPaidMonth.isBlank() ||
                            student.lastPaidMonth < currentMonth

                    if (isJoinedAlready && isUnpaid) {
                        remindersList.add(
                            FeeReminder(
                                studentId = student.id,
                                studentName = student.name,
                                classLevel = student.classLevel,
                                feeAmount = student.monthlyFee,
                                dueMonth = getCurrentMonthDisplayName(),
                                mobileNumber = student.mobileNumber,
                                joiningDate = student.joiningDate
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignore date parsing exceptions and skip
                }
            }
            remindersList
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get student details by ID
    suspend fun getStudentById(id: Int): Student? {
        return repository.getStudentById(id)
    }

    // Login Authentication
    fun loginTeacher(emailOrMobile: String, password: String): Boolean {
        val success = prefs.verifyCredentials(emailOrMobile, password)
        if (success) {
            prefs.isLoggedIn = true
            isLoggedIn.value = true
        }
        return success
    }

    // Logout
    fun logoutTeacher() {
        prefs.logout()
        isLoggedIn.value = false
    }

    // Save customized credentials
    fun updateTeacherCredentials(emailOrMobile: String, password: String) {
        prefs.teacherEmailOrMobile = emailOrMobile
        prefs.teacherPassword = password
        teacherEmailOrMobile.value = emailOrMobile
    }

    // Save customized Google Sheets Web App URL
    fun updateSheetsWebAppUrl(url: String) {
        prefs.sheetsWebAppUrl = url
        sheetsWebAppUrl.value = url
    }

    // Add student record
    fun addStudent(
        name: String,
        classLevel: String,
        joiningDate: String,
        monthlyFee: Double,
        classType: String,
        paidStatus: String,
        mobileNumber: String,
        notes: String
    ) {
        viewModelScope.launch {
            val currentMonth = getCurrentMonthString()
            val student = Student(
                name = name.trim(),
                classLevel = classLevel.trim(),
                joiningDate = joiningDate,
                monthlyFee = monthlyFee,
                classType = classType,
                paidStatus = paidStatus,
                mobileNumber = mobileNumber.trim(),
                notes = notes.trim(),
                lastPaidMonth = if (paidStatus == "Paid") currentMonth else ""
            )
            repository.insertStudent(student)
            
            // Auto sync if Sheet URL configured
            if (sheetsWebAppUrl.value.isNotBlank()) {
                syncGoogleSheets()
            }
        }
    }

    // Update student record
    fun updateStudent(
        id: Int,
        name: String,
        classLevel: String,
        joiningDate: String,
        monthlyFee: Double,
        classType: String,
        paidStatus: String,
        mobileNumber: String,
        notes: String,
        lastPaidMonth: String
    ) {
        viewModelScope.launch {
            val student = Student(
                id = id,
                name = name.trim(),
                classLevel = classLevel.trim(),
                joiningDate = joiningDate,
                monthlyFee = monthlyFee,
                classType = classType,
                paidStatus = paidStatus,
                mobileNumber = mobileNumber.trim(),
                notes = notes.trim(),
                lastPaidMonth = lastPaidMonth
            )
            repository.updateStudent(student)
            
            // Auto sync if Sheet URL configured
            if (sheetsWebAppUrl.value.isNotBlank()) {
                syncGoogleSheets()
            }
        }
    }

    // Delete student record
    fun deleteStudent(id: Int) {
        viewModelScope.launch {
            repository.softDeleteStudent(id)
            
            // Auto sync to delete on sheet, otherwise hard delete locally if sheets not configured
            if (sheetsWebAppUrl.value.isNotBlank()) {
                syncGoogleSheets()
            } else {
                repository.hardDeleteStudent(id)
            }
        }
    }

    // Mark paid or unpaid status dynamically
    fun togglePaidStatus(student: Student) {
        viewModelScope.launch {
            val currentMonth = getCurrentMonthString()
            val newStatus = if (student.paidStatus == "Paid") "Unpaid" else "Paid"
            val newLastPaidMonth = if (newStatus == "Paid") currentMonth else ""
            
            val updatedStudent = student.copy(
                paidStatus = newStatus,
                lastPaidMonth = newLastPaidMonth
            )
            repository.updateStudent(updatedStudent)
            
            // Auto sync if URL configured
            if (sheetsWebAppUrl.value.isNotBlank()) {
                syncGoogleSheets()
            }
        }
    }

    // Synchronize local database with Google Sheets Web App
    fun syncGoogleSheets() {
        val url = sheetsWebAppUrl.value
        if (url.isBlank()) {
            viewModelScope.launch {
                _syncEvent.emit("Sheets URL not set! Go to Settings to configure.")
            }
            return
        }

        viewModelScope.launch {
            isSyncing.value = true
            val result = repository.syncWithGoogleSheets(url)
            isSyncing.value = false
            if (result.isSuccess) {
                _syncEvent.emit("Google Sheets Sync Successful!")
            } else {
                _syncEvent.emit("Sync Failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
            }
        }
    }

    // Date Helper Functions
    private fun getCurrentMonthString(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentMonthDisplayName(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}

// Data models for Dashboard and Reminders
data class DashboardStats(
    val totalStudents: Int = 0,
    val totalMonthlyCollection: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val totalPendingAmount: Double = 0.0,
    val onlineStudentsCount: Int = 0,
    val offlineStudentsCount: Int = 0
)

data class FeeReminder(
    val studentId: Int,
    val studentName: String,
    val classLevel: String,
    val feeAmount: Double,
    val dueMonth: String,
    val mobileNumber: String,
    val joiningDate: String
)
