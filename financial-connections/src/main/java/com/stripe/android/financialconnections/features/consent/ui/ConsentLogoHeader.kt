package com.stripe.android.financialconnections.features.consent.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader

@Composable
@Suppress("MagicNumber")
fun ConsentLogoHeader(
    modifier: Modifier = Modifier,
    logos: List<String>
) {
    val isPreview = LocalInspectionMode.current
    val stripeImageLoader = LocalImageLoader.current
    val localDensity = LocalDensity.current
    val isVisible = remember { mutableStateOf(false) }
    val bitmapLoadSize = remember { with(localDensity) { 36.dp.toPx().toInt() } }

    val images = if (isPreview) {
        isVisible.value = true
        previewBitmaps(bitmapLoadSize).take(logos.size)
    } else {
        // Actual image loading logic for runtime
        val loadedImages = remember { mutableStateListOf<ImageBitmap?>() }
        // Load images
        logos.forEachIndexed { index, logo ->
            LaunchedEffect(logo) {
                stripeImageLoader.load(
                    logo,
                    bitmapLoadSize,
                    bitmapLoadSize
                ).onSuccess { result ->
                    while (loadedImages.size <= index) { loadedImages.add(null) }
                    loadedImages[index] = result?.asImageBitmap()
                    // Set visibility to true when all images are loaded
                    if (loadedImages.size == logos.size && loadedImages.all { it != null }) {
                        isVisible.value = true
                    }
                }
            }
        }
        loadedImages
    }

    Box(modifier = modifier.height(72.dp)) {
        AnimatedVisibility(
            visible = isVisible.value,
            enter = fadeIn(animationSpec = tween(durationMillis = 1000)), // Customize duration as needed
            modifier = modifier.matchParentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (images.all { it != null }) {
                    images.filterNotNull().forEachIndexed { index, imageBitmap ->
                        Logo(imageBitmap)
                        if (index != images.lastIndex) {
                            images[index + 1]?.let { nextImage ->
                                AnimatedDotsWithFixedGradient(
                                    startColor = getDotColor(imageBitmap.asAndroidBitmap(), Quarter.END),
                                    endColor = getDotColor(nextImage.asAndroidBitmap(), Quarter.START)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun previewBitmaps(size: Int): List<ImageBitmap> =
    listOf(Color.Red, Color.Blue, Color.Green).map {
        val androidBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(androidBitmap)
        canvas.drawColor(it.toArgb())
        androidBitmap.asImageBitmap()
    }

@Composable
private fun Logo(imageBitmap: ImageBitmap) {
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        modifier = Modifier
            .width(72.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .shadow(elevation = 50.dp)
    )
}

enum class Quarter {
    START, END
}

@Suppress("MagicNumber")
private fun getDotColor(bitmap: Bitmap, quarter: Quarter): Color {
    val colorMap = HashMap<Int, Int>()
    val width = bitmap.width
    val height = bitmap.height
    val startX = if (quarter == Quarter.START) 0 else (width * 3) / 4
    val endX = if (quarter == Quarter.START) width / 4 else width
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
        targetValue = with(LocalDensity.current) { 10.dp.toPx() }, // One step forward
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing), // Adjust the speed as needed
            repeatMode = RepeatMode.Restart
        ),
    )

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(startColor, endColor)
    )

    Box(
        modifier = modifier
            .width(32.dp)
            .height(6.dp)
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val dotRadius = 3.dp.toPx()
            val dotSpacing = 10.dp.toPx() // The space between the centers of each dot
            val dotY = center.y // Vertical center
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

@Preview
@Composable
fun ConsentLogoHeaderPreview() {
    FinancialConnectionsPreview {
        ConsentLogoHeader(
            modifier = Modifier.fillMaxWidth(),
            logos = listOf(
                "https://stripe.com/img/v3/home/twitter.png",
                "https://stripe.com/img/v3/home/twitter.png",
            )
        )
    }
}

@Preview
@Composable
fun ConsentLogoHeaderPreviewThreeLogos() {
    FinancialConnectionsPreview {
        ConsentLogoHeader(
            modifier = Modifier.fillMaxWidth(),
            logos = listOf(
                "https://stripe.com/img/v3/home/twitter.png",
                "https://stripe.com/img/v3/home/twitter.png",
                "https://stripe.com/img/v3/home/twitter.png",
            )
        )
    }
}
