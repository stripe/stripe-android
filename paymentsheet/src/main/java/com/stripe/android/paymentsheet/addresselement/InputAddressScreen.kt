package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.injection.NonFallbackInjector

@Composable
internal fun InputAddressScreen(
    injector: NonFallbackInjector
) {
    val viewModel: InputAddressViewModel = viewModel(
        factory = InputAddressViewModel.Factory(
            injector
        )
    )

    val collectedAddress by viewModel.collectedAddress.collectAsState()
    Column {
        AddressOptionsAppBar(
            isRootScreen = true,
            onButtonClick = {
                viewModel.navigator.dismiss()
            }
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("BaseAddress Screen")
            collectedAddress?.let { address ->
                address.name?.let {
                    Text(it)
                }
            }
            if (collectedAddress == null) {
                Button(
                    onClick = {
                        viewModel.navigator.navigateTo(AddressElementScreen.Autocomplete)
                    }
                ) {
                    Text(text = "Change to AutoComplete screen")
                }
            }
        }
    }
}
