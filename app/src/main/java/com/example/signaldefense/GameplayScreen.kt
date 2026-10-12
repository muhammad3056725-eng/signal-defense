package com.example.signaldefense

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min

@Composable
fun GameplayScreen(
    levelNumber: Int,
    onVictory: (Int) -> Unit,
    onDefeat: () -> Unit,
    onQuit: () -> Unit
) {
    val gameState = remember { mutableStateOf(GameState(
        waveIndex = 1,
        waveQueue = buildWavePattern(1),
        message = "Wave 1 incoming"
    )) }
    val selectedTower = remember { mutableStateOf(TowerType.PULSE) }
    var isPaused by remember { mutableStateOf(false) }
    var showTowerInfo by remember { mutableStateOf<Tower?>(null) }

    LaunchedEffect(gameState.value.status) {
        if (gameState.value.status == "playing" && !isPaused) {
            while (isActive) {
                delay(16)
                gameState.value = advanceGame(gameState.value, 16L)
            }
        }
    }

    LaunchedEffect(gameState.value.status) {
        if (gameState.value.status == "victory") {
            onVictory(3)
        } else if (gameState.value.status == "defeat") {
            onDefeat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2A))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopGameHud(gameState.value, levelNumber, isPaused, { isPaused = !it }, onQuit)
        GameBoard(gameState, selectedTower, showTowerInfo, { showTowerInfo = it })
        TowerSelector(gameState, selectedTower)
    }

    if (isPaused && gameState.value.status == "playing") {
        PauseOverlay(onResume = { isPaused = false }, onQuit = onQuit)
    }

    showTowerInfo?.let { tower ->
        TowerInfoOverlay(
            tower = tower,
            currency = gameState.value.currency,
            onUpgrade = {
                gameState.value = upgradeTower(gameState.value, tower)
                showTowerInfo = null
            },
            onSell = {
                gameState.value = sellTower(gameState.value, tower)
                showTowerInfo = null
            },
            onClose = { showTowerInfo = null }
        )
    }
}

@Composable
fun TopGameHud(gameState: GameState, levelNumber: Int, isPaused: Boolean, onPauseChange: (Boolean) -> Unit, onQuit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Level $levelNumber",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAE8FF)
            )
            Text(
                text = gameState.message,
                fontSize = 12.sp,
                color = Color(0xFF90CAF9)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip("❤️", gameState.coreHp.toString(), Color(0xFFE57373))
            StatChip("💰", gameState.currency.toString(), Color(0xFF7EDAD0))
            StatChip("🌊", "${gameState.waveIndex}/${gameState.totalWaves}", Color(0xFFFFD166))
        }

        Row {
            IconButton(onClick = { onPauseChange(!isPaused) }) {
                Icon(
                    imageVector = Icons.Filled.Pause,
                    contentDescription = "Pause",
                    tint = Color.White
                )
            }
            IconButton(onClick = onQuit) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Quit",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun GameBoard(
    gameState: MutableState<GameState>,
    selectedTower: MutableState<TowerType>,
    selectedTowerObj: Tower?,
    onTowerSelected: (Tower?) -> Unit
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF0A2338), RoundedCornerShape(18.dp))
            .padding(8.dp)
            .onSizeChanged { boardSize = it }
            .pointerInput(gameState.value) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed && !change.previousPressed) {
                            val current = gameState.value
                            if (current.status != "playing") return@awaitPointerEventScope

                            val x = change.position.x
                            val y = change.position.y
                            if (boardSize.width > 0 && boardSize.height > 0) {
                                val cellX = ((x / boardSize.width.toFloat()) * GRID_WIDTH).toInt().coerceIn(0, GRID_WIDTH - 1)
                                val cellY = ((y / boardSize.height.toFloat()) * GRID_HEIGHT).toInt().coerceIn(0, GRID_HEIGHT - 1)

                                val tower = current.towers.find { it.x == cellX && it.y == cellY }
                                if (tower != null) {
                                    onTowerSelected(tower)
                                } else if (isTileBuildable(cellX, cellY) && current.currency >= selectedTower.value.cost) {
                                    gameState.value = placeTower(current, cellX, cellY, selectedTower.value)
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
                color = if (gameState.value.coreHp > 10) Color(0xFFFFA726) else Color(0xFFEF5350),
                radius = cellW * 0.35f,
                center = coreCenter
            )
            drawCircle(
                color = Color(0xFFFFD166),
                radius = cellW * 0.2f,
                center = coreCenter
            )

            gameState.value.towers.forEach { tower ->
                val center = Offset((tower.x + 0.5f) * cellW, (tower.y + 0.5f) * cellH)
                val isSelected = selectedTowerObj?.x == tower.x && selectedTowerObj?.y == tower.y
                
                drawCircle(
                    color = tower.type.color,
                    radius = cellW * 0.22f,
                    center = center
                )
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = tower.type.range * cellW * 0.7f,
                    center = center,
                    style = Stroke(width = if (isSelected) 3f else 1.5f)
                )
                
                if (tower.level > 1) {
                    drawCircle(
                        color = Color(0xFFFFD166),
                        radius = cellW * 0.08f,
                        center = Offset(center.x + cellW * 0.15f, center.y - cellW * 0.15f)
                    )
                }
            }

            gameState.value.enemies.forEach { enemy ->
                val (posX, posY) = enemyPosition(enemy)
                val center = Offset((posX + 0.5f) * cellW, (posY + 0.5f) * cellH)
                drawCircle(
                    color = enemy.type.color,
                    radius = min(cellW, cellH) * 0.15f,
                    center = center
                )
                
                if (enemy.hp < enemy.type.hp) {
                    drawCircle(
                        color = Color.White,
                        radius = min(cellW, cellH) * 0.08f,
                        center = center
                    )
                }
            }
        }
    }
}

