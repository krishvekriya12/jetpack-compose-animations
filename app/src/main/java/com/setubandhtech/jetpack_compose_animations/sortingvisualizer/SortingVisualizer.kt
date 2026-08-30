package com.setubandhtech.jetpack_compose_animations.sortingvisualizer

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

data class SortArray(
    val name: String,
    val color: Color,
    var arr: List<Float>,
    var comparisons: Int = 0,
    var swaps: Int = 0,
    var isSorted: Boolean = false,
    var activeIndices: Set<Int> = emptySet(),
    var sortedIndices: Set<Int> = emptySet(),
    var timeMs: Long = 0L
)

val ALGORITHMS = listOf(
    "Bubble" to Color(0xFF00E5FF),
    "Quick" to Color(0xFFFF4081),
    "Merge" to Color(0xFF69F0AE),
    "Heap" to Color(0xFFFFD740),
    "Shell" to Color(0xFFEA80FC),
    "Insertion" to Color(0xFFFF6D00),
    "Selection" to Color(0xFF40C4FF),
    "Radix" to Color(0xFFB2FF59)
)

@Composable
fun SortingVisualizer() {
    val arraySize = 40
    var arrays by remember { mutableStateOf<List<SortArray>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    var isComplete by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

    fun generateArrays() {
        val base = (1..arraySize).map { it.toFloat() / arraySize }.shuffled()
        arrays = ALGORITHMS.map { (name, color) ->
            SortArray(name = name, color = color, arr = base.toList())
        }
        isComplete = false
        isRunning = false
        winner = ""
    }

    fun updateArray(index: Int, update: SortArray.() -> SortArray) {
        arrays = arrays.toMutableList().also { list ->
            list[index] = list[index].update()
        }
    }

    suspend fun delay() = kotlinx.coroutines.delay(18)

    suspend fun bubbleSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val n = arr.size
        val start = System.currentTimeMillis()
        for (i in 0 until n - 1) {
            for (j in 0 until n - i - 1) {
                updateArray(idx) { copy(activeIndices = setOf(j, j + 1)) }
                if (arr[j] > arr[j + 1]) {
                    val tmp = arr[j]; arr[j] = arr[j + 1]; arr[j + 1] = tmp
                    updateArray(idx) {
                        copy(arr = arr.toList(), swaps = swaps + 1, comparisons = comparisons + 1)
                    }
                    delay()
                } else {
                    updateArray(idx) { copy(comparisons = comparisons + 1) }
                }
            }
            updateArray(idx) { copy(sortedIndices = sortedIndices + (n - i - 1)) }
        }
        updateArray(idx) {
            copy(
                arr = arr.toList(),
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until n).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun quickSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val start = System.currentTimeMillis()

        suspend fun partition(low: Int, high: Int): Int {
            val pivot = arr[high]
            var i = low - 1
            for (j in low until high) {
                updateArray(idx) { copy(activeIndices = setOf(j, high)) }
                if (arr[j] <= pivot) {
                    i++
                    val tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp
                    updateArray(idx) {
                        copy(arr = arr.toList(), swaps = swaps + 1, comparisons = comparisons + 1)
                    }
                    delay()
                } else {
                    updateArray(idx) { copy(comparisons = comparisons + 1) }
                }
            }
            val tmp = arr[i + 1]; arr[i + 1] = arr[high]; arr[high] = tmp
            updateArray(idx) { copy(arr = arr.toList(), swaps = swaps + 1) }
            return i + 1
        }

        suspend fun sort(low: Int, high: Int) {
            if (low < high) {
                val pi = partition(low, high)
                updateArray(idx) { copy(sortedIndices = sortedIndices + pi) }
                sort(low, pi - 1)
                sort(pi + 1, high)
            }
        }

        sort(0, arr.size - 1)
        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until arr.size).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun mergeSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val start = System.currentTimeMillis()

        suspend fun merge(left: Int, mid: Int, right: Int) {
            val leftArr = arr.subList(left, mid + 1).toMutableList()
            val rightArr = arr.subList(mid + 1, right + 1).toMutableList()
            var i = 0; var j = 0; var k = left
            while (i < leftArr.size && j < rightArr.size) {
                updateArray(idx) { copy(activeIndices = setOf(k), comparisons = comparisons + 1) }
                if (leftArr[i] <= rightArr[j]) {
                    arr[k++] = leftArr[i++]
                } else {
                    arr[k++] = rightArr[j++]
                }
                updateArray(idx) { copy(arr = arr.toList(), swaps = swaps + 1) }
                delay()
            }
            while (i < leftArr.size) { arr[k++] = leftArr[i++] }
            while (j < rightArr.size) { arr[k++] = rightArr[j++] }
            updateArray(idx) { copy(arr = arr.toList()) }
        }

        suspend fun sort(left: Int, right: Int) {
            if (left < right) {
                val mid = (left + right) / 2
                sort(left, mid)
                sort(mid + 1, right)
                merge(left, mid, right)
            }
        }

        sort(0, arr.size - 1)
        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until arr.size).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun heapSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val n = arr.size
        val start = System.currentTimeMillis()

        suspend fun heapify(n: Int, i: Int) {
            var largest = i
            val l = 2 * i + 1
            val r = 2 * i + 2
            updateArray(idx) { copy(activeIndices = setOf(i, l, r), comparisons = comparisons + 1) }
            if (l < n && arr[l] > arr[largest]) largest = l
            if (r < n && arr[r] > arr[largest]) largest = r
            if (largest != i) {
                val tmp = arr[i]; arr[i] = arr[largest]; arr[largest] = tmp
                updateArray(idx) { copy(arr = arr.toList(), swaps = swaps + 1) }
                delay()
                heapify(n, largest)
            }
        }

        for (i in n / 2 - 1 downTo 0) heapify(n, i)
        for (i in n - 1 downTo 1) {
            val tmp = arr[0]; arr[0] = arr[i]; arr[i] = tmp
            updateArray(idx) {
                copy(
                    arr = arr.toList(),
                    swaps = swaps + 1,
                    sortedIndices = sortedIndices + i
                )
            }
            delay()
            heapify(i, 0)
        }
        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until n).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun shellSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val n = arr.size
        val start = System.currentTimeMillis()
        var gap = n / 2
        while (gap > 0) {
            for (i in gap until n) {
                val temp = arr[i]
                var j = i
                updateArray(idx) { copy(activeIndices = setOf(i, j), comparisons = comparisons + 1) }
                while (j >= gap && arr[j - gap] > temp) {
                    arr[j] = arr[j - gap]
                    j -= gap
                    updateArray(idx) { copy(arr = arr.toList(), swaps = swaps + 1) }
                    delay()
                }
                arr[j] = temp
                updateArray(idx) { copy(arr = arr.toList()) }
            }
            gap /= 2
        }
        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until n).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun insertionSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val n = arr.size
        val start = System.currentTimeMillis()
        for (i in 1 until n) {
            val key = arr[i]
            var j = i - 1
            updateArray(idx) { copy(activeIndices = setOf(i), comparisons = comparisons + 1) }
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j]
                j--
                updateArray(idx) { copy(arr = arr.toList(), swaps = swaps + 1) }
                delay()
            }
            arr[j + 1] = key
            updateArray(idx) {
                copy(
                    arr = arr.toList(),
                    sortedIndices = sortedIndices + i
                )
            }
        }
        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until n).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun selectionSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val n = arr.size
        val start = System.currentTimeMillis()
        for (i in 0 until n - 1) {
            var minIdx = i
            for (j in i + 1 until n) {
                updateArray(idx) { copy(activeIndices = setOf(minIdx, j), comparisons = comparisons + 1) }
                if (arr[j] < arr[minIdx]) minIdx = j
                delay()
            }
            val tmp = arr[minIdx]; arr[minIdx] = arr[i]; arr[i] = tmp
            updateArray(idx) {
                copy(
                    arr = arr.toList(),
                    swaps = swaps + 1,
                    sortedIndices = sortedIndices + i
                )
            }
        }
        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until n).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    suspend fun radixSort(idx: Int) {
        val arr = arrays[idx].arr.toMutableList()
        val n = arr.size
        val start = System.currentTimeMillis()
        val scale = 1000
        val intArr = arr.map { (it * scale).toInt() }.toMutableList()

        val max = intArr.max()
        var exp = 1

        while (max / exp > 0) {
            val output = MutableList(n) { 0 }
            val count = MutableList(10) { 0 }

            for (i in 0 until n) {
                val digit = (intArr[i] / exp) % 10
                count[digit]++
                updateArray(idx) { copy(activeIndices = setOf(i), comparisons = comparisons + 1) }
            }

            for (i in 1 until 10) count[i] += count[i - 1]

            for (i in n - 1 downTo 0) {
                val digit = (intArr[i] / exp) % 10
                output[count[digit] - 1] = intArr[i]
                count[digit]--
            }

            for (i in 0 until n) {
                intArr[i] = output[i]
                arr[i] = intArr[i].toFloat() / scale
                updateArray(idx) { copy(arr = arr.toList(), swaps = swaps + 1) }
                delay()
            }

            exp *= 10
        }

        updateArray(idx) {
            copy(
                isSorted = true,
                activeIndices = emptySet(),
                sortedIndices = (0 until n).toSet(),
                timeMs = System.currentTimeMillis() - start
            )
        }
    }

    fun startSorting() {
        if (arrays.isEmpty()) generateArrays()
        isRunning = true
        isComplete = false
        winner = ""

        scope.launch {
            val jobs = listOf(
                launch { bubbleSort(0) },
                launch { quickSort(1) },
                launch { mergeSort(2) },
                launch { heapSort(3) },
                launch { shellSort(4) },
                launch { insertionSort(5) },
                launch { selectionSort(6) },
                launch { radixSort(7) }
            )
            launch {
                while (jobs.any { it.isActive }) {
                    val sorted = arrays.filter { it.isSorted }
                    if (sorted.isNotEmpty() && winner.isEmpty()) {
                        winner = sorted.minByOrNull { it.timeMs }?.name ?: ""
                    }
                    delay(50)
                }
                isComplete = true
                isRunning = false
            }
        }
    }

    LaunchedEffect(Unit) { generateArrays() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020209))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A1A))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ Sorting Race",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1A2E))
                            .then(
                                if (!isRunning) Modifier.pointerInput(Unit) {
                                    detectTapGestures { generateArrays() }
                                } else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 12.sp,
                            color = if (!isRunning) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (!isRunning) Color(0xFF6200EE)
                                else Color(0xFF1A1A2E)
                            )
                            .then(
                                if (!isRunning) Modifier.pointerInput(Unit) {
                                    detectTapGestures { startSorting() }
                                } else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isRunning) "Running..." else "▶ Start Race",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(
                                alpha = if (isRunning) 0.5f else 1f
                            )
                        )
                    }
                }
            }

            if (winner.isNotEmpty()) {
                val winnerColor = ALGORITHMS.find { it.first == winner }?.second ?: Color.White
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(winnerColor.copy(alpha = 0.15f))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏆 $winner wins!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = winnerColor
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (arrays.isEmpty()) return@Canvas

                val cols = 2
                val rows = 4
                val cellW = size.width / cols
                val cellH = size.height / rows
                val padding = 8f

                arrays.forEachIndexed { index, sortArr ->
                    val col = index % cols
                    val row = index / cols
                    val offsetX = col * cellW + padding
                    val offsetY = row * cellH + padding
                    val drawW = cellW - padding * 2f
                    val drawH = cellH - padding * 2f

                    drawSortPanel(
                        sortArr = sortArr,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        width = drawW,
                        height = drawH,
                        isWinner = winner == sortArr.name,
                        pulse = pulse
                    )
                }
            }
        }
    }
}

