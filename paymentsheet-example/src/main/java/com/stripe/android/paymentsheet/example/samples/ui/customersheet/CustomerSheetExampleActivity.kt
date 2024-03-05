package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSheetExampleActivity : AppCompatActivity() {
    private val viewModel by viewModels<CustomerSheetExampleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.customersheet_example_title)

        setContent {
            PaymentSheetExampleTheme {
                val customerSheet = rememberCustomerSheet(
                    customerAdapter = viewModel.customerAdapter,
                    configuration = buildConfig(),
                    callback = viewModel::onCustomerSheetResult,
                )

                val viewState by viewModel.state.collectAsState()

                LaunchedEffect(Unit) {
                    val result = customerSheet.retrievePaymentOptionSelection()
                    viewModel.onCustomerSheetResult(result)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Payment Methods",
                        color = MaterialTheme.colors.onBackground,
                        fontSize = 18.sp,
                    )

                    when (val state = viewState) {
                        is CustomerSheetExampleViewState.Data -> {
                            CustomerPaymentMethods(
                                state = state,
                                onUpdateDefaultPaymentMethod = {
                                    customerSheet.present()
                                }
                            )
                        }
                        is CustomerSheetExampleViewState.FailedToLoad -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colors.onBackground,
                            )
                        }
                        is CustomerSheetExampleViewState.Loading -> {
                            LinearProgressIndicator(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildConfig(): CustomerSheet.Configuration {
        return CustomerSheet.Configuration.builder(merchantDisplayName = "Payment Sheet Example")
            .defaultBillingDetails(
                PaymentSheet.BillingDetails(
                    name = "CustomerSheet Testing"
                )
            ).billingDetailsCollectionConfiguration(
                PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                )
            )
            .googlePayEnabled(true)
            .build()
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
@Composable
private fun CustomerPaymentMethods(
    state: CustomerSheetExampleViewState.Data,
    onUpdateDefaultPaymentMethod: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Payment default",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground,
            )
            TextButton(
                onClick = onUpdateDefaultPaymentMethod,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    state.selection?.paymentOption?.let {
                        Image(
                            painter = it.iconPainter,
                            contentDescription = "Payment Method Icon",
                            modifier = Modifier.height(32.dp)
                        )
                    }
                    Text(
                        text = state.selection?.paymentOption?.label ?: "Select",
                        color = MaterialTheme.colors.onBackground,
                    )
                }
            }
        }
        state.errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
            )
        }
    }
}
