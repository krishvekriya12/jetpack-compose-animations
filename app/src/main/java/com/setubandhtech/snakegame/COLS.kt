package com.setubandhtech.snakegame

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.*
import kotlin.random.Random

private const val COLS = 20
private const val ROWS = 32
private const val BASE_SPEED_MS = 130L
private const val MIN_SPEED_MS = 70L

private object Palette {
    val BgTop = Color(0xFF0B0F1A)
    val BgBottom = Color(0xFF06070D)
    val GridLine = Color(0xFF1C2333)
    val BoardBorder = Color(0xFF2A3550)
    val Accent = Color(0xFF3DDC97)
    val AccentDim = Color(0xFF1F8F63)
    val Danger = Color(0xFFFF5D73)
    val Gold = Color(0xFFF4C542)
    val Sapphire = Color(0xFF4FA8FF)
    val TextPrimary = Color(0xFFEAEFF7)
    val TextSecondary = Color(0xFF8A93A6)
    val SurfaceCard = Color(0xFF12172A)
    val SurfaceCardAlt = Color(0xFF171E36)
}

enum class Direction { UP, DOWN, LEFT, RIGHT }
enum class SnakeState { IDLE, PLAYING, DEAD }

data class Cell(val x: Int, val y: Int)

data class Food(val cell: Cell, val type: FoodType)

enum class FoodType(val points: Int, val color: Color, val ringColor: Color) {
    NORMAL(1, Palette.Accent, Palette.AccentDim),
    BONUS(3, Palette.Gold, Color(0xFFB8862F)),
    SPEED(2, Palette.Sapphire, Color(0xFF2C6FBF))
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val color: Color,
    val size: Float
)

