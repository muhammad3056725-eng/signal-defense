package com.example.signaldefense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SignalDefenseGame()
            }
        }
    }
}

private data class GridPos(val x: Int, val y: Int)

private enum class EnemyType(
    val hp: Int,
    val speed: Float,
    val armor: Int,
    val reward: Int,
    val color: Color
) {
    DRONE(16, 1.0f, 0, 8, Color(0xFF7EDAD0)),
    HAULER(36, 0.55f, 3, 14, Color(0xFFB892FF)),
    SPRINTER(12, 1.7f, 0, 10, Color(0xFFFFB74D)),
    WARDEN(80, 0.48f, 4, 24, Color(0xFFEF5350))
}

private enum class TowerType(
    val label: String,
    val cost: Int,
    val damage: Int,
    val range: Float,
    val fireRateMs: Long,
    val color: Color
) {
    PULSE("Pulse", 30, 9, 2.2f, 650L, Color(0xFF66B3FF)),
    FLAK("Flak", 45, 14, 1.8f, 1100L, Color(0xFFFFD166)),
    FROST("Frost", 40, 6, 2.3f, 900L, Color(0xFF90CAF9)),
    RAIL("Rail", 60, 22, 3.2f, 1500L, Color(0xFFB39DDB))
}

private data class Tower(
    val x: Int,
    val y: Int,
    val type: TowerType,
    val cooldownMs: Long = 0L
)

private data class Enemy(
    val id: Int,
    val type: EnemyType,
    val hp: Int,
    val speed: Float,
    val armor: Int,
    val reward: Int,
    val progress: Float = 0f
)

private data class GameState(
    val currency: Int = 150,
    val coreHp: Int = 20,
    val towers: List<Tower> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val waveQueue: List<EnemyType> = buildWave(1),
    val waveIndex: Int = 1,
    val totalWaves: Int = 5,
    val spawnAccumulatorMs: Long = 0L,
    val nextEnemyId: Int = 1,
    val selectedTower: TowerType = TowerType.PULSE,
    val status: String = "playing",
    val message: String = "Wave 1 incoming"
)

private val GRID_WIDTH = 8
private val GRID_HEIGHT = 12
private val PATH_POINTS = listOf(
    GridPos(0, 3), GridPos(1, 3), GridPos(1, 6), GridPos(2, 6),
    GridPos(2, 8), GridPos(3, 8), GridPos(3, 4), GridPos(4, 4),
    GridPos(4, 9), GridPos(5, 9), GridPos(5, 2), GridPos(6, 2),
    GridPos(6, 10), GridPos(7, 10)
)

private fun buildWave(waveNumber: Int): List<EnemyType> {
    return when (waveNumber) {
        1 -> listOf(EnemyType.DRONE, EnemyType.DRONE, EnemyType.DRONE, EnemyType.SPRINTER)
        2 -> listOf(EnemyType.DRONE, EnemyType.HAULER, EnemyType.DRONE, EnemyType.SPRINTER, EnemyType.DRONE)
        3 -> listOf(EnemyType.SPRINTER, EnemyType.DRONE, EnemyType.HAULER, EnemyType.SPRINTER, EnemyType.DRONE)
        4 -> listOf(EnemyType.WARDEN, EnemyType.DRONE, EnemyType.SPRINTER, EnemyType.HAULER, EnemyType.DRONE)
        else -> listOf(EnemyType.DRONE, EnemyType.SPRINTER, EnemyType.DRONE, EnemyType.HAULER, EnemyType.WARDEN)
    }
}

private val BUILDABLE_TILES = buildSet {
    for (y in 0 until GRID_HEIGHT) {
        for (x in 0 until GRID_WIDTH) {
            if (PATH_POINTS.none { it.x == x && it.y == y }) {
                add(GridPos(x, y))
            }
        }
    }
}

private fun isTileBuildable(x: Int, y: Int): Boolean {
    return BUILDABLE_TILES.contains(GridPos(x, y))
}

private fun createEnemy(type: EnemyType, id: Int): Enemy {
    return Enemy(
        id = id,
        type = type,
        hp = type.hp,
        speed = type.speed,
        armor = type.armor,
        reward = type.reward,
        progress = 0f
    )
}

private fun enemyPosition(enemy: Enemy): Offset {
    val totalSegments = PATH_POINTS.size - 1
    val progress = enemy.progress.coerceIn(0f, totalSegments.toFloat())
    val segmentIndex = progress.toInt()
    val segmentProgress = progress - segmentIndex
    val from = PATH_POINTS[segmentIndex]
    val to = PATH_POINTS[min(segmentIndex + 1, PATH_POINTS.size - 1)]

    val x = from.x + (to.x - from.x) * segmentProgress
    val y = from.y + (to.y - from.y) * segmentProgress
    return Offset(x.toFloat(), y.toFloat())
}

