package com.setubandhtech.jetpack_compose_animations.thorlightning

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

data class LightningBolt(
    val segments: List<Pair<Offset, Offset>>,
    val alpha: Float,
    val color: Color
)

data class Spark(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val life: Float
)

@Composable
fun ThorLightningEffect() {
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    var lightningBolts by remember { mutableStateOf<List<LightningBolt>>(emptyList()) }
    var sparks by remember { mutableStateOf<List<Spark>>(emptyList()) }
    var strikeProgress by remember { mutableStateOf(0f) }
    var isStriking by remember { mutableStateOf(false) }

    val strikeAnim by animateFloatAsState(
        targetValue = if (isStriking) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        finishedListener = {
            if (it == 1f) isStriking = false
        },
        label = "strike"
    )

    val flashAlpha by animateFloatAsState(
        targetValue = if (isStriking) 0.3f else 0f,
        animationSpec = tween(100),
        label = "flash"
    )

    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )

    fun generateLightningBolt(start: Offset, end: Offset): List<Pair<Offset, Offset>> {
        val segments = mutableListOf<Pair<Offset, Offset>>()
        var current = start
        val steps = 12
        val dx = (end.x - start.x) / steps
        val dy = (end.y - start.y) / steps

        repeat(steps) {
            val next = Offset(
                x = current.x + dx + Random.nextFloat() * 60f - 30f,
                y = current.y + dy + Random.nextFloat() * 20f - 10f
            )
            segments.add(Pair(current, next))
            current = next

            if (Random.nextFloat() > 0.7f) {
                val branchEnd = Offset(
                    x = current.x + Random.nextFloat() * 80f - 40f,
                    y = current.y + Random.nextFloat() * 60f + 20f
                )
                segments.add(Pair(current, branchEnd))
            }
        }
        return segments
    }

    fun generateSparks(position: Offset): List<Spark> {
        val sparkColors = listOf(
            Color(0xFFFFD700),
            Color(0xFFFFFFFF),
            Color(0xFF87CEEB),
            Color(0xFFADD8E6),
            Color(0xFFFFE4B5)
        )
        return List(30) {
            Spark(
                position = position,
                velocity = Offset(
                    x = Random.nextFloat() * 20f - 10f,
                    y = Random.nextFloat() * 20f - 10f
                ),
                color = sparkColors.random(),
                life = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    tapPosition = offset
                    isStriking = true

                    val bolts = List(3) { i ->
                        val startX = offset.x + Random.nextFloat() * 40f - 20f
                        LightningBolt(
                            segments = generateLightningBolt(
                                start = Offset(startX, 0f),
                                end = offset
                            ),
                            alpha = 1f - i * 0.2f,
                            color = when (i) {
                                0 -> Color(0xFFFFFFFF)
                                1 -> Color(0xFF87CEEB)
                                else -> Color(0xFF4169E1)
                            }
                        )
                    }
                    lightningBolts = bolts
                    sparks = generateSparks(offset)
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            if (flashAlpha > 0f) {
                drawRect(
                    color = Color(0xFF87CEEB).copy(alpha = flashAlpha)
                )
            }

            tapPosition?.let { tap ->

                if (strikeAnim > 0f) {
                    drawCircle(
                        color = Color(0xFF4169E1).copy(alpha = strikeAnim * glowPulse * 0.5f),
                        radius = 100f * strikeAnim,
                        center = tap
                    )
                    drawCircle(
                        color = Color(0xFF87CEEB).copy(alpha = strikeAnim * 0.3f),
                        radius = 50f * strikeAnim,
                        center = tap
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = strikeAnim * 0.8f),
                        radius = 15f * strikeAnim,
                        center = tap
                    )
                }

                lightningBolts.forEach { bolt ->
                    bolt.segments.forEach { (start, end) ->

                        drawLine(
                            color = Color(0xFF4169E1).copy(
                                alpha = bolt.alpha * strikeAnim * 0.4f
                            ),
                            start = start,
                            end = end,
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            color = Color(0xFF87CEEB).copy(
                                alpha = bolt.alpha * strikeAnim * 0.7f
                            ),
                            start = start,
                            end = end,
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        drawLine(
                            color = bolt.color.copy(
                                alpha = bolt.alpha * strikeAnim
                            ),
                            start = start,
                            end = end,
                            strokeWidth = 2f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                sparks.forEach { spark ->
                    val sparkPos = Offset(
                        x = spark.position.x + spark.velocity.x * strikeAnim * 20f,
                        y = spark.position.y + spark.velocity.y * strikeAnim * 20f
                    )
                    val sparkAlpha = spark.life * (1f - strikeAnim)
                    drawCircle(
                        color = spark.color.copy(alpha = sparkAlpha.coerceIn(0f, 1f)),
                        radius = 4f * spark.life,
                        center = sparkPos
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
                text = "⚡ Thor's Lightning",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF87CEEB),
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = if (isStriking) "STRIKE! ⚡" else "Tap anywhere ⚡",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}