package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * This method calculates the width of the composable. It can be used such that the parent view
 * wrapping the composable can adjust its width.
 *
 * Adapted from: https://stackoverflow.com/a/70508246
 *
 * @param composable the composable that will be measured
 * @param content the content to render, calls back with the calculated width of the composable
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MeasureComposableWidth(
    composable: @Composable () -> Unit,
    content: @Composable (width: Dp) -> Unit
) {
    SubcomposeLayout { constraints ->
        val composableWidth = subcompose("measuredComposable") {
            composable()
        }[0].measure(Constraints()).width.toDp()

        val contentPlaceable = subcompose("content") {
            content(composableWidth)
        }[0].measure(constraints)

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}
