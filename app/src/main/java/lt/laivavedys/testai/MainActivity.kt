package lt.laivavedys.testai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.ui.screens.HomeScreen
import lt.laivavedys.testai.ui.screens.ProgressScreen
import lt.laivavedys.testai.ui.screens.QuizScreen
import lt.laivavedys.testai.ui.screens.ResultScreen
import lt.laivavedys.testai.ui.theme.LaivavedysTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        QuestionBank.load(applicationContext)
        setContent {
            LaivavedysTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App(vm: AppViewModel = viewModel()) {
    BackHandler(enabled = vm.screen != Screen.HOME) {
        when (vm.screen) {
            Screen.QUIZ -> vm.abandonQuiz()
            else -> vm.goHome()
        }
    }
    when (vm.screen) {
        Screen.HOME -> HomeScreen(vm)
        Screen.QUIZ -> QuizScreen(vm)
        Screen.RESULT -> ResultScreen(vm)
        Screen.PROGRESS -> ProgressScreen(vm)
    }
}