@Composable
fun SnakeGame() {
    var snake by remember { mutableStateOf(listOf(Cell(10, 16), Cell(10, 17), Cell(10, 18))) }
    var direction by remember { mutableStateOf(Direction.UP) }
    var nextDirection by remember { mutableStateOf(Direction.UP) }
    var food by remember { mutableStateOf<List<Food>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var bestScore by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf(SnakeState.IDLE) }
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var cellSize by remember { mutableStateOf(0f) }
    var moveCount by remember { mutableStateOf(0) }
    var headPulse by remember { mutableFloatStateOf(1f) }

    val foodPulse by rememberInfiniteTransition(label = "foodPulse")
        .animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "foodPulse"
        )

    val bgDrift by rememberInfiniteTransition(label = "bgDrift")
        .animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bgDrift"
        )

    fun spawnFood(snakeCells: List<Cell>) {
        val occupied = snakeCells.toSet()
        val available = (0 until COLS).flatMap { x -> (0 until ROWS).map { y -> Cell(x, y) } }
            .filter { it !in occupied }
        if (available.isEmpty()) return

        val newFood = mutableListOf<Food>()
        val mainCell = available.random()
        newFood.add(Food(mainCell, FoodType.NORMAL))

        val roll = Random.nextFloat()
        val remaining = available.filter { it != mainCell }
        if (roll < 0.18f && remaining.isNotEmpty()) {
            newFood.add(Food(remaining.random(), FoodType.BONUS))
        } else if (roll < 0.32f && remaining.isNotEmpty()) {
            newFood.add(Food(remaining.random(), FoodType.SPEED))
        }
        food = newFood
    }

    fun spawnParticles(cell: Cell, color: Color, count: Int = 14) {
        val cx = (cell.x + 0.5f) * cellSize
        val cy = (cell.y + 0.5f) * cellSize
        particles = particles + List(count) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 260f + 90f
            Particle(
                x = cx, y = cy,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                life = 1f,
                color = color,
                size = Random.nextFloat() * 6f + 3f
            )
        }
    }

    fun resetGame() {
        val startSnake = listOf(Cell(COLS / 2, ROWS / 2), Cell(COLS / 2, ROWS / 2 + 1), Cell(COLS / 2, ROWS / 2 + 2))
        snake = startSnake
        direction = Direction.UP
        nextDirection = Direction.UP
        score = 0
        particles = emptyList()
        moveCount = 0
        spawnFood(startSnake)
        gameState = SnakeState.PLAYING
    }

    LaunchedEffect(gameState) {
        if (gameState != SnakeState.PLAYING) return@LaunchedEffect

        var lastMoveTime = 0L
        var lastFrameTime = 0L
        var currentSpeed = BASE_SPEED_MS

        while (gameState == SnakeState.PLAYING) {
            val frameTime = awaitFrame()
            val dt = if (lastFrameTime == 0L) 0f else (frameTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = frameTime

            particles = particles.mapNotNull { p ->
                val newLife = p.life - dt * 2.2f
                if (newLife <= 0f) null
                else p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt, vy = p.vy + 420f * dt, life = newLife)
            }
            headPulse = (headPulse - dt * 3f).coerceAtLeast(1f)

            val currentTime = frameTime / 1_000_000
            val speedFloor = (BASE_SPEED_MS - (score / 5) * 6L).coerceAtLeast(MIN_SPEED_MS)
            currentSpeed = speedFloor

            if (currentTime - lastMoveTime >= currentSpeed) {
                lastMoveTime = currentTime
                direction = nextDirection
                moveCount++

                val head = snake.first()
                val newHead = when (direction) {
                    Direction.UP -> Cell(head.x, head.y - 1)
                    Direction.DOWN -> Cell(head.x, head.y + 1)
                    Direction.LEFT -> Cell(head.x - 1, head.y)
                    Direction.RIGHT -> Cell(head.x + 1, head.y)
                }

                val hitWall = newHead.x < 0 || newHead.x >= COLS || newHead.y < 0 || newHead.y >= ROWS
                val hitSelf = newHead in snake.dropLast(1)

                if (hitWall || hitSelf) {
                    spawnParticles(head, Palette.Danger, 22)
                    gameState = SnakeState.DEAD
                    if (score > bestScore) bestScore = score
                    break
                }

                val eaten = food.find { it.cell == newHead }
                val newSnake = if (eaten != null) {
                    spawnParticles(newHead, eaten.type.color)
                    score += eaten.type.points
                    headPulse = 1.6f
                    val remainingFood = food.filter { it != eaten }
                    food = remainingFood
                    val grown = listOf(newHead) + snake
                    if (remainingFood.none { it.type == FoodType.NORMAL }) {
                        spawnFood(grown)
                    }
                    grown
                } else {
                    listOf(newHead) + snake.dropLast(1)
                }

                snake = newSnake
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.BgBottom)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScoreBar(score = score, bestScore = bestScore)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            if (gameState != SnakeState.PLAYING) return@detectDragGestures
                            val dx = dragAmount.x
                            val dy = dragAmount.y
                            if (abs(dx) > abs(dy)) {
                                if (dx > 0 && direction != Direction.LEFT) nextDirection = Direction.RIGHT
                                else if (dx < 0 && direction != Direction.RIGHT) nextDirection = Direction.LEFT
                            } else {
                                if (dy > 0 && direction != Direction.UP) nextDirection = Direction.DOWN
                                else if (dy < 0 && direction != Direction.DOWN) nextDirection = Direction.UP
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridAspect = COLS.toFloat() / ROWS.toFloat()
                    val availableAspect = size.width / size.height
                    val gridW: Float
                    val gridH: Float
                    if (availableAspect > gridAspect) {
                        gridH = size.height
                        gridW = gridH * gridAspect
                    } else {
                        gridW = size.width
                        gridH = gridW / gridAspect
                    }
                    cellSize = gridW / COLS
                    val offsetX = (size.width - gridW) / 2f
                    val offsetY = (size.height - gridH) / 2f

                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Palette.BgTop, Palette.BgBottom),
                            center = Offset(
                                offsetX + gridW / 2f + sin(bgDrift) * gridW * 0.15f,
                                offsetY + gridH / 2f + cos(bgDrift * 0.8f) * gridH * 0.15f
                            ),
                            radius = gridW * 0.9f
                        ),
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(gridW, gridH),
                        cornerRadius = CornerRadius(10f)
                    )

                    for (x in 0..COLS) {
                        drawLine(
                            color = Palette.GridLine.copy(alpha = 0.5f),
                            start = Offset(offsetX + x * cellSize, offsetY),
                            end = Offset(offsetX + x * cellSize, offsetY + gridH),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..ROWS) {
                        drawLine(
                            color = Palette.GridLine.copy(alpha = 0.5f),
                            start = Offset(offsetX, offsetY + y * cellSize),
                            end = Offset(offsetX + gridW, offsetY + y * cellSize),
                            strokeWidth = 1f
                        )
                    }

                    drawRoundRect(
                        color = Palette.BoardBorder,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(gridW, gridH),
                        cornerRadius = CornerRadius(10f),
                        style = Stroke(width = 2f)
                    )

                    food.forEach { f ->
                        val fx = offsetX + f.cell.x * cellSize + cellSize / 2f
                        val fy = offsetY + f.cell.y * cellSize + cellSize / 2f
                        drawCircle(
                            color = f.type.color.copy(alpha = 0.22f),
                            radius = cellSize * 0.68f * foodPulse,
                            center = Offset(fx, fy)
                        )
                        drawCircle(
                            color = f.type.ringColor,
                            radius = cellSize * 0.34f,
                            center = Offset(fx, fy),
                            style = Stroke(width = cellSize * 0.06f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(f.type.color, f.type.ringColor),
                                center = Offset(fx - cellSize * 0.08f, fy - cellSize * 0.08f),
                                radius = cellSize * 0.4f
                            ),
                            radius = cellSize * 0.27f,
                            center = Offset(fx, fy)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.55f),
                            radius = cellSize * 0.08f,
                            center = Offset(fx - cellSize * 0.1f, fy - cellSize * 0.1f)
                        )
                    }

                    snake.forEachIndexed { index, cell ->
                        val cx = offsetX + cell.x * cellSize
                        val cy = offsetY + cell.y * cellSize
                        val isHead = index == 0
                        val fade = (index.toFloat() / snake.size).coerceIn(0f, 1f)
                        val bodyColor = lerpColor(Palette.Accent, Palette.AccentDim, fade)

                        if (isHead) {
                            drawRoundRect(
                                color = Palette.Accent.copy(alpha = 0.25f * headPulse),
                                topLeft = Offset(cx - 5f, cy - 5f),
                                size = Size(cellSize + 10f, cellSize + 10f),
                                cornerRadius = CornerRadius(cellSize * 0.42f)
                            )
                        }

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(bodyColor.copy(alpha = 0.95f), bodyColor.copy(alpha = 0.72f))
                            ),
                            topLeft = Offset(cx + 1.5f, cy + 1.5f),
                            size = Size(cellSize - 3f, cellSize - 3f),
                            cornerRadius = CornerRadius(if (isHead) cellSize * 0.4f else cellSize * 0.26f)
                        )

                        if (isHead) {
                            val eyeRadius = cellSize * 0.1f
                            val eyeInset = cellSize * 0.27f
                            val eyeY = cy + cellSize * 0.34f
                            val (eyeStartX, eyeEndX) = when (direction) {
                                Direction.UP, Direction.DOWN -> Pair(cx + eyeInset, cx + cellSize - eyeInset)
                                Direction.LEFT, Direction.RIGHT -> Pair(cx + cellSize * 0.38f, cx + cellSize * 0.38f)
                            }
                            listOf(eyeStartX, eyeEndX).forEach { ex ->
                                drawCircle(color = Color.White, radius = eyeRadius, center = Offset(ex, eyeY))
                                drawCircle(
                                    color = Color(0xFF0B0F1A),
                                    radius = eyeRadius * 0.55f,
                                    center = Offset(ex, eyeY)
                                )
                            }
                        }
                    }

                    particles.forEach { p ->
                        val alpha = p.life.coerceIn(0f, 1f)
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size * alpha,
                            center = Offset(p.x + offsetX, p.y + offsetY)
                        )
                    }
                }

                GameOverlays(
                    gameState = gameState,
                    score = score,
                    bestScore = bestScore,
                    onRestart = { resetGame() }
                )
            }

            AnimatedVisibility(
                visible = gameState == SnakeState.PLAYING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DPad(
                    onUp = { if (direction != Direction.DOWN) nextDirection = Direction.UP },
                    onDown = { if (direction != Direction.UP) nextDirection = Direction.DOWN },
                    onLeft = { if (direction != Direction.RIGHT) nextDirection = Direction.LEFT },
                    onRight = { if (direction != Direction.LEFT) nextDirection = Direction.RIGHT }
                )
            }
        }
    }
}

