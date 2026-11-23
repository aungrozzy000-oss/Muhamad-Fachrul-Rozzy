package com.example

import com.example.data.model.AttendanceStatus
import com.example.data.model.QrStatus
import com.example.data.model.Student
import com.example.util.DateTimeUtils
import com.example.util.ShareAndExportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun csv_export_formats_student_and_status() {
    val student =
      Student(
        id = "stu_1",
        userId = "admin_1",
        nis = "2026001",
        fullName = "Andi Pratama",
        className = "X-A",
        gender = "L",
        qrToken = "TRN-2026001-ABC123",
        qrStatus = QrStatus.ACTIVE.name,
      )
    val csv = ShareAndExportUtils.buildAttendanceCsv("2026-11-22", listOf(student), emptyList())
    assertTrue(csv.contains("Andi Pratama"))
    assertTrue(csv.contains("2026001"))
    assertTrue(csv.contains("Belum Absen"))
  }

  @Test
  fun deadline_formatting_and_enum_fallbacks_work() {
    assertEquals("07:15", DateTimeUtils.formatDeadline(7, 15))
    assertEquals(AttendanceStatus.HADIR, AttendanceStatus.fromRaw("UNKNOWN_VALUE"))
    assertEquals(QrStatus.PENDING, QrStatus.fromRaw(null))
  }
}
