package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource

const val SAME_AS_SHIPPING_CHECKBOX_TEST_TAG = "SAME_AS_SHIPPING_CHECKBOX_TEST_TAG"

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SameAsShippingElementUI(
    controller: SameAsShippingController
) {
    val checked by controller.value.collectAsState(false)
    val label by controller.label.collectAsState(null)

    CheckboxElementUI(
        automationTestTag = SAME_AS_SHIPPING_CHECKBOX_TEST_TAG,
        isChecked = checked,
        label = label?.let { stringResource(it) },
        isEnabled = true,
        onValueChange = {
            controller.onValueChange(!checked)
        }
    )
}
