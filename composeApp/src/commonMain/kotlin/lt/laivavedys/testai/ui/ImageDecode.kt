package lt.laivavedys.testai.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Decodes a bundled PNG/JPEG. Each platform uses its own image decoder. */
expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?
