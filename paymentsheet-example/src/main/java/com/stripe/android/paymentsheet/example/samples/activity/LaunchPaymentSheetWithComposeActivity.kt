package com.stripe.android.paymentsheet.example.samples.activity

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract

internal class LaunchPaymentSheetWithComposeActivity :
    BasePaymentSheetActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val stripeLauncher =
                    rememberLauncherForActivityResult(contract = PaymentSheetContract())
                    {
                        onPaymentSheetResult(it)
                    }

                val inProgress by viewModel.inProgress.observeAsState(false)
                val status by viewModel.status.observeAsState("")

                if (status.isNotBlank()) {
                    snackbar.setText(status).show()
                    viewModel.statusDisplayed()
                }

                Receipt(inProgress) {
                    BuyButton(
                        buyButtonEnabled = !inProgress,
                        onClick = {
                            prepareCheckout { customerConfig, clientSecret ->
                                stripeLauncher.launch(
                                    PaymentSheetContract.Args.createPaymentIntentArgs(
                                        clientSecret,
                                        PaymentSheet.Configuration(
                                            merchantDisplayName = merchantName,
                                            customer = customerConfig,
                                            googlePay = googlePayConfig,
                                        )
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