@Composable
private fun GameOverlays(
    gameState: SnakeState,
    score: Int,
    bestScore: Int,
    onRestart: () -> Unit
) {
    AnimatedVisibility(
        visible = gameState == SnakeState.IDLE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        IdleOverlay(onRestart)
    }

    AnimatedVisibility(
        visible = gameState == SnakeState.DEAD,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        DeadOverlay(score = score, bestScore = bestScore, onRestart = onRestart)
    }
}

@Composable
private fun ScoreBar(score: Int, bestScore: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatChip(label = "SCORE", value = score.toString(), accent = Palette.Accent)
        Text(
            text = "SNAKE",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Palette.TextSecondary,
            letterSpacing = 4.sp
        )
        StatChip(label = "BEST", value = bestScore.toString(), accent = Palette.Gold)
    }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color) {
    Column(
        modifier = Modifier
            .background(Palette.SurfaceCard, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 10.sp, color = Palette.TextSecondary, letterSpacing = 1.5.sp)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(180.dp)) {
            DPadButton("▲", Modifier.align(Alignment.TopCenter), onUp)
            DPadButton("▼", Modifier.align(Alignment.BottomCenter), onDown)
            DPadButton("◀", Modifier.align(Alignment.CenterStart), onLeft)
            DPadButton("▶", Modifier.align(Alignment.CenterEnd), onRight)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Palette.SurfaceCardAlt)
            )
        }
    }
}

