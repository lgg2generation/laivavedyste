package lt.laivavedys.testai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import lt.laivavedys.testai.AppViewModel
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.ui.QuestionImages
import lt.laivavedys.testai.ui.StatusBar
import lt.laivavedys.testai.ui.StatusLegend
import lt.laivavedys.testai.ui.theme.StatusGreen
import lt.laivavedys.testai.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(vm: AppViewModel) {
    val correct = vm.answers.count { it.correct }
    val total = vm.answers.size
    val percent = if (total == 0) 0 else correct * 100 / total
    val counts = remember(vm.progressRevision) { vm.countsInScope() }
    val delta = remember(vm.progressRevision) { vm.store.changeInLastSession() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sesijos rezultatas") }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { vm.goHome() }, modifier = Modifier.weight(1f)) {
                        Text("Į pradžią")
                    }
                    Button(onClick = { vm.startQuiz() }, modifier = Modifier.weight(1f)) {
                        Text("Dar kartą")
                    }
                }
            }
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
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (percent >= 80) StatusGreen.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("$correct / $total", style = MaterialTheme.typography.headlineSmall)
                        Text("$percent % teisingų atsakymų", style = MaterialTheme.typography.bodyLarge)
                        if (delta != null) {
                            val parts = buildList {
                                if (delta.green != 0) add("${signedText(delta.green)} žalių")
                                if (delta.yellow != 0) add("${signedText(delta.yellow)} geltonų")
                                if (delta.red != 0) add("${signedText(delta.red)} raudonų")
                            }
                            if (parts.isNotEmpty()) {
                                Text(
                                    "Pokytis: " + parts.joinToString(", "),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pasirinktų temų būklė", style = MaterialTheme.typography.titleMedium)
                        StatusBar(counts, height = 12)
                        StatusLegend(counts)
                    }
                }
            }

            item {
                Text("Peržiūrėk atsakymus", style = MaterialTheme.typography.titleMedium)
            }

            itemsIndexed(vm.answers) { i, answered ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (answered.correct) StatusGreen else StatusRed),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (answered.correct) Icons.Filled.Check else Icons.Filled.Close,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(
                                    "${i + 1}. ${answered.question.text}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = if (expanded) 10 else 2,
                                )
                                Text(
                                    QuestionBank.topicName(answered.question.topic),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (expanded) {
                            QuestionImages(answered.question.images)
                            answered.question.options.forEachIndexed { j, opt ->
                                val isAnswer = j == answered.question.answer
                                val isChosen = j == answered.chosen
                                Text(
                                    (if (isAnswer) "✓ " else if (isChosen) "✗ " else "   ") + opt.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isAnswer -> StatusGreen
                                        isChosen -> StatusRed
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            if (answered.question.reference != null) {
                                Text(
                                    answered.question.reference,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (answered.question.isDerived) {
                                DerivedNotice(answered.question)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun signedText(v: Int) = if (v > 0) "+$v" else "$v"
