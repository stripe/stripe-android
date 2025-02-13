package com.stripe.android.ui.core.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.utils.collectAsState

internal const val SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG = "SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG"

@Composable
internal fun SetAsDefaultPaymentMethodElementUI(
    enabled: Boolean,
    element: SetAsDefaultPaymentMethodElement,
    modifier: Modifier = Modifier,
) {
    val controller = element.controller
    val checked by controller.setAsDefaultPaymentMethod.collectAsState()
    val label by controller.label.collectAsState()
    val resources = LocalContext.current.resources

    val shouldShow = element.shouldShowElementFlow.collectAsState()

    if (shouldShow.value) {
        CheckboxElementUI(
            automationTestTag = SET_AS_DEFAULT_PAYMENT_METHOD_TEST_TAG,
            isChecked = checked,
            label = resources.getString(label),
            isEnabled = enabled,
            modifier = modifier,
            onValueChange = {
                controller.onValueChange(!checked)
            },
        )
    }
}
