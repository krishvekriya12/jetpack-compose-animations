package com.setubandhtech.jetpack_compose_animations.spidermanweb

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class WebShot(
    val origin: Offset,
    val target: Offset,
    val progress: Float = 0f,
    val id: Int,
    val rings: Int = Random.nextInt(4, 8)
)

@Composable
fun SpiderManWebShooter() {
    var webShots by remember { mutableStateOf<List<WebShot>>(emptyList()) }
    var shotCounter by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "web")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    LaunchedEffect(tick) {
        webShots = webShots
            .map { it.copy(progress = (it.progress + 0.02f).coerceAtMost(1f)) }
            .filter { it.progress < 1f || it.rings > 0 }
    }

    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val origin = Offset(size.width / 2f, size.height.toFloat())
                    webShots = webShots + WebShot(
                        origin = origin,
                        target = offset,
                        id = shotCounter++
                    )
                    if (webShots.size > 5) {
                        webShots = webShots.drop(1)
                    }
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            webShots.forEach { shot ->
                drawWebShot(shot, glowPulse)
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
                text = "🕷️ Web Shooter",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = "Tap to shoot web 🕸️",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

fun DrawScope.drawWebShot(shot: WebShot, glowPulse: Float) {
    val progress = shot.progress
    val origin = shot.origin
    val target = shot.target

    val currentTip = Offset(
        x = origin.x + (target.x - origin.x) * progress,
        y = origin.y + (target.y - origin.y) * progress
    )

    drawLine(
        color = Color(0xFFE53935).copy(alpha = 0.3f * glowPulse),
        start = origin,
        end = currentTip,
        strokeWidth = 8f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = Color(0xFFFF5252).copy(alpha = 0.6f),
        start = origin,
        end = currentTip,
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = origin,
        end = currentTip,
        strokeWidth = 1f,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.8f * progress),
        radius = 6f,
        center = currentTip
    )
    drawCircle(
        color = Color(0xFFE53935).copy(alpha = 0.4f * glowPulse),
        radius = 14f,
        center = currentTip
    )

    if (progress > 0.8f) {
        val webAlpha = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
        val numRings = shot.rings
        val maxRadius = 80f

        val dx = target.x - origin.x
        val dy = target.y - origin.y
        val length = sqrt(dx * dx + dy * dy)
        val dirX = dx / length
        val dirY = dy / length

        repeat(12) { i ->
            val angle = (i.toFloat() / 12f) * 2f * Math.PI.toFloat()
            val webEndX = target.x + cos(angle) * maxRadius * webAlpha
            val webEndY = target.y + sin(angle) * maxRadius * webAlpha

            drawLine(
                color = Color.White.copy(alpha = 0.4f * webAlpha),
                start = target,
                end = Offset(webEndX, webEndY),
                strokeWidth = 1f,
                cap = StrokeCap.Round
            )
        }

        repeat(numRings) { ring ->
            val ringRadius = (maxRadius / numRings) * (ring + 1) * webAlpha
            val numPoints = 12
            val points = List(numPoints + 1) { i ->
                val angle = (i.toFloat() / numPoints) * 2f * Math.PI.toFloat()
                Offset(
                    x = target.x + cos(angle) * ringRadius,
                    y = target.y + sin(angle) * ringRadius
                )
            }

            for (i in 0 until numPoints) {
                drawLine(
                    color = Color.White.copy(
                        alpha = 0.5f * webAlpha * (1f - ring.toFloat() / numRings)
                    ),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 1f,
                    cap = StrokeCap.Round
                )
            }
        }

        drawCircle(
            color = Color(0xFFE53935).copy(alpha = 0.2f * webAlpha * glowPulse),
            radius = maxRadius * webAlpha,
            center = target
        )
    }

    if (progress > 0.3f) {
        val sideAlpha = ((progress - 0.3f) / 0.7f).coerceIn(0f, 0.5f)
        val midPoint = Offset(
            x = (origin.x + currentTip.x) / 2f,
            y = (origin.y + currentTip.y) / 2f
        )

        listOf(-20f, 20f).forEach { offset ->
            val sidePoint = Offset(
                x = midPoint.x + offset,
                y = midPoint.y + offset * 0.5f
            )
            drawLine(
                color = Color.White.copy(alpha = sideAlpha),
                start = origin,
                end = sidePoint,
                strokeWidth = 0.8f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = sideAlpha),
                start = sidePoint,
                end = currentTip,
                strokeWidth = 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}