private fun enemyDistanceToTile(enemy: Enemy, x: Int, y: Int): Float {
    val pos = enemyPosition(enemy)
    val centerX = x + 0.5f
    val centerY = y + 0.5f
    return hypot(pos.x - centerX, pos.y - centerY)
}

private fun advanceGame(state: GameState, deltaMs: Long): GameState {
    if (state.status != "playing") return state

    var current = state
    var enemyList = current.enemies.toMutableList()
    var nextWaveQueue = current.waveQueue.toMutableList()
    var nextEnemyId = current.nextEnemyId
    var spawnAccumulator = current.spawnAccumulatorMs + deltaMs
    val spawnIntervalMs = 700L

    while (spawnAccumulator >= spawnIntervalMs && nextWaveQueue.isNotEmpty()) {
        val enemyType = nextWaveQueue.removeAt(0)
        enemyList.add(createEnemy(enemyType, nextEnemyId))
        nextEnemyId += 1
        spawnAccumulator -= spawnIntervalMs
    }

    // If no more enemies in queue and all enemies gone, move to next wave or victory.
    if (nextWaveQueue.isEmpty() && enemyList.isEmpty() && current.waveIndex >= current.totalWaves) {
        return current.copy(status = "victory", message = "All waves cleared. Core saved!")
    }

    if (nextWaveQueue.isEmpty() && enemyList.isEmpty()) {
        val nextWave = current.waveIndex + 1
        return current.copy(
            waveIndex = nextWave,
            waveQueue = buildWave(nextWave),
            spawnAccumulatorMs = 0L,
            message = "Wave $nextWave incoming"
        )
    }

    var coreHp = current.coreHp
    val movedEnemies = mutableListOf<Enemy>()

    for (enemy in enemyList) {
        val nextProgress = enemy.progress + (enemy.speed * deltaMs / 1000f)
        if (nextProgress >= (PATH_POINTS.size - 1).toFloat()) {
            coreHp = max(0, coreHp - 1)
        } else {
            movedEnemies.add(enemy.copy(progress = nextProgress))
        }
    }

    if (coreHp <= 0) {
        return current.copy(
            coreHp = 0,
            enemies = emptyList(),
            status = "defeat",
            message = "Core destroyed. Retry?"
        )
    }

    var currency = current.currency
    val updatedTowers = current.towers.map { tower ->
        var towerCopy = tower
        if (towerCopy.cooldownMs > 0) {
            towerCopy = tower.copy(cooldownMs = max(0L, towerCopy.cooldownMs - deltaMs))
            return@map towerCopy
        }

        val target = movedEnemies.filter { enemy ->
            enemyDistanceToTile(enemy, tower.x, tower.y) <= tower.type.range
        }.minByOrNull { enemy -> enemy.progress }

        if (target == null) {
            return@map tower.copy(cooldownMs = 0L)
        }

        val damage = max(1, tower.type.damage - target.armor)
        val targetIndex = movedEnemies.indexOfFirst { it.id == target.id }
        if (targetIndex >= 0) {
            val targetEnemy = movedEnemies[targetIndex]
            val remainingHp = targetEnemy.hp - damage
            if (remainingHp <= 0) {
                currency += targetEnemy.reward
                movedEnemies.removeAt(targetIndex)
            } else {
                movedEnemies[targetIndex] = targetEnemy.copy(hp = remainingHp)
            }
        }

        tower.copy(cooldownMs = tower.type.fireRateMs)
    }

    val finalEnemies = movedEnemies.toList()
    if (nextWaveQueue.isEmpty() && finalEnemies.isEmpty() && current.waveIndex >= current.totalWaves) {
        return current.copy(
            currency = currency,
            coreHp = coreHp,
            towers = updatedTowers,
            enemies = emptyList(),
            status = "victory",
            message = "Victory! All waves cleared."
        )
    }

    return current.copy(
        currency = currency,
        coreHp = coreHp,
        towers = updatedTowers,
        enemies = finalEnemies,
        waveQueue = nextWaveQueue,
        spawnAccumulatorMs = spawnAccumulator,
        nextEnemyId = nextEnemyId,
        message = if (coreHp <= 6) "Core critical!" else "Defend the core."
    )
}