@Composable
private fun DPadButton(glyph: String, modifier: Modifier, onTap: () -> Unit) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.SurfaceCard)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center
    ) {
        Text(text = glyph, color = Palette.Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IdleOverlay(onStart: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "idlePulse")
        .animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(750), repeatMode = RepeatMode.Reverse),
            label = "idlePulse"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.BgBottom.copy(alpha = 0.72f))
            .pointerInput(Unit) { detectTapGestures { onStart() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .background(Palette.SurfaceCard, RoundedCornerShape(24.dp))
                .padding(horizontal = 40.dp, vertical = 36.dp)
        ) {
            Text(
                text = "SNAKE",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Palette.Accent,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Swipe or use the pad to move",
                fontSize = 13.sp,
                color = Palette.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "TAP TO START",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Accent.copy(alpha = pulse),
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
private fun DeadOverlay(score: Int, bestScore: Int, onRestart: () -> Unit) {
    val isNewBest = score == bestScore && score > 0
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "deadScale"
    )
    val pulse by rememberInfiniteTransition(label = "deadPulse")
        .animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(750), repeatMode = RepeatMode.Reverse),
            label = "deadPulse"
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.BgBottom.copy(alpha = 0.78f))
            .pointerInput(Unit) { detectTapGestures { onRestart() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .background(Palette.SurfaceCard, RoundedCornerShape(24.dp))
                .padding(horizontal = 44.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isNewBest) "NEW BEST" else "GAME OVER",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNewBest) Palette.Gold else Palette.Danger,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SCORE", fontSize = 10.sp, color = Palette.TextSecondary, letterSpacing = 2.sp)
                    Text("$score", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Palette.TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(64.dp)
                        .background(Palette.GridLine)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BEST", fontSize = 10.sp, color = Palette.TextSecondary, letterSpacing = 2.sp)
                    Text("$bestScore", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Palette.Gold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "TAP TO RETRY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Accent.copy(alpha = pulse),
                letterSpacing = 3.sp
            )
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val ct = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * ct,
        green = a.green + (b.green - a.green) * ct,
        blue = a.blue + (b.blue - a.blue) * ct,
        alpha = 1f
    )
}