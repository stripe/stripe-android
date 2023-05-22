package com.stripe.android.paymentsheet.example.samples.ui.customer

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.wallet.CustomerSheet

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerActivity : AppCompatActivity() {
    private val viewModel by viewModels<CustomerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customerSheet = CustomerSheet.create(
            activity = this,
            configuration = CustomerSheet.Configuration(
                merchantDisplayName = "Test Merchant",
            ),
            customerAdapter = viewModel.customerAdapter,
            callback = {
                TODO()
            }
        )

        setContent {
            PaymentSheetExampleTheme {
                val context = LocalContext.current
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
                        is CustomerViewState.Data -> {
                            PaymentDefaults(
                                onUpdateDefaultPaymentMethod = {
                                    try {
                                        customerSheet.present()
                                    } catch (_: Error) {
                                        Toast.makeText(
                                            context,
                                            "Customer sheet under construction",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            )
                        }
                        is CustomerViewState.FailedToLoad -> {
                            Text(
                                text = state.message
                            )
                        }
                        CustomerViewState.Loading -> {
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
                text = "Visa 4242",
            )
        }
    }
}
