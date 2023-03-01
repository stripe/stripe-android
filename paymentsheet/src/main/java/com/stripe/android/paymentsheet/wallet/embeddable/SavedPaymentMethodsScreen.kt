package com.stripe.android.paymentsheet.wallet.embeddable

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.PaymentMethodForm
import com.stripe.android.paymentsheet.ui.PaymentSheetLoading
import kotlinx.coroutines.flow.flowOf

@ExperimentalPaymentSheetDecouplingApi
internal sealed interface SavedPaymentMethodsScreen {

    val showsContinueButton: Boolean

    @Composable
    fun Content(viewModel: SavedPaymentMethodsViewModel, modifier: Modifier)

    object Loading : SavedPaymentMethodsScreen {

        override val showsContinueButton: Boolean = false

        @Composable
        override fun Content(viewModel: SavedPaymentMethodsViewModel, modifier: Modifier) {
            PaymentSheetLoading(modifier)
        }
    }

    object SelectSavedPaymentMethods : SavedPaymentMethodsScreen {

        override val showsContinueButton: Boolean = true

        @Composable
        override fun Content(viewModel: SavedPaymentMethodsViewModel, modifier: Modifier) {
            SavedPaymentMethodsUI(viewModel = viewModel)
        }
    }

    object AddPaymentMethod : SavedPaymentMethodsScreen {

        override val showsContinueButton: Boolean = true

        @Composable
        override fun Content(viewModel: SavedPaymentMethodsViewModel, modifier: Modifier) {
            PaymentMethodForm(
                args = FormArguments(
                    paymentMethodCode = "card",
                    showCheckbox = false,
                    showCheckboxControlledFields = false,
                    merchantName = "Test 2",
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                    )
                ),
                enabled = true,
                onFormFieldValuesChanged = { },
                showCheckboxFlow = flowOf(false),
                injector = viewModel.injector,
            )
        }
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
@Composable
internal fun SavedPaymentMethodsScreen.Content(viewModel: SavedPaymentMethodsViewModel) {
    Content(viewModel, modifier = Modifier)
}
