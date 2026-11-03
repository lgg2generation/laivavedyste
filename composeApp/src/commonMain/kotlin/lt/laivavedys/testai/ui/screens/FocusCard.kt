package lt.laivavedys.testai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import lt.laivavedys.testai.focus.FocusGate
import lt.laivavedys.testai.focus.QUESTIONS_PER_REWARD
import lt.laivavedys.testai.focus.REWARD_MINUTES
import lt.laivavedys.testai.focus.formatMinutes

/** Home-screen card for the social-media lock (shown only where it is supported). */
@Composable
fun FocusCard() {
    var status by remember { mutableStateOf(FocusGate.status()) }
    // Picks up the service being switched on in the settings and the time running down.
    LaunchedEffect(Unit) {
        while (true) {
            status = FocusGate.status()
            delay(1_000)
        }
    }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (status.remainingMillis > 0) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Socialinių tinklų užraktas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (!status.active) {
                Text(
                    "Instagram, Facebook, LinkedIn ir YouTube atsirakina tik už išspręstus " +
                        "klausimus: kas $QUESTIONS_PER_REWARD atsakytų – $REWARD_MINUTES min.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Įjungimas (vieną kartą):\n" +
                        "1. „Įjungti užraktą“ → „Įdiegtos programos“ → „Laivavedys: socialinių " +
                        "tinklų užraktas“ → įjungti.\n" +
                        "2. Jei rodo „Apribotas nustatymas“: „Programos informacija“ → ⋮ viršuje " +
                        "dešinėje → „Leisti apribotus nustatymus“ ir pakartok 1 žingsnį. To reikia, " +
                        "nes programa įdiegta ne iš Play parduotuvės.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { FocusGate.openAppInfo() }) { Text("Programos informacija") }
                    Button(onClick = { FocusGate.openServiceSettings() }) { Text("Įjungti užraktą") }
                }
            } else {
                Text(
                    if (status.remainingMillis > 0) "Sukaupta: ${formatMinutes(status.remainingMillis)} min."
                    else "Sukaupto laiko nėra – tinklai užrakinti.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                LinearProgressIndicator(
                    progress = { status.answeredTowardsNext.toFloat() / QUESTIONS_PER_REWARD },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Iki kitų +$REWARD_MINUTES min.: ${status.answeredTowardsNext} / $QUESTIONS_PER_REWARD klausimų. " +
                        "Laikas skaičiuojamas tik kol naudoji šias programas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
