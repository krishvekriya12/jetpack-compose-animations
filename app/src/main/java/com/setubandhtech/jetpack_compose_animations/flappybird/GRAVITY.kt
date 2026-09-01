package com.setubandhtech.jetpack_compose_animations.flappybird

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val GRAVITY = 2000f
private const val JUMP_FORCE = -600f
private const val PIPE_SPEED = 250f
private const val PIPE_WIDTH = 90f
private const val PIPE_GAP = 200f
private const val BIRD_RADIUS = 24f
private const val GROUND_HEIGHT = 100f
private const val PIPE_SPACING = 0.65f

enum class GameState { IDLE, PLAYING, DEAD }

data class GamePipe(
    var x: Float,
    val gapY: Float,
    var scored: Boolean = false
)

data class CloudData(
    var x: Float,
    val y: Float,
    val scale: Float
)

data class ParticleData(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val color: Color,
    val size: Float
)

@SuppressLint("AutoboxingStateCreation")
@Composable
fun FlappyBird() {
    var gameState by remember { mutableStateOf(GameState.IDLE) }
    var birdY by remember { mutableFloatStateOf(0f) }
    var birdVY by remember { mutableStateOf(0f) }
    var birdAngle by remember { mutableStateOf(0f) }
    var pipes by remember { mutableStateOf<List<GamePipe>>(emptyList()) }
    var clouds by remember { mutableStateOf<List<CloudData>>(emptyList()) }
    var particles by remember { mutableStateOf<List<ParticleData>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var bestScore by remember { mutableStateOf(0) }
    var groundScroll by remember { mutableStateOf(0f) }
    var screenW by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }
    var wingPhase by remember { mutableStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    fun initGame() {
        if (screenW == 0f) return
        birdY = screenH * 0.45f
        birdVY = 0f
        birdAngle = 0f
        score = 0
        groundScroll = 0f
        wingPhase = 0f

        pipes = listOf(
            GamePipe(
                x = screenW + 200f,
                gapY = screenH * 0.25f + Random.nextFloat() * screenH * 0.3f
            ),
            GamePipe(
                x = screenW + 200f + screenW * PIPE_SPACING,
                gapY = screenH * 0.25f + Random.nextFloat() * screenH * 0.3f
            )
        )

        clouds = List(6) {
            CloudData(
                x = Random.nextFloat() * screenW,
                y = Random.nextFloat() * screenH * 0.4f,
                scale = Random.nextFloat() * 0.5f + 0.7f
            )
        }

        particles = emptyList()
        gameState = GameState.PLAYING
    }

    fun spawnDeathParticles() {
        val birdX = screenW * 0.3f
        particles = List(25) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 500f + 200f
            ParticleData(
                x = birdX,
                y = birdY,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 200f,
                life = 1f,
                color = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFF6B6B),
                    Color(0xFF4ECDC4),
                    Color(0xFFFFE66D),
                    Color(0xFFFF8E53)
                ).random(),
                size = Random.nextFloat() * 14f + 6f
            )
        }
    }

    fun handleTap() {
        when (gameState) {
            GameState.IDLE -> initGame()
            GameState.PLAYING -> {
                birdVY = JUMP_FORCE
                birdAngle = -25f
            }
            GameState.DEAD -> initGame()
        }
    }

    LaunchedEffect(gameState) {
        if (gameState != GameState.PLAYING) return@LaunchedEffect

        var prevTime = 0L

        while (gameState == GameState.PLAYING) {
            val frameTime = awaitFrame()
            if (prevTime == 0L) {
                prevTime = frameTime
                continue
            }

            val dt = ((frameTime - prevTime) / 1_000_000_000f).coerceAtMost(0.05f)
            prevTime = frameTime

            if (screenW == 0f) continue

            val birdX = screenW * 0.3f
            val playH = screenH - GROUND_HEIGHT

            birdVY += GRAVITY * dt
            birdY += birdVY * dt
            birdAngle = (birdVY / 12f).coerceIn(-30f, 85f)
            wingPhase += dt * 8f

            if (birdY + BIRD_RADIUS >= playH) {
                birdY = playH - BIRD_RADIUS
                spawnDeathParticles()
                gameState = GameState.DEAD
                if (score > bestScore) bestScore = score
                break
            }

            if (birdY - BIRD_RADIUS <= 0f) {
                birdY = BIRD_RADIUS
                birdVY = 0f
            }

            groundScroll = (groundScroll + PIPE_SPEED * dt) % 60f

            clouds = clouds.map { cloud ->
                val newX = cloud.x - 40f * dt
                if (newX + 120f * cloud.scale < 0f)
                    cloud.copy(x = screenW + 50f, y = Random.nextFloat() * screenH * 0.4f)
                else cloud.copy(x = newX)
            }

            pipes = pipes.map { pipe ->
                val newX = pipe.x - PIPE_SPEED * dt
                if (newX + PIPE_WIDTH < 0f) {
                    GamePipe(
                        x = pipes.maxOf { it.x } + screenW * PIPE_SPACING,
                        gapY = screenH * 0.2f + Random.nextFloat() * (playH - PIPE_GAP - screenH * 0.2f)
                    )
                } else pipe.copy(x = newX)
            }

            pipes = pipes.map { pipe ->
                if (!pipe.scored && pipe.x + PIPE_WIDTH < birdX - BIRD_RADIUS) {
                    score++
                    pipe.copy(scored = true)
                } else pipe
            }

            val margin = 6f
            pipes.forEach { pipe ->
                val pipeRight = pipe.x + PIPE_WIDTH
                val pipeLeft = pipe.x

                if (birdX + BIRD_RADIUS - margin > pipeLeft &&
                    birdX - BIRD_RADIUS + margin < pipeRight
                ) {
                    val gapTop = pipe.gapY
                    val gapBottom = pipe.gapY + PIPE_GAP

                    if (birdY - BIRD_RADIUS + margin < gapTop ||
                        birdY + BIRD_RADIUS - margin > gapBottom
                    ) {
                        spawnDeathParticles()
                        gameState = GameState.DEAD
                        if (score > bestScore) bestScore = score
                        return@LaunchedEffect
                    }
                }
            }

            particles = particles.mapNotNull { p ->
                val newLife = p.life - dt * 1.8f
                if (newLife <= 0f) null
                else p.copy(
                    x = p.x + p.vx * dt,
                    y = p.y + p.vy * dt,
                    vy = p.vy + GRAVITY * 0.4f * dt,
                    life = newLife
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { handleTap() }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenW = size.width
            screenH = size.height

            if (!initialized && screenW > 0f) {
                birdY = screenH * 0.45f
                initialized = true
            }

            val playH = screenH - GROUND_HEIGHT

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),
                        Color(0xFF98D8F0),
                        Color(0xFFB0E2F5)
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFDE7),
                        Color(0xFFFFD54F),
                        Color(0xFFFFB300).copy(alpha = 0f)
                    ),
                    center = Offset(screenW * 0.85f, screenH * 0.1f),
                    radius = 80f
                ),
                radius = 80f,
                center = Offset(screenW * 0.85f, screenH * 0.1f)
            )

            clouds.forEach { cloud ->
                drawGameCloud(
                    center = Offset(cloud.x, cloud.y),
                    scale = cloud.scale
                )
            }

            pipes.forEach { pipe ->
                drawGamePipe(
                    pipe = pipe,
                    screenH = screenH,
                    groundH = GROUND_HEIGHT
                )
            }

            drawGameGround(
                screenW = screenW,
                screenH = screenH,
                groundH = GROUND_HEIGHT,
                scroll = groundScroll
            )

            val birdX = screenW * 0.3f
            drawGameBird(
                center = Offset(birdX, birdY),
                angle = birdAngle,
                wingPhase = wingPhase,
                radius = BIRD_RADIUS
            )

            particles.forEach { p ->
                val alpha = p.life.coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * alpha,
                    center = Offset(p.x, p.y)
                )
                drawCircle(
                    color = p.color.copy(alpha = alpha * 0.3f),
                    radius = p.size * 2f * alpha,
                    center = Offset(p.x, p.y)
                )
            }

            if (gameState == GameState.PLAYING) {
                drawContext.canvas.nativeCanvas.apply {
                    val scorePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 96f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setShadowLayer(12f, 2f, 4f, android.graphics.Color.argb(180, 0, 0, 0))
                    }
                    drawText("$score", screenW / 2f, 150f, scorePaint)
                }
            }
        }

        when (gameState) {
            GameState.IDLE -> IdleOverlay()
            GameState.DEAD -> DeadOverlay(score = score, bestScore = bestScore)
            GameState.PLAYING -> {}
        }
    }
}

