package com.stripe.android.paymentsheet.example.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.example.R

internal class LaunchPaymentSheetCompleteActivity : BasePaymentSheetActivity() {
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
