package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.FormUI
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getOuterFormInsets

@Composable
internal fun SavedPaymentMethodConfirmUI(
    savedPaymentMethodConfirmInteractor: SavedPaymentMethodConfirmInteractor,
) {
    val horizontalPadding = StripeTheme.getOuterFormInsets()

    Column(
        modifier = Modifier
            .padding(horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        H4Text(
            text = stringResource(R.string.stripe_paymentsheet_saved_card),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        SavedPaymentMethodRowButton(
            displayableSavedPaymentMethod = savedPaymentMethodConfirmInteractor.displayableSavedPaymentMethod,
            isEnabled = true,
            isSelected = true,
            onClick = { },
            trailingContent = { },
        )

        savedPaymentMethodConfirmInteractor.formElement?.let {
            FormUI(
                elements = listOf(it),
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null,
                enabled = true,
            )
        }
    }
}
