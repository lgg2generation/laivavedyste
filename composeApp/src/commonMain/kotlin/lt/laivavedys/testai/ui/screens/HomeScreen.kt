package lt.laivavedys.testai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.laivavedys.testai.AppState
import lt.laivavedys.testai.QuizMode
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.data.formatTimestamp
import lt.laivavedys.testai.ui.StatusBar
import lt.laivavedys.testai.ui.StatusLegend
import lt.laivavedys.testai.ui.theme.StatusGreen
import lt.laivavedys.testai.ui.theme.StatusRed
import lt.laivavedys.testai.ui.theme.StatusYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: AppState) {
    // Reading the revision makes every summary recompose after an answer is stored.
    val revision = state.progressRevision
    val scope = remember(revision, state.selectedTopics) { state.questionsInScope() }
    val counts = remember(revision, state.selectedTopics) { state.countsInScope() }
    val overall = remember(revision) { state.store.counts() }
    val lastSession = remember(revision) { state.store.lastSession() }
    val delta = remember(revision) { state.store.changeInLastSession() }
    var showReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laivavedžio testai") },
                actions = {
                    IconButton(onClick = { state.goProgress() }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Progresas")
                    }
                    IconButton(onClick = { showReset = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Atstatyti")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { state.startQuiz() },
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                text = { Text("Pradėti (${minOf(state.sessionSize, scope.size)} kl.)") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OverviewCard(
                    counts = overall,
                    lastSessionText = lastSession?.let {
                        "Paskutinė sesija ${formatTimestamp(it.timestamp, withDate = false)} · ${it.correct}/${it.total}"
                    },
                    deltaText = delta?.let { d ->
                        val parts = buildList {
                            if (d.green != 0) add("${signed(d.green)} žalių")
                            if (d.yellow != 0) add("${signed(d.yellow)} geltonų")
                            if (d.red != 0) add("${signed(d.red)} raudonų")
                        }
                        if (parts.isEmpty()) null else "Nuo paskutinės sesijos: " + parts.joinToString(", ")
                    },
                )
            }

            item { SectionTitle("Sesijos nustatymai") }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Klausimų skaičius", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf(10, 20, 30, 50).forEachIndexed { i, n ->
                                SegmentedButton(
                                    selected = state.sessionSize == n,
                                    onClick = { state.chooseSessionSize(n) },
                                    shape = SegmentedButtonDefaults.itemShape(i, 4),
                                ) { Text("$n") }
                            }
                        }

                        Text("Klausimų parinkimas", style = MaterialTheme.typography.labelLarge)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = state.mode == QuizMode.RANDOM,
                                onClick = { state.chooseMode(QuizMode.RANDOM) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text("Atsitiktinai") }
                            SegmentedButton(
                                selected = state.mode == QuizMode.REVIEW,
                                onClick = { state.chooseMode(QuizMode.REVIEW) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text("Kartojimas") }
                        }
                        Text(
                            if (state.mode == QuizMode.RANDOM)
                                "Klausimai išmėtomi atsitiktine tvarka."
                            else
                                "Pirmiausia pateikiami raudoni ir geltoni klausimai.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("Temos")
                    TextButton(onClick = { state.selectAllTopics() }) { Text("Visos temos") }
                }
            }

            item {
                if (state.selectedTopics.isEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Pasirinktos visos ${QuestionBank.topics.size} temos · ${scope.size} klausimų") },
                        leadingIcon = { Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp)) },
                    )
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("Pasirinkta ${state.selectedTopics.size} temų · ${scope.size} klausimų") },
                    )
                }
            }

            items(QuestionBank.topics, key = { it.id }) { topic ->
                val topicQuestions = remember(revision) {
                    QuestionBank.questions.filter { it.topic == topic.id }
                }
                val topicCounts = remember(revision) { state.store.counts(topicQuestions) }
                TopicCard(
                    number = topic.id,
                    name = topic.name,
                    total = topicQuestions.size,
                    counts = topicCounts,
                    selected = state.selectedTopics.isEmpty() || topic.id in state.selectedTopics,
                    explicitlySelected = topic.id in state.selectedTopics,
                    onClick = { state.toggleTopic(topic.id) },
                )
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Atstatyti progresą?") },
            text = { Text("Bus ištrinta visa atsakymų istorija ir sesijų statistika. Klausimai liks.") },
            confirmButton = {
                TextButton(onClick = { state.resetAll(); showReset = false }) { Text("Atstatyti") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Atšaukti") } },
        )
    }
}

private fun signed(v: Int) = if (v > 0) "+$v" else "$v"

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun OverviewCard(
    counts: lt.laivavedys.testai.data.StatusCounts,
    lastSessionText: String?,
    deltaText: String?,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${counts.green}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    " / ${counts.total} klausimų moku",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            StatusBar(counts, height = 12)
            StatusLegend(counts)
            if (lastSessionText != null) {
                Text(
                    lastSessionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (deltaText != null) {
                Text(
                    deltaText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun TopicCard(
    number: Int,
    name: String,
    total: Int,
    counts: lt.laivavedys.testai.data.StatusCounts,
    selected: Boolean,
    explicitlySelected: Boolean,
    onClick: () -> Unit,
) {
    val container =
        if (explicitlySelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$total klausimai · ${counts.green} moku · ${counts.red + counts.yellow} kartoti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusBar(counts, height = 8)
            }
            Checkbox(checked = explicitlySelected, onCheckedChange = { onClick() })
        }
    }
}
