package com.schoolmgmt.app.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.AttendanceEntity
import com.schoolmgmt.app.data.local.entity.AttendanceStatus
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.UserRole
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val db: AppDatabase,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val sectionDao = db.sectionDao()
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()

    val sections = sectionDao.observeAllWithClassName()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedSectionId = MutableStateFlow<String?>(null)
    val selectedSectionId = _selectedSectionId.asStateFlow()

    private val _selectedDate = MutableStateFlow(getMidnightUtc(System.currentTimeMillis()))
    val selectedDate = _selectedDate.asStateFlow()

    private val _statusMap = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val statusMap = _statusMap.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    // Observe students in the selected section
    val students: StateFlow<List<StudentEntity>> = _selectedSectionId
        .flatMapLatest { sectionId ->
            if (sectionId == null) flowOf(emptyList())
            else studentDao.observeBySection(sectionId)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Observe existing attendance for the selected section and date
    private val existingAttendance: StateFlow<List<AttendanceEntity>> = combine(
        _selectedSectionId,
        _selectedDate
    ) { sectionId, date ->
        sectionId to date
    }.flatMapLatest { (sectionId, date) ->
        if (sectionId == null) flowOf(emptyList())
        else attendanceDao.observeForSectionAndDate(sectionId, date)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Automatically initialize statusMap when students or existingAttendance changes
        viewModelScope.launch {
            combine(students, existingAttendance) { studentList, attendanceList ->
                val newMap = studentList.associate { student ->
                    val existing = attendanceList.find { it.studentId == student.id }
                    student.id to (existing?.status ?: AttendanceStatus.PRESENT)
                }
                newMap
            }.collect { map ->
                _statusMap.value = map
            }
        }
    }

    fun selectSection(sectionId: String?) {
        _selectedSectionId.value = sectionId
        _saveSuccess.value = false
    }

    fun selectDate(millis: Long) {
        _selectedDate.value = getMidnightUtc(millis)
        _saveSuccess.value = false
    }

    fun updateStatus(studentId: String, status: AttendanceStatus) {
        val currentMap = _statusMap.value.toMutableMap()
        currentMap[studentId] = status
        _statusMap.value = currentMap
        _saveSuccess.value = false
    }

    fun saveAttendance() {
        val sectionId = _selectedSectionId.value ?: return
        val date = _selectedDate.value
        val studentList = students.value
        val currentMap = _statusMap.value
        val existing = existingAttendance.value

        _isSaving.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val now = System.currentTimeMillis()
                
                val records = studentList.map { student ->
                    val status = currentMap[student.id] ?: AttendanceStatus.PRESENT
                    val matchingRecord = existing.find { it.studentId == student.id }
                    
                    AttendanceEntity(
                        id = matchingRecord?.id ?: UUID.randomUUID().toString(),
                        studentId = student.id,
                        sectionId = sectionId,
                        date = date,
                        status = status,
                        markedById = userId,
                        updatedAt = now,
                        syncedAt = null // flag for sync
                    )
                }

                db.withTransaction {
                    attendanceDao.upsertAll(records)
                }
                
                // Trigger sync immediately to send attendance to server
                syncScheduler.syncNow()
                
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    private fun getMidnightUtc(millis: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
