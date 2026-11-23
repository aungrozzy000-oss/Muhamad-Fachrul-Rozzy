package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class QrStatus(val label: String) {
  PENDING("Belum Terbit"),
  ACTIVE("QR Aktif"),
  REVOKED("Dicabut");

  companion object {
    fun fromRaw(raw: String?): QrStatus =
      entries.firstOrNull { it.name == raw } ?: PENDING
  }
}

enum class AttendanceStatus(val label: String) {
  HADIR("Hadir"),
  TERLAMBAT("Terlambat"),
  IZIN("Izin"),
  SAKIT("Sakit"),
  ALPHA("Alpha");

  companion object {
    fun fromRaw(raw: String?): AttendanceStatus =
      entries.firstOrNull { it.name == raw } ?: HADIR
  }
}

enum class AttendanceMethod(val label: String) {
  QR_SCAN("Scan QR"),
  MANUAL("Input Admin");

  companion object {
    fun fromRaw(raw: String?): AttendanceMethod =
      entries.firstOrNull { it.name == raw } ?: MANUAL
  }
}

enum class ScanMode(val label: String) {
  CHECK_IN("Absen Masuk"),
  CHECK_OUT("Absen Pulang")
}

@Keep
@IgnoreExtraProperties
data class AdminSettings(
  @DocumentId val id: String = "",
  val userId: String = "",
  val adminName: String = "Admin Taruna",
  val schoolName: String = "SMA Taruna Andigha",
  val checkInDeadlineHour: Long = 7L,
  val checkInDeadlineMinute: Long = 15L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Student(
  @DocumentId val id: String = "",
  val userId: String = "",
  val nis: String = "",
  val fullName: String = "",
  val className: String = "",
  val gender: String = "L",
  val qrToken: String = "",
  val qrStatus: String = QrStatus.PENDING.name,
  val qrIssuedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val qrStatusEnum: QrStatus
    get() = QrStatus.fromRaw(qrStatus)
}

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val studentId: String = "",
  val studentNis: String = "",
  val studentName: String = "",
  val className: String = "",
  val dateKey: String = "",
  val status: String = AttendanceStatus.HADIR.name,
  val method: String = AttendanceMethod.QR_SCAN.name,
  val note: String = "",
  val checkInTime: Timestamp? = null,
  val checkOutTime: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val statusEnum: AttendanceStatus
    get() = AttendanceStatus.fromRaw(status)

  val methodEnum: AttendanceMethod
    get() = AttendanceMethod.fromRaw(method)
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}
