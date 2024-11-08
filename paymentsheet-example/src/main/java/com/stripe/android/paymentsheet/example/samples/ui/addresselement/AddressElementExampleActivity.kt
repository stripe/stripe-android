package com.stripe.android.paymentsheet.example.samples.ui.addresselement

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.Settings

class AddressElementExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.address_element_title)

        setContent {
            val viewModel by viewModels<AddressElementExampleViewModel>()
            val viewState by viewModel.state.collectAsState()

            val addressLauncher = rememberAddressLauncher(
                callback = viewModel::handleResult,
            )

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val state = viewState) {
                    is AddressElementExampleViewState.Loading -> {
                        CircularProgressIndicator()
                    }
                    is AddressElementExampleViewState.Content -> {
                        state.address?.let { address ->
                            Address(address)
                        }

                        val context = LocalContext.current
                        Button(
                            onClick = {
                                val config = AddressLauncher.Configuration.Builder()
                                    .additionalFields(
                                        additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                                            phone = AddressLauncher.AdditionalFieldsConfiguration
                                                .FieldConfiguration.REQUIRED
                                        )
                                    )
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
