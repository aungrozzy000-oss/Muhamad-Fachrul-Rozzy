package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.AdminSettings
import com.example.data.model.AttendanceMethod
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.QrStatus
import com.example.data.model.ScanMode
import com.example.data.model.Student
import com.example.util.DateTimeUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

sealed interface AttendanceScanOutcome {
  data class CheckedIn(val student: Student, val status: AttendanceStatus) : AttendanceScanOutcome
  data class CheckedOut(val student: Student) : AttendanceScanOutcome
  data class AlreadyCheckedIn(val student: Student, val existing: AttendanceRecord) : AttendanceScanOutcome
  data class AlreadyCheckedOut(val student: Student, val existing: AttendanceRecord) : AttendanceScanOutcome
  data class NotCheckedInYet(val student: Student) : AttendanceScanOutcome
}

class AttendanceRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val random = SecureRandom()

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Admin harus masuk dengan Google terlebih dahulu.")
  }

  private fun generateQrToken(nis: String): String {
    val cleanNis = nis.trim().replace(Regex("[^a-zA-Z0-9]"), "").take(16).ifEmpty { "SISWA" }
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val suffix = buildString(6) {
      repeat(6) { append(chars[random.nextInt(chars.length)]) }
    }
    return "TRN-$cleanNis-$suffix"
  }

  fun observeAdminSettings(userId: String): Flow<AdminSettings?> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(null)
      return@flow
    }
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              AdminSettings::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  suspend fun ensureDefaultSettings(userId: String, displayName: String?): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val safeName = (displayName?.trim()?.take(100)).orEmpty().ifBlank { "Admin Taruna" }
        val payload =
          mapOf(
            "userId" to uid,
            "adminName" to safeName,
            "schoolName" to "SMA Taruna Andigha",
            "checkInDeadlineHour" to 7,
            "checkInDeadlineMinute" to 15,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun updateSettings(
    userId: String,
    adminName: String,
    deadlineHour: Int,
    deadlineMinute: Int,
  ): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      val trimmedName = adminName.trim()
      require(trimmedName.length in 1..100) { "Nama admin harus 1-100 karakter." }
      require(deadlineHour in 0..23) { "Jam masuk harus 0-23." }
      require(deadlineMinute in 0..59) { "Menit masuk harus 0-59." }

      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef
          .update(
            mapOf(
              "adminName" to trimmedName,
              "checkInDeadlineHour" to deadlineHour,
              "checkInDeadlineMinute" to deadlineMinute,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "adminName" to trimmedName,
              "schoolName" to "SMA Taruna Andigha",
              "checkInDeadlineHour" to deadlineHour,
              "checkInDeadlineMinute" to deadlineMinute,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observeStudents(userId: String): Flow<List<Student>> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(emptyList())
      return@flow
    }
    val path = "users/$userId/students"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("students")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              Student::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedWith(compareBy({ it.className }, { it.fullName }))
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeAttendanceForDate(userId: String, dateKey: String): Flow<List<AttendanceRecord>> = flow {
    if (userId.isBlank() || auth.currentUser?.uid != userId) {
      emit(emptyList())
      return@flow
    }
    val path = "users/$userId/attendance"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("attendance")
        .whereEqualTo("dateKey", dateKey)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              AttendanceRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.updatedAt ?: it.createdAt }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun addStudent(
    userId: String,
    nis: String,
    fullName: String,
    className: String,
    gender: String,
    issueQrImmediately: Boolean,
  ): Result<Student> {
    var docPath = "users/$userId/students"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      val cleanNis = nis.trim()
      val cleanName = fullName.trim()
      val cleanClass = className.trim().uppercase()
      val cleanGender = if (gender == "P") "P" else "L"

      require(cleanNis.length in 1..30) { "NIS harus 1-30 karakter." }
      require(cleanName.length in 1..100) { "Nama siswa harus 1-100 karakter." }
      require(cleanClass.length in 1..30) { "Kelas harus 1-30 karakter." }

      val docRef = db.collection("users").document(uid).collection("students").document()
      docPath = docRef.path

      val qrToken = if (issueQrImmediately) generateQrToken(cleanNis) else ""
      val qrStatus = if (issueQrImmediately) QrStatus.ACTIVE.name else QrStatus.PENDING.name

      val payload =
        mutableMapOf<String, Any?>(
          "userId" to uid,
          "nis" to cleanNis,
          "fullName" to cleanName,
          "className" to cleanClass,
          "gender" to cleanGender,
          "qrToken" to qrToken,
          "qrStatus" to qrStatus,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (issueQrImmediately) {
        payload["qrIssuedAt"] = FieldValue.serverTimestamp()
      }

      @Suppress("UNCHECKED_CAST")
      val sanitized = payload.filterValues { it != null } as Map<String, Any>
      docRef.set(sanitized).await()

      Result.success(
        Student(
          id = docRef.id,
          userId = uid,
          nis = cleanNis,
          fullName = cleanName,
          className = cleanClass,
          gender = cleanGender,
          qrToken = qrToken,
          qrStatus = qrStatus,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateStudent(
    userId: String,
    studentId: String,
    nis: String,
    fullName: String,
    className: String,
    gender: String,
  ): Result<Unit> {
    val docPath = "users/$userId/students/$studentId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      require(studentId.isNotBlank()) { "ID siswa kosong." }
      val cleanNis = nis.trim()
      val cleanName = fullName.trim()
      val cleanClass = className.trim().uppercase()
      val cleanGender = if (gender == "P") "P" else "L"

      require(cleanNis.length in 1..30) { "NIS harus 1-30 karakter." }
      require(cleanName.length in 1..100) { "Nama siswa harus 1-100 karakter." }
      require(cleanClass.length in 1..30) { "Kelas harus 1-30 karakter." }

      db.collection("users")
        .document(uid)
        .collection("students")
        .document(studentId)
        .update(
          mapOf(
            "nis" to cleanNis,
            "fullName" to cleanName,
            "className" to cleanClass,
            "gender" to cleanGender,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun issueOrReissueQr(userId: String, student: Student): Result<String> {
    val docPath = "users/$userId/students/${student.id}"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      require(student.id.isNotBlank()) { "ID siswa kosong." }

      val newToken = generateQrToken(student.nis)
      db.collection("users")
        .document(uid)
        .collection("students")
        .document(student.id)
        .update(
          mapOf(
            "qrToken" to newToken,
            "qrStatus" to QrStatus.ACTIVE.name,
            "qrIssuedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(newToken)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun revokeQr(userId: String, studentId: String): Result<Unit> {
    val docPath = "users/$userId/students/$studentId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      require(studentId.isNotBlank()) { "ID siswa kosong." }

      db.collection("users")
        .document(uid)
        .collection("students")
        .document(studentId)
        .update(
          mapOf(
            "qrStatus" to QrStatus.REVOKED.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun batchIssuePendingQr(userId: String, students: List<Student>): Result<Int> {
    val path = "users/$userId/students"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      val pending = students.filter { it.qrStatusEnum != QrStatus.ACTIVE && it.id.isNotBlank() }
      if (pending.isEmpty()) return Result.success(0)

      val chunks = pending.chunked(400)
      for (chunk in chunks) {
        val batch = db.batch()
        for (student in chunk) {
          val ref = db.collection("users").document(uid).collection("students").document(student.id)
          val token = generateQrToken(student.nis)
          batch.update(
            ref,
            mapOf(
              "qrToken" to token,
              "qrStatus" to QrStatus.ACTIVE.name,
              "qrIssuedAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
        batch.commit().await()
      }
      Result.success(pending.size)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteStudent(userId: String, studentId: String): Result<Unit> {
    val docPath = "users/$userId/students/$studentId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      require(studentId.isNotBlank()) { "ID siswa kosong." }

      db.collection("users")
        .document(uid)
        .collection("students")
        .document(studentId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun recordQrAttendance(
    userId: String,
    student: Student,
    dateKey: String,
    scanMode: ScanMode,
    deadlineHour: Int,
    deadlineMinute: Int,
  ): Result<AttendanceScanOutcome> {
    val docId = "${dateKey}_${student.id}"
    val docPath = "users/$userId/attendance/$docId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      require(student.id.isNotBlank()) { "ID siswa tidak valid." }

      val docRef = db.collection("users").document(uid).collection("attendance").document(docId)
      val existingSnap = docRef.get().await()
      val existing =
        if (existingSnap.exists()) {
          existingSnap.toObject(
            AttendanceRecord::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }

      when (scanMode) {
        ScanMode.CHECK_IN -> {
          if (existing != null && existing.checkInTime != null) {
            return Result.success(AttendanceScanOutcome.AlreadyCheckedIn(student, existing))
          }
          val status =
            if (DateTimeUtils.isAfterDeadline(deadlineHour, deadlineMinute)) {
              AttendanceStatus.TERLAMBAT
            } else {
              AttendanceStatus.HADIR
            }
          if (existing == null) {
            docRef
              .set(
                mapOf(
                  "userId" to uid,
                  "studentId" to student.id,
                  "studentNis" to student.nis,
                  "studentName" to student.fullName,
                  "className" to student.className,
                  "dateKey" to dateKey,
                  "status" to status.name,
                  "method" to AttendanceMethod.QR_SCAN.name,
                  "note" to "",
                  "checkInTime" to FieldValue.serverTimestamp(),
                  "createdAt" to FieldValue.serverTimestamp(),
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
              .await()
          } else {
            docRef
              .update(
                mapOf(
                  "studentNis" to student.nis,
                  "studentName" to student.fullName,
                  "className" to student.className,
                  "status" to status.name,
                  "method" to AttendanceMethod.QR_SCAN.name,
                  "checkInTime" to FieldValue.serverTimestamp(),
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
              .await()
          }
          Result.success(AttendanceScanOutcome.CheckedIn(student, status))
        }
        ScanMode.CHECK_OUT -> {
          if (existing == null || existing.checkInTime == null) {
            return Result.success(AttendanceScanOutcome.NotCheckedInYet(student))
          }
          if (existing.checkOutTime != null) {
            return Result.success(AttendanceScanOutcome.AlreadyCheckedOut(student, existing))
          }
          docRef
            .update(
              mapOf(
                "checkOutTime" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
          Result.success(AttendanceScanOutcome.CheckedOut(student))
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun setManualAttendance(
    userId: String,
    student: Student,
    dateKey: String,
    status: AttendanceStatus,
    note: String,
  ): Result<Unit> {
    val docId = "${dateKey}_${student.id}"
    val docPath = "users/$userId/attendance/$docId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Sesi admin tidak sesuai." }
      val cleanNote = note.trim().take(200)
      val docRef = db.collection("users").document(uid).collection("attendance").document(docId)
      val existingSnap = docRef.get().await()

      val includeCheckIn = status == AttendanceStatus.HADIR || status == AttendanceStatus.TERLAMBAT
      if (!existingSnap.exists()) {
        val payload =
          mutableMapOf<String, Any?>(
            "userId" to uid,
            "studentId" to student.id,
            "studentNis" to student.nis,
            "studentName" to student.fullName,
            "className" to student.className,
            "dateKey" to dateKey,
            "status" to status.name,
            "method" to AttendanceMethod.MANUAL.name,
            "note" to cleanNote,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (includeCheckIn) {
          payload["checkInTime"] = FieldValue.serverTimestamp()
        }
        @Suppress("UNCHECKED_CAST")
        docRef.set(payload.filterValues { it != null } as Map<String, Any>).await()
      } else {
        val updates =
          mutableMapOf<String, Any>(
            "studentNis" to student.nis,
            "studentName" to student.fullName,
            "className" to student.className,
            "status" to status.name,
            "method" to AttendanceMethod.MANUAL.name,
            "note" to cleanNote,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (includeCheckIn && existingSnap.getTimestamp("checkInTime") == null) {
          updates["checkInTime"] = FieldValue.serverTimestamp()
        }
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }
}
