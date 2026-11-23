package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminSettings
import com.example.util.DateTimeUtils

@Composable
fun SettingsScreen(
  userEmail: String?,
  userDisplayName: String?,
  settings: AdminSettings?,
  isMutating: Boolean,
  onSaveSettings: (adminName: String, deadlineHour: Int, deadlineMinute: Int) -> Unit,
  onSignOut: () -> Unit,
) {
  var adminName by remember(settings?.adminName, userDisplayName) {
    mutableStateOf(settings?.adminName ?: userDisplayName ?: "Admin Taruna")
  }
  var hour by remember(settings?.checkInDeadlineHour) {
    mutableIntStateOf((settings?.checkInDeadlineHour ?: 7L).toInt())
  }
  var minute by remember(settings?.checkInDeadlineMinute) {
    mutableIntStateOf((settings?.checkInDeadlineMinute ?: 15L).toInt())
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 680.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = "Pengaturan Sekolah",
        style = MaterialTheme.typography.headlineSmall,
      )

      // Account Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(52.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = settings?.schoolName ?: "SMA Taruna Andigha",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = userDisplayName ?: "Administrator",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (!userEmail.isNullOrBlank()) {
              Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Deadline & Admin Name Config
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Batas Jam Masuk Tepat Waktu",
              style = MaterialTheme.typography.titleMedium,
            )
          }

          OutlinedTextField(
            value = adminName,
            onValueChange = { if (it.length <= 100) adminName = it },
            label = { Text("Nama Admin / Operator") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_admin_name_input"),
          )

          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Jam Batas Masuk",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = DateTimeUtils.formatDeadline(hour, minute),
                  style = MaterialTheme.typography.displaySmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = {
                    val total = (hour * 60 + minute - 15).coerceAtLeast(5 * 60)
                    hour = total / 60
                    minute = total % 60
                  },
                  modifier = Modifier.size(48.dp),
                ) {
                  Icon(Icons.Filled.Remove, contentDescription = "Kurangi 15 menit")
                }
                IconButton(
                  onClick = {
                    val total = (hour * 60 + minute + 15).coerceAtMost(12 * 60)
                    hour = total / 60
                    minute = total % 60
                  },
                  modifier = Modifier.size(48.dp),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = "Tambah 15 menit")
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            listOf(
              (6 to 45),
              (7 to 0),
              (7 to 15),
              (7 to 30),
            ).forEach { (h, m) ->
              FilterChip(
                selected = hour == h && minute == m,
                onClick = {
                  hour = h
                  minute = m
                },
                label = { Text(DateTimeUtils.formatDeadline(h, m)) },
              )
            }
          }

          Button(
            onClick = {
              if (adminName.isNotBlank()) {
                onSaveSettings(adminName.trim(), hour, minute)
              }
            },
            enabled = !isMutating && adminName.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_button"),
          ) {
            Icon(imageVector = Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Pengaturan")
          }
        }
      }

      // Sign Out Button
      OutlinedButton(
        onClick = onSignOut,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sign_out_button"),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Keluar dari Akun Admin")
      }
    }
  }
}
