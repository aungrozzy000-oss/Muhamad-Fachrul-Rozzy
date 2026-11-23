package com.example.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {
  private val localeId = Locale.Builder().setLanguage("id").setRegion("ID").build()

  fun todayDateKey(): String = offsetDateKey(0)

  fun offsetDateKey(daysAgo: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
  }

  fun formatDateKeyReadable(dateKey: String): String {
    return try {
      val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) ?: return dateKey
      val today = todayDateKey()
      val yesterday = offsetDateKey(1)
      val formatted = SimpleDateFormat("EEEE, d MMM yyyy", localeId).format(parsed)
      when (dateKey) {
        today -> "Hari Ini • $formatted"
        yesterday -> "Kemarin • $formatted"
        else -> formatted
      }
    } catch (_: Exception) {
      dateKey
    }
  }

  fun formatTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "-"
    return SimpleDateFormat("HH:mm", Locale.US).format(timestamp.toDate())
  }

  fun formatDateTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "-"
    return SimpleDateFormat("d MMM yyyy, HH:mm", localeId).format(timestamp.toDate())
  }

  fun isAfterDeadline(deadlineHour: Int, deadlineMinute: Int): Boolean {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    return currentHour > deadlineHour ||
      (currentHour == deadlineHour && currentMinute > deadlineMinute)
  }

  fun formatDeadline(hour: Int, minute: Int): String {
    return String.format(Locale.US, "%02d:%02d", hour, minute)
  }

  fun nowClockText(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
  }
}
