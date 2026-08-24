package com.setubandhtech.jetpack_compose_animations.newyearcountdown

import android.R.attr.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

data class ConfettiPiece(
    val id: Int,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun NewYearCountdown() {
    var countdown by remember { mutableStateOf(10) }
    var isStarted by remember { mutableStateOf(false) }
    var isCelebrating by remember { mutableStateOf(false) }
    var confetti by remember { mutableStateOf<List<ConfettiPiece>>(emptyList()) }
    var confettiProgress by remember { mutableStateOf(0f) }

    val confettiColors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFF4081),
        Color(0xFF00E5FF),
        Color(0xFF69F0AE),
        Color(0xFFFFAB40),
        Color(0xFFEA80FC),
        Color(0xFFFF5252),
        Color(0xFF40C4FF)
    )

    val numberScale by animateFloatAsState(
        targetValue = if (countdown <= 3 && isStarted) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "number_scale"
    )

    val numberColor by animateColorAsState(
        targetValue = when {
            isCelebrating -> Color(0xFFFFD700)
            countdown <= 3 && isStarted -> Color(0xFFE53935)
            countdown <= 5 && isStarted -> Color(0xFFFF9800)
            else -> Color.White
        },
        animationSpec = tween(300),
        label = "number_color"
    )

    val ringProgress by rememberInfiniteTransition(label = "ring")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_progress"
        )

    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )

    LaunchedEffect(isStarted) {
        if (isStarted) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            isCelebrating = true

            confetti = List(150) { i ->
                ConfettiPiece(
                    id = i,
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * 0.3f,
                    velocityX = Random.nextFloat() * 0.4f - 0.2f,
                    velocityY = Random.nextFloat() * 0.3f + 0.1f,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 16f + 8f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 10f - 5f,
                    isCircle = Random.nextBoolean()
                )
            }
        }
    }

    val confettiTick by rememberInfiniteTransition(label = "confetti_tick")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(16, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "confetti_tick"
        )

    LaunchedEffect(confettiTick) {
        if (isCelebrating) {
            confettiProgress = (confettiProgress + 0.003f) % 1f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510)),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isCelebrating) {
                confetti.forEach { piece ->
                    val x = (piece.x + piece.velocityX * confettiProgress * 5f) % 1f
                    val y = (piece.y + piece.velocityY * confettiProgress * 5f) % 1.2f
                    val currentX = x * size.width
                    val currentY = y * size.height
                    val rotation = piece.rotation + piece.rotationSpeed * confettiProgress * 100f
                    val alpha = (1f - (y - 0.8f).coerceAtLeast(0f) / 0.4f).coerceIn(0f, 1f)

                    if (piece.isCircle) {
                        drawCircle(
                            color = piece.color.copy(alpha = alpha),
                            radius = piece.size / 2,
                            center = Offset(currentX, currentY)
                        )
                    } else {
                        drawLine(
                            color = piece.color.copy(alpha = alpha),
                            start = Offset(
                                currentX + cos(Math.toRadians(rotation.toDouble()).toFloat()) * piece.size / 2,
                                currentY + sin(Math.toRadians(rotation.toDouble()).toFloat()) * piece.size / 2
                            ),
                            end = Offset(
                                currentX - cos(Math.toRadians(rotation.toDouble()).toFloat()) * piece.size / 2,
                                currentY - sin(Math.toRadians(rotation.toDouble()).toFloat()) * piece.size / 2
                            ),
                            strokeWidth = piece.size * 0.4f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                repeat(5) { i ->
                    val burstX = (i.toFloat() / 5f + confettiProgress * 0.5f) % 1f
                    val burstY = 0.2f + (i * 0.1f) % 0.4f
                    val burstCenter = Offset(burstX * size.width, burstY * size.height)
                    val burstAlpha = sin(confettiProgress * Math.PI.toFloat() * 4f + i) * 0.5f + 0.5f

                    repeat(12) { j ->
                        val angle = (j.toFloat() / 12f) * 2f * Math.PI.toFloat()
                        val dist = 40f + i * 10f
                        drawLine(
                            color = confettiColors[i % confettiColors.size].copy(
                                alpha = burstAlpha * 0.6f
                            ),
                            start = burstCenter,
                            end = Offset(
                                burstCenter.x + cos(angle) * dist,
                                burstCenter.y + sin(angle) * dist
                            ),
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            if (isStarted && !isCelebrating && countdown > 0) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width * 0.35f

                val ringAlpha = (1f - ringProgress) * 0.6f
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = ringAlpha),
                    radius = radius * (0.8f + ringProgress * 0.4f),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4f * (1f - ringProgress)
                    )
                )

                drawCircle(
                    color = when {
                        countdown <= 3 -> Color(0xFFE53935)
                        countdown <= 5 -> Color(0xFFFF9800)
                        else -> Color(0xFF00E5FF)
                    }.copy(alpha = 0.15f * glowPulse),
                    radius = radius * 0.9f,
                    center = center
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {

            when {
                isCelebrating -> {
                    Text(
                        text = "🎊",
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HAPPY NEW YEAR!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2027 🎆",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(56.dp)
                            .background(
                                Color(0xFF6200EE),
                                androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        clickable
                        Text(
                            text = "Again! 🎉",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                isStarted -> {
                    Text(
                        text = "🎆 New Year 🎆",
                        fontSize = 22.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Text(
                        text = "$countdown",
                        fontSize = (120 * numberScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = numberColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when {
                            countdown <= 3 -> "Almost there! 🔥"
                            countdown <= 5 -> "Getting close... ⏳"
                            else -> "Countdown started! ⏱️"
                        },
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }

                else -> {
                    Text(
                        text = "🎆",
                        fontSize = 80.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "New Year\nCountdown",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))

                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(64.dp)
                            .background(
                                Color(0xFFFFD700),
                                androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                            )
                            .then(
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures { isStarted = true }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start Countdown 🎉",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A0A0F)
                        )
                    }
                }
            }
        }
    }
}