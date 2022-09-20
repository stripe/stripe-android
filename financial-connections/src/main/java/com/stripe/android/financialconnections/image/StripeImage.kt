package com.stripe.android.financialconnections.image

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest

@Composable
internal fun StripeImage(
    url: String,
    placeholder: Painter,
    imageLoader: ImageLoader,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var drawableState by remember { mutableStateOf<Drawable?>(null) }
    var loadImageJob: Disposable?
    DisposableEffect(url) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .target { drawable -> drawableState = drawable }
            .build()
        loadImageJob = imageLoader.enqueue(request)
        onDispose { loadImageJob?.dispose() }
    }
    Image(
        modifier = modifier,
        contentDescription = contentDescription,
        painter = drawableState
            ?.toBitmap()
            ?.asImageBitmap()
            ?.let { BitmapPainter(it) }
            ?: placeholder
    )
}

