package com.setubandhtech.jetpack_compose_animations.ganeshchaturthi

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class DivaParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val life: Float = 1f
)

@Composable
fun GaneshChaturthi() {
    var particles by remember { mutableStateOf<List<DivaParticle>>(emptyList()) }
    var idCounter by remember { mutableStateOf(0) }
    var isBlessMode by remember { mutableStateOf(false) }

    val divyaColors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFF9800),
        Color(0xFFFFEB3B),
        Color(0xFFFF6B35),
        Color(0xFFFFF176),
        Color(0xFFFFCC02)
    )

    val flameFlicker by rememberInfiniteTransition(label = "flame")
        .animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flame_flicker"
        )

    val omPulse by rememberInfiniteTransition(label = "om")
        .animateFloat(
            initialValue = 0.8f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "om_pulse"
        )

    val haloRotation by rememberInfiniteTransition(label = "halo")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "halo_rotation"
        )

    val blessGlow by animateFloatAsState(
        targetValue = if (isBlessMode) 1f else 0f,
        animationSpec = tween(500),
        label = "bless_glow"
    )

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
        particles = particles
            .map { it.copy(
                x = it.x + it.velocityX,
                y = it.y + it.velocityY,
                life = it.life - 0.015f
            )}
            .filter { it.life > 0f }

        if (isBlessMode && particles.size < 60) {
            val newParticles = List(3) {
                DivaParticle(
                    id = idCounter++,
                    x = Random.nextFloat(),
                    y = 0.6f,
                    velocityX = Random.nextFloat() * 0.008f - 0.004f,
                    velocityY = -(Random.nextFloat() * 0.008f + 0.003f),
                    color = divyaColors.random(),
                    size = Random.nextFloat() * 8f + 4f
                )
            }
            particles = particles + newParticles
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0A00),
                        Color(0xFF2D1500),
                        Color(0xFF1A0A00)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    isBlessMode = !isBlessMode
                    val burstParticles = List(20) {
                        DivaParticle(
                            id = idCounter++,
                            x = 0.5f,
                            y = 0.45f,
                            velocityX = Random.nextFloat() * 0.02f - 0.01f,
                            velocityY = -(Random.nextFloat() * 0.02f + 0.005f),
                            color = divyaColors.random(),
                            size = Random.nextFloat() * 10f + 5f
                        )
                    }
                    particles = particles + burstParticles
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.45f)

            if (blessGlow > 0f) {
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.1f * blessGlow),
                    radius = size.width * 0.6f,
                    center = center
                )
            }

            drawRotatingHalo(
                center = center,
                radius = size.width * 0.28f,
                rotation = haloRotation,
                alpha = 0.7f
            )

            drawGaneshFace(
                center = center,
                radius = size.width * 0.22f,
                flameFlicker = flameFlicker,
                omScale = omPulse
            )

            particles.forEach { particle ->
                val px = particle.x * size.width
                val py = particle.y * size.height
                val alpha = particle.life.coerceIn(0f, 1f)

                drawCircle(
                    color = particle.color.copy(alpha = alpha * 0.3f),
                    radius = particle.size * 2f,
                    center = Offset(px, py)
                )

                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size,
                    center = Offset(px, py)
                )

                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    radius = particle.size * 0.3f,
                    center = Offset(px - particle.size * 0.2f, py - particle.size * 0.2f)
                )
            }

            drawDiya(
                center = Offset(size.width / 2f, size.height * 0.78f),
                flicker = flameFlicker
            )

            drawDiya(
                center = Offset(size.width * 0.2f, size.height * 0.82f),
                flicker = flameFlicker * 0.9f,
                scale = 0.7f
            )
            drawDiya(
                center = Offset(size.width * 0.8f, size.height * 0.82f),
                flicker = flameFlicker * 0.85f,
                scale = 0.7f
            )

            repeat(8) { i ->
                val angle = (i.toFloat() / 8f) * 2f * Math.PI.toFloat() + haloRotation * 0.005f
                val petalX = center.x + cos(angle) * size.width * 0.35f
                val petalY = center.y + sin(angle) * size.width * 0.35f
                drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = 0.4f),
                    radius = 8f,
                    center = Offset(petalX, petalY)
                )
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                    radius = 4f,
                    center = Offset(petalX, petalY)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🙏 Ganpati Bappa Morya 🙏",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isBlessMode) "🌟 Bappa ki kripa 🌟" else "Tap for Blessings 🙏",
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pure Canvas • No Library",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.2f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

