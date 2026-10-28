package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.ui.core.FormUI
import com.stripe.android.uicore.utils.collectAsState

/**
 * Renders the address fields of [state] without a title, button, or scroll container, so they
 * can sit inline in the host's own layout.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun InlineAddressForm(
    state: InlineAddressFormState,
    appearance: PaymentSheet.Appearance,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val formController = state.formController
    val lastTextFieldIdentifier by formController.lastTextFieldIdentifier.collectAsState()

    PaymentElementTheme(appearance = appearance) {
        FormUI(
            hiddenIdentifiers = emptySet(),
            enabled = enabled,
            elements = formController.elements,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            modifier = modifier,
        )
    }
}
