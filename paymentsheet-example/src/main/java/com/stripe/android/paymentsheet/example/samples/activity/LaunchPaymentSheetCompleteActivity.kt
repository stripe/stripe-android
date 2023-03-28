package com.stripe.android.paymentsheet.example.samples.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.ui.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.Receipt

internal class LaunchPaymentSheetCompleteActivity : BasePaymentSheetActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        setContent {
            MaterialTheme {
                val inProgress by viewModel.inProgress.observeAsState(false)
                val status by viewModel.status.observeAsState()

                status?.let {
                    LaunchedEffect(it) {
                        snackbar.setText(it).show()
                        viewModel.statusDisplayed()
                    }
                }

                Receipt(inProgress) {
                    BuyButton(
                        buyButtonEnabled = !inProgress,
                        onClick = {
                            prepareCheckout { customerConfig, clientSecret ->
                                paymentSheet.presentWithPaymentIntent(
                                    clientSecret,
                                    PaymentSheet.Configuration(
                                        merchantDisplayName = merchantName,
                                        customer = customerConfig,
                                        googlePay = googlePayConfig,
                                        // Set `allowsDelayedPaymentMethods` to true if your
                                        // business can handle payment methods that complete payment
                                        // after a delay, like SEPA Debit and Sofort.
                                        allowsDelayedPaymentMethods = true
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
