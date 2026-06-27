package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Student
import com.example.ui.TuitionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentScreen(
    viewModel: TuitionViewModel,
    studentId: Int?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isEditMode = studentId != null

    // Form Field States
    var name by remember { mutableStateOf("") }
    var classLevel by remember { mutableStateOf("Class 10") }
    var joiningDate by remember { mutableStateOf("") }
    var monthlyFee by remember { mutableStateOf("") }
    var classType by remember { mutableStateOf("Offline") }
    var paidStatus by remember { mutableStateOf("Unpaid") }
    var mobileNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var lastPaidMonth by remember { mutableStateOf("") }

    // Dropdown expanded tracker
    var isClassDropdownExpanded by remember { mutableStateOf(false) }
    val classesList = listOf("Class 5", "Class 6", "Class 7", "Class 8", "Class 9", "Class 10", "Class 11", "Class 12")

    // If joiningDate is empty, default to current date
    if (joiningDate.isBlank()) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        joiningDate = sdf.format(Date())
    }

    // Load data if in edit mode
    LaunchedEffect(studentId) {
        if (studentId != null) {
            val student = viewModel.getStudentById(studentId)
            if (student != null) {
                name = student.name
                classLevel = student.classLevel
                joiningDate = student.joiningDate
                monthlyFee = student.monthlyFee.toInt().toString()
                classType = student.classType
                paidStatus = student.paidStatus
                mobileNumber = student.mobileNumber
                notes = student.notes
                lastPaidMonth = student.lastPaidMonth
            }
        }
    }

    // Calendar native picker launcher
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            // Format single digit months and days
            val formattedMonth = String.format("%02d", month + 1)
            val formattedDay = String.format("%02d", dayOfMonth)
            joiningDate = "$year-$formattedMonth-$formattedDay"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Student Profile" else "Register Student", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Title
            Text(
                text = "Student Information",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                placeholder = { Text("Enter student's full name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_name_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Class Level dropdown (Material 3 ExposedDropdownMenuBox)
            ExposedDropdownMenuBox(
                expanded = isClassDropdownExpanded,
                onExpandedChange = { isClassDropdownExpanded = !isClassDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = classLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Class Level *") },
                    leadingIcon = { Icon(imageVector = Icons.Default.School, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isClassDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("class_dropdown"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = isClassDropdownExpanded,
                    onDismissRequest = { isClassDropdownExpanded = false }
                ) {
                    classesList.forEach { cls ->
                        DropdownMenuItem(
                            text = { Text(cls) },
                            onClick = {
                                classLevel = cls
                                isClassDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Joining Date Picker field
            OutlinedTextField(
                value = joiningDate,
                onValueChange = { joiningDate = it },
                readOnly = true,
                label = { Text("Joining Date *") },
                leadingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .testTag("joining_date_picker"),
                shape = RoundedCornerShape(12.dp)
            )

            // Mobile number input
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Parent Mobile Number *") },
                placeholder = { Text("e.g. +91 98765 43210") },
                leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_mobile_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Monthly fee input
            OutlinedTextField(
                value = monthlyFee,
                onValueChange = { monthlyFee = it },
                label = { Text("Monthly Fee (₹) *") },
                placeholder = { Text("e.g. 1500") },
                leadingIcon = { Icon(imageVector = Icons.Default.CurrencyRupee, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_fee_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Class Type Mode Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Class Learning Mode *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { classType = "Offline" }
                    ) {
                        RadioButton(
                            selected = classType == "Offline",
                            onClick = { classType = "Offline" }
                        )
                        Text("Offline Class", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { classType = "Online" }
                    ) {
                        RadioButton(
                            selected = classType == "Online",
                            onClick = { classType = "Online" }
                        )
                        Text("Online Class", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Paid/Unpaid Status Selection (Only on register)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Current Month Fee Status *",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { paidStatus = "Unpaid" }
                    ) {
                        RadioButton(
                            selected = paidStatus == "Unpaid",
                            onClick = { paidStatus = "Unpaid" }
                        )
                        Text("Unpaid", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { paidStatus = "Paid" }
                    ) {
                        RadioButton(
                            selected = paidStatus == "Paid",
                            onClick = { paidStatus = "Paid" }
                        )
                        Text("Paid", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Notes multi-line
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Extra Remarks / Notes") },
                placeholder = { Text("e.g. Weak in Math, pays on 5th of every month") },
                leadingIcon = { Icon(imageVector = Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("student_notes_input"),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Action Button
            Button(
                onClick = {
                    val doubleFee = monthlyFee.toDoubleOrNull()
                    if (name.isBlank() || mobileNumber.isBlank() || monthlyFee.isBlank() || doubleFee == null) {
                        Toast.makeText(context, "Please enter all required fields * with valid data", Toast.LENGTH_SHORT).show()
                    } else {
                        if (isEditMode && studentId != null) {
                            viewModel.updateStudent(
                                id = studentId,
                                name = name,
                                classLevel = classLevel,
                                joiningDate = joiningDate,
                                monthlyFee = doubleFee,
                                classType = classType,
                                paidStatus = paidStatus,
                                mobileNumber = mobileNumber,
                                notes = notes,
                                lastPaidMonth = lastPaidMonth
                            )
                            Toast.makeText(context, "Student updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addStudent(
                                name = name,
                                classLevel = classLevel,
                                joiningDate = joiningDate,
                                monthlyFee = doubleFee,
                                classType = classType,
                                paidStatus = paidStatus,
                                mobileNumber = mobileNumber,
                                notes = notes
                            )
                            Toast.makeText(context, "Student registered successfully!", Toast.LENGTH_SHORT).show()
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_student_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isEditMode) "UPDATE RECORD" else "REGISTER STUDENT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
