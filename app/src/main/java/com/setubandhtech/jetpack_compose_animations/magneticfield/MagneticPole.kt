package com.setubandhtech.jetpack_compose_animations.magneticfield

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

data class MagneticPole(
    val id: Int,
    var x: Float,
    var y: Float,
    val charge: Float,
    var isDragging: Boolean = false
)

data class FieldLine(
    val points: List<Offset>,
    val strength: Float
)

@Composable
fun MagneticField() {
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    var poles by remember { mutableStateOf<List<MagneticPole>>(emptyList()) }

    LaunchedEffect(canvasW, canvasH) {
        if (canvasW > 0f && canvasH > 0f && poles.isEmpty()) {
            poles = listOf(
                MagneticPole(0, canvasW * 0.35f, canvasH * 0.5f, +1f),
                MagneticPole(1, canvasW * 0.65f, canvasH * 0.5f, -1f)
            )
        }
    }
    var draggingId by remember { mutableStateOf(-1) }
    var fieldLines by remember { mutableStateOf<List<FieldLine>>(emptyList()) }

    LaunchedEffect(poles) {
        if (canvasW == 0f) return@LaunchedEffect
        fieldLines = computeFieldLines(poles, canvasW, canvasH)
    }

    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

    val shimmer by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0805))
    ) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            canvasW = size.width.toFloat()
                            canvasH = size.height.toFloat()
                            val closest = poles.minByOrNull { p ->
                                val dx = p.x - offset.x
                                val dy = p.y - offset.y
                                dx * dx + dy * dy
                            }
                            closest?.let { pole ->
                                val dx = pole.x - offset.x
                                val dy = pole.y - offset.y
                                if (dx * dx + dy * dy < 80f * 80f) {
                                    draggingId = pole.id
                                }
                            }
                        },
                        onDragEnd = { draggingId = -1 },
                        onDrag = { _, dragAmount ->
                            if (draggingId >= 0) {
                                poles = poles.map { pole ->
                                    if (pole.id == draggingId) {
                                        pole.copy(
                                            x = (pole.x + dragAmount.x)
                                                .coerceIn(60f, canvasW - 60f),
                                            y = (pole.y + dragAmount.y)
                                                .coerceIn(60f, canvasH - 60f)
                                        )
                                    } else pole
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        canvasW = size.width.toFloat()
                        canvasH = size.height.toFloat()
                    }
                }
        ) {
            canvasW = size.width
            canvasH = size.height

            if (fieldLines.isEmpty()) {
                fieldLines = computeFieldLines(poles, canvasW, canvasH)
            }

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1208),
                        Color(0xFF0A0805)
                    ),
                    center = Offset(canvasW / 2f, canvasH / 2f),
                    radius = canvasW * 0.8f
                )
            )

            drawIronFilings(poles, canvasW, canvasH, shimmer)

            fieldLines.forEach { line ->
                drawFieldLine(line, shimmer)
            }

            poles.forEach { pole ->
                drawMagneticPole(pole, pulse)
            }

            drawForceConnection(poles, pulse)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🧲 Magnetic Field",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700).copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "N",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935).copy(alpha = 0.8f)
                )
                Text(
                    text = "Drag poles to move 🧲",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
                Text(
                    text = "S",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0).copy(alpha = 0.8f)
                )
            }

            Text(
                text = "Real Physics • Iron Filings • Field Lines",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.15f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

fun computeFieldLines(
    poles: List<MagneticPole>,
    width: Float,
    height: Float
): List<FieldLine> {
    val lines = mutableListOf<FieldLine>()
    val numLines = 24
    val stepSize = 8f
    val maxSteps = 300

    val northPoles = poles.filter { it.charge > 0 }

    northPoles.forEach { north ->
        repeat(numLines) { i ->
            val startAngle = (i.toFloat() / numLines) * 2f * PI.toFloat()
            val startX = north.x + cos(startAngle) * 45f
            val startY = north.y + sin(startAngle) * 45f

            val points = mutableListOf<Offset>()
            var x = startX
            var y = startY
            var strength = 0f

            repeat(maxSteps) { step ->
                if (x < 0f || x > width || y < 0f || y > height) return@repeat

                points.add(Offset(x, y))

                var bx = 0f
                var by = 0f
                poles.forEach { pole ->
                    val dx = x - pole.x
                    val dy = y - pole.y
                    val r2 = dx * dx + dy * dy
                    val r = sqrt(r2)
                    if (r > 5f) {
                        val magnitude = pole.charge / (r2 * r) * 50000f
                        bx += magnitude * dx
                        by += magnitude * dy
                    }
                }

                val bmag = sqrt(bx * bx + by * by)
                if (bmag < 0.001f) return@repeat

                strength += bmag
                x += (bx / bmag) * stepSize
                y += (by / bmag) * stepSize
            }

            if (points.size > 5) {
                lines.add(FieldLine(points, strength / points.size))
            }
        }
    }

    return lines
}

fun DrawScope.drawFieldLine(line: FieldLine, shimmer: Float) {
    if (line.points.size < 2) return

    val totalPoints = line.points.size

    for (i in 0 until totalPoints - 1) {
        val p1 = line.points[i]
        val p2 = line.points[i + 1]

        val progress = i.toFloat() / totalPoints

        val shimmerOffset = (shimmer + progress) % 1f
        val shimmerAlpha = sin(shimmerOffset * PI.toFloat()) * 0.4f + 0.3f

        val alpha = (1f - progress * 0.7f) * shimmerAlpha

        val color = when {
            progress < 0.3f -> Color(0xFFE53935)
            progress < 0.7f -> Color(0xFFFFD700)
            else -> Color(0xFF1565C0)
        }

        drawLine(
            color = color.copy(alpha = alpha * 0.3f),
            start = p1,
            end = p2,
            strokeWidth = 4f
        )

        drawLine(
            color = color.copy(alpha = alpha),
            start = p1,
            end = p2,
            strokeWidth = 1.2f
        )
    }

    val arrowInterval = totalPoints / 4
    for (k in 1..3) {
        val idx = k * arrowInterval
        if (idx >= totalPoints - 1) continue

        val p1 = line.points[idx]
        val p2 = line.points[idx + 1]
        val progress = idx.toFloat() / totalPoints

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val len = sqrt(dx * dx + dy * dy)
        if (len < 0.01f) continue

        val nx = dx / len
        val ny = dy / len
        val perpX = -ny
        val perpY = nx

        val arrowSize = 6f
        val tip = Offset(p1.x + nx * arrowSize * 2f, p1.y + ny * arrowSize * 2f)
        val left = Offset(p1.x - perpX * arrowSize * 0.7f, p1.y - perpY * arrowSize * 0.7f)
        val right = Offset(p1.x + perpX * arrowSize * 0.7f, p1.y + perpY * arrowSize * 0.7f)

        val arrowPath = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }

        val alpha = (1f - progress * 0.5f) * 0.7f
        drawPath(
            path = arrowPath,
            color = Color(0xFFFFD700).copy(alpha = alpha)
        )
    }
}

