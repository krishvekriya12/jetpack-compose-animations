package com.setubandhtech.jetpack_compose_animations.diwalifireworks

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Firework(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val color: Color,
    val burstColors: List<Color>,
    val numParticles: Int = Random.nextInt(20, 40),
    val size: Float = Random.nextFloat() * 4f + 2f
)

data class FireworkState(
    val firework: Firework,
    val launchProgress: Float = 0f,
    val burstProgress: Float = 0f,
    val isLaunching: Boolean = true
)

@Composable
fun DiwaliFireworks() {
    var fireworks by remember { mutableStateOf<List<FireworkState>>(emptyList()) }
    var idCounter by remember { mutableStateOf(0) }

    val fireworkColors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFF6B35),
        Color(0xFFE53935),
        Color(0xFF00E5FF),
        Color(0xFFFF4081),
        Color(0xFF69F0AE),
        Color(0xFFFFAB40),
        Color(0xFFEA80FC),
        Color(0xFFFFFF00),
        Color(0xFF40C4FF)
    )

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(800)
            val color = fireworkColors.random()
            val burstColors = List(3) { fireworkColors.random() }
            val newFirework = FireworkState(
                firework = Firework(
                    id = idCounter++,
                    startX = Random.nextFloat(),
                    startY = 1f,
                    targetX = Random.nextFloat(),
                    targetY = Random.nextFloat() * 0.5f,
                    color = color,
                    burstColors = burstColors
                )
            )
            fireworks = (fireworks + newFirework).takeLast(8)
        }
    }

    val tick by rememberInfiniteTransition(label = "tick")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(16, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "tick"
        )

    LaunchedEffect(tick) {
        fireworks = fireworks.map { state ->
            when {
                state.isLaunching -> {
                    val newProgress = state.launchProgress + 0.025f
                    if (newProgress >= 1f) {
                        state.copy(
                            launchProgress = 1f,
                            isLaunching = false,
                            burstProgress = 0f
                        )
                    } else {
                        state.copy(launchProgress = newProgress)
                    }
                }
                state.burstProgress < 1f -> {
                    state.copy(burstProgress = state.burstProgress + 0.015f)
                }
                else -> state
            }
        }.filter { it.burstProgress < 1f || it.isLaunching }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val color = fireworkColors.random()
                    val burstColors = List(3) { fireworkColors.random() }
                    val newFirework = FireworkState(
                        firework = Firework(
                            id = idCounter++,
                            startX = offset.x / size.width,
                            startY = 1f,
                            targetX = offset.x / size.width,
                            targetY = offset.y / size.height,
                            color = color,
                            burstColors = burstColors
                        )
                    )
                    fireworks = (fireworks + newFirework).takeLast(8)
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            fireworks.forEach { state ->
                val fw = state.firework
                val startPos = Offset(fw.startX * size.width, fw.startY * size.height)
                val targetPos = Offset(fw.targetX * size.width, fw.targetY * size.height)

                if (state.isLaunching) {
                    val currentPos = Offset(
                        x = startPos.x + (targetPos.x - startPos.x) * state.launchProgress,
                        y = startPos.y + (targetPos.y - startPos.y) * state.launchProgress
                    )

                    repeat(8) { i ->
                        val trailProgress = (state.launchProgress - i * 0.03f).coerceIn(0f, 1f)
                        val trailPos = Offset(
                            x = startPos.x + (targetPos.x - startPos.x) * trailProgress,
                            y = startPos.y + (targetPos.y - startPos.y) * trailProgress
                        )
                        val trailAlpha = (1f - i.toFloat() / 8f) * 0.8f
                        drawCircle(
                            color = fw.color.copy(alpha = trailAlpha),
                            radius = fw.size - i * 0.3f,
                            center = trailPos
                        )
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = fw.size,
                        center = currentPos
                    )
                    drawCircle(
                        color = fw.color.copy(alpha = 0.5f),
                        radius = fw.size * 3f,
                        center = currentPos
                    )

                } else {
                    val burstProgress = state.burstProgress
                    val alpha = (1f - burstProgress).coerceIn(0f, 1f)

                    if (burstProgress < 0.3f) {
                        val flashAlpha = (1f - burstProgress / 0.3f) * 0.8f
                        drawCircle(
                            color = Color.White.copy(alpha = flashAlpha),
                            radius = 30f * burstProgress,
                            center = targetPos
                        )
                    }

                    repeat(fw.numParticles) { i ->
                        val angle = (i.toFloat() / fw.numParticles) * 2f * Math.PI.toFloat()
                        val particleColor = fw.burstColors[i % fw.burstColors.size]
                        val distance = 150f * burstProgress
                        val gravity = 50f * burstProgress * burstProgress

                        val particleX = targetPos.x + cos(angle) * distance
                        val particleY = targetPos.y + sin(angle) * distance + gravity

                        val tailDistance = distance * 0.85f
                        val tailX = targetPos.x + cos(angle) * tailDistance
                        val tailY = targetPos.y + sin(angle) * tailDistance + gravity * 0.85f

                        drawLine(
                            color = particleColor.copy(alpha = alpha * 0.6f),
                            start = Offset(tailX, tailY),
                            end = Offset(particleX, particleY),
                            strokeWidth = fw.size * 0.8f,
                            cap = StrokeCap.Round
                        )

                        drawCircle(
                            color = particleColor.copy(alpha = alpha),
                            radius = fw.size * (1f - burstProgress * 0.5f),
                            center = Offset(particleX, particleY)
                        )

                        if (i % 4 == 0) {
                            drawCircle(
                                color = Color.White.copy(alpha = alpha * 0.8f),
                                radius = fw.size * 0.4f,
                                center = Offset(particleX, particleY)
                            )
                        }
                    }

                    drawCircle(
                        color = fw.color.copy(alpha = alpha * 0.3f),
                        radius = 60f * burstProgress,
                        center = targetPos
                    )
                }
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
                text = "🎆 Happy Diwali 🪔",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = "Tap anywhere to launch 🎆",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}