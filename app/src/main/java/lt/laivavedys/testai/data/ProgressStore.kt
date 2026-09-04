package lt.laivavedys.testai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Per-question attempt history and session history, kept in a small JSON file in
 * internal storage. Only the last [HISTORY] results per question are retained -
 * the traffic-light status only ever looks at the last two.
 */
class ProgressStore(context: Context) {

    private val file = File(context.filesDir, "progress.json")

    private val history = LinkedHashMap<String, MutableList<Boolean>>()
    private val sessions = mutableListOf<SessionRecord>()

    init {
        load()
    }

    fun progressFor(id: String): QuestionProgress =
        QuestionProgress(history[id]?.toList() ?: emptyList())

    fun statusFor(id: String): Status = progressFor(id).status

    /** Records one answer. Returns nothing; call [saveSession] when the quiz ends. */
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

    fun saveSession(total: Int, correct: Int, topics: List<Int>) {
        sessions.add(
            SessionRecord(
                timestamp = System.currentTimeMillis(),
                total = total,
                correct = correct,
                topics = topics,
                countsAfter = counts(),
            )
        )
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

    // ---------------------------------------------------------------- storage

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())

            val q = root.optJSONObject("questions")
            if (q != null) {
                for (key in q.keys()) {
                    val arr = q.getJSONArray(key)
                    history[key] = (0 until arr.length())
                        .map { arr.getBoolean(it) }
                        .toMutableList()
                }
            }

            val s = root.optJSONArray("sessions")
            if (s != null) {
                for (i in 0 until s.length()) {
                    val o = s.getJSONObject(i)
                    val c = o.getJSONObject("countsAfter")
                    val t = o.optJSONArray("topics")
                    sessions.add(
                        SessionRecord(
                            timestamp = o.getLong("timestamp"),
                            total = o.getInt("total"),
                            correct = o.getInt("correct"),
                            topics = if (t == null) emptyList() else (0 until t.length()).map { t.getInt(it) },
                            countsAfter = StatusCounts(
                                c.getInt("green"), c.getInt("yellow"), c.getInt("red"), c.getInt("new"),
                            ),
                        )
                    )
                }
            }
        }
    }

    private fun save() {
        runCatching {
            val questions = JSONObject()
            for ((key, results) in history) {
                questions.put(key, JSONArray().apply { results.forEach { put(it) } })
            }
            val sessionArray = JSONArray()
            for (s in sessions) {
                sessionArray.put(
                    JSONObject().apply {
                        put("timestamp", s.timestamp)
                        put("total", s.total)
                        put("correct", s.correct)
                        put("topics", JSONArray().apply { s.topics.forEach { put(it) } })
                        put(
                            "countsAfter",
                            JSONObject().apply {
                                put("green", s.countsAfter.green)
                                put("yellow", s.countsAfter.yellow)
                                put("red", s.countsAfter.red)
                                put("new", s.countsAfter.new)
                            },
                        )
                    }
                )
            }
            file.writeText(
                JSONObject().apply {
                    put("questions", questions)
                    put("sessions", sessionArray)
                }.toString()
            )
        }
    }

    /** Persists the attempt history without closing a session (used when a quiz is abandoned). */
    fun flush() = save()

    private companion object {
        const val HISTORY = 10
        const val MAX_SESSIONS = 200
    }
}
