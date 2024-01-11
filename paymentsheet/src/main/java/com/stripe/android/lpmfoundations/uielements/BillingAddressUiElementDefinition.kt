package com.stripe.android.lpmfoundations.uielements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.lpmfoundations.UiElementDefinition
import com.stripe.android.lpmfoundations.UiRenderer
import com.stripe.android.lpmfoundations.UiState

internal class BillingAddressUiElementDefinition(
    val availableCountries: Set<String>
) : UiElementDefinition {
    override fun isValid(uiState: UiState.Snapshot): Boolean = false

    override fun renderer(uiState: UiState): UiRenderer {
        return BillingAddressUi()
    }
}

private class BillingAddressUi : UiRenderer {
    @Composable
    override fun Content(enabled: Boolean, modifier: Modifier) {
        // TODO(jaynewstrom): Add UI.
    }
}
