package com.setubandhtech.jetpack_compose_animations.holicolors

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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ColorSplash(
    val id: Int,
    val centerX: Float,
    val centerY: Float,
    val color: Color,
    val drops: List<ColorDrop>
)

data class ColorDrop(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val wobble: Float
)

data class SplashState(
    val splash: ColorSplash,
    val progress: Float = 0f
)

@Composable
fun HoliColorSplash() {
    var splashes by remember { mutableStateOf<List<SplashState>>(emptyList()) }
    var idCounter by remember { mutableStateOf(0) }
    var backgroundColor by remember { mutableStateOf(Color(0xFF0A0A0F)) }

    val holiColors = listOf(
        Color(0xFFE53935),
        Color(0xFFFF9800),
        Color(0xFFFFEB3B),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFF9C27B0),
        Color(0xFFFF4081),
        Color(0xFF00BCD4),
        Color(0xFFFF6B35),
        Color(0xFF8BC34A)
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
        splashes = splashes
            .map { it.copy(progress = it.progress + 0.018f) }
            .filter { it.progress < 1f }
    }

    fun createSplash(x: Float, y: Float): SplashState {
        val mainColor = holiColors.random()
        val drops = List(Random.nextInt(20, 35)) {
            ColorDrop(
                angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                speed = Random.nextFloat() * 0.6f + 0.4f,
                size = Random.nextFloat() * 20f + 8f,
                color = holiColors.random(),
                wobble = Random.nextFloat() * 0.3f
            )
        }
        return SplashState(
            splash = ColorSplash(
                id = idCounter++,
                centerX = x,
                centerY = y,
                color = mainColor,
                drops = drops
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    splashes = (splashes + createSplash(offset.x, offset.y)).takeLast(10)
                    backgroundColor = holiColors.random().copy(alpha = 0.15f)
                        .compositeOver(backgroundColor)
                }
            }
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            splashes.forEach { state ->
                drawColorSplash(state)
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
                text = "🌈 Happy Holi! 🎨",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFEB3B),
                modifier = Modifier.padding(top = 32.dp)
            )

            Text(
                text = "Tap to throw colors! 🎨",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

fun DrawScope.drawColorSplash(state: SplashState) {
    val splash = state.splash
    val progress = state.progress
    val center = Offset(splash.centerX, splash.centerY)
    val alpha = (1f - progress).coerceIn(0f, 1f)

    if (progress < 0.4f) {
        val splashRadius = 60f * (progress / 0.4f)
        val splashAlpha = (1f - progress / 0.4f) * 0.8f
        drawCircle(
            color = splash.color.copy(alpha = splashAlpha),
            radius = splashRadius,
            center = center
        )

        if (progress < 0.15f) {
            drawCircle(
                color = Color.White.copy(alpha = (1f - progress / 0.15f) * 0.6f),
                radius = 30f * (progress / 0.15f),
                center = center
            )
        }
    }

    splash.drops.forEach { drop ->
        val distance = drop.speed * progress * 300f
        val gravity = 120f * progress * progress
        val wobbleX = sin(progress * 10f + drop.wobble * 20f) * 15f * drop.wobble

        val dropX = center.x + cos(drop.angle) * distance + wobbleX
        val dropY = center.y + sin(drop.angle) * distance + gravity

        val dropAlpha = alpha * drop.speed
        val dropSize = drop.size * (1f - progress * 0.3f)

        val tailDistance = distance * 0.8f
        val tailX = center.x + cos(drop.angle) * tailDistance + wobbleX * 0.8f
        val tailY = center.y + sin(drop.angle) * tailDistance + gravity * 0.8f

        drawLine(
            color = drop.color.copy(alpha = dropAlpha * 0.6f),
            start = Offset(tailX, tailY),
            end = Offset(dropX, dropY),
            strokeWidth = dropSize * 0.6f,
            cap = StrokeCap.Round
        )

        drawOval(
            color = drop.color.copy(alpha = dropAlpha),
            topLeft = Offset(dropX - dropSize / 2, dropY - dropSize * 0.7f),
            size = Size(dropSize, dropSize * 1.4f)
        )

        drawCircle(
            color = Color.White.copy(alpha = dropAlpha * 0.4f),
            radius = dropSize * 0.25f,
            center = Offset(dropX - dropSize * 0.15f, dropY - dropSize * 0.25f)
        )

        if (progress > 0.3f) {
            repeat(3) { i ->
                val splatterAngle = drop.angle + (i - 1) * 0.5f
                val splatterDist = distance * 0.2f * (i + 1)
                val splatterX = dropX + cos(splatterAngle) * splatterDist
                val splatterY = dropY + sin(splatterAngle) * splatterDist

                drawCircle(
                    color = drop.color.copy(alpha = dropAlpha * 0.5f),
                    radius = dropSize * 0.25f,
                    center = Offset(splatterX, splatterY)
                )
            }
        }
    }
}

fun Color.compositeOver(background: Color): Color {
    val fg = this
    val a = fg.alpha + background.alpha * (1f - fg.alpha)
    if (a == 0f) return Color.Transparent
    return Color(
        red = (fg.red * fg.alpha + background.red * background.alpha * (1f - fg.alpha)) / a,
        green = (fg.green * fg.alpha + background.green * background.alpha * (1f - fg.alpha)) / a,
        blue = (fg.blue * fg.alpha + background.blue * background.alpha * (1f - fg.alpha)) / a,
        alpha = a
    )
}