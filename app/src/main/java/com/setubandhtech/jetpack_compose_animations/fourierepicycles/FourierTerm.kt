package com.setubandhtech.jetpack_compose_animations.fourierepicycles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

data class FourierTerm(
    val frequency: Float,
    val amplitude: Float,
    val phase: Float
)

@Composable
fun FourierEpicycles() {
    var fourierTerms by remember { mutableStateOf<List<FourierTerm>>(emptyList()) }
    var time by remember { mutableStateOf(0f) }
    var tracePath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedShape by remember { mutableStateOf(0) }
    var screenW by remember { mutableStateOf(1080f) }
    var screenH by remember { mutableStateOf(1920f) }
    var ready by remember { mutableStateOf(false) }

    val shapes = listOf("Heart", "Star", "Wave", "Circle", "Infinity")

    fun heartShape(t: Float, cx: Float, cy: Float, scale: Float): Offset {
        val x = 16f * sin(t).pow(3)
        val y = -(13f * cos(t) - 5f * cos(2 * t) - 2f * cos(3 * t) - cos(4 * t))
        return Offset(cx + x * scale, cy + y * scale)
    }

    fun starShape(t: Float, cx: Float, cy: Float, scale: Float): Offset {
        val r = scale * (0.5f + 0.5f * cos(5 * t))
        return Offset(cx + r * cos(t), cy + r * sin(t))
    }

    fun waveShape(t: Float, cx: Float, cy: Float, scale: Float): Offset {
        val x = t / (2f * PI.toFloat()) * scale * 2f - scale
        val y = sin(t * 3f) * scale * 0.4f + cos(t * 2f) * scale * 0.2f
        return Offset(cx + x, cy + y)
    }

    fun circleShape(t: Float, cx: Float, cy: Float, scale: Float): Offset =
        Offset(cx + cos(t) * scale, cy + sin(t) * scale)

    fun infinityShape(t: Float, cx: Float, cy: Float, scale: Float): Offset {
        val denom = 1f + sin(t) * sin(t)
        return Offset(
            cx + scale * cos(t) / denom,
            cy + scale * sin(t) * cos(t) / denom
        )
    }

    fun generatePoints(shapeIdx: Int): List<Offset> {
        val cx = screenW / 2f
        val cy = screenH * 0.42f
        val scale = minOf(screenW, screenH) * 0.20f
        val n = 256
        return (0 until n).map { i ->
            val t = i.toFloat() / n * 2f * PI.toFloat()
            when (shapeIdx) {
                0 -> heartShape(t, cx, cy, scale)
                1 -> starShape(t, cx, cy, scale)
                2 -> waveShape(t, cx, cy, scale)
                3 -> circleShape(t, cx, cy, scale)
                4 -> infinityShape(t, cx, cy, scale)
                else -> heartShape(t, cx, cy, scale)
            }
        }
    }

    fun computeDFT(points: List<Offset>): List<FourierTerm> {
        val n = points.size
        return (0 until n).map { k ->
            var re = 0f
            var im = 0f
            for (j in 0 until n) {
                val angle = 2f * PI.toFloat() * k * j / n
                re += points[j].x * cos(angle) + points[j].y * sin(angle)
                im += -points[j].x * sin(angle) + points[j].y * cos(angle)
            }
            FourierTerm(
                frequency = k.toFloat(),
                amplitude = sqrt(re * re + im * im) / n,
                phase = atan2(im, re)
            )
        }.sortedByDescending { it.amplitude }
    }

    fun recompute() {
        val pts = generatePoints(selectedShape)
        fourierTerms = computeDFT(pts).take(80)
        time = 0f
        tracePath = emptyList()
    }

    LaunchedEffect(ready, selectedShape) {
        if (!ready) return@LaunchedEffect
        recompute()
    }

    LaunchedEffect(fourierTerms) {
        if (fourierTerms.isEmpty()) return@LaunchedEffect

        while (true) {
            time += 0.03f
            if (time > 2f * PI.toFloat()) {
                time = 0f
                tracePath = emptyList()
            }

            // Compute tip
            var tx = 0f
            var ty = 0f
            fourierTerms.forEach { term ->
                val a = term.frequency * time + term.phase
                tx += term.amplitude * cos(a)
                ty += term.amplitude * sin(a)
            }
            tracePath = (tracePath + Offset(tx, ty)).takeLast(600)
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
            .onSizeChanged { size ->
                screenW = size.width.toFloat()
                screenH = size.height.toFloat()
                ready = true
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (fourierTerms.isEmpty()) return@Canvas

            // Grid
            val gc = Color(0xFF00E5FF).copy(alpha = 0.04f)
            var gx = 0f
            while (gx < size.width) {
                drawLine(gc, Offset(gx, 0f), Offset(gx, size.height), 1f)
                gx += 50f
            }
            var gy = 0f
            while (gy < size.height) {
                drawLine(gc, Offset(0f, gy), Offset(size.width, gy), 1f)
                gy += 50f
            }

            // Epicycles
            var x = size.width / 2f
            var y = size.height * 0.42f
            val maxAmp = fourierTerms.firstOrNull()?.amplitude ?: 1f

            fourierTerms.take(40).forEach { term ->
                val px = x
                val py = y
                val a = term.frequency * time + term.phase
                x += term.amplitude * cos(a)
                y += term.amplitude * sin(a)

                val alpha = (term.amplitude / maxAmp * 0.8f).coerceIn(0.04f, 0.6f)

                // Circle ring
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = alpha * 0.35f),
                    radius = term.amplitude,
                    center = Offset(px, py),
                    style = Stroke(width = 1f)
                )

                // Arm
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = alpha),
                    start = Offset(px, py),
                    end = Offset(x, y),
                    strokeWidth = 1.5f
                )

                // Joint
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = alpha * 0.8f),
                    radius = 2.5f,
                    center = Offset(x, y)
                )
            }

            // Tip glow
            drawCircle(
                color = Color(0xFFFF4081).copy(alpha = 0.4f),
                radius = 16f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color(0xFFFF4081),
                radius = 5f,
                center = Offset(x, y)
            )

            // Trace path
            if (tracePath.size > 2) {
                val p = Path().apply {
                    moveTo(tracePath[0].x, tracePath[0].y)
                    tracePath.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = p,
                    color = Color(0xFFFF4081).copy(alpha = 0.2f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = p,
                    color = Color(0xFFFF4081),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Progress ring
            drawArc(
                color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                startAngle = -90f,
                sweepAngle = (time / (2f * PI.toFloat())) * 360f,
                useCenter = false,
                topLeft = Offset(size.width - 52f, size.height - 52f),
                size = androidx.compose.ui.geometry.Size(40f, 40f),
                style = Stroke(width = 3f)
            )
        }

        // UI overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🎵 Fourier Epicycles",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "40 rotating circles → any shape",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SELECT SHAPE",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    shapes.forEachIndexed { index, shape ->
                        val isSelected = selectedShape == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                    else Color(0xFF0D0D1A),
                                    RoundedCornerShape(8.dp)
                                )
                                .pointerInput(index) {
                                    detectTapGestures {
                                        selectedShape = index
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shape,
                                fontSize = 10.sp,
                                color = if (isSelected) Color(0xFF00E5FF)
                                else Color.White.copy(alpha = 0.4f),
                                fontWeight = if (isSelected) FontWeight.Bold
                                else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}