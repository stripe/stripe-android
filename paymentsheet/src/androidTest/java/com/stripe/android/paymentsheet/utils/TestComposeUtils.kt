package com.stripe.android.paymentsheet.utils

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.time.Duration.Companion.seconds

private const val IS_PLACED = "is_placed_in_layout"

internal fun String.withLtrIsolate(): String = "\u2066$this\u2069"

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

internal fun ComposeTestRule.replaceText(label: String, text: String) {
    val matcher = hasText(label).and(hasSetTextAction())
    waitForNode(matcher)
    onNode(matcher).performScrollTo().performTextReplacement(text)
}

internal fun ComposeTestRule.waitForText(text: String) {
    waitForNode(hasText(text))
}

internal fun ComposeTestRule.waitForNode(matcher: SemanticsMatcher) {
    waitUntil(
        conditionDescription = "node matching $matcher to appear",
        timeoutMillis = 5.seconds.inWholeMilliseconds,
    ) {
        onAllNodes(matcher)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .isNotEmpty()
    }
}
