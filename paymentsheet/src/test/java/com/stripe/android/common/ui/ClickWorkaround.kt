package com.stripe.android.common.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.invokeGlobalAssertions
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.requestFocus

fun SemanticsNodeInteraction.performClickWithKeyboard(): SemanticsNodeInteraction {
    @OptIn(ExperimentalTestApi::class)
    return invokeGlobalAssertions()
        .requestFocus()
        .performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
}
