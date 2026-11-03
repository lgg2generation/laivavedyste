package lt.laivavedys.testai.focus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Set from MainActivity, BlockActivity and the service before anything else runs. */
internal object FocusContext {
    lateinit var app: Context
}

/**
 * The time account, kept in SharedPreferences. The activity and the
 * accessibility service run in the same process and on the main thread, so
 * they always see each other's writes.
 */
internal object FocusStore {
    private const val PREFS = "focus"
    private const val KEY_ANSWERED = "answered_towards_next"
    private const val KEY_REMAINING = "remaining_ms"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun answered(c: Context): Int = prefs(c).getInt(KEY_ANSWERED, 0)

    fun remaining(c: Context): Long = prefs(c).getLong(KEY_REMAINING, 0L)

    /** Returns the minutes unlocked by this answer. */
    fun recordAnswer(c: Context): Int {
        var answered = answered(c) + 1
        var reward = 0
        var remaining = remaining(c)
        if (answered >= QUESTIONS_PER_REWARD) {
            answered = 0
            reward = REWARD_MINUTES
            remaining += REWARD_MINUTES * 60_000L
        }
        prefs(c).edit()
            .putInt(KEY_ANSWERED, answered)
            .putLong(KEY_REMAINING, remaining)
            .apply()
        return reward
    }

    /** Takes [millis] off the unlocked time; returns what is left. */
    fun consume(c: Context, millis: Long): Long {
        val left = (remaining(c) - millis.coerceAtLeast(0)).coerceAtLeast(0)
        prefs(c).edit().putLong(KEY_REMAINING, left).apply()
        return left
    }
}

internal fun isBlockerEnabled(c: Context): Boolean {
    val enabled = Settings.Secure.getString(
        c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val me = ComponentName(c, BlockerService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
}

actual object FocusGate {
    actual val supported: Boolean = true

    actual fun onQuestionAnswered(): Int = FocusStore.recordAnswer(FocusContext.app)

    actual fun status(): FocusStatus {
        val c = FocusContext.app
        return FocusStatus(
            active = isBlockerEnabled(c),
            remainingMillis = FocusStore.remaining(c),
            answeredTowardsNext = FocusStore.answered(c),
        )
    }

    actual fun openServiceSettings() {
        open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    actual fun openAppInfo() {
        val c = FocusContext.app
        open(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", c.packageName, null)),
        )
    }

    private fun open(intent: Intent) {
        runCatching {
            FocusContext.app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
