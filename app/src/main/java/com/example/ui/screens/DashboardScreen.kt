package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdminSettings
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Student
import com.example.data.model.UiState
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.StatusAlpha
import com.example.ui.theme.StatusAlphaBg
import com.example.ui.theme.StatusHadir
import com.example.ui.theme.StatusHadirBg
import com.example.ui.theme.StatusTerlambat
import com.example.ui.theme.StatusTerlambatBg
import com.example.ui.theme.TarunaGold
import com.example.ui.theme.TarunaNavy
import com.example.util.DateTimeUtils
import com.example.util.ShareAndExportUtils

@Composable
fun DashboardScreen(
  studentsState: UiState<List<Student>>,
  attendanceState: UiState<List<AttendanceRecord>>,
  selectedDateKey: String,
  settings: AdminSettings?,
  onSelectDate: (String) -> Unit,
  onNavigateToScanner: () -> Unit,
  onNavigateToStudents: () -> Unit,
  onOpenStudentQr: (Student) -> Unit,
  onOpenManualAttendance: (Student, AttendanceRecord?) -> Unit,
) {
  val context = LocalContext.current
  var selectedClassFilter by remember { mutableStateOf("SEMUA") }

  val students = (studentsState as? UiState.Success)?.data ?: emptyList()
  val records = (attendanceState as? UiState.Success)?.data ?: emptyList()

  val classOptions =
    remember(students) {
      listOf("SEMUA") + students.map { it.className }.distinct().sorted()
    }

  val filteredStudents =
    remember(students, selectedClassFilter) {
      if (selectedClassFilter == "SEMUA") students
      else students.filter { it.className == selectedClassFilter }
    }

  val recordByStudentId = remember(records) { records.associateBy { it.studentId } }

  val totalCount = filteredStudents.size
  val hadirCount =
    filteredStudents.count {
      recordByStudentId[it.id]?.statusEnum == AttendanceStatus.HADIR
    }
  val terlambatCount =
    filteredStudents.count {
      recordByStudentId[it.id]?.statusEnum == AttendanceStatus.TERLAMBAT
    }
  val belumAbsenCount =
    filteredStudents.count { recordByStudentId[it.id] == null }

  val deadlineText =
    DateTimeUtils.formatDeadline(
      (settings?.checkInDeadlineHour ?: 7L).toInt(),
      (settings?.checkInDeadlineMinute ?: 15L).toInt(),
    )

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Hero Banner Card
      item {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(156.dp)
              .clip(RoundedCornerShape(22.dp))
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_school_banner),
            contentDescription = "Banner SMA Taruna Andigha",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors =
                      listOf(
                        TarunaNavy.copy(alpha = 0.94f),
                        TarunaNavy.copy(alpha = 0.68f),
                      )
                  )
                )
          )
          Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                color = TarunaGold,
                shape = RoundedCornerShape(50),
              ) {
                Text(
                  text = "SMA TARUNA ANDIGHA",
                  style = MaterialTheme.typography.labelMedium,
                  color = TarunaNavy,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
              Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = TarunaGold,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "Masuk ≤ $deadlineText",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                  )
                }
              }
            }

            Column {
              Text(
                text = "Rekap Kehadiran Siswa",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
              )
              Text(
                text = DateTimeUtils.formatDateKeyReadable(selectedDateKey),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
              )
            }
          }
        }
      }

      // Quick Actions
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onNavigateToScanner,
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
              ),
            modifier = Modifier.weight(1f).height(50.dp).testTag("dashboard_scan_button"),
          ) {
            Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan QR Absen")
          }
          FilledTonalButton(
            onClick = {
              val csv = ShareAndExportUtils.buildAttendanceCsv(selectedDateKey, filteredStudents, records)
              ShareAndExportUtils.shareAttendanceCsv(context, selectedDateKey, csv)
            },
            enabled = filteredStudents.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("export_csv_button"),
          ) {
            Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ekspor CSV")
          }
        }
      }

      // Date Switcher Chips
      item {
        val dateOptions =
          remember {
            listOf(
              DateTimeUtils.todayDateKey() to "Hari Ini",
              DateTimeUtils.offsetDateKey(1) to "Kemarin",
              DateTimeUtils.offsetDateKey(2) to "2 Hari Lalu",
            )
          }
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          dateOptions.forEach { (key, label) ->
            FilterChip(
              selected = selectedDateKey == key,
              onClick = { onSelectDate(key) },
              label = { Text(label) },
            )
          }
        }
      }

      // 2x2 Summary Metrics
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            StatMetricCard(
              label = "Total Siswa",
              value = totalCount.toString(),
              icon = Icons.Filled.Groups,
              accentColor = MaterialTheme.colorScheme.primary,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.weight(1f),
            )
            StatMetricCard(
              label = "Hadir Tepat Waktu",
              value = hadirCount.toString(),
              icon = Icons.Filled.CheckCircle,
              accentColor = StatusHadir,
              containerColor = StatusHadirBg,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            StatMetricCard(
              label = "Terlambat",
              value = terlambatCount.toString(),
              icon = Icons.Filled.AccessTime,
              accentColor = StatusTerlambat,
              containerColor = StatusTerlambatBg,
              modifier = Modifier.weight(1f),
            )
            StatMetricCard(
              label = "Belum Absen",
              value = belumAbsenCount.toString(),
              icon = Icons.Filled.HourglassEmpty,
              accentColor = StatusAlpha,
              containerColor = StatusAlphaBg,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Class Filter Row
      if (classOptions.size > 1) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            classOptions.forEach { kelas ->
              FilterChip(
                selected = selectedClassFilter == kelas,
                onClick = { selectedClassFilter = kelas },
                label = { Text(if (kelas == "SEMUA") "Semua Kelas" else "Kelas $kelas") },
              )
            }
          }
        }
      }

      // Attendance List Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Daftar Kehadiran (${filteredStudents.size})",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Ketuk siswa untuk ubah status",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Loading / Error / Empty / List
      when {
        studentsState is UiState.Loading || attendanceState is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        studentsState is UiState.Error -> {
          item {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = studentsState.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        attendanceState is UiState.Error -> {
          item {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = attendanceState.message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        filteredStudents.isEmpty() -> {
          item {
            EmptyStateCard(
              title = "Belum Ada Data Siswa",
              subtitle =
                "Tambahkan siswa SMA Taruna Andigha dan keluarkan QR Code masing-masing siswa untuk mulai mencatat absensi.",
              ctaLabel = "Kelola Siswa & QR Code",
              onCtaClick = onNavigateToStudents,
            )
          }
        }
        else -> {
          items(filteredStudents, key = { it.id }) { student ->
            val rec = recordByStudentId[student.id]
            AttendanceStudentRow(
              student = student,
              record = rec,
              onClick = { onOpenManualAttendance(student, rec) },
              onOpenQr = { onOpenStudentQr(student) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AttendanceStudentRow(
  student: Student,
  record: AttendanceRecord?,
  onClick: () -> Unit,
  onOpenQr: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("attendance_row_${student.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = student.fullName.take(2).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = student.fullName,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "NIS ${student.nis} • ${student.className}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (record != null) {
          val masuk = DateTimeUtils.formatTime(record.checkInTime)
          val pulang = DateTimeUtils.formatTime(record.checkOutTime)
          val timeText =
            if (record.checkOutTime != null) "Masuk $masuk • Pulang $pulang"
            else if (record.checkInTime != null) "Masuk $masuk • ${record.methodEnum.label}"
            else record.note.ifBlank { record.methodEnum.label }
          Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (record != null) {
        AttendanceStatusPill(status = record.statusEnum)
      } else {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = "Belum Absen",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      IconButton(
        onClick = onOpenQr,
        modifier = Modifier.size(48.dp).testTag("row_qr_button_${student.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.QrCode2,
          contentDescription = "Lihat QR ${student.fullName}",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
