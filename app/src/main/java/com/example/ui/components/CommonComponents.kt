package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AttendanceStatus
import com.example.data.model.QrStatus
import com.example.ui.theme.StatusAlpha
import com.example.ui.theme.StatusAlphaBg
import com.example.ui.theme.StatusHadir
import com.example.ui.theme.StatusHadirBg
import com.example.ui.theme.StatusIzin
import com.example.ui.theme.StatusIzinBg
import com.example.ui.theme.StatusSakit
import com.example.ui.theme.StatusSakitBg
import com.example.ui.theme.StatusTerlambat
import com.example.ui.theme.StatusTerlambatBg

@Composable
fun AttendanceStatusPill(
  status: AttendanceStatus,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) =
    when (status) {
      AttendanceStatus.HADIR -> StatusHadirBg to StatusHadir
      AttendanceStatus.TERLAMBAT -> StatusTerlambatBg to StatusTerlambat
      AttendanceStatus.IZIN -> StatusIzinBg to StatusIzin
      AttendanceStatus.SAKIT -> StatusSakitBg to StatusSakit
      AttendanceStatus.ALPHA -> StatusAlphaBg to StatusAlpha
    }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    modifier = modifier,
  ) {
    Text(
      text = status.label,
      color = fg,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun QrStatusBadge(
  status: QrStatus,
  modifier: Modifier = Modifier,
) {
  val (bg, fg) =
    when (status) {
      QrStatus.ACTIVE -> StatusHadirBg to StatusHadir
      QrStatus.PENDING -> StatusTerlambatBg to StatusTerlambat
      QrStatus.REVOKED -> StatusAlphaBg to StatusAlpha
    }
  Surface(
    color = bg,
    shape = RoundedCornerShape(50),
    modifier = modifier,
  ) {
    Text(
      text = status.label,
      color = fg,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun StatMetricCard(
  label: String,
  value: String,
  icon: ImageVector,
  accentColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    shape = RoundedCornerShape(18.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          color = containerColor,
          shape = CircleShape,
          modifier = Modifier.size(32.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun EmptyStateCard(
  title: String,
  subtitle: String,
  ctaLabel: String? = null,
  onCtaClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_attendance),
        contentDescription = title,
        modifier = Modifier.fillMaxWidth(0.68f).height(150.dp).clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (ctaLabel != null && onCtaClick != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onCtaClick,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(48.dp).testTag("empty_state_cta_button"),
        ) {
          Text(ctaLabel)
        }
      }
    }
  }
}
