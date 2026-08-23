package com.setubandhtech.jetpack_compose_animations.particleexplosion

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val id: Int,
    val angle: Float,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val decay: Float
)

@Composable
fun ParticleExplosion() {
    var exploding by remember { mutableStateOf(false) }
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }

    val progress by animateFloatAsState(
        targetValue = if (exploding) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (it == 1f) exploding = false
        },
        label = "explosion"
    )

    val particleColors = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFFD700),
        Color(0xFFE53935),
        Color(0xFF00E5FF),
        Color(0xFFFF4081),
        Color(0xFF69F0AE),
        Color(0xFFFFAB40)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            particles.forEach { particle ->
                val distance = particle.speed * progress * 400f
                val x = centerX + cos(particle.angle) * distance
                val y = centerY + sin(particle.angle) * distance
                val alpha = (1f - progress * particle.decay).coerceIn(0f, 1f)
                val currentRadius = particle.radius * (1f - progress * 0.3f)

                drawParticle(
                    x = x,
                    y = y,
                    radius = currentRadius,
                    color = particle.color.copy(alpha = alpha)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = if (exploding) "💥 BOOM!" else "Tap Me!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        if (exploding) Color(0xFFE53935)
                        else Color(0xFF6200EE)
                    )
                    .clickable {
                        if (!exploding) {
                            particles = List(80) { i ->
                                Particle(
                                    id = i,
                                    angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                                    speed = Random.nextFloat() * 0.6f + 0.4f,
                                    radius = Random.nextFloat() * 12f + 4f,
                                    color = particleColors.random(),
                                    decay = Random.nextFloat() * 0.5f + 0.5f
                                )
                            }
                            exploding = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (exploding) "💥 Exploding!" else "🎯 EXPLODE!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Pure Canvas • No Library 🔥",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
        }
    }
}

fun DrawScope.drawParticle(
    x: Float,
    y: Float,
    radius: Float,
    color: Color
) {
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(x, y)
    )
}