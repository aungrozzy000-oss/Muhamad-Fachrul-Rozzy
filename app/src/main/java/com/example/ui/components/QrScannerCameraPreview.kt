package com.example.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.TarunaGold
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerCameraPreview(
  isTorchEnabled: Boolean,
  onQrDetected: (String) -> Unit,
  onCameraError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val latestOnQrDetected by rememberUpdatedState(onQrDetected)
  val latestOnCameraError by rememberUpdatedState(onCameraError)

  val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
  val scanner =
    remember {
      val options =
        BarcodeScannerOptions.Builder()
          .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
          .build()
      BarcodeScanning.getClient(options)
    }

  val previewView =
    remember(context) {
      PreviewView(context).apply {
        layoutParams =
          ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
        scaleType = PreviewView.ScaleType.FILL_CENTER
      }
    }

  DisposableEffect(lifecycleOwner, isTorchEnabled) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    var boundProvider: ProcessCameraProvider? = null

    cameraProviderFuture.addListener(
      {
        try {
          val cameraProvider = cameraProviderFuture.get()
          boundProvider = cameraProvider

          val preview =
            Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

          val imageAnalysis =
            ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
              .also { analysis ->
                analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                  val mediaImage = imageProxy.image
                  if (mediaImage != null) {
                    val inputImage =
                      InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner
                      .process(inputImage)
                      .addOnSuccessListener { barcodes ->
                        val firstRaw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                        if (firstRaw != null) {
                          latestOnQrDetected(firstRaw)
                        }
                      }
                      .addOnFailureListener { e ->
                        Log.w("QrScanner", "Barcode process failed: ${e.message}")
                      }
                      .addOnCompleteListener { imageProxy.close() }
                  } else {
                    imageProxy.close()
                  }
                }
              }

          cameraProvider.unbindAll()
          val camera =
            cameraProvider.bindToLifecycle(
              lifecycleOwner,
              CameraSelector.DEFAULT_BACK_CAMERA,
              preview,
              imageAnalysis,
            )
          if (camera.cameraInfo.hasFlashUnit()) {
            camera.cameraControl.enableTorch(isTorchEnabled)
          }
        } catch (e: Exception) {
          Log.e("QrScanner", "Camera binding failed", e)
          latestOnCameraError(
            "Kamera tidak tersedia pada perangkat ini. Gunakan Input NIS / Token Manual di bawah."
          )
        }
      },
      ContextCompat.getMainExecutor(context),
    )

    onDispose {
      try {
        boundProvider?.unbindAll()
      } catch (_: Exception) {}
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      scanner.close()
      analyzerExecutor.shutdown()
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
  val scanLineProgress by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1900, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "scan_line_y",
    )

  Box(modifier = modifier) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize(),
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
      val boxSize = minOf(size.width, size.height) * 0.64f
      val left = (size.width - boxSize) / 2f
      val top = (size.height - boxSize) / 2f
      val right = left + boxSize
      val bottom = top + boxSize
      val cornerLen = boxSize * 0.14f
      val stroke = 5.dp.toPx()

      // Subtle darkened vignette around viewfinder
      val scrim = Color.Black.copy(alpha = 0.42f)
      drawRect(color = scrim, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, top))
      drawRect(
        color = scrim,
        topLeft = Offset(0f, bottom),
        size = androidx.compose.ui.geometry.Size(size.width, size.height - bottom),
      )
      drawRect(
        color = scrim,
        topLeft = Offset(0f, top),
        size = androidx.compose.ui.geometry.Size(left, boxSize),
      )
      drawRect(
        color = scrim,
        topLeft = Offset(right, top),
        size = androidx.compose.ui.geometry.Size(size.width - right, boxSize),
      )

      // Gold corner brackets
      val bracketColor = TarunaGold
      // Top-left
      drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), stroke, StrokeCap.Round)
      drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), stroke, StrokeCap.Round)
      // Top-right
      drawLine(bracketColor, Offset(right - cornerLen, top), Offset(right, top), stroke, StrokeCap.Round)
      drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), stroke, StrokeCap.Round)
      // Bottom-left
      drawLine(bracketColor, Offset(left, bottom - cornerLen), Offset(left, bottom), stroke, StrokeCap.Round)
      drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke, StrokeCap.Round)
      // Bottom-right
      drawLine(bracketColor, Offset(right, bottom - cornerLen), Offset(right, bottom), stroke, StrokeCap.Round)
      drawLine(bracketColor, Offset(right - cornerLen, bottom), Offset(right, bottom), stroke, StrokeCap.Round)

      // Animated scanning line
      val lineY = top + (boxSize * scanLineProgress)
      drawLine(
        color = TarunaGold.copy(alpha = 0.85f),
        start = Offset(left + 12.dp.toPx(), lineY),
        end = Offset(right - 12.dp.toPx(), lineY),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}
