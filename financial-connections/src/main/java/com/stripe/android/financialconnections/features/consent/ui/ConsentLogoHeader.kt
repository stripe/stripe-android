package com.stripe.android.financialconnections.features.consent.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors

private val LogoSize = 72.dp
private val DotsContainerHeight = 6.dp
private val DotsContainerWidth = 32.dp

@Composable
internal fun ConsentLogoHeader(
    modifier: Modifier = Modifier,
    logos: List<String>
) {
    val isPreview = LocalInspectionMode.current
    val localDensity = LocalDensity.current
    val bitmapLoadSize = remember { with(localDensity) { 36.dp.toPx().toInt() } }
    val stripeImageLoader = LocalImageLoader.current
    val placeholderBitmap = rememberPlaceholderBitmap(bitmapLoadSize, colors.border)
    val loadedImages: SnapshotStateList<ImageBitmap> = remember {
        SnapshotStateList<ImageBitmap>().also {
            if (isPreview) {
                // preview: initially fill with placeholders
                it.addAll(debugPreviewBitmaps(logos, bitmapLoadSize))
            } else {
                // prod: initially fill with placeholders
                for (i in logos.indices) it.add(placeholderBitmap)
            }
        }
    }

    logos.forEachIndexed { index, logo ->
        LaunchedEffect(logo) {
            stripeImageLoader.load(logo, bitmapLoadSize, bitmapLoadSize)
                .onSuccess { result ->
                    // TODO handle null image.
                    result?.let { loadedImages[index] = it.asImageBitmap() }
                }
                .onFailure {
                    // TODO handle image failure.
                    Log.e("ConsentLogoHeader", "Failed to load image: $logo")
                }
        }
    }

    Box(
        modifier = modifier
            .height(LogoSize)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        BackgroundRow(images = loadedImages)
        ForegroundRow(images = loadedImages)
    }
}

@Composable
private fun BackgroundRow(images: List<ImageBitmap>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for ((index, image) in images.withIndex()) {
            Spacer(modifier = Modifier.width(LogoSize))

            if (index != images.lastIndex) {
                val nextImage = images[index + 1]
                AnimatedDotsWithFixedGradient(
                    startColor = getPrevalentColorCloseToDots(
                        bitmap = image.asAndroidBitmap(),
                        startSide = true,
                    ),
                    endColor = getPrevalentColorCloseToDots(
                        bitmap = nextImage.asAndroidBitmap(),
                        startSide = false,
                    )
                )
            }
        }
    }
}

@Composable
private fun ForegroundRow(images: List<ImageBitmap>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for ((index, image) in images.withIndex()) {
            Logo(image)
            if (index != images.lastIndex) {
                Spacer(modifier = Modifier.width(DotsContainerWidth))
            }
        }
    }
}

private fun debugPreviewBitmaps(
    size: List<String>,
    bitmapLoadSize: Int,
): List<ImageBitmap> {
    return listOf(Color.Red, Color.Blue, Color.Green).take(size.size).map {
        val androidBitmap = Bitmap.createBitmap(bitmapLoadSize, bitmapLoadSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(androidBitmap)
        canvas.drawColor(it.toArgb())
        androidBitmap.asImageBitmap()
    }
}

@Composable
private fun rememberPlaceholderBitmap(bitmapLoadSize: Int, placeholderColor: Color): ImageBitmap = remember {
    val androidBitmap = Bitmap.createBitmap(
        bitmapLoadSize,
        bitmapLoadSize,
        Bitmap.Config.ARGB_8888
    )
    androidBitmap.eraseColor(placeholderColor.toArgb())
    androidBitmap.asImageBitmap()
}

@Composable
private fun AnimatedDotsWithFixedGradient(
    modifier: Modifier = Modifier,
    startColor: Color,
    endColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "animated-dots-transition"
    )
    val animatedOffset by infiniteTransition.animateFloat(
        label = "animated-dots",
        initialValue = 0f,
        targetValue = with(LocalDensity.current) { 10.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
    )

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(startColor, endColor)
    )

    Box(
        modifier = modifier
            .width(DotsContainerWidth)
            .height(DotsContainerHeight)
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val dotRadius = 3.dp.toPx()
            val dotSpacing = 10.dp.toPx()
            val dotY = center.y
            val numberOfDots = (size.width / dotSpacing).toInt() + 2 // Enough dots to fill the screen + 2 extra

            // Create a path for the animated dots
            val path = Path().apply {
                for (i in -1 until numberOfDots) { // Start with -1 to have an off-screen dot
                    val x = i * dotSpacing + animatedOffset - dotSpacing // Offset by one dot spacing
                    addOval(Rect(Offset(x, dotY - dotRadius), Size(dotRadius * 2, dotRadius * 2)))
                }
            }

            // Clip the canvas to the path of the dots
            clipPath(path) {
                // Draw the gradient within the clipped area (where the dots are)
                drawRect(
                    brush = gradientBrush,
                    size = Size(size.width, dotRadius * 2),
                    topLeft = Offset(0f, dotY - dotRadius)
                )
            }
        }
    }
}

@Composable
private fun Logo(imageBitmap: ImageBitmap) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .size(LogoSize)
            .shadow(8.dp, shape)
            .clip(shape)
    ) {
        Crossfade(
            targetState = imageBitmap,
            animationSpec = tween(durationMillis = 300)
        ) { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Get the prevalent color of the bitmap in the given quarter to generate the gradient for the dots.
 *
 * It focuses on the horizontal center and either on the 1st or 4th vertical quarter of the
 * bitmap, depending on the [startSide] parameter, narrowing the main color search to the area of the logo
 * the dots will be merging with.
 *
 * @param startSide whether the quarter is the start or end of the bitmap
 */
private fun getPrevalentColorCloseToDots(bitmap: Bitmap, startSide: Boolean): Color {
    val colorMap = HashMap<Int, Int>()
    val width = bitmap.width
    val height = bitmap.height
    val startX = if (startSide) 0 else (width * 3) / 4
    val endX = if (startSide) width / 4 else width
    val startY = height * 2 / 5 // 40% of the height
    val endY = height * 3 / 5 // 60% of the height

    for (x in startX until endX) {
        for (y in startY until endY) {
            val pixelColor = bitmap.getPixel(x, y)
            colorMap[pixelColor] = (colorMap[pixelColor] ?: 0) + 1
        }
    }
    return colorMap
        .maxByOrNull { it.value }
        ?.let { Color(it.key) }
        ?: Color.Black // Default color if no prevalent color is found
}
