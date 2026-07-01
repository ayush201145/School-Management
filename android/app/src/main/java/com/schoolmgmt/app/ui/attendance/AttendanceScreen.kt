package com.schoolmgmt.app.ui.attendance

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.AttendanceStatus
import com.schoolmgmt.app.data.local.entity.StudentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sections by viewModel.sections.collectAsState()
    val selectedSectionId by viewModel.selectedSectionId.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val students by viewModel.students.collectAsState()
    val statusMap by viewModel.statusMap.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var sectionDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_SHORT).show()
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedSectionId != null && students.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.saveAttendance() },
                            enabled = !isSaving,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(size = 18.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Section Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedSection = sections.find { it.id == selectedSectionId }
                        val displayText = selectedSection?.let { "${it.className} - ${it.sectionName}" } ?: "-- Select Class & Section --"
                        
                        OutlinedTextField(
                            value = displayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class & Section") },
                            trailingIcon = {
                                IconButton(onClick = { sectionDropdownExpanded = true }) {
                                    Icon(Icons.Filled.ExpandMore, contentDescription = "Select Section")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sectionDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = sectionDropdownExpanded,
                            onDismissRequest = { sectionDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text("${section.className} - ${section.sectionName}") },
                                    onClick = {
                                        viewModel.selectSection(section.id)
                                        sectionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Picker Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                cal.timeInMillis = selectedDate
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                        selectedCal.set(year, month, dayOfMonth)
                                        viewModel.selectDate(selectedCal.timeInMillis)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Attendance Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = dateFormatter.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Error display
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Students grid / list
            if (selectedSectionId == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a Class & Section to mark attendance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students enrolled in this section", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val status = statusMap[student.id] ?: AttendanceStatus.PRESENT
                        StudentAttendanceRow(
                            student = student,
                            status = status,
                            onStatusChange = { newStatus ->
                                viewModel.updateStatus(student.id, newStatus)
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    student: StudentEntity,
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${student.firstName} ${student.lastName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Roll No: ${student.rollNo ?: "N/A"} · Adm: ${student.admissionNo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AttendanceStatusButton(
                label = "P",
                isSelected = status == AttendanceStatus.PRESENT,
                selectedBg = Color(0xFF4CAF50),
                onClick = { onStatusChange(AttendanceStatus.PRESENT) }
            )
            AttendanceStatusButton(
                label = "A",
                isSelected = status == AttendanceStatus.ABSENT,
                selectedBg = Color(0xFFE53935),
                onClick = { onStatusChange(AttendanceStatus.ABSENT) }
            )
            AttendanceStatusButton(
                label = "L",
                isSelected = status == AttendanceStatus.LATE,
                selectedBg = Color(0xFFFF9800),
                onClick = { onStatusChange(AttendanceStatus.LATE) }
            )
            AttendanceStatusButton(
                label = "E",
                isSelected = status == AttendanceStatus.LEAVE,
                selectedBg = Color(0xFF2196F3),
                onClick = { onStatusChange(AttendanceStatus.LEAVE) }
            )
        }
    }
}

@Composable
fun AttendanceStatusButton(
    label: String,
    isSelected: Boolean,
    selectedBg: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) selectedBg else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) Color.WHITE else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bgColor, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = fontWeight
        )
    }
}
