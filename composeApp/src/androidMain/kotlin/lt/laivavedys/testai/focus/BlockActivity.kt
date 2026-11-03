package lt.laivavedys.testai.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lt.laivavedys.testai.MainActivity
import lt.laivavedys.testai.ui.theme.LaivavedysTheme

/** Shown by [BlockerService] instead of a locked app. */
class BlockActivity : ComponentActivity() {

    private var label by mutableStateOf("Socialiniai tinklai")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FocusContext.app = applicationContext
        readLabel(intent)
        setContent {
            LaivavedysTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BlockScreen(
                        label = label,
                        answered = FocusStore.answered(this),
                        onSolve = ::openQuiz,
                        onClose = ::finish,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readLabel(intent)
    }

    private fun readLabel(intent: Intent?) {
        intent?.getStringExtra(EXTRA_LABEL)?.let { label = it }
    }

    /** Same as tapping the app icon: an unfinished quiz is resumed, not restarted. */
    private fun openQuiz() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_LABEL = "label"
    }
}

@androidx.compose.runtime.Composable
private fun BlockScreen(label: String, answered: Int, onSolve: () -> Unit, onClose: () -> Unit) {
    val left = QUESTIONS_PER_REWARD - answered
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            "$label užrakintas",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            "Sukaupto laiko neliko. Atsakyk dar $left kl. ir gausi +$REWARD_MINUTES min.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { answered.toFloat() / QUESTIONS_PER_REWARD },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "$answered / $QUESTIONS_PER_REWARD",
            style = MaterialTheme.typography.labelLarge,
        )
        Button(onClick = onSolve, modifier = Modifier.fillMaxWidth()) {
            Text("Spręsti klausimus")
        }
        TextButton(onClick = onClose) { Text("Uždaryti") }
    }
}
