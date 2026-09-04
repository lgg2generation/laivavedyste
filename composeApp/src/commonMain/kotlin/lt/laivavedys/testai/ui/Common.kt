package lt.laivavedys.testai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import lt.laivavedys.testai.resources.Res
import lt.laivavedys.testai.data.StatusCounts
import lt.laivavedys.testai.ui.theme.StatusGreen
import lt.laivavedys.testai.ui.theme.StatusNew
import lt.laivavedys.testai.ui.theme.StatusRed
import lt.laivavedys.testai.ui.theme.StatusYellow
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Decoded images, kept for the life of the process - they are small. */
private val imageCache = mutableMapOf<String, ImageBitmap>()

/** Draws a picture bundled in the shared resources under files/images. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun AssetImage(name: String, modifier: Modifier = Modifier) {
    var bitmap by remember(name) { mutableStateOf(imageCache[name]) }

    LaunchedEffect(name) {
        if (bitmap == null) {
            val decoded = runCatching {
                Res.readBytes("files/images/$name").decodeToImageBitmap()
            }.getOrNull()
            if (decoded != null) {
                imageCache[name] = decoded
                bitmap = decoded
            }
        }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

@Composable
fun QuestionImages(images: List<String>, modifier: Modifier = Modifier) {
    if (images.isEmpty()) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            images.forEach { AssetImage(it, Modifier.heightIn(max = 220.dp)) }
        }
    }
}

/** A single horizontal bar split into the traffic-light segments. */
@Composable
fun StatusBar(counts: StatusCounts, modifier: Modifier = Modifier, height: Int = 10) {
    val total = if (counts.total <= 0) 1 else counts.total
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Segment(counts.green, total, StatusGreen)
        Segment(counts.yellow, total, StatusYellow)
        Segment(counts.red, total, StatusRed)
    }
}

@Composable
private fun RowScope.Segment(value: Int, total: Int, color: Color) {
    if (value <= 0) return
    Box(
        modifier = Modifier
            .weight(value.toFloat() / total)
            .fillMaxWidth()
            .background(color),
    ) {}
}

@Composable
fun LegendDot(color: Color, label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Column(Modifier.padding(start = 6.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusLegend(counts: StatusCounts, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LegendDot(StatusGreen, "moku", counts.green)
        LegendDot(StatusYellow, "kartoti", counts.yellow)
        LegendDot(StatusRed, "silpna", counts.red)
        LegendDot(StatusNew, "nespręsta", counts.new)
    }
}
