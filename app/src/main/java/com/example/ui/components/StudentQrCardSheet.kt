package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.QrStatus
import com.example.data.model.Student
import com.example.ui.theme.TarunaGold
import com.example.ui.theme.TarunaNavy
import com.example.util.DateTimeUtils
import com.example.util.QrCodeGenerator
import com.example.util.ShareAndExportUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQrCardSheet(
  student: Student,
  isMutating: Boolean,
  onDismiss: () -> Unit,
  onIssueOrReissueQr: (Student) -> Unit,
  onRevokeQr: (Student) -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val qrBitmap =
    remember(student.qrToken, student.qrStatus) {
      if (student.qrStatusEnum == QrStatus.ACTIVE && student.qrToken.isNotBlank()) {
        QrCodeGenerator.generateQrBitmap(student.qrToken, sizePx = 560)
      } else {
        null
      }
    }

  val saveCardLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
      if (uri != null) {
        val cardBitmap = QrCodeGenerator.renderStudentQrCardBitmap(student)
        val ok = ShareAndExportUtils.saveBitmapToUri(context, uri, cardBitmap)
        Toast.makeText(
            context,
            if (ok) "Kartu QR berhasil disimpan." else "Gagal menyimpan Kartu QR.",
            Toast.LENGTH_SHORT,
          )
          .show()
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Digital Student Card Preview
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          // School Header Band
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .background(TarunaNavy)
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "SMA TARUNA ANDIGHA",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "KARTU QR ABSENSI SISWA",
                style = MaterialTheme.typography.labelMedium,
                color = TarunaGold,
              )
            }
          }
          Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(TarunaGold))

          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            QrStatusBadge(status = student.qrStatusEnum)

            if (qrBitmap != null) {
              Box(
                modifier =
                  Modifier.size(216.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFD))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
              ) {
                Image(
                  bitmap = qrBitmap.asImageBitmap(),
                  contentDescription = "QR Code milik ${student.fullName}",
                  modifier = Modifier.size(196.dp),
                )
              }
            } else {
              Box(
                modifier =
                  Modifier.size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(16.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                  )
                  Text(
                    text =
                      if (student.qrStatusEnum == QrStatus.REVOKED) {
                        "QR Code dicabut. Terbitkan ulang agar siswa dapat absen."
                      } else {
                        "QR Code belum diterbitkan untuk siswa ini."
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = student.fullName,
                style = MaterialTheme.typography.titleLarge,
                color = TarunaNavy,
                textAlign = TextAlign.Center,
              )
              Text(
                text = "NIS: ${student.nis}  •  Kelas ${student.className}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
              )
            }

            if (student.qrToken.isNotBlank()) {
              Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
              ) {
                Text(
                  text = student.qrToken,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF334155),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
            }

            if (student.qrIssuedAt != null) {
              Text(
                text = "Diterbitkan: ${DateTimeUtils.formatDateTime(student.qrIssuedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
              )
            }
          }
        }
      }

      // Admin Actions
      if (student.qrStatusEnum != QrStatus.ACTIVE) {
        Button(
          onClick = { onIssueOrReissueQr(student) },
          enabled = !isMutating,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("issue_qr_button"),
        ) {
          Icon(imageVector = Icons.Filled.QrCode2, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Keluarkan QR Code Sekarang")
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { ShareAndExportUtils.shareStudentQrCard(context, student) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("share_qr_button"),
          ) {
            Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bagikan")
          }
          FilledTonalButton(
            onClick = {
              val safeNis = student.nis.replace(Regex("[^a-zA-Z0-9_-]"), "_")
              saveCardLauncher.launch("kartu_qr_${safeNis}.png")
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(50.dp).testTag("save_qr_button"),
          ) {
            Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan PNG")
          }
        }

        HorizontalDivider()

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { onIssueOrReissueQr(student) },
            enabled = !isMutating,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("reissue_qr_button"),
          ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Terbitkan Ulang")
          }
          OutlinedButton(
            onClick = { onRevokeQr(student) },
            enabled = !isMutating,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("revoke_qr_button"),
          ) {
            Icon(imageVector = Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cabut QR")
          }
        }
      }

      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp),
      ) {
        Text("Tutup")
      }
    }
  }
}
