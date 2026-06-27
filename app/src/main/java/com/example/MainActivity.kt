package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import com.example.ui.TuitionViewModel
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddEditStudentScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize central ViewModel using AndroidViewModel provider
        val viewModel = ViewModelProvider(this)[TuitionViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val isLoggedInState = viewModel.isLoggedIn.collectAsState()

                // If user is logged in, start at Dashboard; otherwise show Login Screen
                val startDest = if (isLoggedInState.value) Screen.Dashboard.route else Screen.Login.route

                NavHost(
                    navController = navController,
                    startDestination = startDest
                ) {
                    // Teacher Login route
                    composable(route = Screen.Login.route) {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Stats Dashboard route
                    composable(route = Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToStudents = {
                                navController.navigate(Screen.StudentList.route)
                            },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            onLogout = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Student Directory list route
                    composable(route = Screen.StudentList.route) {
                        StudentListScreen(
                            viewModel = viewModel,
                            onNavigateToAddStudent = {
                                navController.navigate(Screen.AddEditStudent.createRoute())
                            },
                            onNavigateToEditStudent = { studentId ->
                                navController.navigate(Screen.AddEditStudent.createRoute(studentId))
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Add / Edit Student Profile route
                    composable(
                        route = Screen.AddEditStudent.route,
                        arguments = listOf(
                            navArgument("studentId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val studentIdStr = backStackEntry.arguments?.getString("studentId")
                        val studentId = studentIdStr?.toIntOrNull()

                        AddEditStudentScreen(
                            viewModel = viewModel,
                            studentId = studentId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Application Settings & Sync guide route
                    composable(route = Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
