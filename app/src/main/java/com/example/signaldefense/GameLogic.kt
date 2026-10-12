package com.example.signaldefense

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

fun createEnemy(type: EnemyType, id: Int): Enemy {
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

fun enemyPosition(enemy: Enemy): Pair<Float, Float> {
    val totalSegments = PATH_POINTS.size - 1
    val progress = enemy.progress.coerceIn(0f, totalSegments.toFloat())
    val segmentIndex = progress.toInt()
    val segmentProgress = progress - segmentIndex
    val from = PATH_POINTS[segmentIndex]
    val to = PATH_POINTS[min(segmentIndex + 1, PATH_POINTS.size - 1)]

    val x = from.x + (to.x - from.x) * segmentProgress
    val y = from.y + (to.y - from.y) * segmentProgress
    return Pair(x.toFloat(), y.toFloat())
}

fun enemyDistanceToTile(enemy: Enemy, x: Int, y: Int): Float {
    val (posX, posY) = enemyPosition(enemy)
    val centerX = x + 0.5f
    val centerY = y + 0.5f
    return hypot(posX - centerX, posY - centerY)
}

fun advanceGame(state: GameState, deltaMs: Long): GameState {
    if (state.status != "playing") return state

    var current = state.copy(gameTime = state.gameTime + deltaMs)
    var enemyList = current.enemies.toMutableList()
    var nextWaveQueue = current.waveQueue.toMutableList()
    var nextEnemyId = current.nextEnemyId
    var spawnAccumulator = current.spawnAccumulatorMs + deltaMs
    val spawnIntervalMs = 700L

    // Spawn enemies
    while (spawnAccumulator >= spawnIntervalMs && nextWaveQueue.isNotEmpty()) {
        val enemyType = nextWaveQueue.removeAt(0)
        enemyList.add(createEnemy(enemyType, nextEnemyId))
        nextEnemyId += 1
        spawnAccumulator -= spawnIntervalMs
    }

    // Check wave completion
    if (nextWaveQueue.isEmpty() && enemyList.isEmpty()) {
        if (current.waveIndex >= current.totalWaves) {
            return current.copy(
                status = "victory",
                message = "All waves cleared. Core saved!"
            )
        } else {
            val nextWave = current.waveIndex + 1
            return current.copy(
                waveIndex = nextWave,
                waveQueue = buildWavePattern(nextWave),
                spawnAccumulatorMs = 0L,
                message = "Wave $nextWave incoming"
            )
        }
    }

    // Move enemies and handle core damage
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
            message = "Core destroyed."
        )
    }

    // Tower attacks
    var currency = current.currency
    val updatedTowers = current.towers.map { tower ->
        if (tower.cooldownMs > 0) {
            tower.copy(cooldownMs = max(0L, tower.cooldownMs - deltaMs))
        } else {
            val target = movedEnemies
                .filter { enemy -> enemyDistanceToTile(enemy, tower.x, tower.y) <= tower.range }
                .minByOrNull { enemy -> enemy.progress }

            if (target == null) {
                tower.copy(cooldownMs = 0L)
            } else {
                val damage = max(1, tower.displayDamage - target.armor)
                val targetIndex = movedEnemies.indexOfFirst { it.id == target.id }
                if (targetIndex >= 0) {
                    val remainingHp = movedEnemies[targetIndex].hp - damage
                    if (remainingHp <= 0) {
                        currency += movedEnemies[targetIndex].reward
                        movedEnemies.removeAt(targetIndex)
                    } else {
                        movedEnemies[targetIndex] = movedEnemies[targetIndex].copy(hp = remainingHp)
                    }
                }
                tower.copy(cooldownMs = tower.type.fireRateMs)
            }
        }
    }

    val finalEnemies = movedEnemies.toList()

    return current.copy(
        currency = currency,
        coreHp = coreHp,
        towers = updatedTowers,
        enemies = finalEnemies,
        waveQueue = nextWaveQueue,
        spawnAccumulatorMs = spawnAccumulator,
        nextEnemyId = nextEnemyId,
        message = when {
            coreHp <= 6 -> "Core critical!"
            else -> "Defend the core."
        }
    )
}

fun placeTower(state: GameState, x: Int, y: Int, towerType: TowerType): GameState {
    if (!isTileBuildable(x, y)) return state
    if (state.currency < towerType.cost) return state
    if (state.towers.any { it.x == x && it.y == y }) return state

    val newTower = Tower(x, y, towerType)
    return state.copy(
        towers = state.towers + newTower,
        currency = state.currency - towerType.cost,
        message = "${towerType.label} deployed"
    )
}

fun upgradeTower(state: GameState, tower: Tower): GameState {
    if (!tower.canUpgrade) return state
    if (state.currency < tower.upgradeCost) return state

    val newTower = tower.copy(level = tower.level + 1)
    val updatedTowers = state.towers.map {
        if (it.x == tower.x && it.y == tower.y) newTower else it
    }

    return state.copy(
        towers = updatedTowers,
        currency = state.currency - tower.upgradeCost,
        message = "Tower upgraded to level ${newTower.level}"
    )
}

fun sellTower(state: GameState, tower: Tower): GameState {
    val refund = tower.type.cost / 2
    val updatedTowers = state.towers.filterNot { it.x == tower.x && it.y == tower.y }

    return state.copy(
        towers = updatedTowers,
        currency = state.currency + refund,
        message = "Tower sold for $refund bits"
    )
}
