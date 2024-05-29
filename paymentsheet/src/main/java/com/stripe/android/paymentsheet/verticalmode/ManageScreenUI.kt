package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun ManageScreenUI(interactor: ManageScreenInteractor, modifier: Modifier) {
    val state by interactor.state.collectAsState()

    val currentPaymentMethods = state.paymentMethods

    if (currentPaymentMethods != null) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            currentPaymentMethods.forEach {
                SavedPaymentMethodRowButton(
                    displayableSavedPaymentMethod = it,
                    resources = LocalContext.current.resources,
                    isEnabled = true,
                    isSelected = false
                )
            }
        }
    }
}
