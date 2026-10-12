package com.example.signaldefense

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun SignalDefenseApp() {
    val screen = remember { mutableStateOf("menu") }
    val selectedLevel = remember { mutableStateOf(1) }
    val levelProgress = remember { mutableStateOf(mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)) }
    val soundEnabled = remember { mutableStateOf(true) }

    when (screen.value) {
        "menu" -> MainMenuScreen(
            onPlayClick = { screen.value = "levelSelect" },
            onSettingsClick = { screen.value = "settings" }
        )
        "levelSelect" -> LevelSelectScreen(
            onLevelSelected = {
                selectedLevel.value = it
                screen.value = "gameplay"
            },
            onBackClick = { screen.value = "menu" },
            levelProgress = levelProgress.value
        )
        "gameplay" -> GameplayScreen(
            levelNumber = selectedLevel.value,
            onVictory = { stars ->
                levelProgress.value = levelProgress.value.toMutableMap().apply {
                    this[selectedLevel.value] = maxOf(this[selectedLevel.value] ?: 0, stars)
                }
                screen.value = "victory"
            },
            onDefeat = { screen.value = "defeat" },
            onQuit = { screen.value = "levelSelect" }
        )
        "victory" -> VictoryScreen(
            levelNumber = selectedLevel.value,
            onNextClick = {
                if (selectedLevel.value < 5) {
                    selectedLevel.value += 1
                    screen.value = "gameplay"
                } else {
                    screen.value = "levelSelect"
                }
            },
            onRetryClick = { screen.value = "gameplay" },
            onMenuClick = { screen.value = "levelSelect" }
        )
        "defeat" -> DefeatScreen(
            levelNumber = selectedLevel.value,
            onRetryClick = { screen.value = "gameplay" },
            onMenuClick = { screen.value = "levelSelect" }
        )
        "settings" -> SettingsScreen(
            soundEnabled = soundEnabled.value,
            onSoundToggle = { soundEnabled.value = !soundEnabled.value },
            onBackClick = { screen.value = "menu" }
        )
    }
}
