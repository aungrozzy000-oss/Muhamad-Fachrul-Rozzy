package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceStatus
import com.example.data.model.Student

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualAttendanceSheet(
  student: Student,
  currentStatus: AttendanceStatus?,
  currentNote: String,
  isMutating: Boolean,
  onDismiss: () -> Unit,
  onSave: (AttendanceStatus, String) -> Unit,
) {
  var selectedStatus by remember(student.id, currentStatus) {
    mutableStateOf(currentStatus ?: AttendanceStatus.HADIR)
  }
  var note by remember(student.id, currentNote) { mutableStateOf(currentNote) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Column {
        Text(
          text = "Atur Kehadiran Manual",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "${student.fullName} (${student.nis}) • ${student.className}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AttendanceStatus.entries.forEach { status ->
          FilterChip(
            selected = selectedStatus == status,
            onClick = { selectedStatus = status },
            label = { Text(status.label) },
          )
        }
      }

      OutlinedTextField(
        value = note,
        onValueChange = { if (it.length <= 200) note = it },
        label = { Text("Catatan (opsional)") },
        placeholder = { Text("Contoh: Izin acara keluarga / Surat dokter") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("manual_attendance_note"),
      )

      Button(
        onClick = { onSave(selectedStatus, note) },
        enabled = !isMutating,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_manual_attendance_button"),
      ) {
        Text("Simpan Status Kehadiran")
      }

      TextButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Text("Batal")
      }
    }
  }
}
