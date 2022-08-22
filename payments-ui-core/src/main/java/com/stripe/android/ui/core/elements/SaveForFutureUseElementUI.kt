package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

const val SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG = "SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG"

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SaveForFutureUseElementUI(
    enabled: Boolean,
    element: SaveForFutureUseElement
) {
    val controller = element.controller
    val checked by controller.saveForFutureUse.collectAsState(true)
    val label by controller.label.collectAsState(null)
    val resources = LocalContext.current.resources

    CheckboxElementUI(
        automationTestTag = SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG,
        isChecked = checked,
        label = label?.let { resources.getString(it, element.merchantName) },
        isEnabled = enabled,
        onValueChange = {
            controller.onValueChange(!checked)
        }
    )
}
