package lt.laivavedys.testai.data

/** One answer option. [images] are file names inside assets/images. */
data class Option(
    val text: String,
    val images: List<String> = emptyList(),
)

data class Question(
    val id: String,
    val topic: Int,
    val number: Int,
    val text: String,
    val images: List<String>,
    val options: List<Option>,
    /** Index of the correct option. */
    val answer: Int,
    /** "doc" when the source document underlined the answer, "derived" when it was reconstructed. */
    val answerSource: String,
    /** Rule reference lifted out of the option text, shown only after answering. */
    val reference: String? = null,
    /** Why a derived answer was chosen. */
    val note: String? = null,
    /** high / medium / low, for derived answers. */
    val confidence: String? = null,
) {
    val isDerived: Boolean get() = answerSource != "doc"
}

data class Topic(val id: Int, val name: String)

/**
 * How well a question is known, from the last two attempts.
 *
 *  GREEN  - both of the last two attempts were correct
 *  YELLOW - one of the last two was wrong, or it has only ever been answered right once
 *  RED    - both of the last two were wrong, or the only attempt was wrong
 *  NEW    - never answered
 */
enum class Status { NEW, RED, YELLOW, GREEN }

data class QuestionProgress(
    /** Most recent results last; true = correct. */
    val results: List<Boolean> = emptyList(),
) {
    val attempts: Int get() = results.size

    val status: Status
        get() {
            if (results.isEmpty()) return Status.NEW
            val lastTwo = results.takeLast(2)
            val wrong = lastTwo.count { !it }
            return when {
                wrong == 0 && lastTwo.size >= 2 -> Status.GREEN
                wrong == 0 -> Status.YELLOW      // answered right once, not yet proven twice
                wrong == lastTwo.size -> Status.RED
                else -> Status.YELLOW
            }
        }
}

data class StatusCounts(
    val green: Int = 0,
    val yellow: Int = 0,
    val red: Int = 0,
    val new: Int = 0,
) {
    val total: Int get() = green + yellow + red + new
    val seen: Int get() = green + yellow + red

    operator fun minus(other: StatusCounts) = StatusCounts(
        green - other.green, yellow - other.yellow, red - other.red, new - other.new,
    )
}

data class SessionRecord(
    val timestamp: Long,
    val total: Int,
    val correct: Int,
    val topics: List<Int>,
    /** Status counts over the whole bank right after this session finished. */
    val countsAfter: StatusCounts,
)
