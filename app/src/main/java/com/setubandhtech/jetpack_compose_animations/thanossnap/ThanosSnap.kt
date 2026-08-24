package com.setubandhtech.jetpack_compose_animations.thanossnap

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

data class DustParticle(
    val id: Int,
    val originX: Float,
    val originY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color,
    val delay: Float
)

@Composable
fun ThanosSnap() {
    var isSnapped by remember { mutableStateOf(false) }
    var isRestored by remember { mutableStateOf(true) }
    var particles by remember { mutableStateOf<List<DustParticle>>(emptyList()) }

    val snapProgress by animateFloatAsState(
        targetValue = if (isSnapped) 1f else 0f,
        animationSpec = tween(2500, easing = LinearEasing),
        finishedListener = {
            if (it == 1f) isRestored = false
        },
        label = "snap_progress"
    )

    val gauntletGlow by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )

    val stoneColors = listOf(
        Color(0xFF9C27B0),
        Color(0xFF2196F3),
        Color(0xFFE53935),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFFFD700)
    )

    fun generateParticles(canvasWidth: Float, canvasHeight: Float): List<DustParticle> {
        val particleList = mutableListOf<DustParticle>()
        val cols = 40
        val rows = 60
        val cellW = canvasWidth / cols
        val cellH = canvasHeight / rows

        repeat(cols) { col ->
            repeat(rows) { row ->
                val x = col * cellW + cellW / 2
                val y = row * cellH + cellH / 2
                particleList.add(
                    DustParticle(
                        id = col * rows + row,
                        originX = x,
                        originY = y,
                        velocityX = Random.nextFloat() * 6f - 1f,
                        velocityY = Random.nextFloat() * -8f - 2f,
                        size = cellW * Random.nextFloat() * 0.8f + 0.5f,
                        color = stoneColors.random().copy(
                            alpha = Random.nextFloat() * 0.5f + 0.5f
                        ),
                        delay = Random.nextFloat() * 0.4f
                    )
                )
            }
        }
        return particleList
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isSnapped && particles.isNotEmpty()) {
                drawDisintegration(
                    particles = particles,
                    progress = snapProgress,
                    canvasSize = size
                )
            } else if (!isSnapped) {
                drawAvengersSymbol(
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.3f
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
                text = "Infinity Gauntlet 💜",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0),
                modifier = Modifier.padding(top = 32.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    stoneColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(if (isSnapped) 18.dp else 24.dp)
                                .background(
                                    color = color.copy(
                                        alpha = if (isSnapped) gauntletGlow else 1f
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(64.dp)
                        .background(
                            color = if (isSnapped) Color(0xFF1A1A2E)
                            else Color(0xFF9C27B0),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures {
                                if (!isSnapped) {
                                    isSnapped = true
                                    isRestored = false
                                    particles = generateParticles(
                                        size.width.toFloat(),
                                        size.height.toFloat()
                                    )
                                } else {
                                    isSnapped = false
                                    isRestored = true
                                    particles = emptyList()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            isSnapped && !isRestored -> "Restore Universe 💫"
                            isSnapped -> "Snapping... 💜"
                            else -> "🫰 SNAP!"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (!isSnapped) "\"Perfectly balanced, as all things should be\"" else "Half of all life... gone.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 48.dp)
                )
            }
        }
    }
}

fun DrawScope.drawDisintegration(
    particles: List<DustParticle>,
    progress: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    particles.forEach { particle ->
        val adjustedProgress = ((progress - particle.delay) / (1f - particle.delay))
            .coerceIn(0f, 1f)

        if (adjustedProgress <= 0f) {
            drawRect(
                color = particle.color,
                topLeft = Offset(
                    particle.originX - particle.size / 2,
                    particle.originY - particle.size / 2
                ),
                size = Size(particle.size, particle.size)
            )
        } else {
            val newX = particle.originX + particle.velocityX * adjustedProgress * 80f
            val newY = particle.originY + particle.velocityY * adjustedProgress * 80f
            val alpha = (1f - adjustedProgress).coerceIn(0f, 1f)
            val currentSize = particle.size * (1f - adjustedProgress * 0.5f)

            if (alpha > 0.01f) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(
                        newX - currentSize / 2,
                        newY - currentSize / 2
                    ),
                    size = Size(currentSize, currentSize)
                )
            }
        }
    }
}

fun DrawScope.drawAvengersSymbol(center: Offset, radius: Float) {
    drawCircle(
        color = Color(0xFF9C27B0).copy(alpha = 0.2f),
        radius = radius + 20f,
        center = center
    )

    drawCircle(
        color = Color(0xFF9C27B0).copy(alpha = 0.8f),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
    )

    val topPoint = Offset(center.x, center.y - radius * 0.7f)
    val bottomLeft = Offset(center.x - radius * 0.5f, center.y + radius * 0.5f)
    val bottomRight = Offset(center.x + radius * 0.5f, center.y + radius * 0.5f)
    val crossLeft = Offset(center.x - radius * 0.25f, center.y + radius * 0.1f)
    val crossRight = Offset(center.x + radius * 0.25f, center.y + radius * 0.1f)

    drawLine(
        color = Color(0xFF9C27B0),
        start = topPoint,
        end = bottomLeft,
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = Color(0xFF9C27B0),
        start = topPoint,
        end = bottomRight,
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = Color(0xFF9C27B0),
        start = crossLeft,
        end = crossRight,
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )

    listOf(topPoint, bottomLeft, bottomRight).forEach { point ->
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.8f),
            radius = 8f,
            center = point
        )
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.3f),
            radius = 16f,
            center = point
        )
    }
}