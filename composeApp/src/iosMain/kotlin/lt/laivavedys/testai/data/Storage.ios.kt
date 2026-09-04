package lt.laivavedys.testai.data

import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual class Storage actual constructor() {

    private val path: String
        get() {
            val dirs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            val dir = dirs.firstOrNull() as? String ?: ""
            return "$dir/progress.json"
        }

    actual fun read(): String? =
        NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

    actual fun write(text: String) {
        (text as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
    }
}

actual fun nowMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun formatTimestamp(millis: Long, withDate: Boolean): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = if (withDate) "yyyy-MM-dd HH:mm" else "MM-dd HH:mm"
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    return formatter.stringFromDate(date)
}
