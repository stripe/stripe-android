package com.stripe.android.financialconnections.features.consent.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.LocalImageLoader
import kotlinx.coroutines.launch


@Composable
@Suppress("MagicNumber")
fun ConsentLogoHeader(
    modifier: Modifier = Modifier,
    logos: List<String>
) {
    val coroutineScope = rememberCoroutineScope()
    val stripeImageLoader = LocalImageLoader.current
    val localDensity = LocalDensity.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        /**
         * - 2 logos: (platform or institution)
         * - 3 logos: (connected account)
         * - Other # of logos: Fallback to Stripe logo as client can't render.
         */
        if (logos.size != 2 && logos.size != 3) {
            Image(
                painterResource(id = R.drawable.stripe_logo),
                contentDescription = null,
                modifier = Modifier
                    .width(60.dp)
                    .height(25.dp)
                    .clip(CircleShape),
            )
        } else {
            var image1 by remember { mutableStateOf<ImageBitmap?>(null) }
            var image2 by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(key1 = logos[0]) {
                coroutineScope.launch {
                    stripeImageLoader.load(
                        logos[0],
                        with(localDensity) { 36.dp.toPx().toInt() },
                        with(localDensity) { 36.dp.toPx().toInt() }
                    ).onSuccess { image1 = it?.asImageBitmap() }
                }
            }
            LaunchedEffect(key1 = logos[1]) {
                coroutineScope.launch {
                    stripeImageLoader.load(
                        logos[1],
                        with(localDensity) { 36.dp.toPx().toInt() },
                        with(localDensity) { 36.dp.toPx().toInt() }
                    ).onSuccess { image2 = it?.asImageBitmap() }
                }
            }

            if (image1 != null && image2 != null) {
                Image(
                    bitmap = image1!!,
                    contentDescription = null,
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                AnimatedDotsWithFixedGradient(
                    startColor = getMainColorOfCentralSection(image1!!.asAndroidBitmap(), Quarter.FOURTH),
                    endColor = getMainColorOfCentralSection(image2!!.asAndroidBitmap(), Quarter.FIRST),
                )
                Image(
                    bitmap = image2!!,
                    contentDescription = null,
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }
    }
}

enum class Quarter {
    FIRST, FOURTH
}

/**
 *
 */
fun getMainColorOfCentralSection(bitmap: Bitmap, quarter: Quarter): Color {
    val colorMap = HashMap<Int, Int>()
    val width = bitmap.width
    val height = bitmap.height
    val startX = if (quarter == Quarter.FIRST) 0 else (width * 3) / 4
    val endX = if (quarter == Quarter.FIRST) width / 4 else width
    val startY = height * 2 / 5 // 40% of the height
    val endY = height * 3 / 5 // 60% of the height

    for (x in startX until endX) {
        for (y in startY until endY) {
            val pixelColor = bitmap.getPixel(x, y)
            colorMap[pixelColor] = (colorMap[pixelColor] ?: 0) + 1
        }
    }

    val maxEntry = colorMap.maxByOrNull { it.value }

    return maxEntry?.let {
        Color(it.key)
    } ?: Color.Black // Default color if no max is found
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