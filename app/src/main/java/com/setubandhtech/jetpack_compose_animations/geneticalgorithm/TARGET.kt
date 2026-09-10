package com.setubandhtech.jetpack_compose_animations.geneticalgorithm

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

private const val TARGET = "JETPACK COMPOSE"
private const val POPULATION_SIZE = 200
private const val MUTATION_RATE = 0.015f
private const val ELITE_COUNT = 2
private const val TOURNAMENT_SIZE = 5
private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ "

data class DNA(
    var genes: CharArray,
    var fitness: Float = 0f
) {
    fun phrase() = String(genes)

    fun calculateFitness(target: String) {
        var score = 0
        genes.forEachIndexed { i, c ->
            if (i < target.length && c == target[i]) score++
        }
        fitness = (score.toFloat() / target.length).pow(2)
    }

    fun crossover(partner: DNA): DNA {
        val childGenes = CharArray(genes.size)
        val midpoint = Random.nextInt(genes.size)
        for (i in genes.indices) {
            childGenes[i] = if (i > midpoint) genes[i] else partner.genes[i]
        }
        return DNA(childGenes)
    }

    fun mutate(rate: Float) {
        for (i in genes.indices) {
            if (Random.nextFloat() < rate) {
                genes[i] = CHARS.random()
            }
        }
    }

    fun copy(): DNA = DNA(genes.copyOf(), fitness)
}

data class EvolutionStats(
    val generation: Int = 0,
    val bestFitness: Float = 0f,
    val avgFitness: Float = 0f,
    val diversity: Float = 0f,
    val bestPhrase: String = "",
    val perfectFound: Boolean = false,
    val genPerSecond: Float = 0f
)

private fun tournamentSelect(pool: List<DNA>, size: Int): DNA {
    var best = pool.random()
    repeat(size - 1) {
        val challenger = pool.random()
        if (challenger.fitness > best.fitness) best = challenger
    }
    return best
}

private fun calculateDiversity(population: List<DNA>): Float {
    val unique = population.map { it.phrase() }.toSet().size
    return unique.toFloat() / population.size
}

