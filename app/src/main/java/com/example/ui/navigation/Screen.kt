package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object StudentList : Screen("student_list")
    object AddEditStudent : Screen("add_edit_student?studentId={studentId}") {
        fun createRoute(studentId: Int? = null): String {
            return if (studentId != null) "add_edit_student?studentId=$studentId" else "add_edit_student"
        }
    }
    object Settings : Screen("settings")
}