fun DrawScope.drawGameBird(
    center: Offset,
    angle: Float,
    wingPhase: Float,
    radius: Float
) {
    rotate(angle, pivot = center) {

        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = radius * 1.1f,
            center = Offset(center.x + 3f, center.y + 5f)
        )

        val wingY = center.y + sin(wingPhase) * radius * 0.3f
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF8C00), Color(0xFFFFB347)),
                start = Offset(center.x - radius, wingY),
                end = Offset(center.x, wingY + radius * 0.5f)
            ),
            topLeft = Offset(center.x - radius * 0.9f, wingY - radius * 0.25f),
            size = Size(radius * 1.4f, radius * 0.6f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF176),
                    Color(0xFFFFD700),
                    Color(0xFFFF8F00)
                ),
                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.2f),
                radius = radius * 1.2f
            ),
            radius = radius,
            center = center
        )

        drawCircle(
            color = Color(0xFFFFF9C4).copy(alpha = 0.6f),
            radius = radius * 0.55f,
            center = Offset(center.x + radius * 0.1f, center.y + radius * 0.15f)
        )

        drawCircle(
            color = Color.White,
            radius = radius * 0.38f,
            center = Offset(center.x + radius * 0.32f, center.y - radius * 0.22f)
        )

        drawCircle(
            color = Color(0xFF1565C0),
            radius = radius * 0.24f,
            center = Offset(center.x + radius * 0.36f, center.y - radius * 0.18f)
        )

        drawCircle(
            color = Color.Black,
            radius = radius * 0.14f,
            center = Offset(center.x + radius * 0.38f, center.y - radius * 0.16f)
        )

        drawCircle(
            color = Color.White,
            radius = radius * 0.07f,
            center = Offset(center.x + radius * 0.32f, center.y - radius * 0.24f)
        )

        val beakPath = Path().apply {
            moveTo(center.x + radius * 0.55f, center.y + radius * 0.05f)
            lineTo(center.x + radius * 1.15f, center.y + radius * 0.2f)
            lineTo(center.x + radius * 1.15f, center.y + radius * 0.4f)
            lineTo(center.x + radius * 0.55f, center.y + radius * 0.42f)
            close()
        }
        drawPath(beakPath, Color(0xFFFF6D00))
        drawPath(beakPath, Color.Black.copy(alpha = 0.1f), style = Stroke(width = 1f))

        drawLine(
            color = Color(0xFFE65100),
            start = Offset(center.x + radius * 0.55f, center.y + radius * 0.24f),
            end = Offset(center.x + radius * 1.15f, center.y + radius * 0.3f),
            strokeWidth = 1.5f
        )

        drawCircle(
            color = Color(0xFFFF8A80).copy(alpha = 0.5f),
            radius = radius * 0.22f,
            center = Offset(center.x + radius * 0.1f, center.y + radius * 0.3f)
        )
    }
}