@Composable
private fun SignalDefenseGame() {
    val state = remember { mutableStateOf(GameState()) }

    LaunchedEffect(state.value.status) {
        if (state.value.status == "playing") {
            while (isActive) {
                delay(16)
                state.value = advanceGame(state.value, 16L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2A))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopHud(state.value)
        GameBoard(state)
        TowerBar(state)
    }
}

@Composable
private fun TopHud(gameState: GameState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatChip("Core", gameState.coreHp.toString(), Color(0xFFE57373))
        StatChip("Bits", gameState.currency.toString(), Color(0xFF7EDAD0))
        StatChip("Wave", "${gameState.waveIndex}/${gameState.totalWaves}", Color(0xFFFFD166))
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Surface(
        color = Color(0xFF12314A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label: $value",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun GameBoard(state: MutableState<GameState>) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF0A2338), RoundedCornerShape(18.dp))
            .padding(8.dp)
            .onSizeChanged { boardSize = it }
            .pointerInput(state.value) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed && !change.previousPressed) {
                            val current = state.value
                            if (current.status != "playing") return@awaitPointerEventScope

                            val x = change.position.x
                            val y = change.position.y
                            if (boardSize.width > 0 && boardSize.height > 0) {
                                val cellX = ((x / boardSize.width.toFloat()) * GRID_WIDTH).toInt().coerceIn(0, GRID_WIDTH - 1)
                                val cellY = ((y / boardSize.height.toFloat()) * GRID_HEIGHT).toInt().coerceIn(0, GRID_HEIGHT - 1)

                                if (isTileBuildable(cellX, cellY)) {
                                    val occupied = current.towers.any { it.x == cellX && it.y == cellY }
                                    if (!occupied && current.currency >= current.selectedTower.cost) {
                                        state.value = current.copy(
                                            towers = current.towers + Tower(cellX, cellY, current.selectedTower),
                                            currency = current.currency - current.selectedTower.cost,
                                            message = "${current.selectedTower.label} deployed"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cellW = width / GRID_WIDTH
            val cellH = height / GRID_HEIGHT

            for (y in 0 until GRID_HEIGHT) {
                for (x in 0 until GRID_WIDTH) {
                    val left = x * cellW
                    val top = y * cellH
                    val isPath = PATH_POINTS.any { it.x == x && it.y == y }
                    val color = if (isPath) Color(0xFF5D728A) else Color(0xFF1B3551)
                    drawRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(cellW, cellH)
                    )
                    drawRect(
                        color = Color(0x30FFFFFF),
                        topLeft = Offset(left, top),
                        size = Size(cellW, cellH),
                        style = Stroke(width = 1f)
                    )
                }
            }

            PATH_POINTS.forEachIndexed { index, pos ->
                val next = PATH_POINTS.getOrNull(index + 1) ?: return@forEachIndexed
                val from = Offset((pos.x + 0.5f) * cellW, (pos.y + 0.5f) * cellH)
                val to = Offset((next.x + 0.5f) * cellW, (next.y + 0.5f) * cellH)
                drawLine(
                    color = Color(0xFFAAE8FF),
                    start = from,
                    end = to,
                    strokeWidth = 10f
                )
            }

            val corePos = PATH_POINTS.last()
            val coreCenter = Offset((corePos.x + 0.5f) * cellW, (corePos.y + 0.5f) * cellH)
            drawCircle(
                color = Color(0xFFFFA726),
                radius = cellW * 0.38f,
                center = coreCenter
            )

            state.value.towers.forEach { tower ->
                val center = Offset((tower.x + 0.5f) * cellW, (tower.y + 0.5f) * cellH)
                drawCircle(
                    color = tower.type.color,
                    radius = cellW * 0.22f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = tower.type.range * cellW * 0.7f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            state.value.enemies.forEach { enemy ->
                val pos = enemyPosition(enemy)
                val center = Offset((pos.x + 0.5f) * cellW, (pos.y + 0.5f) * cellH)
                drawCircle(
                    color = enemy.type.color,
                    radius = min(cellW, cellH) * 0.16f,
                    center = center
                )
            }
        }

        if (state.value.status != "playing") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA071B2A)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF0E2E45),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (state.value.status == "victory") "Victory" else "Defeat",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Text(
                            text = state.value.message,
                            color = Color(0xFFD9EAF7),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        Button(onClick = { state.value = GameState() }) {
                            Text("Restart")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TowerBar(state: MutableState<GameState>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TowerType.values().forEach { towerType ->
            val selected = state.value.selectedTower == towerType
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        state.value = state.value.copy(selectedTower = towerType)
                    },
                color = if (selected) towerType.color.copy(alpha = 0.85f) else Color(0xFF12314A),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = towerType.label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${towerType.cost} bits",
                        color = Color(0xFFEAF4FF),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
