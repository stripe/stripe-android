package com.stripe.android.connect.example.util

import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry

fun SemanticsNodeInteractionsProvider.onNodeWithTextRes(
    @StringRes textRes: Int,
    useUnmergedTree: Boolean = false
): SemanticsNodeInteraction {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val text = context.getString(textRes)
    return onNodeWithText(
        text = text,
        useUnmergedTree = useUnmergedTree,
    )
}
