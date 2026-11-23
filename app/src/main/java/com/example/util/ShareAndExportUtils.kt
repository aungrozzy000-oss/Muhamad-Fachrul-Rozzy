package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import java.io.File
import java.io.FileOutputStream

object ShareAndExportUtils {

  fun shareStudentQrCard(context: Context, student: Student) {
    val cardBitmap = QrCodeGenerator.renderStudentQrCardBitmap(student)
    val safeNis = student.nis.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val outFile = File(context.cacheDir, "kartu_qr_${safeNis}.png")
    FileOutputStream(outFile).use { stream ->
      cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
          Intent.EXTRA_SUBJECT,
          "Kartu QR Absensi - ${student.fullName} (${student.className})",
        )
        putExtra(
          Intent.EXTRA_TEXT,
          "Kartu QR Absensi SMA Taruna Andigha\nNama: ${student.fullName}\nNIS: ${student.nis}\nKelas: ${student.className}\nToken: ${student.qrToken}",
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Kartu QR Siswa"))
  }

  fun saveBitmapToUri(context: Context, uri: Uri, bitmap: Bitmap): Boolean {
    return try {
      context.contentResolver.openOutputStream(uri)?.use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      } ?: false
    } catch (_: Exception) {
      false
    }
  }

  fun buildAttendanceCsv(
    dateKey: String,
    students: List<Student>,
    records: List<AttendanceRecord>,
  ): String {
    val recordByStudentId = records.associateBy { it.studentId }
    val sb = StringBuilder()
    sb.appendLine("Tanggal,NIS,Nama Siswa,Kelas,Status,Metode,Jam Masuk,Jam Pulang,Catatan")
    for (student in students.sortedWith(compareBy({ it.className }, { it.fullName }))) {
      val rec = recordByStudentId[student.id]
      val status = rec?.statusEnum?.label ?: "Belum Absen"
      val method = rec?.methodEnum?.label ?: "-"
      val checkIn = DateTimeUtils.formatTime(rec?.checkInTime)
      val checkOut = DateTimeUtils.formatTime(rec?.checkOutTime)
      val note = (rec?.note ?: "").replace("\"", "\"\"")
      val nameEsc = student.fullName.replace("\"", "\"\"")
      sb.appendLine(
        "$dateKey,\"${student.nis}\",\"$nameEsc\",\"${student.className}\",$status,$method,$checkIn,$checkOut,\"$note\""
      )
    }
    return sb.toString()
  }

  fun shareAttendanceCsv(context: Context, dateKey: String, csvContent: String) {
    val outFile = File(context.cacheDir, "absensi_taruna_${dateKey}.csv")
    outFile.writeText(csvContent, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Rekap Absensi SMA Taruna Andigha - $dateKey")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Rekap Absensi CSV"))
  }
}
