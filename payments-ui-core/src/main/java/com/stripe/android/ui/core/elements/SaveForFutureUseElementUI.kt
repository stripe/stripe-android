package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.strings.resolve
import com.stripe.android.uicore.utils.collectAsState

const val SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG = "SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG"

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SaveForFutureUseElementUI(
    enabled: Boolean,
    element: SaveForFutureUseElement,
    modifier: Modifier = Modifier,
) {
    val controller = element.controller
    val checked by controller.saveForFutureUse.collectAsState()
    val label by controller.label.collectAsState()

    CheckboxElementUI(
        automationTestTag = SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG,
        isChecked = checked,
        label = label.resolve(),
        isEnabled = enabled,
        modifier = modifier,
        onValueChange = {
            controller.onValueChange(!checked)
        },
    )
}
