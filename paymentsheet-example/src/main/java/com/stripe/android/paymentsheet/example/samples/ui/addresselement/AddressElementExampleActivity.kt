package com.stripe.android.paymentsheet.example.samples.ui.addresselement

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.Settings

class AddressElementExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = getString(R.string.address_element_title)
        val viewModel by viewModels<AddressElementExampleViewModel>()
        setContent {
            AddressElementExampleScreen(viewModel)
        }
    }
}

@Composable
private fun AddressElementExampleScreen(viewModel: AddressElementExampleViewModel) {
    val viewState by viewModel.state.collectAsState()
    val addressLauncher = rememberAddressLauncher(
        callback = viewModel::handleResult,
    )
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                paddingValues = WindowInsets.systemBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                ).asPaddingValues()
            ),
    ) {
        when (val state = viewState) {
            is AddressElementExampleViewState.Loading -> {
                CircularProgressIndicator()
            }
            is AddressElementExampleViewState.Content -> {
                state.address?.let { address ->
                    Address(address)
                }
                var inlineAutocompleteEnabled by remember {
                    mutableStateOf(FeatureFlags.inlineAddressAutocompleteEnabled.isEnabled)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Inline autocomplete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = inlineAutocompleteEnabled,
                        onCheckedChange = { enabled ->
                            inlineAutocompleteEnabled = enabled
                            FeatureFlags.inlineAddressAutocompleteEnabled.setEnabled(enabled)
                        },
                    )
                }
                val context = LocalContext.current
                Button(
                    onClick = {
                        val config = AddressLauncher.Configuration.Builder()
                            // Provide your Google Places API key to enable autocomplete
                            .googlePlacesApiKey(Settings(context).googlePlacesApiKey)
                            .build()
                        addressLauncher.present(
                            publishableKey = state.publishableKey,
                            configuration = config,
                        )
                    },
                    modifier = Modifier.testTag(SELECT_ADDRESS_BUTTON)
                ) {
                    Text("Select address")
                }
            }
            is AddressElementExampleViewState.Error -> {
                Text(state.message)
            }
        }
    }
}

@Composable
private fun Address(addressDetails: AddressDetails) {
    val text = remember(addressDetails) {
        val address = addressDetails.address

        val cityAndState = buildString {
            // SF, CA 12345
            address?.city?.let {
                append(it)
                append(", ")
            }

            address?.state?.let {
                append(it)
                append(" ")
            }

            address?.postalCode?.let {
                append(it)
            }
        }.takeIf { it.isNotBlank() }

        val lines = listOfNotNull(
            addressDetails.name,
            address?.line1,
            address?.line2,
            cityAndState,
            address?.country,
        )

        lines.joinToString("\n")
    }

    Text(text)
}

internal const val SELECT_ADDRESS_BUTTON = "SELECT_ADDRESS_BUTTON"
