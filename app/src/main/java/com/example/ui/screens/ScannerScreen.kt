package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.ScanMode
import com.example.ui.components.QrScannerCameraPreview
import com.example.ui.theme.StatusHadir
import com.example.ui.theme.StatusHadirBg
import com.example.ui.theme.StatusTerlambat
import com.example.ui.theme.StatusTerlambatBg
import com.example.ui.viewmodel.ScanFeedback

@Composable
fun ScannerScreen(
  scanMode: ScanMode,
  scanBanner: ScanFeedback?,
  isMutating: Boolean,
  onSelectScanMode: (ScanMode) -> Unit,
  onQrScanned: (raw: String, isManual: Boolean) -> Unit,
  onClearBanner: () -> Unit,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var cameraError by remember { mutableStateOf<String?>(null) }
  var isTorchEnabled by remember { mutableStateOf(false) }
  var manualTokenInput by remember { mutableStateOf("") }

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      hasCameraPermission = granted
    }

  LaunchedEffect(scanBanner) {
    if (scanBanner is ScanFeedback.Success) {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 720.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Pemindai QR Absensi",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Arahkan kamera ke Kartu QR Siswa SMA Taruna Andigha",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (hasCameraPermission && cameraError == null) {
          FilledIconToggleButton(
            checked = isTorchEnabled,
            onCheckedChange = { isTorchEnabled = it },
            modifier = Modifier.size(48.dp).testTag("torch_toggle_button"),
          ) {
            Icon(
              imageVector =
                if (isTorchEnabled) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
              contentDescription = "Lampu senter kamera",
            )
          }
        }
      }

      // Check-in / Check-out Mode Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ScanMode.entries.forEach { mode ->
          FilterChip(
            selected = scanMode == mode,
            onClick = { onSelectScanMode(mode) },
            label = { Text(mode.label) },
            modifier = Modifier.weight(1f).testTag("scan_mode_${mode.name}"),
          )
        }
      }

      // Scan Result Banner
      AnimatedVisibility(visible = scanBanner != null) {
        when (val banner = scanBanner) {
          is ScanFeedback.Success -> {
            Surface(
              color = StatusHadirBg,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().testTag("scan_feedback_success"),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = StatusHadir,
                  modifier = Modifier.size(30.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StatusHadir,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = banner.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusHadir,
                  )
                }
                Surface(color = StatusHadir, shape = RoundedCornerShape(50)) {
                  Text(
                    text = banner.badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
          is ScanFeedback.Warning -> {
            Surface(
              color = StatusTerlambatBg,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().testTag("scan_feedback_warning"),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Warning,
                  contentDescription = null,
                  tint = StatusTerlambat,
                  modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = StatusTerlambat,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = banner.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusTerlambat,
                  )
                }
              }
            }
          }
          is ScanFeedback.Error -> {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().testTag("scan_feedback_error"),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Error,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = banner.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }
          null -> Unit
        }
      }

      // Live Camera Preview or Permission Card
      if (hasCameraPermission && cameraError == null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(330.dp)
              .clip(RoundedCornerShape(24.dp))
        ) {
          QrScannerCameraPreview(
            isTorchEnabled = isTorchEnabled,
            onQrDetected = { raw -> onQrScanned(raw, false) },
            onCameraError = { err -> cameraError = err },
            modifier = Modifier.fillMaxSize(),
          )
        }
      } else {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CameraAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp),
            )
            Text(
              text =
                cameraError ?: stringResource(id = R.string.camera_permission_rationale),
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (cameraError == null) {
              Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp).testTag("grant_camera_button"),
              ) {
                Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.grant_camera_permission))
              }
            }
          }
        }
      }

      // Manual Token / NIS Entry Fallback Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Keyboard,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Input NIS / Token QR Manual",
              style = MaterialTheme.typography.titleSmall,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = manualTokenInput,
              onValueChange = {
                manualTokenInput = it
                if (scanBanner != null) onClearBanner()
              },
              placeholder = { Text("NIS atau TRN-...") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("manual_qr_input"),
            )
            Button(
              onClick = {
                if (manualTokenInput.isNotBlank()) {
                  onQrScanned(manualTokenInput.trim(), true)
                  manualTokenInput = ""
                }
              },
              enabled = manualTokenInput.isNotBlank() && !isMutating,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(52.dp).testTag("manual_qr_submit_button"),
            ) {
              Text("Catat")
            }
          }
        }
      }
    }
  }
}
