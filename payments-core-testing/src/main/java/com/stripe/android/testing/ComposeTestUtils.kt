package com.stripe.android.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement

private const val IS_PLACED = "is_placed_in_layout"

enum class ScrollBehavior {
    Never,
    Required,
    BestEffort,
}

/**
 * This matcher checks if a given composable node is placed on the screen.
 *
 * Composable nodes may be removed but cached for reuse. This happens when using
 * LazyColumn/LazyRow. The Compose testing framework will still be able to find
 * these nodes even if they are not displayed. This matcher checks ensures that found
 * node is placed on the laid out screen and not cached by lazy lists or another
 * recycling composable.
 */
fun isPlaced() = SemanticsMatcher(IS_PLACED) { node ->
    node.layoutInfo.isPlaced
}

fun ComposeTestRule.waitForNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long,
    atLeastOneRootRequired: Boolean = false,
) {
    waitUntil(
        conditionDescription = "node matching $matcher to appear",
        timeoutMillis = timeoutMillis,
    ) {
        onAllNodes(matcher)
            .fetchSemanticsNodes(atLeastOneRootRequired = atLeastOneRootRequired)
            .isNotEmpty()
    }
}

fun ComposeTestRule.waitForExactlyOneNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long,
) {
    waitUntil(
        conditionDescription = "exactly one node matching $matcher to appear",
        timeoutMillis = timeoutMillis,
    ) {
        onAllNodes(matcher)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .size == 1
    }
}

fun ComposeTestRule.waitForText(
    text: String,
    timeoutMillis: Long,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
) {
    waitForNode(
        matcher = hasText(text, substring = substring, ignoreCase = ignoreCase),
        timeoutMillis = timeoutMillis,
    )
}

fun ComposeTestRule.replaceText(
    targetText: String,
    text: String,
    substring: Boolean = false,
) {
    replaceText(
        matcher = hasText(targetText, substring = substring),
        text = text,
        scrollBehavior = ScrollBehavior.Required,
        settleAfterReplacement = false,
    )
}

fun ComposeTestRule.fillExpirationDate(text: String) {
    replaceText(
        matcher = hasContentDescription(value = "Expiration date", substring = true),
        text = text,
    )
}

fun ComposeTestRule.replaceText(
    matcher: SemanticsMatcher,
    text: String,
    scrollBehavior: ScrollBehavior = ScrollBehavior.Never,
    settleAfterReplacement: Boolean = false,
) {
    replaceText(
        node = onNode(matcher),
        text = text,
        scrollBehavior = scrollBehavior,
        settleAfterReplacement = settleAfterReplacement,
    )
}

fun ComposeTestRule.replaceText(
    node: SemanticsNodeInteraction,
    text: String,
    scrollBehavior: ScrollBehavior = ScrollBehavior.Never,
    settleAfterReplacement: Boolean = false,
) {
    when (scrollBehavior) {
        ScrollBehavior.Never -> Unit
        ScrollBehavior.Required -> node.performScrollTo()
        ScrollBehavior.BestEffort -> {
            runCatching { node.performScrollTo() }
        }
    }

    node.performTextReplacement(text)

    if (settleAfterReplacement) {
        waitForIdle()
    }
}