fun DrawScope.drawGamePipe(
    pipe: GamePipe,
    screenH: Float,
    groundH: Float
) {
    val topPipeBottom = pipe.gapY
    val bottomPipeTop = pipe.gapY + PIPE_GAP
    val bottomPipeBottom = screenH - groundH
    val capH = 32f
    val capExtra = 12f

    val shadowOffset = 4f

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF388E3C),
                Color(0xFF4CAF50),
                Color(0xFF66BB6A),
                Color(0xFF4CAF50),
                Color(0xFF2E7D32)
            ),
            startX = pipe.x,
            endX = pipe.x + PIPE_WIDTH
        ),
        topLeft = Offset(pipe.x, 0f),
        size = Size(PIPE_WIDTH, topPipeBottom - capH)
    )

    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2E7D32),
                Color(0xFF43A047),
                Color(0xFF66BB6A),
                Color(0xFF43A047),
                Color(0xFF1B5E20)
            ),
            startX = pipe.x - capExtra,
            endX = pipe.x + PIPE_WIDTH + capExtra
        ),
        topLeft = Offset(pipe.x - capExtra, topPipeBottom - capH),
        size = Size(PIPE_WIDTH + capExtra * 2f, capH),
        cornerRadius = CornerRadius(8f, 8f)
    )

    drawRect(
        color = Color.White.copy(alpha = 0.2f),
        topLeft = Offset(pipe.x + 8f, 0f),
        size = Size(12f, topPipeBottom - capH)
    )
    drawRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(pipe.x + PIPE_WIDTH - 12f, 0f),
        size = Size(12f, topPipeBottom - capH)
    )

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF388E3C),
                Color(0xFF4CAF50),
                Color(0xFF66BB6A),
                Color(0xFF4CAF50),
                Color(0xFF2E7D32)
            ),
            startX = pipe.x,
            endX = pipe.x + PIPE_WIDTH
        ),
        topLeft = Offset(pipe.x, bottomPipeTop + capH),
        size = Size(PIPE_WIDTH, bottomPipeBottom - bottomPipeTop - capH)
    )

    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2E7D32),
                Color(0xFF43A047),
                Color(0xFF66BB6A),
                Color(0xFF43A047),
                Color(0xFF1B5E20)
            ),
            startX = pipe.x - capExtra,
            endX = pipe.x + PIPE_WIDTH + capExtra
        ),
        topLeft = Offset(pipe.x - capExtra, bottomPipeTop),
        size = Size(PIPE_WIDTH + capExtra * 2f, capH),
        cornerRadius = CornerRadius(8f, 8f)
    )

    drawRect(
        color = Color.White.copy(alpha = 0.2f),
        topLeft = Offset(pipe.x + 8f, bottomPipeTop + capH),
        size = Size(12f, bottomPipeBottom - bottomPipeTop - capH)
    )
    drawRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(pipe.x + PIPE_WIDTH - 12f, bottomPipeTop + capH),
        size = Size(12f, bottomPipeBottom - bottomPipeTop - capH)
    )
}

