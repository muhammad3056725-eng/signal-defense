package com.example.signaldefense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainMenuScreen(onPlayClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.foundation.background(
                    color = Color(0xFF071B2A)
                ).brush
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFF66B3FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡",
                    fontSize = 60.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "SIGNAL DEFENSE",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAE8FF)
            )
            Text(
                text = "Defend the Core",
                fontSize = 18.sp,
                color = Color(0xFF90CAF9),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF66B3FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "PLAY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF12314A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "SETTINGS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LevelSelectScreen(
    onLevelSelected: (Int) -> Unit,
    onBackClick: () -> Unit,
    levelProgress: Map<Int, Int>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "SELECT LEVEL",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAE8FF),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(5) { index ->
                val levelNum = index + 1
                val stars = levelProgress[levelNum] ?: 0
                LevelCard(
                    level = levelNum,
                    stars = stars,
                    onClick = { onLevelSelected(levelNum) }
                )
            }
        }
    }
}

@Composable
fun LevelCard(level: Int, stars: Int, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        color = Color(0xFF12314A),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LEVEL $level",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAE8FF)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                repeat(3) { index ->
                    Text(
                        text = if (index < stars) "⭐" else "☆",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VictoryScreen(levelNumber: Int, onNextClick: () -> Unit, onRetryClick: () -> Unit, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2A)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0E2E45),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 VICTORY!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD166)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Core Protected",
                    fontSize = 18.sp,
                    color = Color(0xFFD9EAF7)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row {
                    repeat(3) {
                        Text(text = "⭐", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onNextClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66B3FF)
                    )
                ) {
                    Text("NEXT LEVEL")
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF12314A)
                    )
                ) {
                    Text("LEVEL SELECT", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DefeatScreen(levelNumber: Int, onRetryClick: () -> Unit, onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2A)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0E2E45),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💀 DEFEAT",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF5350)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Core Destroyed",
                    fontSize = 18.sp,
                    color = Color(0xFFD9EAF7)
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66B3FF)
                    )
                ) {
                    Text("RETRY")
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF12314A)
                    )
                ) {
                    Text("LEVEL SELECT", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(soundEnabled: Boolean, onSoundToggle: () -> Unit, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071B2A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "SETTINGS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFAAE8FF),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            color = Color(0xFF12314A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Sound Effects",
                    fontSize = 16.sp,
                    color = Color.White
                )
                IconButton(onClick = onSoundToggle) {
                    Icon(
                        imageVector = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "Toggle Sound",
                        tint = Color(0xFF66B3FF)
                    )
                }
            }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            color = Color(0xFF12314A),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "About",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Signal Defense v1.0\nA tower defense game where you protect your data core from signal corruption.",
                    fontSize = 14.sp,
                    color = Color(0xFFD9EAF7),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
