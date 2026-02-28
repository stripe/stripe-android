package com.stripe.android.paymentsheet.example.playground.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun LoggerBubble(
    logCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var bubbleSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val initialOffsetPx = with(density) { 16.dp.roundToPx() }

    var offset by remember(containerSize, bubbleSize) {
        mutableStateOf(
            IntOffset(
                x = (containerSize.width - bubbleSize.width - initialOffsetPx).coerceAtLeast(0),
                y = ((containerSize.height - bubbleSize.height) / 2).coerceAtLeast(0),
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                containerSize = coordinates.size
            },
    ) {
        Box(
            modifier = Modifier
                .offset { offset }
                .onGloballyPositioned { coordinates ->
                    bubbleSize = coordinates.size
                }
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        val maxX = (containerSize.width - bubbleSize.width).coerceAtLeast(0)
                        val maxY = (containerSize.height - bubbleSize.height).coerceAtLeast(0)
                        offset = IntOffset(
                            x = (offset.x + dragAmount.x.roundToInt()).coerceIn(0, maxX),
                            y = (offset.y + dragAmount.y.roundToInt()).coerceIn(0, maxY),
                        )
                    }
                },
        ) {
            FloatingActionButton(
                onClick = onClick,
                backgroundColor = MaterialTheme.colors.primary,
                modifier = Modifier.size(56.dp),
            ) {
                Text(
                    text = "LOG",
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (logCount > 0) {
                Badge(
                    count = logCount,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun Badge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    @Suppress("MagicNumber")
    val displayText = if (count > 99) "99+" else count.toString()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(20.dp)
            .background(Color.Red, CircleShape),
    ) {
        Text(
            text = displayText,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
