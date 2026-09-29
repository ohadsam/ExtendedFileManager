package com.efm.filemanager.data.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

// Renders every page eagerly (simplest correct approach for a preview pane), capped so an
// unusually large PDF can't exhaust memory -- lazy per-page rendering is a reasonable later
// refinement once this preview pane needs to handle that case well, not a correctness fix.
private const val MAX_EAGER_PAGES = 30

internal fun renderPdfPages(
    context: Context,
    uri: Uri,
): List<Bitmap> {
    val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
    return descriptor.use { pfd -> renderAllPages(pfd) }
}

private fun renderAllPages(pfd: ParcelFileDescriptor): List<Bitmap> =
    PdfRenderer(pfd).use { renderer ->
        (0 until minOf(renderer.pageCount, MAX_EAGER_PAGES)).map { index -> renderPage(renderer, index) }
    }

private fun renderPage(
    renderer: PdfRenderer,
    index: Int,
): Bitmap =
    renderer.openPage(index).use { page ->
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }
