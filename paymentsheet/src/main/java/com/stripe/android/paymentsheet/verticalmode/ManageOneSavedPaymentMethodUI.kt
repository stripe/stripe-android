package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.stripe.android.paymentsheet.R

@Composable
internal fun ManageOneSavedPaymentMethodUI(interactor: ManageOneSavedPaymentMethodInteractor) {
    val horizontalPadding = dimensionResource(
        id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal
    )

    val paymentMethod = interactor.state.paymentMethod

    SavedPaymentMethodRowButton(
        displayableSavedPaymentMethod = paymentMethod,
        isEnabled = true,
        isSelected = false,
        trailingContent = {
            DeleteIcon(
                paymentMethod = paymentMethod,
                deletePaymentMethod = {
                    interactor.handleViewAction(
                        ManageOneSavedPaymentMethodInteractor.ViewAction.DeletePaymentMethod
                    )
                }
            )
        },
        modifier = Modifier.padding(horizontal = horizontalPadding)
    )
}
