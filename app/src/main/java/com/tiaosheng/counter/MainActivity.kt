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
import com.tiaosheng.counter.ui.main.MainScreen
import com.tiaosheng.counter.ui.settings.SettingsScreen
import com.tiaosheng.counter.ui.theme.TiaoshengTheme

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

enum class Screen { MAIN, HISTORY, SETTINGS }

@Composable
fun TiaoshengNavHost() {
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.MAIN -> MainScreen(
                onNavigateToHistory = { currentScreen = Screen.HISTORY },
                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
            )
            Screen.HISTORY -> HistoryScreen(
                onBack = { currentScreen = Screen.MAIN }
            )
            Screen.SETTINGS -> SettingsScreen(
                onBack = { currentScreen = Screen.MAIN }
            )
        }
    }
}
