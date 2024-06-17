package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@Composable
internal fun ManageOneSavedPaymentMethodUI(interactor: ManageOneSavedPaymentMethodInteractor) {
    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    val paymentMethod = interactor.state.paymentMethod

    Column(
        modifier = Modifier
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SavedPaymentMethodRowButton(
            displayableSavedPaymentMethod = paymentMethod,
            resources = LocalContext.current.resources,
            isEnabled = true,
            isSelected = true,
            trailingContent = {
                DeleteIcon(
                    paymentMethod = paymentMethod,
                    deletePaymentMethod = {
                        interactor.handleViewAction(
                            ManageOneSavedPaymentMethodInteractor.ViewAction.DeletePaymentMethod
                        )
                    }
                )
            }
        )
    }
}
