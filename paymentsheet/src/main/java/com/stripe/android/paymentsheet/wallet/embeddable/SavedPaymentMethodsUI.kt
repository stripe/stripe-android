@file:OptIn(ExperimentalPaymentSheetDecouplingApi::class)

package com.stripe.android.paymentsheet.wallet.embeddable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.customer.CustomerAdapterConfig
import com.stripe.android.paymentsheet.ui.PaymentOptions

@Composable
fun SavedPaymentMethods(
    customerAdapterConfig: CustomerAdapterConfig,
    modifier: Modifier = Modifier,
) {
    val viewModel: SavedPaymentMethodsViewModel = viewModel(
        factory = SavedPaymentMethodsViewModel.Factory(
            context = LocalContext.current.applicationContext,
            customerAdapterConfig = customerAdapterConfig
        )
    )

    val currentScreen by viewModel.currentScreen.collectAsState()

    currentScreen.Content(viewModel, modifier)
}

@Composable
internal fun SavedPaymentMethodsUI(
    viewModel: SavedPaymentMethodsViewModel,
    modifier: Modifier = Modifier,
) {
    println("SavedPaymentMethodsUI screen")
    val paymentMethods = viewModel.paymentMethods.collectAsState(
        initial = emptyList()
    )

    val isEditing = viewModel.isEditing.collectAsState()

    val selectedPaymentMethod = viewModel.selectedPaymentMethod.collectAsState()

    LaunchedEffect(viewModel.customerAdapterConfig) {
        println("JAMES: HERE")
        viewModel.getPaymentMethods()
    }

    PaymentOptions(
        state = PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods.value,
            showGooglePay = true,
            showLink = false,
            currentSelection = selectedPaymentMethod.value,
            nameProvider = { code ->
                code.toString()
            }
        ),
        isEditing = isEditing.value,
        isProcessing = false,
        onAddCardPressed = { viewModel.transitionToAddCard() },
        onItemSelected =  {
            viewModel.selectPaymentMethod(it)
        },
        onItemRemoved = {
            viewModel.removePaymentMethod(it)
        },
    )
}