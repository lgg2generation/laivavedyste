package lt.laivavedys.testai.focus

/** Answered questions that unlock one portion of social-media time. */
const val QUESTIONS_PER_REWARD = 30

/** Minutes unlocked per [QUESTIONS_PER_REWARD] answered questions. */
const val REWARD_MINUTES = 5

data class FocusStatus(
    /** The blocking service is switched on in the system settings. */
    val active: Boolean,
    /** Unlocked social-media time not used yet. */
    val remainingMillis: Long,
    /** Questions answered since the last reward, 0 until [QUESTIONS_PER_REWARD]. */
    val answeredTowardsNext: Int,
)

/**
 * Keeps Instagram, Facebook, LinkedIn and YouTube locked until enough
 * questions have been answered. Android only: iOS gives third-party apps no
 * way to block other apps without Apple's Screen Time entitlement.
 */
expect object FocusGate {
    val supported: Boolean

    /** Records one answered question; returns the minutes it unlocked (usually 0). */
    fun onQuestionAnswered(): Int

    fun status(): FocusStatus

    /** Opens the system screen where the blocking service is switched on. */
    fun openServiceSettings()

    /** Opens this app's info page (needed once to allow restricted settings). */
    fun openAppInfo()
}

/** "4:05" for 245 000 ms. */
fun formatMinutes(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) + 999) / 1000
    val s = totalSeconds % 60
    return "${totalSeconds / 60}:${if (s < 10) "0" else ""}$s"
}
