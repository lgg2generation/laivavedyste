package lt.laivavedys.testai.focus

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/** Apps that stay locked while no unlocked time is left. */
private val BLOCKED_APPS = mapOf(
    "com.instagram.android" to "Instagram",
    "com.instagram.lite" to "Instagram",
    "com.facebook.katana" to "Facebook",
    "com.facebook.lite" to "Facebook",
    "com.linkedin.android" to "LinkedIn",
    "com.google.android.youtube" to "YouTube",
)

/** The same sites opened in a browser. */
private val BLOCKED_SITES = mapOf(
    "instagram.com" to "Instagram",
    "facebook.com" to "Facebook",
    "fb.com" to "Facebook",
    "linkedin.com" to "LinkedIn",
    "youtube.com" to "YouTube",
    "youtu.be" to "YouTube",
)

/** Browsers whose address bar we can read, with the view ids of that bar. */
private val BROWSERS = mapOf(
    "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
    "com.sec.android.app.sbrowser" to listOf(
        "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "com.sec.android.app.sbrowser:id/sbrowser_url_bar",
    ),
)

/** Windows that sit on top of an app without the user leaving it. */
private val OVERLAY_PACKAGES = setOf(
    "com.android.systemui",
    "com.samsung.android.honeyboard",
    "com.google.android.inputmethod.latin",
    "com.samsung.android.app.cocktailbarservice",
)

private const val TICK_MS = 1_000L
private const val BROWSER_SCAN_MS = 1_000L
private const val BLOCK_SCREEN_GAP_MS = 2_000L

/**
 * Watches which app is in front. While Instagram, Facebook, LinkedIn or
 * YouTube is open the unlocked time runs down; when none is left the user is
 * sent to the home screen and shown [BlockActivity].
 */
class BlockerService : AccessibilityService() {

    private data class Target(val key: String, val label: String)

    private val handler = Handler(Looper.getMainLooper())
    private var current: Target? = null
    private var lastTick = 0L
    private var warned = false
    private var lastBlockScreen = 0L
    private var lastBrowserScan = 0L
    private var keyboardPackage: String? = null

    /** Last verdict per browser; the address bar is hidden in full-screen video. */
    private val browserVerdict = mutableMapOf<String, Target?>()

    private val tick = object : Runnable {
        override fun run() {
            val target = current ?: return
            // Fallback for a missed window event: another app is in front.
            val front = rootInActiveWindow?.packageName?.toString()
            if (front != null && front !in BLOCKED_APPS && front !in BROWSERS && !isOverlay(front)) {
                leave()
                return
            }
            val left = account()
            if (left <= 0) {
                block(target)
            } else {
                if (!warned && left <= 60_000L) {
                    warned = true
                    toast("Liko 1 min.")
                }
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    override fun onServiceConnected() {
        FocusContext.app = applicationContext
        keyboardPackage = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (isOverlay(pkg)) return
        val windowChanged = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val label = BLOCKED_APPS[pkg]
        when {
            label != null -> if (current?.key != pkg && (windowChanged || isInFront(pkg))) {
                enter(Target(pkg, label))
            }
            pkg in BROWSERS -> onBrowserEvent(pkg, windowChanged)
            windowChanged && isActivity(pkg, event.className) -> leave()
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        leave()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        leave()
        super.onDestroy()
    }

    private fun onBrowserEvent(pkg: String, windowChanged: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (!windowChanged && now - lastBrowserScan < BROWSER_SCAN_MS) return
        lastBrowserScan = now
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val url = BROWSERS.getValue(pkg).firstNotNullOfOrNull { id ->
            root.findAccessibilityNodeInfosByViewId(id).firstOrNull()?.text?.toString()
        }
        if (url != null) {
            browserVerdict[pkg] = blockedSite(url)?.let { Target("web:$pkg:$it", "$it (naršyklėje)") }
        }
        val verdict = browserVerdict[pkg]
        when {
            verdict != null -> enter(verdict)
            windowChanged || url != null -> leave()
        }
    }

    private fun enter(target: Target) {
        if (current?.key == target.key) return
        if (current != null) account()
        current = target
        lastTick = SystemClock.elapsedRealtime()
        warned = false
        handler.removeCallbacks(tick)

        val left = FocusStore.remaining(this)
        if (left <= 0) {
            block(target)
        } else {
            toast("${target.label}: liko ${formatMinutes(left)} min.")
            handler.postDelayed(tick, TICK_MS)
        }
    }

    private fun leave() {
        if (current == null) return
        account()
        current = null
        handler.removeCallbacks(tick)
    }

    /** Charges the time since the last tick; nothing is charged while the screen is off. */
    private fun account(): Long {
        val now = SystemClock.elapsedRealtime()
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        val spent = if (interactive) now - lastTick else 0L
        lastTick = now
        return FocusStore.consume(this, spent)
    }

    private fun block(target: Target) {
        current = null
        handler.removeCallbacks(tick)
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockScreen < BLOCK_SCREEN_GAP_MS) return
        lastBlockScreen = now
        runCatching {
            startActivity(
                Intent(this, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(BlockActivity.EXTRA_LABEL, target.label),
            )
        }
    }

    private fun isOverlay(pkg: String) = pkg in OVERLAY_PACKAGES || pkg == keyboardPackage

    private fun isInFront(pkg: String) = rootInActiveWindow?.packageName?.toString() == pkg

    /** Dialogs, menus and toasts also raise window events; only an activity means a switch. */
    private fun isActivity(pkg: String, className: CharSequence?): Boolean {
        if (className == null) return false
        return try {
            packageManager.getActivityInfo(ComponentName(pkg, className.toString()), 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}

/** "Instagram" when the address bar shows one of the blocked sites. */
private fun blockedSite(url: String): String? {
    val host = url.trim().lowercase()
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore(':')
    return BLOCKED_SITES.entries.firstOrNull { (domain, _) ->
        host == domain || host.endsWith(".$domain")
    }?.value
}
