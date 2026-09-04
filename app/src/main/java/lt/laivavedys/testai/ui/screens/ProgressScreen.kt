package lt.laivavedys.testai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import lt.laivavedys.testai.AppViewModel
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.data.SessionRecord
import lt.laivavedys.testai.ui.StatusBar
import lt.laivavedys.testai.ui.StatusLegend
import lt.laivavedys.testai.ui.theme.StatusGreen
import lt.laivavedys.testai.ui.theme.StatusNew
import lt.laivavedys.testai.ui.theme.StatusRed
import lt.laivavedys.testai.ui.theme.StatusYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(vm: AppViewModel) {
    val revision = vm.progressRevision
    val overall = remember(revision) { vm.store.counts() }
    val sessions = remember(revision) { vm.store.sessionHistory().reversed() }
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progresas") },
                navigationIcon = {
                    IconButton(onClick = { vm.goHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atgal")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Visas klausimynas", style = MaterialTheme.typography.titleMedium)
                        StatusBar(overall, height = 12)
                        StatusLegend(overall)
                        Text(
                            "Spręsta ${overall.seen} iš ${overall.total} klausimų.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ką reiškia spalvos", style = MaterialTheme.typography.titleMedium)
                        LegendLine(StatusGreen, "Žalia – paskutinius 2 kartus atsakyta teisingai.")
                        LegendLine(StatusYellow, "Geltona – vieną iš paskutinių 2 kartų suklysta (arba teisingai atsakyta tik kartą).")
                        LegendLine(StatusRed, "Raudona – paskutinius 2 kartus atsakyta neteisingai.")
                        LegendLine(StatusNew, "Pilka – klausimas dar nespręstas.")
                    }
                }
            }

            item { Text("Temos", style = MaterialTheme.typography.titleMedium) }

            items(QuestionBank.topics, key = { it.id }) { topic ->
                val qs = remember(revision) { QuestionBank.questions.filter { it.topic == topic.id } }
                val counts = remember(revision) { vm.store.counts(qs) }
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${topic.id}. ${topic.name}", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { vm.resetTopic(topic.id) }) { Text("Valyti") }
                        }
                        StatusBar(counts, height = 8)
                        Text(
                            "${counts.green} moku · ${counts.yellow} kartoti · ${counts.red} silpna · ${counts.new} nespręsta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (sessions.isNotEmpty()) {
                item { Text("Sesijų istorija", style = MaterialTheme.typography.titleMedium) }
                items(sessions) { s -> SessionRow(s, fmt) }
            }
        }
    }
}

@Composable
private fun LegendLine(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SessionRow(s: SessionRecord, fmt: SimpleDateFormat) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(fmt.format(Date(s.timestamp)), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (s.topics.isEmpty()) "Visos temos"
                    else "Temos: " + s.topics.joinToString(", "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${s.correct}/${s.total}",
                style = MaterialTheme.typography.titleMedium,
                color = if (s.total > 0 && s.correct * 100 / s.total >= 80) StatusGreen
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
