package com.example.signaldefense

import androidx.compose.ui.graphics.Color

data class GridPos(val x: Int, val y: Int)

enum class EnemyType(
    val label: String,
    val hp: Int,
    val speed: Float,
    val armor: Int,
    val reward: Int,
    val color: Color
) {
    DRONE("Drone", 16, 1.0f, 0, 8, Color(0xFF7EDAD0)),
    HAULER("Hauler", 36, 0.55f, 3, 14, Color(0xFFB892FF)),
    SPRINTER("Sprinter", 12, 1.7f, 0, 10, Color(0xFFFFB74D)),
    WARDEN("Warden", 80, 0.48f, 4, 24, Color(0xFFEF5350))
}

enum class TowerType(
    val label: String,
    val cost: Int,
    val baseDamage: Int,
    val range: Float,
    val fireRateMs: Long,
    val color: Color,
    val description: String
) {
    PULSE("Pulse Turret", 30, 9, 2.2f, 650L, Color(0xFF66B3FF), "Fast, single-target"),
    FLAK("Flak Array", 45, 14, 1.8f, 1100L, Color(0xFFFFD166), "Splash damage"),
    FROST("Frost Node", 40, 6, 2.3f, 900L, Color(0xFF90CAF9), "Slows enemies"),
    RAIL("Rail Spire", 60, 22, 3.2f, 1500L, Color(0xFFB39DDB), "Long-range sniper")
}

data class Tower(
    val x: Int,
    val y: Int,
    val type: TowerType,
    val level: Int = 1,
    val cooldownMs: Long = 0L
) {
    val displayName: String
        get() = when (level) {
            1 -> type.label
            2 -> "${type.label} +"
            else -> "${type.label} ++"
        }
    
    val displayDamage: Int
        get() = type.baseDamage * level
    
    val upgradeCost: Int
        get() = when (level) {
            1 -> type.cost / 2
            2 -> type.cost
            else -> Int.MAX_VALUE
        }
    
    val canUpgrade: Boolean
        get() = level < 3
}

data class Enemy(
    val id: Int,
    val type: EnemyType,
    val hp: Int,
    val speed: Float,
    val armor: Int,
    val reward: Int,
    val progress: Float = 0f
) {
    val healthPercentage: Float
        get() = (hp.toFloat() / type.hp).coerceIn(0f, 1f)
}

data class GameState(
    val currency: Int = 150,
    val coreHp: Int = 20,
    val maxCoreHp: Int = 20,
    val towers: List<Tower> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val waveQueue: List<EnemyType> = emptyList(),
    val waveIndex: Int = 0,
    val totalWaves: Int = 5,
    val spawnAccumulatorMs: Long = 0L,
    val nextEnemyId: Int = 1,
    val selectedTower: TowerType = TowerType.PULSE,
    val status: String = "playing",
    val message: String = "",
    val gameTime: Long = 0L,
    val towerUnderCursor: Int? = null
)

fun buildWavePattern(waveNumber: Int): List<EnemyType> {
    return when (waveNumber) {
        1 -> listOf(EnemyType.DRONE, EnemyType.DRONE, EnemyType.DRONE, EnemyType.SPRINTER)
        2 -> listOf(EnemyType.DRONE, EnemyType.HAULER, EnemyType.DRONE, EnemyType.SPRINTER, EnemyType.DRONE)
        3 -> listOf(EnemyType.SPRINTER, EnemyType.DRONE, EnemyType.HAULER, EnemyType.SPRINTER, EnemyType.DRONE, EnemyType.SPRINTER)
        4 -> listOf(EnemyType.WARDEN, EnemyType.DRONE, EnemyType.SPRINTER, EnemyType.HAULER, EnemyType.DRONE, EnemyType.HAULER)
        5 -> listOf(EnemyType.WARDEN, EnemyType.WARDEN, EnemyType.HAULER, EnemyType.SPRINTER, EnemyType.DRONE, EnemyType.DRONE, EnemyType.HAULER, EnemyType.SPRINTER)
        else -> listOf(EnemyType.DRONE, EnemyType.SPRINTER, EnemyType.HAULER, EnemyType.WARDEN)
    }
}

const val GRID_WIDTH = 8
const val GRID_HEIGHT = 12

val PATH_POINTS = listOf(
    GridPos(0, 3), GridPos(1, 3), GridPos(1, 6), GridPos(2, 6),
    GridPos(2, 8), GridPos(3, 8), GridPos(3, 4), GridPos(4, 4),
    GridPos(4, 9), GridPos(5, 9), GridPos(5, 2), GridPos(6, 2),
    GridPos(6, 10), GridPos(7, 10)
)

val BUILDABLE_TILES = buildSet {
    for (y in 0 until GRID_HEIGHT) {
        for (x in 0 until GRID_WIDTH) {
            if (PATH_POINTS.none { it.x == x && it.y == y }) {
                add(GridPos(x, y))
            }
        }
    }
}

fun isTileBuildable(x: Int, y: Int): Boolean {
    return BUILDABLE_TILES.contains(GridPos(x, y))
}
