package ru.example.prichastie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.example.prichastie.ui.theme.PrichastieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrichastieTheme {
                // null = показан список, иначе открыт шаг с этим id
                var openStepId by rememberSaveable { mutableStateOf<String?>(null) }

                BackHandler(enabled = openStepId != null) { openStepId = null }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val m = Modifier.padding(innerPadding)
                    val id = openStepId
                    if (id == null) {
                        StepListScreen(modifier = m, onOpenStep = { openStepId = it })
                    } else {
                        StepScreen(
                            stepId = id,
                            onClose = { openStepId = null },
                            onOpenStep = { openStepId = it },
                            modifier = m
                        )
                    }
                }
            }
        }
    }
}