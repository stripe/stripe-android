package com.stripe.android.paymentsheet

import android.app.Activity
import android.view.View
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt

private const val KEYBOARD_VISIBILITY_TIMEOUT_MS = 5_000L

/** Waits for the IME opened by a real form-field focus action to become visible. */
internal fun ComposeTestRule.waitForKeyboardToBeVisible(activity: Activity) {
    waitUntil(KEYBOARD_VISIBILITY_TIMEOUT_MS) {
        activity.window.decorView.imeBottomInset() > 0
    }
}

/** Asserts that [testTag]'s bottom is not occluded by the currently visible IME. */
internal fun ComposeTestRule.assertNodeWithTagVisibleAboveKeyboard(
    activity: Activity,
    testTag: String,
) {
    waitForIdle()
    val node = onNodeWithTag(testTag).fetchSemanticsNode()
    val rootView = activity.window.decorView
    val imeBottomInset = rootView.imeBottomInset()
    assertThat(imeBottomInset).isGreaterThan(0)

    val rootLocation = IntArray(2)
    rootView.getLocationOnScreen(rootLocation)
    val keyboardTop = rootLocation[1] + rootView.height - imeBottomInset
    val nodeBottom = rootLocation[1] + node.boundsInRoot.bottom.roundToInt()

    assertThat(nodeBottom).isAtMost(keyboardTop)
}

private fun View.imeBottomInset(): Int {
    return ViewCompat.getRootWindowInsets(this)
        ?.getInsets(WindowInsetsCompat.Type.ime())
        ?.bottom
        ?: 0
}