fun DrawScope.drawGameGround(
    screenW: Float,
    screenH: Float,
    groundH: Float,
    scroll: Float
) {
    val groundY = screenH - groundH

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.1f), Color.Transparent),
            startY = groundY - 10f,
            endY = groundY + 10f
        ),
        topLeft = Offset(0f, groundY - 10f),
        size = Size(screenW, 20f)
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFDEB887),
                Color(0xFFD2B48C),
                Color(0xFFC19A6B)
            ),
            startY = groundY,
            endY = screenH
        ),
        topLeft = Offset(0f, groundY),
        size = Size(screenW, groundH)
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF8BC34A),
                Color(0xFF689F38)
            ),
            startY = groundY,
            endY = groundY + 24f
        ),
        topLeft = Offset(0f, groundY),
        size = Size(screenW, 24f)
    )

    var gx = -scroll
    while (gx < screenW) {
        drawCircle(
            color = Color(0xFF558B2F).copy(alpha = 0.5f),
            radius = 3f,
            center = Offset(gx, groundY + 12f)
        )
        gx += 20f
    }

    gx = -scroll
    while (gx < screenW) {
        drawLine(
            color = Color(0xFFBF8C60).copy(alpha = 0.4f),
            start = Offset(gx, groundY + 28f),
            end = Offset(gx + 30f, screenH),
            strokeWidth = 1.5f
        )
        gx += 60f
    }
}

fun DrawScope.drawGameCloud(center: Offset, scale: Float) {
    val c = Color.White.copy(alpha = 0.85f)
    val s = scale

    drawCircle(color = c, radius = 30f * s, center = center)
    drawCircle(color = c, radius = 40f * s, center = Offset(center.x + 28f * s, center.y - 5f * s))
    drawCircle(color = c, radius = 28f * s, center = Offset(center.x + 58f * s, center.y))
    drawCircle(color = c, radius = 22f * s, center = Offset(center.x - 25f * s, center.y + 5f * s))
    drawCircle(color = c, radius = 35f * s, center = Offset(center.x + 15f * s, center.y - 18f * s))
}

@Composable
fun IdleOverlay() {
    val bounce by rememberInfiniteTransition(label = "bounce")
        .animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bounce_y"
        )

    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_a"
        )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🐦",
            fontSize = 72.sp,
            modifier = Modifier.offset(y = bounce.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "FLAPPY BIRD",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = Color(0xFF1565C0),
                    offset = Offset(2f, 4f),
                    blurRadius = 8f
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pure Jetpack Compose",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "TAP TO START",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = pulse),
            letterSpacing = 3.sp
        )
    }
}

@Composable
fun DeadOverlay(score: Int, bestScore: Int) {
    val isNewBest = score == bestScore && score > 0

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dead_scale"
    )

    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "tap_pulse"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E),
                            Color(0xFF283593)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 48.dp, vertical = 36.dp)
        ) {
            Text(
                text = if (isNewBest) "🏆 NEW BEST!" else "💀 GAME OVER",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNewBest) Color(0xFFFFD700) else Color(0xFFEF5350)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCORE",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$score",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(80.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BEST",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$bestScore",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "TAP TO RETRY",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = pulse),
                letterSpacing = 3.sp
            )
        }
    }
}