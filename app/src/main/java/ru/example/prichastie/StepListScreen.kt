package ru.example.prichastie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.example.prichastie.model.Catalog

@Composable
fun StepListScreen(
    modifier: Modifier = Modifier,
    vm: MainViewModel = viewModel(),
    onOpenStep: (String) -> Unit = {}
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val visible = ui.steps.filter { it.included }
    val doneCount = visible.count { it.done }

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Text("Весь чин", style = MaterialTheme.typography.headlineSmall)
            Text("$doneCount из ${visible.size} шагов")
            LinearProgressIndicator(
                progress = { ui.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !ui.modeLocked) { vm.setFullMode(!ui.fullMode) }
            ) {
                Checkbox(
                    checked = ui.fullMode,
                    onCheckedChange = null,
                    enabled = !ui.modeLocked
                )
                Text(
                    "Полное прохождение (с тремя канонами)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Catalog.groups.forEach { group ->
            val rows = visible.filter { it.step.groupId == group.id }
            if (rows.isNotEmpty()) {
                item(key = "group_${group.id}") {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(group.subtitle, style = MaterialTheme.typography.bodySmall)
                }
                items(rows, key = { it.step.id }) { row ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = row.included) { onOpenStep(row.step.id) }
                    ) {
                        Checkbox(
                            checked = row.done,
                            onCheckedChange = { vm.setDone(row.step.id, it) }
                        )
                        Text(
                            "${row.number}. ${row.step.title}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { vm.restart() },
                modifier = Modifier.padding(vertical = 16.dp)
            ) { Text("Начать всё сначала") }
        }
    }
}