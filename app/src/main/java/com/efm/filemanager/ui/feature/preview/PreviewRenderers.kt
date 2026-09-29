package com.efm.filemanager.ui.feature.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.efm.filemanager.R
import com.efm.filemanager.data.preview.renderPdfPages
import com.efm.filemanager.domain.model.FileEntry
import com.efm.filemanager.domain.model.PreviewType
import com.efm.filemanager.domain.model.previewType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f

/**
 * [onLongPress] is a second, outer gesture layer -- entering selection mode the same long-press
 * way Browse/Search/Duplicates already do, docs/PLAN.md Phase 9 -- deliberately kept on its own
 * outer [Box] rather than threaded into each preview type's own gesture handling (pinch-zoom,
 * ExoPlayer, PDF scroll): since [detectTapGestures] here only ever consumes a recognized tap or
 * long-press and never a drag or a second pointer, an in-progress pinch/scroll/video-tap still
 * reaches its own handler underneath unaffected.
 */
@Composable
internal fun PreviewPage(
    entry: FileEntry,
    onLongPress: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(entry.uri) { detectTapGestures(onLongPress = { onLongPress() }) },
    ) {
        when (entry.previewType()) {
            PreviewType.IMAGE -> ImagePreview(entry)
            PreviewType.VIDEO, PreviewType.AUDIO -> MediaPreview(entry)
            PreviewType.PDF -> PdfPreview(entry)
            PreviewType.NONE -> UnsupportedPreview()
        }
    }
}

@Composable
private fun ImagePreview(entry: FileEntry) {
    var scale by remember(entry.uri) { mutableFloatStateOf(1f) }
    var offset by remember(entry.uri) { mutableStateOf(Offset.Zero) }
    AsyncImage(
        model = entry.uri,
        contentDescription = entry.name,
        contentScale = ContentScale.Fit,
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                .pointerInput(entry.uri) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        offset += pan
                    }
                },
    )
}

@Composable
private fun MediaPreview(entry: FileEntry) {
    val context = LocalContext.current
    val exoPlayer =
        remember(entry.uri) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(entry.uri))
                prepare()
                playWhenReady = true
            }
        }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }
    AndroidView(factory = { context2 -> PlayerView(context2).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun PdfPreview(entry: FileEntry) {
    val context = LocalContext.current
    var pages by remember(entry.uri) { mutableStateOf<List<Bitmap>?>(null) }

    LaunchedEffect(entry.uri) {
        pages = withContext(Dispatchers.IO) { renderPdfPages(context, entry.uri) }
    }

    val loadedPages = pages
    when {
        loadedPages == null -> PreviewCentered { CircularProgressIndicator() }
        loadedPages.isEmpty() -> PreviewCentered { Text(stringResource(R.string.preview_pdf_failed)) }
        else -> PdfPageList(loadedPages)
    }
}

@Composable
private fun PdfPageList(pages: List<Bitmap>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pages) { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun UnsupportedPreview() {
    PreviewCentered { Text(stringResource(R.string.preview_unsupported)) }
}

@Composable
private fun PreviewCentered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
