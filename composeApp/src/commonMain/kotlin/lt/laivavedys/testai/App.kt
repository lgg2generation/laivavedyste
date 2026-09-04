package lt.laivavedys.testai

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.ui.screens.HomeScreen
import lt.laivavedys.testai.ui.screens.ProgressScreen
import lt.laivavedys.testai.ui.screens.QuizScreen
import lt.laivavedys.testai.ui.screens.ResultScreen
import lt.laivavedys.testai.ui.theme.LaivavedysTheme

/** Shared entry point used by both the Android Activity and the iOS view controller. */
@Composable
fun App() {
    var state by remember { mutableStateOf<AppState?>(null) }

    LaunchedEffect(Unit) {
        QuestionBank.load()
        state = AppState()
    }

    LaivavedysTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val current = state
            if (current == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PlatformBackHandler(enabled = current.screen != Screen.HOME) {
                    when (current.screen) {
                        Screen.QUIZ -> current.abandonQuiz()
                        else -> current.goHome()
                    }
                }
                when (current.screen) {
                    Screen.HOME -> HomeScreen(current)
                    Screen.QUIZ -> QuizScreen(current)
                    Screen.RESULT -> ResultScreen(current)
                    Screen.PROGRESS -> ProgressScreen(current)
                }
            }
        }
    }
}
