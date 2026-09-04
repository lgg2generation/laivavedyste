package lt.laivavedys.testai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import lt.laivavedys.testai.data.ProgressStore
import lt.laivavedys.testai.data.Question
import lt.laivavedys.testai.data.QuestionBank
import lt.laivavedys.testai.data.Status
import lt.laivavedys.testai.data.StatusCounts
import lt.laivavedys.testai.data.nowMillis

enum class Screen { HOME, QUIZ, RESULT, PROGRESS }

/** How the questions for a session are picked. */
enum class QuizMode {
    RANDOM,   // straight shuffle across the chosen topics
    REVIEW,   // weakest first: red, then yellow, then unseen, then green
}

class AnsweredQuestion(
    val question: Question,
    val chosen: Int,
    val correct: Boolean,
)

/**
 * All app state, shared by Android and iOS. Held across recompositions by the
 * platform entry point.
 */
class AppState(val store: ProgressStore = ProgressStore()) {

    var screen by mutableStateOf(Screen.HOME)
        private set

    /** Empty means "all topics". */
    var selectedTopics by mutableStateOf<Set<Int>>(emptySet())
        private set

    var mode by mutableStateOf(QuizMode.RANDOM)
        private set

    var sessionSize by mutableStateOf(30)
        private set

    /** Bumped whenever progress changes so the summaries recompose. */
    var progressRevision by mutableStateOf(0)
        private set

    // ------------------------------------------------------------- quiz state

    var quiz by mutableStateOf<List<Question>>(emptyList())
        private set

    var index by mutableStateOf(0)
        private set

    var chosen by mutableStateOf<Int?>(null)
        private set

    var revealed by mutableStateOf(false)
        private set

    var answers by mutableStateOf<List<AnsweredQuestion>>(emptyList())
        private set

    val current: Question? get() = quiz.getOrNull(index)

    // ---------------------------------------------------------------- actions

    fun toggleTopic(id: Int) {
        selectedTopics = if (id in selectedTopics) selectedTopics - id else selectedTopics + id
    }

    fun selectAllTopics() {
        selectedTopics = emptySet()
    }

    fun chooseMode(m: QuizMode) {
        mode = m
    }

    fun chooseSessionSize(n: Int) {
        sessionSize = n
    }

    fun questionsInScope(): List<Question> =
        if (selectedTopics.isEmpty()) QuestionBank.questions
        else QuestionBank.questions.filter { it.topic in selectedTopics }

    fun countsInScope(): StatusCounts = store.counts(questionsInScope())

    fun startQuiz() {
        val pool = questionsInScope()
        if (pool.isEmpty()) return

        val picked = when (mode) {
            QuizMode.RANDOM -> pool.shuffled()
            QuizMode.REVIEW -> pool.shuffled().sortedBy {
                when (store.statusFor(it.id)) {
                    Status.RED -> 0
                    Status.YELLOW -> 1
                    Status.NEW -> 2
                    Status.GREEN -> 3
                }
            }
        }.take(if (sessionSize < pool.size) sessionSize else pool.size)

        quiz = picked
        index = 0
        chosen = null
        revealed = false
        answers = emptyList()
        screen = Screen.QUIZ
    }

    fun choose(option: Int) {
        if (revealed) return
        chosen = option
    }

    /** Confirms the chosen option, records it and reveals the correct answer. */
    fun confirm() {
        val q = current ?: return
        val pick = chosen ?: return
        if (revealed) return
        val ok = pick == q.answer
        store.record(q.id, ok)
        answers = answers + AnsweredQuestion(q, pick, ok)
        revealed = true
        progressRevision++
    }

    fun next() {
        if (!revealed) return
        if (index + 1 >= quiz.size) {
            finishQuiz()
        } else {
            index++
            chosen = null
            revealed = false
        }
    }

    private fun finishQuiz() {
        saveSession()
        screen = Screen.RESULT
    }

    /** Leaves a quiz early; answers already given are kept. */
    fun abandonQuiz() {
        if (answers.isNotEmpty()) saveSession() else store.flush()
        progressRevision++
        screen = Screen.HOME
    }

    private fun saveSession() {
        store.saveSession(
            timestamp = nowMillis(),
            total = answers.size,
            correct = answers.count { it.correct },
            topics = selectedTopics.toList().sorted(),
        )
        progressRevision++
    }

    fun goHome() {
        screen = Screen.HOME
    }

    fun goProgress() {
        screen = Screen.PROGRESS
    }

    fun resetAll() {
        store.resetAll()
        progressRevision++
    }

    fun resetTopic(topic: Int) {
        store.resetTopic(topic)
        progressRevision++
    }
}
