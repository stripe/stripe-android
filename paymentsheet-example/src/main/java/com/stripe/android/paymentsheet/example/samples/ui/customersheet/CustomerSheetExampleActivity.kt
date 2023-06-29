package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSheetExampleActivity : AppCompatActivity() {
    private val viewModel by viewModels<CustomerSheetExampleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.customer_toolbar_title)

        setContent {
            PaymentSheetExampleTheme {
                val customerSheet = rememberCustomerSheet(
                    customerAdapter = viewModel.customerAdapter,
                    configuration = CustomerSheet.Configuration.Builder()
                        .googlePayEnabled(true)
                        .build(),
                    callback = viewModel::onCustomerSheetResult,
                )

                val viewState by viewModel.state.collectAsState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Payment Methods",
                        fontSize = 18.sp
                    )

                    when (val state = viewState) {
                        is CustomerSheetExampleViewState.Data -> {
                            val label = (state.result as? CustomerSheetResult.Selected)
                                ?.selection
                                ?.paymentOption
                                ?.label
                            PaymentDefaults(
                                paymentMethodLabel = label ?: "Select",
                                onUpdateDefaultPaymentMethod = {
                                    customerSheet.present()
                                }
                            )
                        }
                        is CustomerSheetExampleViewState.FailedToLoad -> {
                            Text(
                                text = state.message
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
}

@Composable
private fun PaymentDefaults(
    paymentMethodLabel: String,
    onUpdateDefaultPaymentMethod: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Payment default",
            fontWeight = FontWeight.Bold,
        )
        TextButton(
            onClick = onUpdateDefaultPaymentMethod
        ) {
            Text(
                text = paymentMethodLabel,
            )
        }
    }
}