@Composable
fun TowerSelector(gameState: MutableState<GameState>, selectedTower: MutableState<TowerType>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TowerType.values().forEach { towerType ->
            val selected = selectedTower.value == towerType
            val affordable = gameState.value.currency >= towerType.cost
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        selectedTower.value = towerType
                    },
                color = when {
                    selected -> towerType.color.copy(alpha = 0.9f)
                    affordable -> towerType.color.copy(alpha = 0.6f)
                    else -> Color(0xFF12314A)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = towerType.label.take(10),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${towerType.cost}💰",
                        color = if (affordable) Color(0xFFFFD166) else Color(0xFF888888),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onQuit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA071B2A)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0E2E45),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAUSED",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAE8FF)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66B3FF)
                    )
                ) {
                    Text("RESUME")
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onQuit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF12314A)
                    )
                ) {
                    Text("QUIT", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TowerInfoOverlay(
    tower: Tower,
    currency: Int,
    onUpgrade: () -> Unit,
    onSell: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA071B2A))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0E2E45),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clickable(enabled = false) { }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(tower.type.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⚔️", fontSize = 20.sp)
                    }
                    Text(
                        text = tower.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF12314A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        StatLine("Damage:", "${tower.displayDamage}")
                        StatLine("Range:", String.format("%.1f", tower.type.range))
                        StatLine("Speed:", "${tower.type.fireRateMs}ms")
                        StatLine("Level:", "${tower.level}/3")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (tower.canUpgrade) {
                    Button(
                        onClick = onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currency >= tower.upgradeCost) Color(0xFF66B3FF) else Color(0xFF555555)
                        ),
                        enabled = currency >= tower.upgradeCost
                    ) {
                        Text("UPGRADE (${tower.upgradeCost}💰)")
                    }
                } else {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF555555)
                        )
                    ) {
                        Text("MAX LEVEL")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onSell,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("SELL (${tower.type.cost / 2}💰)")
                }
            }
        }
    }
}

@Composable
fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFD9EAF7), fontSize = 12.sp)
        Text(text = value, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
