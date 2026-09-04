package lt.laivavedys.testai.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private class StoredCounts(
    val green: Int = 0,
    val yellow: Int = 0,
    val red: Int = 0,
    val new: Int = 0,
)

@Serializable
private class StoredSession(
    val timestamp: Long,
    val total: Int,
    val correct: Int,
    val topics: List<Int> = emptyList(),
    val countsAfter: StoredCounts = StoredCounts(),
)

@Serializable
private class StoredProgress(
    val questions: Map<String, List<Boolean>> = emptyMap(),
    val sessions: List<StoredSession> = emptyList(),
)

/**
 * Per-question attempt history and session history, kept in a small JSON file.
 * Only the last [HISTORY] results per question are retained - the traffic-light
 * status only ever looks at the last two.
 */
class ProgressStore(private val storage: Storage = Storage()) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val history = LinkedHashMap<String, MutableList<Boolean>>()
    private val sessions = mutableListOf<SessionRecord>()

    init {
        load()
    }

    fun progressFor(id: String): QuestionProgress =
        QuestionProgress(history[id]?.toList() ?: emptyList())

    fun statusFor(id: String): Status = progressFor(id).status

    fun record(id: String, correct: Boolean) {
        val list = history.getOrPut(id) { mutableListOf() }
        list.add(correct)
        while (list.size > HISTORY) list.removeAt(0)
    }

    fun counts(questions: List<Question> = QuestionBank.questions): StatusCounts {
        var g = 0; var y = 0; var r = 0; var n = 0
        for (q in questions) {
            when (statusFor(q.id)) {
                Status.GREEN -> g++
                Status.YELLOW -> y++
                Status.RED -> r++
                Status.NEW -> n++
            }
        }
        return StatusCounts(g, y, r, n)
    }

    fun saveSession(timestamp: Long, total: Int, correct: Int, topics: List<Int>) {
        sessions.add(SessionRecord(timestamp, total, correct, topics, counts()))
        while (sessions.size > MAX_SESSIONS) sessions.removeAt(0)
        save()
    }

    fun lastSession(): SessionRecord? = sessions.lastOrNull()

    fun sessionHistory(): List<SessionRecord> = sessions.toList()

    /** How the traffic lights moved during the most recent session. */
    fun changeInLastSession(): StatusCounts? {
        val last = sessions.lastOrNull() ?: return null
        val before = sessions.getOrNull(sessions.size - 2)?.countsAfter
            ?: StatusCounts(new = QuestionBank.questions.size)
        return last.countsAfter - before
    }

    fun resetAll() {
        history.clear()
        sessions.clear()
        save()
    }

    fun resetTopic(topic: Int) {
        QuestionBank.questions.filter { it.topic == topic }.forEach { history.remove(it.id) }
        save()
    }

    fun flush() = save()

    // ---------------------------------------------------------------- storage

    private fun load() {
        val text = storage.read() ?: return
        runCatching {
            val stored = json.decodeFromString<StoredProgress>(text)
            for ((key, results) in stored.questions) {
                history[key] = results.toMutableList()
            }
            for (s in stored.sessions) {
                sessions.add(
                    SessionRecord(
                        timestamp = s.timestamp,
                        total = s.total,
                        correct = s.correct,
                        topics = s.topics,
                        countsAfter = StatusCounts(
                            s.countsAfter.green,
                            s.countsAfter.yellow,
                            s.countsAfter.red,
                            s.countsAfter.new,
                        ),
                    )
                )
            }
        }
    }

    private fun save() {
        runCatching {
            val stored = StoredProgress(
                questions = history.mapValues { it.value.toList() },
                sessions = sessions.map { s ->
                    StoredSession(
                        timestamp = s.timestamp,
                        total = s.total,
                        correct = s.correct,
                        topics = s.topics,
                        countsAfter = StoredCounts(
                            s.countsAfter.green,
                            s.countsAfter.yellow,
                            s.countsAfter.red,
                            s.countsAfter.new,
                        ),
                    )
                },
            )
            storage.write(json.encodeToString(StoredProgress.serializer(), stored))
        }
    }

    private companion object {
        const val HISTORY = 10
        const val MAX_SESSIONS = 200
    }
}
