package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.QrStatus
import com.example.data.model.Student
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.QrStatusBadge

private enum class QrFilter(val label: String) {
  ALL("Semua"),
  ACTIVE("QR Aktif"),
  UNISSUED("Belum Terbit / Dicabut"),
}

@Composable
fun StudentsScreen(
  studentsState: UiState<List<Student>>,
  isMutating: Boolean,
  onAddStudent: () -> Unit,
  onEditStudent: (Student) -> Unit,
  onDeleteStudent: (Student) -> Unit,
  onIssueOrOpenQr: (Student) -> Unit,
  onBatchIssuePendingQr: () -> Unit,
  onOpenManualAttendance: (Student) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var qrFilter by remember { mutableStateOf(QrFilter.ALL) }
  var selectedClass by remember { mutableStateOf("SEMUA") }
  var studentPendingDelete by remember { mutableStateOf<Student?>(null) }

  val students = (studentsState as? UiState.Success)?.data ?: emptyList()
  val pendingCount =
    remember(students) { students.count { it.qrStatusEnum != QrStatus.ACTIVE } }
  val classOptions =
    remember(students) { listOf("SEMUA") + students.map { it.className }.distinct().sorted() }

  val filteredStudents =
    remember(students, searchQuery, qrFilter, selectedClass) {
      students.filter { student ->
        val matchesQuery =
          searchQuery.isBlank() ||
            student.fullName.contains(searchQuery, ignoreCase = true) ||
            student.nis.contains(searchQuery, ignoreCase = true) ||
            student.className.contains(searchQuery, ignoreCase = true)
        val matchesQr =
          when (qrFilter) {
            QrFilter.ALL -> true
            QrFilter.ACTIVE -> student.qrStatusEnum == QrStatus.ACTIVE
            QrFilter.UNISSUED -> student.qrStatusEnum != QrStatus.ACTIVE
          }
        val matchesClass = selectedClass == "SEMUA" || student.className == selectedClass
        matchesQuery && matchesQr && matchesClass
      }
    }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddStudent,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_student_fab"),
      ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Tambah Siswa")
      }
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Data Siswa & Kartu QR",
              style = MaterialTheme.typography.headlineSmall,
            )
            Text(
              text = "Kelola siswa SMA Taruna Andigha dan terbitkan QR Code untuk masing-masing siswa.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Batch Issue Banner if any student lacks an active QR
        if (pendingCount > 0) {
          item {
            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
              shape = RoundedCornerShape(18.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "$pendingCount siswa belum memiliki QR aktif",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                  Text(
                    text = "Terbitkan QR Code secara langsung untuk semua siswa yang tertunda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                  )
                }
                Button(
                  onClick = onBatchIssuePendingQr,
                  enabled = !isMutating,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.height(48.dp).testTag("batch_issue_qr_button"),
                ) {
                  Text("Terbitkan")
                }
              }
            }
          }
        }

        // Search Field
        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari nama siswa, NIS, atau kelas...") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(imageVector = Icons.Filled.Close, contentDescription = "Bersihkan pencarian")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("student_search_input"),
          )
        }

        // Filter Chips
        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              QrFilter.entries.forEach { filter ->
                FilterChip(
                  selected = qrFilter == filter,
                  onClick = { qrFilter = filter },
                  label = { Text(filter.label) },
                )
              }
            }
            if (classOptions.size > 1) {
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                classOptions.forEach { kelas ->
                  FilterChip(
                    selected = selectedClass == kelas,
                    onClick = { selectedClass = kelas },
                    label = { Text(if (kelas == "SEMUA") "Semua Kelas" else kelas) },
                  )
                }
              }
            }
          }
        }

        when (studentsState) {
          is UiState.Loading -> {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            }
          }
          is UiState.Error -> {
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
          is UiState.Success -> {
            if (filteredStudents.isEmpty()) {
              item {
                EmptyStateCard(
                  title =
                    if (students.isEmpty()) "Belum Ada Siswa Terdaftar"
                    else "Tidak Ada Siswa yang Cocok",
                  subtitle =
                    if (students.isEmpty()) {
                      "Tekan tombol tambah untuk mendaftarkan siswa SMA Taruna Andigha dan mengeluarkan kartu QR Code."
                    } else {
                      "Coba ubah kata kunci pencarian atau filter status QR."
                    },
                  ctaLabel = if (students.isEmpty()) "Tambah Siswa Pertama" else null,
                  onCtaClick = if (students.isEmpty()) onAddStudent else null,
                )
              }
            } else {
              items(filteredStudents, key = { it.id }) { student ->
                StudentCard(
                  student = student,
                  onIssueOrOpenQr = { onIssueOrOpenQr(student) },
                  onEdit = { onEditStudent(student) },
                  onManualAttendance = { onOpenManualAttendance(student) },
                  onRequestDelete = { studentPendingDelete = student },
                )
              }
            }
          }
        }
      }
    }
  }

  if (studentPendingDelete != null) {
    val target = studentPendingDelete!!
    AlertDialog(
      onDismissRequest = { studentPendingDelete = null },
      title = { Text("Hapus Data Siswa?") },
      text = {
        Text(
          "Data ${target.fullName} (NIS ${target.nis}) dan QR Code-nya akan dihapus permanen. Lanjutkan?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteStudent(target)
            studentPendingDelete = null
          },
          modifier = Modifier.height(48.dp).testTag("confirm_delete_student_button"),
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { studentPendingDelete = null },
          modifier = Modifier.height(48.dp),
        ) {
          Text("Batal")
        }
      },
    )
  }
}

@Composable
private fun StudentCard(
  student: Student,
  onIssueOrOpenQr: () -> Unit,
  onEdit: () -> Unit,
  onManualAttendance: () -> Unit,
  onRequestDelete: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth().testTag("student_card_${student.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = CircleShape,
          modifier = Modifier.size(46.dp),
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
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = student.fullName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "NIS ${student.nis} • Kelas ${student.className} • ${if (student.gender == "P") "Putri" else "Putra"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(48.dp).testTag("student_menu_${student.id}"),
          ) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Opsi siswa")
          }
          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
          ) {
            DropdownMenuItem(
              text = { Text("Ubah Data Siswa") },
              leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onEdit()
              },
            )
            DropdownMenuItem(
              text = { Text("Atur Kehadiran Manual") },
              leadingIcon = { Icon(Icons.Filled.HowToReg, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onManualAttendance()
              },
            )
            DropdownMenuItem(
              text = { Text("Hapus Siswa", color = MaterialTheme.colorScheme.error) },
              leadingIcon = {
                Icon(
                  Icons.Filled.Delete,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
              },
              onClick = {
                menuExpanded = false
                onRequestDelete()
              },
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          QrStatusBadge(status = student.qrStatusEnum)
          if (student.qrToken.isNotBlank()) {
            Text(
              text = student.qrToken,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (student.qrStatusEnum == QrStatus.ACTIVE) {
          FilledTonalButton(
            onClick = onIssueOrOpenQr,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("open_qr_button_${student.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.QrCode2,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Kartu QR")
          }
        } else {
          Button(
            onClick = onIssueOrOpenQr,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("issue_student_qr_button_${student.id}"),
          ) {
            Icon(
              imageVector = Icons.Filled.QrCode2,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Keluarkan QR")
          }
        }
      }
    }
  }
}
