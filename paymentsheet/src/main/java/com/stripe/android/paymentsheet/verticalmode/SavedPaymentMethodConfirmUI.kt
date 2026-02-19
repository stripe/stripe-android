package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun SavedPaymentMethodConfirmUI(
    savedPaymentMethodConfirmInteractor: SavedPaymentMethodConfirmInteractor,
) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()
    val state by savedPaymentMethodConfirmInteractor.state.collectAsState()

    Column(
        modifier = Modifier
            .padding(horizontalPadding),
//            .testTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SavedPaymentMethodRowButton(
            displayableSavedPaymentMethod = savedPaymentMethodConfirmInteractor.displayableSavedPaymentMethod,
            isEnabled = true, // TODO: we want it to look enabled but not actually be clickable.
            isSelected = true,
            onClick = { },
            trailingContent = { },
        )

        savedPaymentMethodConfirmInteractor.savedPaymentMethodLinkFormHelper.formElement?.let {
            FormUI(
                elements = listOf(it),
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null,
                enabled = state.linkFormEnabled,
            )
        }
    }
}