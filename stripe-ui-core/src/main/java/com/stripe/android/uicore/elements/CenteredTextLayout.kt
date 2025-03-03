package com.stripe.android.uicore.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun CenteredTextLayout(
    startContent: @Composable () -> Unit,
    textContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(startContent, textContent),
        modifier = modifier,
    ) { (startContentMeasurables, textContentMeasurables), constraints ->
        require(startContentMeasurables.size == 1) {
            "startContentMeasurables should only emit one composable"
        }
        require(textContentMeasurables.size == 1) {
            "textContentMeasurables should only emit one composable"
        }

        val startContentPlaceable = startContentMeasurables.first().measure(
            constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        )
        val targetTextWidth = constraints.maxWidth - startContentPlaceable.width
        val textContentPlaceable = textContentMeasurables.first().measure(
            constraints.copy(minWidth = targetTextWidth, maxWidth = targetTextWidth)
        )

        require(textContentPlaceable[FirstBaseline] != AlignmentLine.Unspecified) {
            "textContentPlaceable should have a first baseline"
        }

        val textBaseline = (textContentPlaceable[FirstBaseline] * TEXT_BASELINE_SCALE_FACTOR).toInt()
        val offsetToCenter = abs(startContentPlaceable.height - textBaseline) / 2

        val totalWidth = startContentPlaceable.width + textContentPlaceable.width
        val totalHeight = if (textBaseline > startContentPlaceable.height) {
            max(startContentPlaceable.height + offsetToCenter, textContentPlaceable.height)
        } else {
            max(startContentPlaceable.height, textContentPlaceable.height + offsetToCenter)
        }

        layout(totalWidth, totalHeight) {
            if (textBaseline > startContentPlaceable.height) {
                startContentPlaceable.place(0, offsetToCenter)
                textContentPlaceable.place(startContentPlaceable.width, 0)
            } else {
                startContentPlaceable.place(0, 0)
                textContentPlaceable.place(startContentPlaceable.width, offsetToCenter)
            }
        }
    }
}

// Text has intrinsic padding that we want to offset.
private const val TEXT_BASELINE_SCALE_FACTOR = 1.2
