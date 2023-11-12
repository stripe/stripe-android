package com.stripe.android.paymentsheet.prototype.uielement

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.paymentsheet.prototype.UiElementDefinition
import com.stripe.android.paymentsheet.prototype.UiRenderer
import com.stripe.android.paymentsheet.prototype.UiState

internal class BillingAddressUiElementDefinition(
    private val availableCountries: Set<String>
) : UiElementDefinition {
    override fun isComplete(uiState: UiState.Snapshot): Boolean {
        // TODO: see if the fields in the state are filled out.
        return true
    }

    override fun renderer(uiState: UiState): UiRenderer {
        return BillingAddressUi()
    }
}

private class BillingAddressUi : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        // TODO:
    }
}
