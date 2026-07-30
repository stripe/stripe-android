package com.stripe.android.paymentsheet

import android.view.View
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt

private const val KEYBOARD_VISIBILITY_TIMEOUT_MS = 5_000L

/** Waits for the IME opened by a real form-field focus action to become visible. */
internal fun ComposeTestRule.waitForKeyboardToBeVisible() {
    waitUntil(KEYBOARD_VISIBILITY_TIMEOUT_MS) {
        currentRootView().imeBottomInset() > 0
    }
}

/** Asserts that [testTag]'s bottom is not occluded by the currently visible IME. */
internal fun ComposeTestRule.assertNodeWithTagVisibleAboveKeyboard(
    testTag: String,
) {
    waitUntil(KEYBOARD_VISIBILITY_TIMEOUT_MS) {
        nodePositionRelativeToKeyboard(testTag).isAboveKeyboard
    }
    val position = nodePositionRelativeToKeyboard(testTag)
    assertThat(position.imeBottomInset).isGreaterThan(0)
    assertThat(position.nodeBottom).isAtMost(position.keyboardTop)
}

private fun ComposeTestRule.nodePositionRelativeToKeyboard(testTag: String): NodePosition {
    waitForIdle()
    val node = onNodeWithTag(testTag).fetchSemanticsNode()
    val rootView = currentRootView()
    val imeBottomInset = rootView.imeBottomInset()
    val rootLocation = IntArray(2)
    rootView.getLocationOnScreen(rootLocation)
    val keyboardTop = rootLocation[1] + rootView.height - imeBottomInset
    val nodeBottom = rootLocation[1] + node.boundsInWindow.bottom.roundToInt()

    return NodePosition(
        imeBottomInset = imeBottomInset,
        keyboardTop = keyboardTop,
        nodeBottom = nodeBottom,
    )
}

private fun currentRootView(): View {
    var rootView: View? = null
    onView(isRoot()).check { view, _ ->
        rootView = view
    }
    return requireNotNull(rootView)
}

private fun View.imeBottomInset(): Int {
    return ViewCompat.getRootWindowInsets(this)
        ?.getInsets(WindowInsetsCompat.Type.ime())
        ?.bottom
        ?: 0
}

private data class NodePosition(
    val imeBottomInset: Int,
    val keyboardTop: Int,
    val nodeBottom: Int,
) {
    val isAboveKeyboard: Boolean
        get() = imeBottomInset > 0 && nodeBottom <= keyboardTop
}
