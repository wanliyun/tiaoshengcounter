package com.tiaosheng.counter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tiaosheng.counter.ui.history.HistoryScreen
import com.tiaosheng.counter.ui.home.HomeScreen
import com.tiaosheng.counter.ui.main.MainScreen
import com.tiaosheng.counter.ui.settings.SettingsScreen
import com.tiaosheng.counter.ui.theme.TiaoshengTheme
import com.tiaosheng.counter.ui.video.VideoCountingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiaoshengTheme {
                TiaoshengNavHost()
            }
        }
    }
}

enum class Screen { HOME, MAIN, VIDEO_COUNTING, HISTORY, SETTINGS }

sealed class ExerciseMode {
    data object Free : ExerciseMode()
    data class Timed(val durationSeconds: Int) : ExerciseMode()
    data class Count(val targetCount: Int) : ExerciseMode()
}

@Composable
fun TiaoshengNavHost() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var exerciseMode by remember { mutableStateOf<ExerciseMode>(ExerciseMode.Free) }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.HOME -> HomeScreen(
                onStartTimed = { seconds ->
                    exerciseMode = ExerciseMode.Timed(seconds)
                    currentScreen = Screen.MAIN
                },
                onStartCount = { target ->
                    exerciseMode = ExerciseMode.Count(target)
                    currentScreen = Screen.MAIN
                },
                onVideoCount = { currentScreen = Screen.VIDEO_COUNTING },
                onHistory = { currentScreen = Screen.HISTORY },
                onSettings = { currentScreen = Screen.SETTINGS }
            )
            Screen.MAIN -> MainScreen(
                exerciseMode = exerciseMode,
                onBack = { currentScreen = Screen.HOME },
                onComplete = { currentScreen = Screen.HOME }
            )
            Screen.VIDEO_COUNTING -> VideoCountingScreen(
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.HISTORY -> HistoryScreen(
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.SETTINGS -> SettingsScreen(
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
