package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.SemanticsMatcher

private const val IS_PLACED = "is_placed_in_layout"

/**
 * This matcher checks if a given composable node is placed on the screen.
 *
 * Composable nodes may be removed but cached for reuse. This happens when using
 * LazyColumn/LazyRow. The Compose testing framework will still be able to find
 * these nodes even if they are not displayed. This matcher checks ensures that found
 * node is placed on the laid out screen and not cached by lazy lists or another
 * recycling composable.
 */
internal fun isPlaced() = SemanticsMatcher(IS_PLACED) { node ->
    node.layoutInfo.isPlaced
}