@Composable
fun GeneticAlgorithm() {
    var population by remember { mutableStateOf<List<DNA>>(emptyList()) }
    var stats by remember { mutableStateOf(EvolutionStats()) }
    var isRunning by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1) } // 1x, 2x, 5x
    var fitnessHistory by remember { mutableStateOf<List<Float>>(emptyList()) }
    var startTimeMs by remember { mutableStateOf(0L) }

    val glowPulse by rememberInfiniteTransition(label = "glow")
        .animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )

    val animatedFitness by animateFloatAsState(
        targetValue = stats.bestFitness,
        animationSpec = tween(200),
        label = "fitness"
    )

    fun randomDNA() = DNA(CharArray(TARGET.length) { CHARS.random() })

    fun initPopulation() {
        population = List(POPULATION_SIZE) { randomDNA() }
        stats = EvolutionStats()
        fitnessHistory = emptyList()
        startTimeMs = System.currentTimeMillis()
    }

    fun evolveGeneration() {
        population.forEach { it.calculateFitness(TARGET) }

        val sorted = population.sortedByDescending { it.fitness }
        val best = sorted.first()
        val avgFitness = population.map { it.fitness }.average().toFloat()
        val diversity = calculateDiversity(population)
        val perfectFound = best.phrase() == TARGET

        val elapsedSec = (System.currentTimeMillis() - startTimeMs) / 1000f
        val genPerSecond = if (elapsedSec > 0) (stats.generation + 1) / elapsedSec else 0f

        stats = stats.copy(
            generation = stats.generation + 1,
            bestFitness = best.fitness,
            avgFitness = avgFitness,
            diversity = diversity,
            bestPhrase = best.phrase(),
            perfectFound = perfectFound,
            genPerSecond = genPerSecond
        )

        fitnessHistory = (fitnessHistory + best.fitness).takeLast(100)

        if (perfectFound) {
            isRunning = false
            return
        }

        val newPopulation = mutableListOf<DNA>()
        repeat(ELITE_COUNT) { newPopulation.add(sorted[it].copy()) }

        while (newPopulation.size < POPULATION_SIZE) {
            val parentA = tournamentSelect(population, TOURNAMENT_SIZE)
            val parentB = tournamentSelect(population, TOURNAMENT_SIZE)
            val child = parentA.crossover(parentB)
            child.mutate(MUTATION_RATE)
            newPopulation.add(child)
        }

        population = newPopulation
    }

    LaunchedEffect(Unit) { initPopulation() }

    LaunchedEffect(isRunning, speed) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning && !stats.perfectFound) {
            repeat(speed) {
                if (isRunning && !stats.perfectFound) evolveGeneration()
            }
            delay(if (speed == 1) 80L else if (speed == 2) 40L else 8L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF050510), Color(0xFF0A0A1F))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    text = "🧬 Genetic Algorithm",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF69F0AE)
                )
                Text(
                    text = "Target: \"$TARGET\"",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Best match card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0A0A1A))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "BEST MATCH",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        stats.bestPhrase.padEnd(TARGET.length).forEachIndexed { i, c ->
                            val isCorrect = i < TARGET.length && c == TARGET[i]
                            val charColor by animateColorAsState(
                                targetValue = if (isCorrect)
                                    Color(0xFF69F0AE).copy(alpha = glowPulse)
                                else Color.White.copy(alpha = 0.3f),
                                label = "charColor"
                            )
                            Text(
                                text = c.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = charColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFitness)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF00E5FF), Color(0xFF69F0AE))
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid — now with diversity + speed, not just fitness
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(Modifier.weight(1f), "GENERATION", "${stats.generation}", Color(0xFF00E5FF))
                StatBox(Modifier.weight(1f), "BEST FIT", "${(stats.bestFitness * 100).toInt()}%", Color(0xFF69F0AE))
                StatBox(Modifier.weight(1f), "DIVERSITY", "${(stats.diversity * 100).toInt()}%", Color(0xFFFFD740))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBox(Modifier.weight(1f), "AVG FIT", "${(stats.avgFitness * 100).toInt()}%", Color(0xFFFF4081))
                StatBox(Modifier.weight(1f), "SPEED", "${"%.1f".format(stats.genPerSecond)}/s", Color(0xFF00E5FF))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FITNESS OVER TIME",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(80.dp)
                    .padding(top = 8.dp)
            ) {
                if (fitnessHistory.size < 2) return@Canvas

                val path = Path()
                val stepX = size.width / (fitnessHistory.size - 1).coerceAtLeast(1)

                fitnessHistory.forEachIndexed { i, f ->
                    val x = i * stepX
                    val y = size.height - (f * size.height)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF69F0AE).copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = Color(0xFF69F0AE),
                    style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TOP CANDIDATES",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val topCandidates = population.sortedByDescending { it.fitness }.take(8)
                topCandidates.forEach { dna ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0A1A))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f)) {
                            dna.phrase().padEnd(TARGET.length).forEachIndexed { i, c ->
                                val isCorrect = i < TARGET.length && c == TARGET[i]
                                Text(
                                    text = c.toString(),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isCorrect) Color(0xFF69F0AE)
                                    else Color.White.copy(alpha = 0.35f)
                                )
                            }
                        }
                        Text(
                            text = "${(dna.fitness * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                stats.perfectFound -> Color(0xFF69F0AE).copy(alpha = 0.2f)
                                isRunning -> Color(0xFFFF4081).copy(alpha = 0.2f)
                                else -> Color(0xFF6200EE)
                            }
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { if (!stats.perfectFound) isRunning = !isRunning }
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            stats.perfectFound -> "🎉 SOLVED!"
                            isRunning -> "⏸ PAUSE"
                            else -> "▶ EVOLVE"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A2E))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                speed = when (speed) { 1 -> 2; 2 -> 5; else -> 1 }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "${speed}x", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD740))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A2E))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                isRunning = false
                                initPopulation()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↻", fontSize = 18.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatBox(modifier: Modifier = Modifier, label: String, value: String, color: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0A1A))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}