fun DrawScope.drawIronFilings(
    poles: List<MagneticPole>,
    width: Float,
    height: Float,
    shimmer: Float
) {
    val spacing = 22f
    val cols = (width / spacing).toInt() + 1
    val rows = (height / spacing).toInt() + 1

    for (col in 0..cols) {
        for (row in 0..rows) {
            val x = col * spacing + (row % 2) * spacing * 0.5f
            val y = row * spacing

            var bx = 0f
            var by = 0f
            var totalStrength = 0f

            poles.forEach { pole ->
                val dx = x - pole.x
                val dy = y - pole.y
                val r2 = dx * dx + dy * dy
                val r = sqrt(r2)
                if (r > 20f) {
                    val magnitude = pole.charge / (r2 * 0.01f)
                    bx += magnitude * dx / r
                    by += magnitude * dy / r
                    totalStrength += abs(magnitude)
                }
            }

            val bmag = sqrt(bx * bx + by * by)
            if (bmag < 0.001f) continue

            val angle = atan2(by, bx)
            val strength = (totalStrength / 5000f).coerceIn(0f, 1f)

            val filingLen = 6f + strength * 12f
            val filingAlpha = 0.15f + strength * 0.35f

            val shimmerVal = sin(shimmer * 2f * PI.toFloat() +
                    col * 0.3f + row * 0.4f) * 0.1f + filingAlpha

            val cosA = cos(angle)
            val sinA = sin(angle)

            val startX = x - cosA * filingLen / 2f
            val startY = y - sinA * filingLen / 2f
            val endX = x + cosA * filingLen / 2f
            val endY = y + sinA * filingLen / 2f

            val filingColor = Color(
                red = 0.7f + strength * 0.3f,
                green = 0.65f + strength * 0.2f,
                blue = 0.5f + strength * 0.1f,
                alpha = shimmerVal.coerceIn(0f, 0.6f)
            )

            drawLine(
                color = filingColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.5f + strength * 1f
            )

            drawCircle(
                color = Color.White.copy(alpha = strength * 0.15f),
                radius = 1f,
                center = Offset(x, y)
            )
        }
    }
}

