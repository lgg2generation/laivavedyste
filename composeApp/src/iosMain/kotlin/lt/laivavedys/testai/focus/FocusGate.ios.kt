package lt.laivavedys.testai.focus

actual object FocusGate {
    actual val supported: Boolean = false

    actual fun onQuestionAnswered(): Int = 0

    actual fun status(): FocusStatus = FocusStatus(active = false, remainingMillis = 0, answeredTowardsNext = 0)

    actual fun openServiceSettings() {}

    actual fun openAppInfo() {}
}
