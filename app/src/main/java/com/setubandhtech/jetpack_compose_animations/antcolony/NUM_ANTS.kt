package com.setubandhtech.jetpack_compose_animations.antcolony

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import kotlin.random.Random

private const val NUM_ANTS = 200
private const val GRID_SIZE = 8f
private const val EVAPORATION = 0.995f
private const val ANT_SPEED = 2.2f
private const val SENSOR_ANGLE = 0.5f
private const val SENSOR_DIST = 14f
private const val TURN_SPEED = 0.3f

data class Ant(
    var x: Float,
    var y: Float,
    var angle: Float,
    var hasFood: Boolean = false
)

data class FoodSource(var x: Float, var y: Float, var amount: Float = 200f)

@Composable
fun AntColony() {
    var screenW by remember { mutableStateOf(0f) }
    var screenH by remember { mutableStateOf(0f) }
    var gridCols by remember { mutableStateOf(0) }
    var gridRows by remember { mutableStateOf(0) }

    val ants = remember { mutableStateListOf<Ant>() }
    var toHomeTrail by remember { mutableStateOf(FloatArray(0)) }
    var toFoodTrail by remember { mutableStateOf(FloatArray(0)) }
    var foods by remember { mutableStateOf<List<FoodSource>>(emptyList()) }
    var nestPos by remember { mutableStateOf(Offset.Zero) }
    var totalFoodCollected by remember { mutableStateOf(0) }
    var tick by remember { mutableStateOf(0L) }

    val nestPulse by rememberInfiniteTransition(label = "nest")
        .animateFloat(
            initialValue = 0.8f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nest_pulse"
        )

    fun initColony(w: Float, h: Float) {
        gridCols = (w / GRID_SIZE).toInt() + 1
        gridRows = (h / GRID_SIZE).toInt() + 1
        toHomeTrail = FloatArray(gridCols * gridRows)
        toFoodTrail = FloatArray(gridCols * gridRows)
        nestPos = Offset(w / 2f, h / 2f)

        ants.clear()
        repeat(NUM_ANTS) {
            ants.add(
                Ant(
                    x = nestPos.x,
                    y = nestPos.y,
                    angle = Random.nextFloat() * 2f * PI.toFloat()
                )
            )
        }

        foods = listOf(
            FoodSource(w * 0.18f, h * 0.22f),
            FoodSource(w * 0.82f, h * 0.25f),
            FoodSource(w * 0.2f, h * 0.78f),
            FoodSource(w * 0.8f, h * 0.75f)
        )
        totalFoodCollected = 0
    }

    fun trailIndex(x: Float, y: Float): Int {
        val gx = (x / GRID_SIZE).toInt().coerceIn(0, gridCols - 1)
        val gy = (y / GRID_SIZE).toInt().coerceIn(0, gridRows - 1)
        return gy * gridCols + gx
    }

    fun sampleTrail(trail: FloatArray, x: Float, y: Float, angle: Float, sensorAngle: Float): Float {
        val sx = x + cos(angle + sensorAngle) * SENSOR_DIST
        val sy = y + sin(angle + sensorAngle) * SENSOR_DIST
        if (sx < 0 || sx >= screenW || sy < 0 || sy >= screenH) return 0f
        val idx = trailIndex(sx, sy)
        return if (idx in trail.indices) trail[idx] else 0f
    }

    LaunchedEffect(screenW, screenH) {
        if (screenW == 0f || screenH == 0f) return@LaunchedEffect
        initColony(screenW, screenH)

        while (true) {
            kotlinx.coroutines.android.awaitFrame()
            tick++

            for (i in ants.indices) {
                val ant = ants[i]

                val followTrail = if (ant.hasFood) toHomeTrail else toFoodTrail

                val left = sampleTrail(followTrail, ant.x, ant.y, ant.angle, -SENSOR_ANGLE)
                val center = sampleTrail(followTrail, ant.x, ant.y, ant.angle, 0f)
                val right = sampleTrail(followTrail, ant.x, ant.y, ant.angle, SENSOR_ANGLE)

                var newAngle = ant.angle
                when {
                    center > left && center > right -> {

                    }
                    left > right -> newAngle -= TURN_SPEED
                    right > left -> newAngle += TURN_SPEED
                    else -> newAngle += (Random.nextFloat() - 0.5f) * TURN_SPEED
                }

                newAngle += (Random.nextFloat() - 0.5f) * 0.15f

                var newX = ant.x + cos(newAngle) * ANT_SPEED
                var newY = ant.y + sin(newAngle) * ANT_SPEED

                if (newX < 5f || newX > screenW - 5f) {
                    newAngle = PI.toFloat() - newAngle
                    newX = ant.x.coerceIn(5f, screenW - 5f)
                }
                if (newY < 5f || newY > screenH - 5f) {
                    newAngle = -newAngle
                    newY = ant.y.coerceIn(5f, screenH - 5f)
                }

                var hasFood = ant.hasFood

                if (!hasFood) {
                    foods.forEach { food ->
                        val dx = newX - food.x
                        val dy = newY - food.y
                        if (dx * dx + dy * dy < 20f * 20f && food.amount > 0f) {
                            hasFood = true
                            food.amount -= 1f
                            newAngle += PI.toFloat()
                        }
                    }
                }

                if (hasFood) {
                    val dx = newX - nestPos.x
                    val dy = newY - nestPos.y
                    if (dx * dx + dy * dy < 25f * 25f) {
                        hasFood = false
                        totalFoodCollected++
                        newAngle += PI.toFloat()
                    }
                }

                val idx = trailIndex(newX, newY)
                if (idx in toHomeTrail.indices) {
                    if (hasFood) {
                        toFoodTrail[idx] = (toFoodTrail[idx] + 3f).coerceAtMost(255f)
                    } else {
                        toHomeTrail[idx] = (toHomeTrail[idx] + 3f).coerceAtMost(255f)
                    }
                }

                ants[i] = ant.copy(x = newX, y = newY, angle = newAngle, hasFood = hasFood)
            }

            if (tick % 2 == 0L) {
                for (i in toHomeTrail.indices) {
                    toHomeTrail[i] *= EVAPORATION
                    toFoodTrail[i] *= EVAPORATION
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0805))
            .onSizeChanged {
                screenW = it.width.toFloat()
                screenH = it.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    foods = foods + FoodSource(offset.x, offset.y)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (gridCols == 0) return@Canvas

            for (gy in 0 until gridRows) {
                for (gx in 0 until gridCols) {
                    val idx = gy * gridCols + gx
                    val foodStrength = toFoodTrail.getOrElse(idx) { 0f }
                    val homeStrength = toHomeTrail.getOrElse(idx) { 0f }

                    if (foodStrength > 2f) {
                        drawRect(
                            color = Color(0xFF00E676).copy(
                                alpha = (foodStrength / 255f).coerceIn(0f, 0.7f)
                            ),
                            topLeft = Offset(gx * GRID_SIZE, gy * GRID_SIZE),
                            size = Size(GRID_SIZE, GRID_SIZE)
                        )
                    }
                    if (homeStrength > 2f) {
                        drawRect(
                            color = Color(0xFFFF6D00).copy(
                                alpha = (homeStrength / 255f).coerceIn(0f, 0.5f)
                            ),
                            topLeft = Offset(gx * GRID_SIZE, gy * GRID_SIZE),
                            size = Size(GRID_SIZE, GRID_SIZE)
                        )
                    }
                }
            }

            drawCircle(
                color = Color(0xFF6D4C41).copy(alpha = 0.3f * nestPulse),
                radius = 45f * nestPulse,
                center = nestPos
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8D6E63), Color(0xFF5D4037)),
                    center = nestPos,
                    radius = 25f
                ),
                radius = 25f,
                center = nestPos
            )
            drawCircle(
                color = Color(0xFF3E2723),
                radius = 12f,
                center = nestPos
            )

            foods.forEach { food ->
                if (food.amount > 0f) {
                    val scale = (food.amount / 200f).coerceIn(0.3f, 1f)
                    drawCircle(
                        color = Color(0xFF69F0AE).copy(alpha = 0.25f),
                        radius = 22f * scale,
                        center = Offset(food.x, food.y)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFB9F6CA), Color(0xFF00C853)),
                            center = Offset(food.x, food.y),
                            radius = 14f * scale
                        ),
                        radius = 14f * scale,
                        center = Offset(food.x, food.y)
                    )
                }
            }

            ants.forEach { ant ->
                val color = if (ant.hasFood) Color(0xFF00E676) else Color(0xFFFFAB40)

                rotate(degrees = Math.toDegrees(ant.angle.toDouble()).toFloat(), pivot = Offset(ant.x, ant.y)) {
                    drawOval(
                        color = color,
                        topLeft = Offset(ant.x - 3f, ant.y - 1.5f),
                        size = Size(6f, 3f)
                    )
                    drawCircle(
                        color = color,
                        radius = 1.5f,
                        center = Offset(ant.x + 3f, ant.y)
                    )
                }

                if (ant.hasFood) {
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.4f),
                        radius = 4f,
                        center = Offset(ant.x, ant.y)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1208).copy(alpha = 0.85f))
                .padding(14.dp)
        ) {
            Text(
                text = "🐜 Ant Colony",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFAB40)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("Ants: $NUM_ANTS", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            Text("Food collected: $totalFoodCollected", fontSize = 11.sp, color = Color(0xFF00E676))
            Text("Food sources: ${foods.count { it.amount > 0f }}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap anywhere to add food 🟢",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = "🟠 Home trail  •  🟢 Food trail",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}