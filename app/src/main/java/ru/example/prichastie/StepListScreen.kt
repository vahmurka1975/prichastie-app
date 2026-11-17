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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.example.prichastie.data.Steps
import ru.example.prichastie.model.Phase

@Composable
fun StepListScreen(modifier: Modifier = Modifier) {
    var done by remember { mutableStateOf(setOf<String>()) }

    val counted = Steps.all.filter { it.countsInProgress }
    val doneCount = counted.count { it.id in done }
    val progress = if (counted.isEmpty()) 0f else doneCount / counted.size.toFloat()

    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        item {
            Text("Весь чин", style = MaterialTheme.typography.headlineSmall)
            Text("$doneCount из ${counted.size} шагов")
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        Phase.values().forEach { phase ->
            item {
                Text(
                    phase.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }
            items(Steps.all.filter { it.phase == phase }, key = { it.id }) { step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            done = if (step.id in done) done - step.id else done + step.id
                        }
                ) {
                    Checkbox(checked = step.id in done, onCheckedChange = null)
                    Text(step.title, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}