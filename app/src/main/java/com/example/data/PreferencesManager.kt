package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("simran_tuition_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_TEACHER_EMAIL_OR_MOBILE = "teacher_email_or_mobile"
        private const val KEY_TEACHER_PASSWORD = "teacher_password"
        private const val KEY_SHEETS_WEB_APP_URL = "sheets_web_app_url"
        private const val KEY_IS_INITIALIZED = "is_initialized"
    }

    init {
        // Initialize with default credentials on first launch
        if (!prefs.getBoolean(KEY_IS_INITIALIZED, false)) {
            prefs.edit()
                .putString(KEY_TEACHER_EMAIL_OR_MOBILE, "teacher@simran.com")
                .putString(KEY_TEACHER_PASSWORD, "simran123") // Friendly default password
                .putBoolean(KEY_IS_INITIALIZED, true)
                .apply()
        }
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var teacherEmailOrMobile: String
        get() = prefs.getString(KEY_TEACHER_EMAIL_OR_MOBILE, "teacher@simran.com") ?: "teacher@simran.com"
        set(value) = prefs.edit().putString(KEY_TEACHER_EMAIL_OR_MOBILE, value).apply()

    var teacherPassword: String
        get() = prefs.getString(KEY_TEACHER_PASSWORD, "simran123") ?: "simran123"
        set(value) = prefs.edit().putString(KEY_TEACHER_PASSWORD, value).apply()

    var sheetsWebAppUrl: String
        get() = prefs.getString(KEY_SHEETS_WEB_APP_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SHEETS_WEB_APP_URL, value).apply()

    fun verifyCredentials(emailOrMobile: String, password: String): Boolean {
        val savedEmail = teacherEmailOrMobile.trim()
        val savedPassword = teacherPassword.trim()
        return (emailOrMobile.trim().equals(savedEmail, ignoreCase = true)) && (password.trim() == savedPassword)
    }

    fun logout() {
        isLoggedIn = false
    }
}
