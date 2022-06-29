package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.injection.NonFallbackInjector

@Composable
internal fun AutoCompleteScreen(
    injector: NonFallbackInjector
) {
    val viewModel: AutoCompleteViewModel = viewModel(
        factory = AutoCompleteViewModel.Factory(
            injector
        )
    )

    Column {
        AddressOptionsAppBar(
            isRootScreen = false,
            onButtonClick = {
                viewModel.navigator.onBack()
            }
        )
        Column(Modifier.padding(horizontal = 20.dp).fillMaxHeight()) {
            Text("AutoComplete Screen")
            Button(
                onClick = {
                    // TODO implement this screen
                    val dummyAddress = ShippingAddress(line1 = "123 street", line2 = "711", state = "ohio", postalCode = "12345", country = "US", city = "coolcity")
                    viewModel.navigator.setResult(ShippingAddress.KEY, dummyAddress)
                    viewModel.navigator.onBack()
                },
                content = { Text(text = "Input Autocompleted Address") }
            )
        }
    }
}
