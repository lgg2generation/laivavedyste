package lt.laivavedys.testai.data

import android.content.Context
import org.json.JSONObject

/** Loads the bundled question bank from assets/questions.json exactly once. */
object QuestionBank {

    lateinit var topics: List<Topic>
        private set

    lateinit var questions: List<Question>
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val raw = context.assets.open("questions.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)

        val topicArray = root.getJSONArray("topics")
        topics = (0 until topicArray.length()).map { i ->
            val o = topicArray.getJSONObject(i)
            Topic(o.getInt("id"), o.getString("name"))
        }

        val questionArray = root.getJSONArray("questions")
        questions = (0 until questionArray.length()).map { i ->
            val o = questionArray.getJSONObject(i)
            val optionArray = o.getJSONArray("options")
            Question(
                id = o.getString("id"),
                topic = o.getInt("topic"),
                number = o.getInt("number"),
                text = o.getString("text"),
                images = o.optJSONArray("images").toStringList(),
                options = (0 until optionArray.length()).map { j ->
                    val po = optionArray.getJSONObject(j)
                    Option(po.getString("text"), po.optJSONArray("images").toStringList())
                },
                answer = o.getInt("answer"),
                answerSource = o.optString("answerSource", "doc"),
                reference = o.optStringOrNull("reference"),
                note = o.optStringOrNull("note"),
                confidence = o.optStringOrNull("confidence"),
            )
        }
        loaded = true
    }

    fun topicName(id: Int): String = topics.firstOrNull { it.id == id }?.name ?: "Tema $id"

    fun countByTopic(id: Int): Int = questions.count { it.topic == id }

    private fun org.json.JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