fun DrawScope.drawSortPanel(
    sortArr: SortArray,
    offsetX: Float,
    offsetY: Float,
    width: Float,
    height: Float,
    isWinner: Boolean,
    pulse: Float
) {
    val n = sortArr.arr.size
    val barW = width / n
    val headerH = 40f
    val barAreaH = height - headerH - 30f

    drawRoundRect(
        color = Color(0xFF0A0A1A),
        topLeft = Offset(offsetX, offsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f)
    )

    if (isWinner) {
        drawRoundRect(
            color = sortArr.color.copy(alpha = 0.3f * pulse),
            topLeft = Offset(offsetX - 3f, offsetY - 3f),
            size = Size(width + 6f, height + 6f),
            cornerRadius = CornerRadius(10f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }

    // Algorithm name
    drawContext.canvas.nativeCanvas.apply {
        val namePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                255,
                (sortArr.color.red * 255).toInt(),
                (sortArr.color.green * 255).toInt(),
                (sortArr.color.blue * 255).toInt()
            )
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        drawText(sortArr.name, offsetX + 8f, offsetY + 26f, namePaint)

        // Stats
        val statPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(150, 200, 200, 200)
            textSize = 16f
        }
        val statsText = "C:${sortArr.comparisons} S:${sortArr.swaps}"
        drawText(statsText, offsetX + width * 0.35f, offsetY + 26f, statPaint)

        // Time
        if (sortArr.isSorted) {
            val timePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    200,
                    (sortArr.color.red * 255).toInt(),
                    (sortArr.color.green * 255).toInt(),
                    (sortArr.color.blue * 255).toInt()
                )
                textSize = 16f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawText("${sortArr.timeMs}ms ✓", offsetX + width - 8f, offsetY + 26f, timePaint)
        }
    }

    // Bars
    sortArr.arr.forEachIndexed { i, value ->
        val barH = value * barAreaH
        val barX = offsetX + i * barW
        val barY = offsetY + headerH + (barAreaH - barH)

        val isActive = i in sortArr.activeIndices
        val isSorted = i in sortArr.sortedIndices || sortArr.isSorted

        val barColor = when {
            isActive -> Color.White
            isSorted -> sortArr.color
            else -> sortArr.color.copy(alpha = 0.4f)
        }

        // Glow for active
        if (isActive) {
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(barX, barY - 4f),
                size = Size(barW - 1f, barH + 4f)
            )
        }

        drawRect(
            color = barColor,
            topLeft = Offset(barX + 0.5f, barY),
            size = Size(barW - 1f, barH)
        )
    }

    // Sorted checkmark
    if (sortArr.isSorted) {
        drawContext.canvas.nativeCanvas.apply {
            val checkPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    200,
                    (sortArr.color.red * 255).toInt(),
                    (sortArr.color.green * 255).toInt(),
                    (sortArr.color.blue * 255).toInt()
                )
                textSize = 18f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText("SORTED ✓", offsetX + width / 2f, offsetY + height - 8f, checkPaint)
        }
    }
}