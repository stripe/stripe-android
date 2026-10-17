package com.stripe.android.tta.testing

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.ComposeTestRule

internal fun ComposeTestRule.retrieveCloseButton(): SemanticsNodeInteraction {
    return onNode(hasContentDescription("Close"))
}
