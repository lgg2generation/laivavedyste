package lt.laivavedys.testai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.laivavedys.testai.AppState
import lt.laivavedys.testai.data.Question
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.ui.QuestionImages
import lt.laivavedys.testai.ui.theme.StatusGreen
import lt.laivavedys.testai.ui.theme.StatusRed
import lt.laivavedys.testai.ui.theme.statusColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(state: AppState) {
    val question = state.current ?: return
    var confirmExit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Klausimas ${state.index + 1} iš ${state.quiz.size}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                QuestionBank.topicName(question.topic),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { confirmExit = true }) {
                            Icon(Icons.Filled.Close, contentDescription = "Baigti")
                        }
                    },
                    actions = {
                        val right = state.answers.count { it.correct }
                        Text(
                            "$right/${state.answers.size}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    },
                )
                LinearProgressIndicator(
                    progress = { (state.index).toFloat() / state.quiz.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (!state.revealed) {
                        Button(
                            onClick = { state.confirm() },
                            enabled = state.chosen != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Atsakyti") }
                    } else {
                        Button(
                            onClick = { state.next() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.index + 1 >= state.quiz.size) "Baigti testą" else "Kitas klausimas")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip(state, question)

            Text(question.text, style = MaterialTheme.typography.titleLarge)

            QuestionImages(question.images)

            question.options.forEachIndexed { i, option ->
                OptionRow(
                    index = i,
                    text = option.text,
                    images = option.images,
                    chosen = state.chosen == i,
                    revealed = state.revealed,
                    isCorrect = i == question.answer,
                    onClick = { state.choose(i) },
                )
            }

            AnimatedVisibility(visible = state.revealed) {
                Explanation(question)
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Baigti testą?") },
            text = { Text("Jau atsakyti klausimai bus įskaityti į progresą.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; state.abandonQuiz() }) { Text("Baigti") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Tęsti") } },
        )
    }
}

@Composable
private fun StatusChip(state: AppState, question: Question) {
    val status = remember(question.id, state.progressRevision) { state.store.statusFor(question.id) }
    val progress = remember(question.id, state.progressRevision) { state.store.progressFor(question.id) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor(status)),
        )
        Text(
            when (progress.attempts) {
                0 -> "Naujas klausimas"
                1 -> "Spręsta 1 kartą"
                else -> "Spręsta ${progress.attempts} k."
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionRow(
    index: Int,
    text: String,
    images: List<String>,
    chosen: Boolean,
    revealed: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit,
) {
    val correctColor = StatusGreen
    val wrongColor = StatusRed

    val border = when {
        revealed && isCorrect -> correctColor
        revealed && chosen -> wrongColor
        chosen -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    val container by animateColorAsState(
        targetValue = when {
            revealed && isCorrect -> correctColor.copy(alpha = 0.14f)
            revealed && chosen -> wrongColor.copy(alpha = 0.14f)
            chosen -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "optionColor",
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = container,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (chosen || (revealed && isCorrect)) 2.dp else 1.dp, border, RoundedCornerShape(14.dp))
            .clickable(enabled = !revealed, onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            revealed && isCorrect -> correctColor
                            revealed && chosen -> wrongColor
                            chosen -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (revealed && isCorrect) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else if (revealed && chosen) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        ('A' + index).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (chosen) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                if (images.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    QuestionImages(images)
                }
            }
        }
    }
}

@Composable
private fun Explanation(question: Question) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (question.reference != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, Modifier.size(18.dp))
                    Text(
                        question.reference,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        if (question.isDerived) {
            DerivedNotice(question)
        }
    }
}

@Composable
fun DerivedNotice(question: Question) {
    val verified = question.confidence == "verified"
    val tint = when (question.confidence) {
        "verified" -> StatusGreen.copy(alpha = 0.14f)
        "high" -> MaterialTheme.colorScheme.surfaceVariant
        "medium" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                when (question.confidence) {
                    "verified" -> "Atsakymas patikrintas"
                    "high" -> "Atsakymas atkurtas (patikimumas: aukštas)"
                    "medium" -> "Atsakymas atkurtas (patikimumas: vidutinis)"
                    else -> "Atsakymas atkurtas (patikimumas: žemas – pasitikrinkite)"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (verified)
                    "Šios temos dokumente atsakymas nebuvo pažymėtas, bet šis patikrintas pagal oficialų testą."
                else
                    "Šios temos dokumente teisingas atsakymas nebuvo pažymėtas.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (question.note != null) {
                Text(question.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