fun DrawScope.drawGaneshFace(
    center: Offset,
    radius: Float,
    flameFlicker: Float,
    omScale: Float
) {
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = 0.15f),
        radius = radius * 1.2f,
        center = center
    )
    drawCircle(
        color = Color(0xFFFFD700),
        radius = radius,
        center = center,
        style = Stroke(width = 4f)
    )

    drawCircle(
        color = Color(0xFFFF9800).copy(alpha = 0.3f),
        radius = radius * 0.85f,
        center = center
    )

    val trunkPath = Path().apply {
        moveTo(center.x, center.y + radius * 0.2f)
        cubicTo(
            center.x + radius * 0.4f, center.y + radius * 0.5f,
            center.x + radius * 0.6f, center.y + radius * 0.8f,
            center.x + radius * 0.3f, center.y + radius * 1.0f
        )
    }
    drawPath(
        path = trunkPath,
        color = Color(0xFFFFD700),
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )

    listOf(-0.25f, 0.25f).forEach { xOffset ->
        drawCircle(
            color = Color.White,
            radius = radius * 0.12f,
            center = Offset(center.x + radius * xOffset, center.y - radius * 0.1f)
        )
        drawCircle(
            color = Color(0xFF1A0A00),
            radius = radius * 0.07f,
            center = Offset(center.x + radius * xOffset, center.y - radius * 0.1f)
        )
    }

    listOf(-1f, 1f).forEach { side ->
        drawCircle(
            color = Color(0xFFFF9800).copy(alpha = 0.5f),
            radius = radius * 0.45f,
            center = Offset(center.x + side * radius * 0.9f, center.y)
        )
        drawCircle(
            color = Color(0xFFFFD700),
            radius = radius * 0.45f,
            center = Offset(center.x + side * radius * 0.9f, center.y),
            style = Stroke(width = 3f)
        )
    }

    val omCenter = Offset(center.x, center.y + radius * 1.6f)
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = 0.3f * omScale),
        radius = 30f * omScale,
        center = omCenter
    )
    drawCircle(
        color = Color(0xFFFFD700),
        radius = 30f * omScale,
        center = omCenter,
        style = Stroke(width = 2f)
    )

    repeat(5) { i ->
        val crownAngle = (-60f + i * 30f) * Math.PI.toFloat() / 180f - Math.PI.toFloat() / 2f
        val crownX = center.x + cos(crownAngle) * radius * 0.9f
        val crownY = center.y + sin(crownAngle) * radius * 0.9f
        drawCircle(
            color = Color(0xFFFFD700),
            radius = if (i == 2) 10f else 7f,
            center = Offset(crownX, crownY)
        )
        if (i == 2) {
            drawCircle(
                color = Color(0xFFE53935),
                radius = 5f,
                center = Offset(crownX, crownY)
            )
        }
    }

    drawCircle(
        color = Color(0xFFE53935),
        radius = radius * 0.08f,
        center = Offset(center.x, center.y - radius * 0.35f)
    )
    drawLine(
        color = Color(0xFFFFFFFF),
        start = Offset(center.x, center.y - radius * 0.5f),
        end = Offset(center.x, center.y - radius * 0.25f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
}

fun DrawScope.drawRotatingHalo(
    center: Offset,
    radius: Float,
    rotation: Float,
    alpha: Float
) {
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = alpha * 0.2f),
        radius = radius + 20f,
        center = center
    )

    repeat(24) { i ->
        val angle = (i.toFloat() / 24f) * 2f * Math.PI.toFloat() +
                rotation * Math.PI.toFloat() / 180f
        val dotX = center.x + cos(angle) * radius
        val dotY = center.y + sin(angle) * radius
        val dotAlpha = (sin(angle * 3f) + 1f) / 2f * alpha

        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = dotAlpha),
            radius = if (i % 3 == 0) 6f else 3f,
            center = Offset(dotX, dotY)
        )
    }
}

fun DrawScope.drawDiya(
    center: Offset,
    flicker: Float,
    scale: Float = 1f
) {
    val diyaWidth = 50f * scale
    val diyaHeight = 20f * scale

    val diyaPath = Path().apply {
        moveTo(center.x - diyaWidth / 2, center.y)
        cubicTo(
            center.x - diyaWidth / 3, center.y + diyaHeight,
            center.x + diyaWidth / 3, center.y + diyaHeight,
            center.x + diyaWidth / 2, center.y
        )
        cubicTo(
            center.x + diyaWidth / 3, center.y - diyaHeight * 0.3f,
            center.x - diyaWidth / 3, center.y - diyaHeight * 0.3f,
            center.x - diyaWidth / 2, center.y
        )
        close()
    }

    drawPath(diyaPath, Color(0xFFCD7F32).copy(alpha = 0.9f))
    drawPath(diyaPath, Color(0xFFFFD700), style = Stroke(width = 2f))

    drawOval(
        color = Color(0xFFFFD700).copy(alpha = 0.5f),
        topLeft = Offset(center.x - diyaWidth * 0.3f, center.y - diyaHeight * 0.1f),
        size = Size(diyaWidth * 0.6f, diyaHeight * 0.4f)
    )

    drawLine(
        color = Color(0xFF5D4037),
        start = Offset(center.x, center.y - diyaHeight * 0.1f),
        end = Offset(center.x, center.y - diyaHeight * 0.5f),
        strokeWidth = 2f * scale,
        cap = StrokeCap.Round
    )

    val flameBase = Offset(center.x, center.y - diyaHeight * 0.5f)
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = 0.3f * flicker),
        radius = 20f * scale * flicker,
        center = flameBase
    )

    drawOval(
        color = Color(0xFFFF9800).copy(alpha = 0.8f * flicker),
        topLeft = Offset(
            flameBase.x - 8f * scale,
            flameBase.y - 20f * scale * flicker
        ),
        size = Size(16f * scale, 22f * scale * flicker)
    )

    drawOval(
        color = Color(0xFFFFEB3B).copy(alpha = 0.9f * flicker),
        topLeft = Offset(
            flameBase.x - 5f * scale,
            flameBase.y - 14f * scale * flicker
        ),
        size = Size(10f * scale, 16f * scale * flicker)
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.8f * flicker),
        radius = 3f * scale,
        center = Offset(flameBase.x, flameBase.y - 12f * scale * flicker)
    )
}