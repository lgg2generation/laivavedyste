package lt.laivavedys.testai.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import lt.laivavedys.testai.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
private class BankFile(
    val topics: List<TopicDto>,
    val questions: List<QuestionDto>,
)

@Serializable
private class TopicDto(val id: Int, val name: String)

@Serializable
private class QuestionDto(
    val id: String,
    val topic: Int,
    val number: Int,
    val text: String,
    val images: List<String> = emptyList(),
    val options: List<OptionDto>,
    val answer: Int,
    @SerialName("answerSource") val answerSource: String = "doc",
    val reference: String? = null,
    val note: String? = null,
    val confidence: String? = null,
)

@Serializable
private class OptionDto(val text: String, val images: List<String> = emptyList())

/** The bundled question bank, read once from the shared Compose resources. */
object QuestionBank {

    var topics: List<Topic> = emptyList()
        private set

    var questions: List<Question> = emptyList()
        private set

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (questions.isNotEmpty()) return
        val bytes = Res.readBytes("files/questions.json")
        val parsed = json.decodeFromString<BankFile>(bytes.decodeToString())
        topics = parsed.topics.map { Topic(it.id, it.name) }
        questions = parsed.questions.map { q ->
            Question(
                id = q.id,
                topic = q.topic,
                number = q.number,
                text = q.text,
                images = q.images,
                options = q.options.map { Option(it.text, it.images) },
                answer = q.answer,
                answerSource = q.answerSource,
                reference = q.reference,
                note = q.note,
                confidence = q.confidence,
            )
        }
    }

    fun topicName(id: Int): String = topics.firstOrNull { it.id == id }?.name ?: "Tema $id"
}
