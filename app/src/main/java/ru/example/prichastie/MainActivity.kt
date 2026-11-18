package ru.example.prichastie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.example.prichastie.ui.theme.PrichastieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val settings by settingsVm.settings.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Значки строки состояния и навигации подстраиваются под тему приложения
            DisposableEffect(dark) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { dark }
                )
                onDispose { }
            }

            PrichastieTheme(darkTheme = dark) {
                // null = показан список, иначе открыт шаг с этим id
                var openStepId by rememberSaveable { mutableStateOf<String?>(null) }
                var showSettings by rememberSaveable { mutableStateOf(false) }
                val listState = rememberLazyListState()

                BackHandler(enabled = openStepId != null) { openStepId = null }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val m = Modifier.padding(innerPadding)
                    val id = openStepId
                    if (id == null) {
                        StepListScreen(
                            modifier = m,
                            listState = listState,
                            onOpenStep = { openStepId = it }
                        )
                    } else {
                        StepScreen(
                            stepId = id,
                            fontSizeSp = settings.fontSizeSp,
                            onOpenSettings = { showSettings = true },
                            onClose = { openStepId = null },
                            onOpenStep = { openStepId = it },
                            modifier = m
                        )
                    }
                }

                if (showSettings) {
                    SettingsDialog(
                        fontSizeSp = settings.fontSizeSp,
                        themeMode = settings.themeMode,
                        onFontSizeChange = { settingsVm.setFontSize(it) },
                        onThemeChange = { settingsVm.setThemeMode(it) },
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}