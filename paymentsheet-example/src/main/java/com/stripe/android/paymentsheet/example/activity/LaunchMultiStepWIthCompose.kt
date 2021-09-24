package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract

internal class LaunchMultiStepWithCompose : BasePaymentSheetActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val activity = LocalContext.current as ComponentActivity
                LaunchedEffect(key1 = Unit, block = { PaymentSheet.FlowController.create(activity, {}, {}) })
                val stripeLauncher =
                    rememberLauncherForActivityResult(contract = PaymentSheetContract())
                    {
                        onPaymentSheetResult(it)
                    }

                val inProgress by viewModel.inProgress.observeAsState(false)
                val status by viewModel.status.observeAsState("")

                if (status.isNotBlank()) {
                    Toast.makeText(LocalContext.current, status, Toast.LENGTH_SHORT).show()
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
