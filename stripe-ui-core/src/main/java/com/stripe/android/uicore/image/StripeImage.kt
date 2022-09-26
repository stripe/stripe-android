package com.stripe.android.uicore.image

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp.Companion.Infinity
import androidx.compose.ui.unit.IntSize.Companion.Zero
import kotlinx.coroutines.launch

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StripeImage(
    url: String,
    placeholder: Painter,
    imageLoader: StripeImageLoader,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val (width, height) = calculateBoxSize()
        val painter: MutableState<Painter> =
            remember { mutableStateOf(BitmapPainter(ImageBitmap(width, height))) }
        LaunchedEffect(url) {
            launch {
                imageLoader
                    .load(url, width, height)
                    .fold(
                        onSuccess = { bitmap -> BitmapPainter(bitmap.asImageBitmap()) },
                        onFailure = { placeholder }
                    ).let { painter.value = it }
            }
        }
        Image(
            modifier = modifier,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            painter = painter.value
        )
    }
}

private fun BoxWithConstraintsScope.calculateBoxSize(): Pair<Int, Int> {
    var width =
        if (constraints.maxWidth > Zero.width &&
            constraints.maxWidth < Infinity.value.toInt()
        ) {
            constraints.maxWidth
        } else {
            -1
        }

    var height =
        if (constraints.maxHeight > Zero.height &&
            constraints.maxHeight < Infinity.value.toInt()
        ) {
            constraints.maxHeight
        } else {
            -1
        }

    // if height xor width not able to be determined, make image a square of the determined dimension
    if (width == -1) width = height
    if (height == -1) height = width
    return Pair(width, height)
}
