package com.stripe.android.paymentsheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.ui.AddPaymentMethod
import com.stripe.android.paymentsheet.ui.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.PaymentOptions
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal sealed interface PaymentSheetScreen {

    val showsBuyButton: Boolean
    val showsContinueButton: Boolean
    val showsWalletsHeader: Boolean

    @Composable
    fun Content(viewModel: BaseSheetViewModel, modifier: Modifier)

    object Loading : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val showsWalletsHeader: Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            BottomSheetLoadingIndicator(modifier)
        }
    }

    object SelectSavedPaymentMethods : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = false
        override val showsWalletsHeader: Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            val state by viewModel.paymentOptionsState.collectAsState()
            val isEditing by viewModel.editing.collectAsState()
            val isProcessing by viewModel.processing.collectAsState()

            PaymentOptions(
                state = state,
                isEditing = isEditing,
                isProcessing = isProcessing,
                onAddCardPressed = viewModel::transitionToAddPaymentScreen,
                onItemSelected = viewModel::handlePaymentMethodSelected,
                onModifyItem = viewModel::modifyPaymentMethod,
                onItemRemoved = viewModel::removePaymentMethod,
                modifier = modifier,
            )
        }
    }

    object AddAnotherPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val showsWalletsHeader: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(viewModel, modifier)
        }
    }

    object AddFirstPaymentMethod : PaymentSheetScreen {

        override val showsBuyButton: Boolean = true
        override val showsContinueButton: Boolean = true
        override val showsWalletsHeader: Boolean = true

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            AddPaymentMethod(viewModel, modifier)
        }
    }

    data class EditPaymentMethod(
        val paymentMethod: PaymentMethod,
    ) : PaymentSheetScreen {

        override val showsBuyButton: Boolean = false
        override val showsContinueButton: Boolean = false
        override val showsWalletsHeader: Boolean = false

        @Composable
        override fun Content(viewModel: BaseSheetViewModel, modifier: Modifier) {
            EditPaymentMethod(paymentMethod, modifier)
        }
    }
}

@Composable
internal fun PaymentSheetScreen.Content(viewModel: BaseSheetViewModel) {
    Content(viewModel, modifier = Modifier)
}
