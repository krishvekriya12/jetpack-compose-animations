package com.setubandhtech.jetpack_compose_animations.arcreactor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun IronManArcReactor() {
    var isPowered by remember { mutableStateOf(true) }

    val outerRotation by rememberInfiniteTransition(label = "outer")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "outer_rotation"
        )

    val innerRotation by rememberInfiniteTransition(label = "inner")
        .animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "inner_rotation"
        )

    val corePulse by rememberInfiniteTransition(label = "core")
        .animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "core_pulse"
        )

    val energyWave by rememberInfiniteTransition(label = "energy")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "energy_wave"
        )

    val powerLevel by animateFloatAsState(
        targetValue = if (isPowered) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "power"
    )

    val reactorColor = Color(0xFF00E5FF)
    val glowColor = Color(0xFF0288D1)
    val coreColor = Color(0xFFE1F5FE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .pointerInput(Unit) {
                detectTapGestures {
                    isPowered = !isPowered
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width * 0.38f

            if (powerLevel > 0f) {

                repeat(3) { i ->
                    val waveProgress = (energyWave + i * 0.33f) % 1f
                    val waveRadius = maxRadius * 0.5f + maxRadius * waveProgress
                    val waveAlpha = (1f - waveProgress) * 0.3f * powerLevel

                    drawCircle(
                        color = reactorColor.copy(alpha = waveAlpha),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 3f)
                    )
                }

                drawCircle(
                    color = glowColor.copy(alpha = 0.15f * powerLevel * corePulse),
                    radius = maxRadius * 1.2f,
                    center = center
                )

                rotate(outerRotation, pivot = center) {
                    drawRotatingRing(
                        center = center,
                        radius = maxRadius * 0.9f,
                        segments = 8,
                        color = reactorColor.copy(alpha = 0.8f * powerLevel),
                        strokeWidth = 4f
                    )
                }

                drawCircle(
                    color = reactorColor.copy(alpha = 0.6f * powerLevel),
                    radius = maxRadius * 0.9f,
                    center = center,
                    style = Stroke(width = 3f)
                )

                rotate(innerRotation, pivot = center) {
                    drawRotatingRing(
                        center = center,
                        radius = maxRadius * 0.65f,
                        segments = 6,
                        color = reactorColor.copy(alpha = 0.9f * powerLevel),
                        strokeWidth = 5f
                    )
                }

                drawCircle(
                    color = reactorColor.copy(alpha = 0.5f * powerLevel),
                    radius = maxRadius * 0.65f,
                    center = center,
                    style = Stroke(width = 2f)
                )

                rotate(outerRotation * 0.5f, pivot = center) {
                    drawHexagonRing(
                        center = center,
                        radius = maxRadius * 0.45f,
                        color = reactorColor.copy(alpha = 0.7f * powerLevel)
                    )
                }

                rotate(innerRotation * 0.3f, pivot = center) {
                    repeat(6) { i ->
                        val angle = (i.toFloat() / 6f) * 2f * Math.PI.toFloat()
                        val spokeStart = Offset(
                            x = center.x + cos(angle) * maxRadius * 0.3f,
                            y = center.y + sin(angle) * maxRadius * 0.3f
                        )
                        val spokeEnd = Offset(
                            x = center.x + cos(angle) * maxRadius * 0.6f,
                            y = center.y + sin(angle) * maxRadius * 0.6f
                        )
                        drawLine(
                            color = reactorColor.copy(alpha = 0.6f * powerLevel),
                            start = spokeStart,
                            end = spokeEnd,
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                drawCircle(
                    color = reactorColor.copy(alpha = 0.2f * corePulse * powerLevel),
                    radius = maxRadius * 0.32f,
                    center = center
                )
                drawCircle(
                    color = reactorColor.copy(alpha = 0.4f * corePulse * powerLevel),
                    radius = maxRadius * 0.22f,
                    center = center
                )
                drawCircle(
                    color = reactorColor.copy(alpha = 0.7f * corePulse * powerLevel),
                    radius = maxRadius * 0.15f,
                    center = center
                )

                drawCircle(
                    color = coreColor.copy(alpha = corePulse * powerLevel),
                    radius = maxRadius * 0.1f,
                    center = center
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * corePulse * powerLevel),
                    radius = maxRadius * 0.05f,
                    center = center
                )

                repeat(12) { i ->
                    val angle = (i.toFloat() / 12f) * 2f * Math.PI.toFloat() + outerRotation * 0.01f
                    val rayEnd = Offset(
                        x = center.x + cos(angle) * maxRadius * 0.85f,
                        y = center.y + sin(angle) * maxRadius * 0.85f
                    )
                    drawLine(
                        color = reactorColor.copy(alpha = 0.05f * powerLevel),
                        start = center,
                        end = rayEnd,
                        strokeWidth = 1f
                    )
                }
            } else {
                drawCircle(
                    color = Color(0xFF1A1A2E),
                    radius = maxRadius * 0.9f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF263238).copy(alpha = 0.5f),
                    radius = maxRadius * 0.9f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = Color(0xFF263238).copy(alpha = 0.3f),
                    radius = maxRadius * 0.1f,
                    center = center
                )
            }
        }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡ Arc Reactor",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF),
                modifier = Modifier.padding(top = 32.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    text = if (isPowered) "POWER: ONLINE ⚡" else "POWER: OFFLINE 💀",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPowered) Color(0xFF00E5FF) else Color(0xFF546E7A),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to toggle power",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun DrawScope.drawRotatingRing(
    center: Offset,
    radius: Float,
    segments: Int,
    color: Color,
    strokeWidth: Float
) {
    repeat(segments) { i ->
        val startAngle = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
        val endAngle = ((i + 0.6f) / segments) * 2f * Math.PI.toFloat()

        val startX = center.x + cos(startAngle) * radius
        val startY = center.y + sin(startAngle) * radius
        val endX = center.x + cos(endAngle) * radius
        val endY = center.y + sin(endAngle) * radius

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

fun DrawScope.drawHexagonRing(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    repeat(6) { i ->
        val angle = (i.toFloat() / 6f) * 2f * Math.PI.toFloat() - Math.PI.toFloat() / 6f
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3f)
    )
}