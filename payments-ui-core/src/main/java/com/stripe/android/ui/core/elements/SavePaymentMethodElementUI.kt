package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.uicore.elements.CheckboxElementUI
import com.stripe.android.uicore.utils.collectAsState

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SavePaymentMethodSectionUI(
    enabled: Boolean,
    saveForFutureUseElement: SaveForFutureUseElement,
    setAsDefaultPaymentMethodElement: SetAsDefaultPaymentMethodElement,
    modifier: Modifier = Modifier,
) {
    val saveForFutureUseController = saveForFutureUseElement.controller
    val setAsDefaultPaymentMethodController = setAsDefaultPaymentMethodElement.controller

    val saveForFutureUseChecked by saveForFutureUseController.saveForFutureUse.collectAsState()
    val setAsDefaultChecked by setAsDefaultPaymentMethodController.setAsDefaultPaymentMethod.collectAsState()

    val saveForFutureUseLabel by saveForFutureUseController.label.collectAsState()
    val setAsDefaultPaymentMethodLabel by saveForFutureUseController.label.collectAsState()

    val resources = LocalContext.current.resources

    CheckboxElementUI(
        automationTestTag = SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG,
        isChecked = saveForFutureUseChecked,
        label = resources.getString(saveForFutureUseLabel, saveForFutureUseElement.merchantName),
        isEnabled = enabled,
        modifier = modifier,
        onValueChange = {
            saveForFutureUseController.onValueChange(!saveForFutureUseChecked)
        },
    )

    if (saveForFutureUseChecked) {
        CheckboxElementUI(
            automationTestTag = SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG,
            isChecked = setAsDefaultChecked,
            label = resources.getString(setAsDefaultPaymentMethodLabel),
            isEnabled = enabled,
            modifier = modifier,
            onValueChange = {
                setAsDefaultPaymentMethodController.onValueChange(!setAsDefaultChecked)
            },
        )
    }
}
