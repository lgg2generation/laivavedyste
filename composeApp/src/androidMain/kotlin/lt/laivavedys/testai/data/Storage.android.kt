package lt.laivavedys.testai.data

import java.io.File

/** Set once from the Activity before any progress is read. */
object AndroidStorageDir {
    lateinit var filesDir: File
}

actual class Storage actual constructor() {

    private val file: File get() = File(AndroidStorageDir.filesDir, "progress.json")

    actual fun read(): String? = runCatching {
        if (file.exists()) file.readText() else null
    }.getOrNull()

    actual fun write(text: String) {
        runCatching { file.writeText(text) }
    }
}

actual fun nowMillis(): Long = System.currentTimeMillis()

actual fun formatTimestamp(millis: Long, withDate: Boolean): String {
    val pattern = if (withDate) "yyyy-MM-dd HH:mm" else "MM-dd HH:mm"
    return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        .format(java.util.Date(millis))
}
