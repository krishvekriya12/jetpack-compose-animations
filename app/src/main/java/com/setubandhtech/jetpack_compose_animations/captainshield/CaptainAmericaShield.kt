package com.setubandhtech.jetpack_compose_animations.captainshield

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CaptainAmericaShield() {
    var isThrown by remember { mutableStateOf(false) }
    var throwProgress by remember { mutableFloatStateOf(0f) }

    val spinAngle by rememberInfiniteTransition(label = "spin")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (isThrown) 400 else 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin_angle"
        )

    val throwAnim by animateFloatAsState(
        targetValue = if (isThrown) 1f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        finishedListener = {
            if (it == 1f) isThrown = false
        },
        label = "throw"
    )

    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )

    val shieldX by animateFloatAsState(
        targetValue = if (isThrown) 0.8f else 0.5f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "shield_x"
    )

    val shieldY by animateFloatAsState(
        targetValue = if (isThrown) 0.2f else 0.5f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "shield_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .pointerInput(Unit) {
                detectTapGestures {
                    isThrown = !isThrown
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width * shieldX
            val centerY = size.height * shieldY
            val shieldCenter = Offset(centerX, centerY)
            val shieldRadius = if (isThrown) 80f else 120f

            if (throwAnim > 0f) {
                repeat(8) { i ->
                    val trailAlpha = (1f - i.toFloat() / 8f) * throwAnim * 0.3f
                    val trailOffset = i * 15f
                    drawCircle(
                        color = Color(0xFF1565C0).copy(alpha = trailAlpha),
                        radius = shieldRadius - i * 5f,
                        center = Offset(
                            centerX + trailOffset,
                            centerY + trailOffset * 0.5f
                        )
                    )
                }
            }

            drawCircle(
                color = Color(0xFF1565C0).copy(alpha = 0.3f * glowPulse),
                radius = shieldRadius + 30f,
                center = shieldCenter
            )

            rotate(spinAngle, pivot = shieldCenter) {
                drawShield(
                    center = shieldCenter,
                    radius = shieldRadius
                )
            }

            if (throwAnim > 0.5f) {
                repeat(12) { i ->
                    val angle = (i.toFloat() / 12f) * 2f * Math.PI.toFloat()
                    val sparkDist = shieldRadius + 40f * throwAnim
                    val sparkPos = Offset(
                        x = centerX + cos(angle) * sparkDist,
                        y = centerY + sin(angle) * sparkDist
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = throwAnim * 0.8f),
                        radius = 4f,
                        center = sparkPos
                    )
                    drawLine(
                        color = Color(0xFF1565C0).copy(alpha = throwAnim * 0.5f),
                        start = shieldCenter,
                        end = sparkPos,
                        strokeWidth = 1f,
                        cap = StrokeCap.Round
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
                text = "🛡️ Captain America",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0),
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = if (isThrown) "Shield Thrown! 🛡️" else "Tap to Throw 🛡️",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

fun DrawScope.drawShield(center: Offset, radius: Float) {

    drawCircle(
        color = Color(0xFFB71C1C),
        radius = radius,
        center = center
    )

    drawCircle(
        color = Color.White,
        radius = radius * 0.8f,
        center = center
    )

    drawCircle(
        color = Color(0xFF0D47A1),
        radius = radius * 0.6f,
        center = center
    )

    drawCircle(
        color = Color.White,
        radius = radius * 0.4f,
        center = center
    )

    drawStar(
        center = center,
        outerRadius = radius * 0.35f,
        innerRadius = radius * 0.15f,
        color = Color(0xFF0D47A1)
    )

    listOf(1f, 0.8f, 0.6f, 0.4f).forEach { ratio ->
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = radius * ratio,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    drawCircle(
        color = Color.White.copy(alpha = 0.15f),
        radius = radius * 0.5f,
        center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f)
    )
}

fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    val points = 5
    val angleStep = (2 * Math.PI / points).toFloat()
    val startAngle = -Math.PI.toFloat() / 2

    for (i in 0 until points) {
        val outerAngle = startAngle + i * angleStep
        val innerAngle = outerAngle + angleStep / 2

        val outerX = center.x + cos(outerAngle) * outerRadius
        val outerY = center.y + sin(outerAngle) * outerRadius
        val innerX = center.x + cos(innerAngle) * innerRadius
        val innerY = center.y + sin(innerAngle) * innerRadius

        if (i == 0) path.moveTo(outerX, outerY)
        else path.lineTo(outerX, outerY)
        path.lineTo(innerX, innerY)
    }
    path.close()

    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = 2f)
    )
}