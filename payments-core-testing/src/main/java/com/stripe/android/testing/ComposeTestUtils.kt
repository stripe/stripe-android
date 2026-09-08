@file:Suppress("TooManyFunctions")

package com.stripe.android.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement

private const val IS_PLACED = "is_placed_in_layout"
private const val DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS = 5_000L

enum class ScrollBehavior {
    Never,
    Required,
    BestEffort,
}

private fun isPlaced() = SemanticsMatcher(IS_PLACED) { node ->
    node.layoutInfo.isPlaced
}

fun ComposeTestRule.waitForNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    atLeastOneRootRequired: Boolean = false,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitUntil(
        conditionDescription = conditionDescription ?: "node matching $matcher to appear",
        timeoutMillis = timeoutMillis,
    ) {
        onAllNodes(matcher, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes(atLeastOneRootRequired = atLeastOneRootRequired)
            .isNotEmpty()
    }
}

fun ComposeTestRule.waitForExactlyOneNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    atLeastOneRootRequired: Boolean = false,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitUntil(
        conditionDescription = conditionDescription ?: "exactly one node matching $matcher to appear",
        timeoutMillis = timeoutMillis,
    ) {
        onAllNodes(matcher, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes(atLeastOneRootRequired = atLeastOneRootRequired)
            .size == 1
    }
}

fun ComposeTestRule.waitForDisplayedNode(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitUntil(
        conditionDescription = conditionDescription ?: "node matching $matcher to be displayed",
        timeoutMillis = timeoutMillis,
    ) {
        onNode(matcher, useUnmergedTree = useUnmergedTree).isDisplayed()
    }
}

fun ComposeTestRule.waitForNoNodes(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    atLeastOneRootRequired: Boolean = false,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitUntil(
        conditionDescription = conditionDescription ?: "nodes matching $matcher to disappear",
        timeoutMillis = timeoutMillis,
    ) {
        onAllNodes(matcher, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes(atLeastOneRootRequired = atLeastOneRootRequired)
            .isEmpty()
    }
}

/**
 * Waits until no placed nodes match [matcher].
 *
 * Lazy lists can retain removed nodes for reuse, so ordinary semantics lookup can
 * still find them after they leave the layout.
 */
fun ComposeTestRule.waitForNoPlacedNodes(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    atLeastOneRootRequired: Boolean = false,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitForNoNodes(
        matcher = matcher.and(isPlaced()),
        timeoutMillis = timeoutMillis,
        atLeastOneRootRequired = atLeastOneRootRequired,
        useUnmergedTree = useUnmergedTree,
        conditionDescription = conditionDescription,
    )
}

fun ComposeTestRule.waitForText(
    text: String,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    atLeastOneRootRequired: Boolean = false,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitForNode(
        matcher = hasText(text, substring = substring, ignoreCase = ignoreCase),
        timeoutMillis = timeoutMillis,
        atLeastOneRootRequired = atLeastOneRootRequired,
        useUnmergedTree = useUnmergedTree,
        conditionDescription = conditionDescription,
    )
}

fun ComposeTestRule.waitForContentDescription(
    description: String,
    timeoutMillis: Long = DEFAULT_COMPOSE_WAIT_TIMEOUT_MILLIS,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
    atLeastOneRootRequired: Boolean = false,
    useUnmergedTree: Boolean = false,
    conditionDescription: String? = null,
) {
    waitForNode(
        matcher = hasContentDescription(
            value = description,
            substring = substring,
            ignoreCase = ignoreCase,
        ),
        timeoutMillis = timeoutMillis,
        atLeastOneRootRequired = atLeastOneRootRequired,
        useUnmergedTree = useUnmergedTree,
        conditionDescription = conditionDescription,
    )
}

fun ComposeTestRule.replaceText(
    targetText: String,
    text: String,
    substring: Boolean = false,
    scrollBehavior: ScrollBehavior = ScrollBehavior.Required,
    settleAfterReplacement: Boolean = false,
) {
    replaceText(
        matcher = hasText(targetText, substring = substring),
        text = text,
        scrollBehavior = scrollBehavior,
        settleAfterReplacement = settleAfterReplacement,
    )
}

fun ComposeTestRule.fillExpirationDate(text: String) {
    replaceText(
        matcher = hasContentDescription(value = "Expiration date", substring = true),
        text = text,
    )
}

fun ComposeTestRule.fillCardDetails(
    cardNumber: String?,
    expirationDate: String,
    cvc: String,
    zipCode: String?,
    textFieldScrollBehavior: ScrollBehavior,
) {
    cardNumber?.let {
        replaceText(
            targetText = "Card number",
            text = it,
            scrollBehavior = textFieldScrollBehavior,
        )
    }
    fillExpirationDate(expirationDate)
    replaceText(
        targetText = "CVC",
        text = cvc,
        scrollBehavior = textFieldScrollBehavior,
    )
    zipCode?.let {
        replaceText(
            targetText = "ZIP Code",
            text = it,
            scrollBehavior = textFieldScrollBehavior,
        )
    }
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
    scrollTo(node, scrollBehavior)

    node.performTextReplacement(text)

    if (settleAfterReplacement) {
        waitForIdle()
    }
}

fun ComposeTestRule.inputText(
    matcher: SemanticsMatcher,
    text: String,
    scrollBehavior: ScrollBehavior = ScrollBehavior.Never,
    settleAfterInput: Boolean = false,
) {
    inputText(
        node = onNode(matcher),
        text = text,
        scrollBehavior = scrollBehavior,
        settleAfterInput = settleAfterInput,
    )
}

fun ComposeTestRule.inputText(
    node: SemanticsNodeInteraction,
    text: String,
    scrollBehavior: ScrollBehavior = ScrollBehavior.Never,
    settleAfterInput: Boolean = false,
) {
    scrollTo(node, scrollBehavior)

    node.performTextInput(text)

    if (settleAfterInput) {
        waitForIdle()
    }
}

fun ComposeTestRule.clickNode(
    matcher: SemanticsMatcher,
    scrollBehavior: ScrollBehavior,
    useUnmergedTree: Boolean = false,
) {
    clickNode(
        node = onNode(matcher, useUnmergedTree = useUnmergedTree),
        scrollBehavior = scrollBehavior,
    )
}

fun ComposeTestRule.clickNode(
    node: SemanticsNodeInteraction,
    scrollBehavior: ScrollBehavior,
) {
    scrollTo(node, scrollBehavior)

    node.performClick()
}

fun ComposeTestRule.clickEnabledNode(
    node: SemanticsNodeInteraction,
    scrollBehavior: ScrollBehavior,
) {
    node.assertIsEnabled()
    scrollTo(node, scrollBehavior)
    node.assertIsDisplayed()
    node.performClick()
}

private fun scrollTo(
    node: SemanticsNodeInteraction,
    scrollBehavior: ScrollBehavior,
) {
    when (scrollBehavior) {
        ScrollBehavior.Never -> Unit
        ScrollBehavior.Required -> node.performScrollTo()
        ScrollBehavior.BestEffort -> {
            runCatching { node.performScrollTo() }
        }
    }
}
