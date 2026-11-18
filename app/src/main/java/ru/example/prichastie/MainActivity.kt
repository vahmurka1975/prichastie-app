package ru.example.prichastie

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.example.prichastie.model.Phase
import ru.example.prichastie.ui.Confirm
import ru.example.prichastie.ui.ConfirmDialog
import ru.example.prichastie.ui.NavBar
import ru.example.prichastie.ui.Tab
import ru.example.prichastie.ui.theme.LocalPalette
import ru.example.prichastie.ui.theme.PrichastieColors
import ru.example.prichastie.ui.theme.PrichastieTheme

class MainActivity : ComponentActivity() {

    // Запрос разрешения на уведомления (нужен для аудио в фоне, Android 13+)
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val settings by settingsVm.settings.collectAsStateWithLifecycle()
            val vm: MainViewModel = viewModel()
            val ui by vm.ui.collectAsStateWithLifecycle()

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
                PrichastieColors(dark) {
                    val p = LocalPalette.current
                    val context = LocalContext.current

                    var tabName by rememberSaveable { mutableStateOf(Tab.HOME.name) }
                    val tab = Tab.valueOf(tabName)
                    // null = показана вкладка, иначе открыт шаг с этим id
                    var openStepId by rememberSaveable { mutableStateOf<String?>(null) }
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    var confirm by remember { mutableStateOf<Confirm?>(null) }

                    val homeScroll = rememberScrollState()
                    val sumScroll = rememberScrollState()
                    val setScroll = rememberScrollState()
                    val listState = rememberLazyListState()

                    BackHandler(enabled = openStepId != null) { openStepId = null }
                    BackHandler(enabled = openStepId == null && tab != Tab.HOME) {
                        tabName = Tab.HOME.name
                    }

                    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                    // Открытие шага: благодарственные молитвы закрыты до отметки о Причастии
                    val openStep: (String) -> Unit = { id ->
                        val s = ui.steps.firstOrNull { it.step.id == id }
                        when {
                            s != null && !s.included ->
                                toast("Каноны №1–3 не входят в ваш режим подготовки")
                            s != null && s.step.phase == Phase.AFTER && !ui.communionDone ->
                                toast("Благодарственные молитвы откроются после отметки о Причастии")
                            else -> openStepId = id
                        }
                    }

                    val restartPhase: (Phase) -> Unit = { phase ->
                        val name = if (phase == Phase.BEFORE) "«До Причастия»" else "«После Причастия»"
                        confirm = Confirm(
                            title = "Начать этап $name сначала?",
                            body = "Будет сброшен прогресс всех шагов этапа." +
                                if (phase == Phase.BEFORE && ui.communionDone) " Отметка о Причастии останется." else "",
                            action = "Начать этап сначала",
                            onYes = { vm.restartPhase(phase) }
                        )
                    }

                    val restartAll: () -> Unit = {
                        confirm = Confirm(
                            title = "Начать всё сначала?",
                            body = "Будет сброшен прогресс обоих этапов и отметка о Причастии; режим " +
                                "«Полное прохождение» можно будет выбрать заново. " +
                                "Настройки (тема, размер текста) сохранятся.",
                            action = "Начать всё сначала",
                            onYes = {
                                vm.restart()
                                tabName = Tab.HOME.name
                                toast("Всё сброшено. Можно начинать заново")
                            }
                        )
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = p.bg,
                        contentColor = p.ink,
                        bottomBar = {
                            if (openStepId == null) NavBar(tab) { tabName = it.name }
                        }
                    ) { innerPadding ->
                        val m = Modifier.padding(innerPadding)
                        val id = openStepId
                        if (id != null) {
                            StepScreen(
                                stepId = id,
                                fontSizeSp = settings.fontSizeSp,
                                onOpenSettings = { showSettings = true },
                                onClose = { openStepId = null },
                                onOpenStep = openStep,
                                onOpenSummary = {
                                    openStepId = null
                                    tabName = Tab.SUM.name
                                },
                                modifier = m
                            )
                        } else when (tab) {
                            Tab.HOME -> HomeScreen(
                                ui = ui,
                                scroll = homeScroll,
                                onOpenStep = openStep,
                                onSetFullMode = { vm.setFullMode(it) },
                                onCommunion = { vm.setDone("communion", it) },
                                onRestartPhase = restartPhase,
                                onOpenSummary = { tabName = Tab.SUM.name },
                                modifier = m
                            )
                            Tab.LIST -> StepListScreen(
                                ui = ui,
                                listState = listState,
                                onOpenStep = openStep,
                                onRestartPhase = restartPhase,
                                onGoSettings = { tabName = Tab.SET.name },
                                modifier = m
                            )
                            Tab.SUM -> SummaryScreen(
                                ui = ui,
                                scroll = sumScroll,
                                onOpenStep = openStep,
                                onRestartAll = restartAll,
                                onGoHome = { tabName = Tab.HOME.name },
                                modifier = m
                            )
                            Tab.SET -> SettingsScreen(
                                ui = ui,
                                settings = settings,
                                scroll = setScroll,
                                onSetFullMode = { vm.setFullMode(it) },
                                onFontSize = { settingsVm.setFontSize(it) },
                                onTheme = { settingsVm.setThemeMode(it) },
								onLanguage = { vm.setLanguage(it) },
                                onDefaultMode = { vm.setDefaultMode(it) },
                                onRestartAll = restartAll,
                                modifier = m
                            )
                        }
                    }

                    confirm?.let { c -> ConfirmDialog(c) { confirm = null } }

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
}