fun DrawScope.drawMagneticPole(pole: MagneticPole, pulse: Float) {
    val isNorth = pole.charge > 0
    val poleColor = if (isNorth) Color(0xFFE53935) else Color(0xFF1565C0)
    val label = if (isNorth) "N" else "S"

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                poleColor.copy(alpha = 0.15f * pulse),
                Color.Transparent
            ),
            center = Offset(pole.x, pole.y),
            radius = 80f * pulse
        ),
        radius = 80f * pulse,
        center = Offset(pole.x, pole.y)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                poleColor.copy(alpha = 0.3f),
                poleColor.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = Offset(pole.x, pole.y),
            radius = 50f
        ),
        radius = 50f,
        center = Offset(pole.x, pole.y)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                poleColor.copy(alpha = 0.9f),
                poleColor.copy(alpha = 0.7f),
                poleColor.copy(alpha = 0.5f)
            ),
            center = Offset(pole.x - 8f, pole.y - 8f),
            radius = 36f
        ),
        radius = 36f,
        center = Offset(pole.x, pole.y)
    )

    drawCircle(
        color = poleColor.copy(alpha = 0.8f),
        radius = 36f,
        center = Offset(pole.x, pole.y),
        style = Stroke(width = 2f)
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.6f),
                Color.Transparent
            ),
            center = Offset(pole.x - 10f, pole.y - 12f),
            radius = 14f
        ),
        radius = 14f,
        center = Offset(pole.x - 10f, pole.y - 12f)
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            alpha = 220
        }
        drawText(label, pole.x, pole.y + 13f, paint)
    }
}

fun DrawScope.drawForceConnection(poles: List<MagneticPole>, pulse: Float) {
    if (poles.size < 2) return

    val p1 = poles[0]
    val p2 = poles[1]

    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val dist = sqrt(dx * dx + dy * dy)

    val isAttracting = p1.charge != p2.charge

    val lineColor = if (isAttracting) Color(0xFFFFD700) else Color(0xFF9C27B0)

    val dashCount = (dist / 30f).toInt()
    repeat(dashCount) { i ->
        val t = ((i.toFloat() / dashCount) + pulse * 0.3f) % 1f
        val dashX = p1.x + dx * t
        val dashY = p1.y + dy * t
        val alpha = sin(t * PI.toFloat()) * 0.8f

        drawCircle(
            color = lineColor.copy(alpha = alpha.toFloat()),
            radius = 3f,
            center = Offset(dashX, dashY)
        )
    }

    val midX = (p1.x + p2.x) / 2f
    val midY = (p1.y + p2.y) / 2f - 20f

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = if (isAttracting)
                android.graphics.Color.argb(180, 255, 215, 0)
            else
                android.graphics.Color.argb(180, 156, 39, 176)
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawText(
            if (isAttracting) "← Attract →" else "→ Repel ←",
            midX, midY, paint
        )
    }
}