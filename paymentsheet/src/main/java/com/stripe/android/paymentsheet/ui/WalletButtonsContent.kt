package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.utils.collectAsState

@Immutable
internal class WalletButtonsContent(
    private val interactor: WalletButtonsInteractor,
) {
    @Composable
    fun Content() {
        val state by interactor.state.collectAsState()

        if (state.walletButtons.isNotEmpty()) {
            StripeTheme {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.walletButtons.forEach { button ->
                        button.Content(state.buttonsEnabled)
                    }
                }
            }
        }
    }
}
