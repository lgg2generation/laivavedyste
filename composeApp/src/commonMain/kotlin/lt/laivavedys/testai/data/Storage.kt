package lt.laivavedys.testai.data

/**
 * The platform-specific pieces: where the progress file lives, the clock and
 * date formatting. Everything else in the app is shared code.
 */
expect class Storage() {
    fun read(): String?
    fun write(text: String)
}

expect fun nowMillis(): Long

/** Short local date/time, e.g. "2026-09-04 14:32". */
expect fun formatTimestamp(millis: Long, withDate: Boolean = true): String
