package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.Student
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {

  fun generateQrBitmap(
    content: String,
    sizePx: Int = 512,
    darkColor: Int = 0xFF0F2847.toInt(),
    lightColor: Int = Color.WHITE,
  ): Bitmap? {
    if (content.isBlank()) return null
    return try {
      val hints =
        mapOf(
          EncodeHintType.CHARACTER_SET to "UTF-8",
          EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
          EncodeHintType.MARGIN to 1,
        )
      val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      val width = bitMatrix.width
      val height = bitMatrix.height
      val pixels = IntArray(width * height)
      for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
          pixels[offset + x] = if (bitMatrix[x, y]) darkColor else lightColor
        }
      }
      Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
      }
    } catch (_: Exception) {
      null
    }
  }

  fun renderStudentQrCardBitmap(
    student: Student,
    schoolName: String = "SMA TARUNA ANDIGHA",
  ): Bitmap {
    val width = 900
    val height = 1220
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background
    canvas.drawColor(0xFFF6F8FB.toInt())

    // Outer card surface
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD6E0EE.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
      }
    val cardRect = RectF(36f, 36f, width - 36f, height - 36f)
    canvas.drawRoundRect(cardRect, 40f, 40f, cardPaint)
    canvas.drawRoundRect(cardRect, 40f, 40f, borderPaint)

    // Navy Header Band
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F2847.toInt() }
    val headerRect = RectF(36f, 36f, width - 36f, 250f)
    canvas.drawRoundRect(headerRect, 40f, 40f, headerPaint)
    canvas.drawRect(36f, 180f, width - 36f, 250f, headerPaint)

    // Gold Accent Stripe
    val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD99B26.toInt() }
    canvas.drawRect(36f, 250f, width - 36f, 264f, goldPaint)

    // Header Texts
    val titlePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val subHeaderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFE5B4.toInt()
        textSize = 26f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    canvas.drawText(schoolName.uppercase(), width / 2f, 128f, titlePaint)
    canvas.drawText("KARTU QR ABSENSI SISWA", width / 2f, 182f, subHeaderPaint)

    // QR Frame
    val qrBitmap = generateQrBitmap(student.qrToken, sizePx = 560)
    val qrFramePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8FAFD.toInt()
        style = Paint.Style.FILL
      }
    val qrFrameStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE2E8F0.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
      }
    val qrBox = RectF(155f, 310f, width - 155f, 900f)
    canvas.drawRoundRect(qrBox, 28f, 28f, qrFramePaint)
    canvas.drawRoundRect(qrBox, 28f, 28f, qrFrameStroke)

    if (qrBitmap != null) {
      canvas.drawBitmap(qrBitmap, 170f, 325f, null)
    }

    // Student Info
    val namePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F2847.toInt()
        textSize = 42f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
    val metaPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF475569.toInt()
        textSize = 30f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
      }
    val tokenPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF64748B.toInt()
        textSize = 24f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
      }

    val safeName =
      if (student.fullName.length > 28) student.fullName.take(26) + "..." else student.fullName
    canvas.drawText(safeName, width / 2f, 976f, namePaint)
    canvas.drawText(
      "NIS: ${student.nis}   |   Kelas: ${student.className}",
      width / 2f,
      1034f,
      metaPaint,
    )
    canvas.drawText("Token: ${student.qrToken}", width / 2f, 1110f, tokenPaint)

    return bitmap
  }
}
