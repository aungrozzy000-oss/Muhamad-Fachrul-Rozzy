package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.AttendanceRepository
import com.example.data.firestore.AttendanceScanOutcome
import com.example.data.model.AdminSettings
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.QrStatus
import com.example.data.model.ScanMode
import com.example.data.model.Student
import com.example.data.model.UiState
import com.example.util.DateTimeUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ScanFeedback {
  data class Success(val title: String, val subtitle: String, val badge: String) : ScanFeedback
  data class Warning(val title: String, val subtitle: String) : ScanFeedback
  data class Error(val title: String, val subtitle: String) : ScanFeedback
}

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(
  private val repository: AttendanceRepository,
  private val currentUserId: String,
  displayName: String?,
) : ViewModel() {

  private val _selectedDateKey = MutableStateFlow(DateTimeUtils.todayDateKey())
  val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

  private val _scanMode = MutableStateFlow(ScanMode.CHECK_IN)
  val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

  private val _scanBanner = MutableStateFlow<ScanFeedback?>(null)
  val scanBanner: StateFlow<ScanFeedback?> = _scanBanner.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _isMutating = MutableStateFlow(false)
  val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

  private var lastScannedToken: String = ""
  private var lastScannedTimestampMs: Long = 0L

  val settingsState: StateFlow<AdminSettings?> =
    repository
      .observeAdminSettings(currentUserId)
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Failed observing settings", firestoreError ?: error)
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val studentsState: StateFlow<UiState<List<Student>>> =
    repository
      .observeStudents(currentUserId)
      .map<List<Student>, UiState<List<Student>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing students", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat daftar siswa."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val attendanceState: StateFlow<UiState<List<AttendanceRecord>>> =
    _selectedDateKey
      .flatMapLatest { dateKey -> repository.observeAttendanceForDate(currentUserId, dateKey) }
      .map<List<AttendanceRecord>, UiState<List<AttendanceRecord>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing attendance", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat riwayat absensi."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  init {
    viewModelScope.launch {
      repository.ensureDefaultSettings(currentUserId, displayName)
    }
  }

  fun setSelectedDate(dateKey: String) {
    _selectedDateKey.value = dateKey
  }

  fun setScanMode(mode: ScanMode) {
    _scanMode.value = mode
    _scanBanner.value = null
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  fun clearScanBanner() {
    _scanBanner.value = null
  }

  fun addStudent(
    nis: String,
    fullName: String,
    className: String,
    gender: String,
    issueQrImmediately: Boolean,
    onCreated: (Student) -> Unit = {},
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      val res =
        repository.addStudent(
          userId = currentUserId,
          nis = nis,
          fullName = fullName,
          className = className,
          gender = gender,
          issueQrImmediately = issueQrImmediately,
        )
      _isMutating.value = false
      res
        .onSuccess { student ->
          _actionMessage.value =
            if (issueQrImmediately) {
              "Siswa ${student.fullName} ditambahkan & QR Code diterbitkan."
            } else {
              "Siswa ${student.fullName} berhasil ditambahkan."
            }
          onCreated(student)
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menambahkan siswa."
        }
    }
  }

  fun updateStudent(
    studentId: String,
    nis: String,
    fullName: String,
    className: String,
    gender: String,
  ) {
    viewModelScope.launch {
      _isMutating.value = true
      val res =
        repository.updateStudent(
          userId = currentUserId,
          studentId = studentId,
          nis = nis,
          fullName = fullName,
          className = className,
          gender = gender,
        )
      _isMutating.value = false
      res
        .onSuccess { _actionMessage.value = "Data siswa berhasil diperbarui." }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal memperbarui data siswa."
        }
    }
  }

  fun issueOrReissueQr(student: Student, onSuccess: (String) -> Unit = {}) {
    viewModelScope.launch {
      _isMutating.value = true
      val res = repository.issueOrReissueQr(currentUserId, student)
      _isMutating.value = false
      res
        .onSuccess { token ->
          _actionMessage.value = "QR Code diterbitkan untuk ${student.fullName} ($token)."
          onSuccess(token)
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menerbitkan QR Code."
        }
    }
  }

  fun revokeQr(student: Student) {
    viewModelScope.launch {
      _isMutating.value = true
      val res = repository.revokeQr(currentUserId, student.id)
      _isMutating.value = false
      res
        .onSuccess { _actionMessage.value = "QR Code ${student.fullName} telah dicabut." }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal mencabut QR Code."
        }
    }
  }

  fun batchIssuePendingQr() {
    val students = (studentsState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      _isMutating.value = true
      val res = repository.batchIssuePendingQr(currentUserId, students)
      _isMutating.value = false
      res
        .onSuccess { count ->
          _actionMessage.value =
            if (count == 0) {
              "Semua siswa sudah memiliki QR Code aktif."
            } else {
              "Berhasil menerbitkan QR Code untuk $count siswa."
            }
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menerbitkan QR massal."
        }
    }
  }

  fun deleteStudent(student: Student) {
    viewModelScope.launch {
      _isMutating.value = true
      val res = repository.deleteStudent(currentUserId, student.id)
      _isMutating.value = false
      res
        .onSuccess { _actionMessage.value = "Data ${student.fullName} dihapus." }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menghapus data siswa."
        }
    }
  }

  fun processScannedQr(rawInput: String, isManualFallback: Boolean = false) {
    val cleaned = rawInput.trim()
    if (cleaned.isEmpty()) return

    val nowMs = System.currentTimeMillis()
    if (!isManualFallback && cleaned == lastScannedToken && nowMs - lastScannedTimestampMs < 2500L) {
      return
    }
    lastScannedToken = cleaned
    lastScannedTimestampMs = nowMs

    val students = (studentsState.value as? UiState.Success)?.data
    if (students == null) {
      _scanBanner.value =
        ScanFeedback.Warning("Data Siswa Belum Siap", "Tunggu sebentar hingga daftar siswa dimuat.")
      return
    }

    val matchedStudent =
      students.firstOrNull { student ->
        student.qrToken.equals(cleaned, ignoreCase = true) ||
          (isManualFallback && student.nis.equals(cleaned, ignoreCase = true))
      }

    if (matchedStudent == null) {
      _scanBanner.value =
        ScanFeedback.Error(
          "QR Tidak Terdaftar",
          "Kode \"$cleaned\" tidak cocok dengan data siswa SMA Taruna Andigha.",
        )
      return
    }

    if (!isManualFallback && matchedStudent.qrStatusEnum != QrStatus.ACTIVE) {
      val reason =
        if (matchedStudent.qrStatusEnum == QrStatus.REVOKED) {
          "QR Code milik ${matchedStudent.fullName} telah dicabut oleh admin. Terbitkan QR baru terlebih dahulu."
        } else {
          "QR Code milik ${matchedStudent.fullName} belum diterbitkan secara resmi."
        }
      _scanBanner.value = ScanFeedback.Error("QR Tidak Aktif", reason)
      return
    }

    val settings = settingsState.value
    val deadlineHour = (settings?.checkInDeadlineHour ?: 7L).toInt()
    val deadlineMinute = (settings?.checkInDeadlineMinute ?: 15L).toInt()
    val todayKey = DateTimeUtils.todayDateKey()
    val mode = _scanMode.value

    viewModelScope.launch {
      _isMutating.value = true
      val res =
        repository.recordQrAttendance(
          userId = currentUserId,
          student = matchedStudent,
          dateKey = todayKey,
          scanMode = mode,
          deadlineHour = deadlineHour,
          deadlineMinute = deadlineMinute,
        )
      _isMutating.value = false

      res
        .onSuccess { outcome ->
          _scanBanner.value =
            when (outcome) {
              is AttendanceScanOutcome.CheckedIn ->
                ScanFeedback.Success(
                  title = outcome.student.fullName,
                  subtitle =
                    "NIS ${outcome.student.nis} • Kelas ${outcome.student.className} • Masuk ${DateTimeUtils.nowClockText()}",
                  badge = outcome.status.label.uppercase(),
                )
              is AttendanceScanOutcome.CheckedOut ->
                ScanFeedback.Success(
                  title = outcome.student.fullName,
                  subtitle =
                    "NIS ${outcome.student.nis} • Kelas ${outcome.student.className} • Pulang ${DateTimeUtils.nowClockText()}",
                  badge = "PULANG",
                )
              is AttendanceScanOutcome.AlreadyCheckedIn ->
                ScanFeedback.Warning(
                  title = "Sudah Absen Masuk",
                  subtitle =
                    "${outcome.student.fullName} (${outcome.student.className}) tercatat masuk pukul ${DateTimeUtils.formatTime(outcome.existing.checkInTime)}.",
                )
              is AttendanceScanOutcome.AlreadyCheckedOut ->
                ScanFeedback.Warning(
                  title = "Sudah Absen Pulang",
                  subtitle =
                    "${outcome.student.fullName} sudah tercatat pulang pukul ${DateTimeUtils.formatTime(outcome.existing.checkOutTime)}.",
                )
              is AttendanceScanOutcome.NotCheckedInYet ->
                ScanFeedback.Warning(
                  title = "Belum Absen Masuk",
                  subtitle =
                    "${outcome.student.fullName} belum melakukan scan masuk hari ini. Ubah ke mode Absen Masuk terlebih dahulu.",
                )
            }
        }
        .onFailure { err ->
          _scanBanner.value =
            ScanFeedback.Error(
              "Gagal Mencatat Absensi",
              err.localizedMessage ?: "Terjadi kendala saat menyimpan ke server.",
            )
        }
    }
  }

  fun setManualAttendance(
    student: Student,
    status: AttendanceStatus,
    note: String,
  ) {
    val dateKey = _selectedDateKey.value
    viewModelScope.launch {
      _isMutating.value = true
      val res =
        repository.setManualAttendance(
          userId = currentUserId,
          student = student,
          dateKey = dateKey,
          status = status,
          note = note,
        )
      _isMutating.value = false
      res
        .onSuccess {
          _actionMessage.value = "Status ${student.fullName} diubah menjadi ${status.label}."
        }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menyimpan status absensi."
        }
    }
  }

  fun updateSettings(adminName: String, deadlineHour: Int, deadlineMinute: Int) {
    viewModelScope.launch {
      _isMutating.value = true
      val res =
        repository.updateSettings(
          userId = currentUserId,
          adminName = adminName,
          deadlineHour = deadlineHour,
          deadlineMinute = deadlineMinute,
        )
      _isMutating.value = false
      res
        .onSuccess { _actionMessage.value = "Pengaturan jam masuk berhasil disimpan." }
        .onFailure { err ->
          _actionMessage.value = err.localizedMessage ?: "Gagal menyimpan pengaturan."
        }
    }
  }

  private companion object {
    const val TAG = "AttendanceVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
