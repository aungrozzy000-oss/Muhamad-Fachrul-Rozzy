package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.data.model.Student

private val SUGGESTED_CLASSES = listOf("X-A", "X-B", "XI-IPA-1", "XI-IPS-1", "XII-IPA-1", "XII-IPS-1")

@Composable
fun StudentFormDialog(
  initialStudent: Student?,
  isMutating: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (nis: String, fullName: String, className: String, gender: String, issueQr: Boolean) -> Unit,
) {
  var nis by remember(initialStudent) { mutableStateOf(initialStudent?.nis ?: "") }
  var fullName by remember(initialStudent) { mutableStateOf(initialStudent?.fullName ?: "") }
  var className by remember(initialStudent) { mutableStateOf(initialStudent?.className ?: "X-A") }
  var gender by remember(initialStudent) { mutableStateOf(initialStudent?.gender ?: "L") }
  var issueQrImmediately by remember(initialStudent) { mutableStateOf(true) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val isEdit = initialStudent != null

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEdit) "Ubah Data Siswa" else "Tambah Siswa Baru",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = nis,
          onValueChange = {
            if (it.length <= 30) {
              nis = it
              validationError = null
            }
          },
          label = { Text("NIS (Nomor Induk Siswa)") },
          placeholder = { Text("Misal: 2026001") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("student_nis_input"),
        )

        OutlinedTextField(
          value = fullName,
          onValueChange = {
            if (it.length <= 100) {
              fullName = it
              validationError = null
            }
          },
          label = { Text("Nama Lengkap Siswa") },
          placeholder = { Text("Misal: Pratama Wijaya") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("student_name_input"),
        )

        OutlinedTextField(
          value = className,
          onValueChange = {
            if (it.length <= 30) {
              className = it
              validationError = null
            }
          },
          label = { Text("Kelas") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("student_class_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SUGGESTED_CLASSES.forEach { preset ->
            FilterChip(
              selected = className.equals(preset, ignoreCase = true),
              onClick = { className = preset },
              label = { Text(preset) },
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Jenis Kelamin:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 4.dp),
          )
          FilterChip(
            selected = gender == "L",
            onClick = { gender = "L" },
            label = { Text("Putra (L)") },
          )
          FilterChip(
            selected = gender == "P",
            onClick = { gender = "P" },
            label = { Text("Putri (P)") },
          )
        }

        if (!isEdit) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Langsung Keluarkan QR Code",
                style = MaterialTheme.typography.titleSmall,
              )
              Text(
                text = "Aktifkan kartu QR siswa saat disimpan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = issueQrImmediately,
              onCheckedChange = { issueQrImmediately = it },
              modifier = Modifier.testTag("issue_qr_switch"),
            )
          }
        }

        if (validationError != null) {
          Text(
            text = validationError ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            nis.trim().isEmpty() -> validationError = "NIS tidak boleh kosong."
            fullName.trim().isEmpty() -> validationError = "Nama lengkap tidak boleh kosong."
            className.trim().isEmpty() -> validationError = "Kelas tidak boleh kosong."
            else ->
              onConfirm(
                nis.trim(),
                fullName.trim(),
                className.trim().uppercase(),
                gender,
                issueQrImmediately,
              )
          }
        },
        enabled = !isMutating,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp).testTag("student_save_button"),
      ) {
        Text(if (isEdit) "Simpan" else "Tambah")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text("Batal")
      }
    },
  )
}
