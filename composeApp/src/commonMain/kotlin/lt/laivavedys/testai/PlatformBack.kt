package lt.laivavedys.testai

import androidx.compose.runtime.Composable

/**
 * Android maps the system back gesture onto the shared navigation;
 * on iOS there is no system back button, so the actual is a no-op.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
