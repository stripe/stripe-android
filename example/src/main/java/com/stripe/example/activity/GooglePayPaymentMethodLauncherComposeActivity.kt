package com.stripe.example.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.rememberGooglePayPaymentMethodLauncher
import kotlinx.coroutines.launch

class GooglePayPaymentMethodLauncherComposeActivity : AppCompatActivity() {
    private val googlePayConfig = GooglePayPaymentMethodLauncher.Config(
        environment = GooglePayEnvironment.Test,
        merchantCountryCode = "US",
        merchantName = "Widget Store",
        billingAddressConfig = GooglePayPaymentMethodLauncher.BillingAddressConfig(
            isRequired = true,
            format = GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full,
            isPhoneNumberRequired = false
        ),
        existingPaymentMethodRequired = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GooglePayPaymentMethodLauncherScreen()
        }
    }

    @Composable
    private fun GooglePayPaymentMethodLauncherScreen() {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()
        var enabled by remember { mutableStateOf(false) }

        val googlePayLauncher = rememberGooglePayPaymentMethodLauncher(
            config = googlePayConfig,
            readyCallback = { ready ->
                if (ready) {
                    enabled = true
                }
                scope.launch {
                    scaffoldState.snackbarHostState.showSnackbar("Google Pay ready? $ready")
                }
            },
            resultCallback = { result ->
                when (result) {
                    is GooglePayPaymentMethodLauncher.Result.Completed -> {
                        "Successfully created a PaymentMethod. ${result.paymentMethod}"
                    }
                    GooglePayPaymentMethodLauncher.Result.Canceled -> {
                        "Customer cancelled Google Pay."
                    }
                    is GooglePayPaymentMethodLauncher.Result.Failed -> {
                        "Google Pay failed: ${result.errorCode}: ${result.error.message}"
                    }
                }.let {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(it)
                        enabled = false
                    }
                }
            }
        )

        GooglePayPaymentMethodLauncherScreen(
            scaffoldState = scaffoldState,
            enabled = enabled,
            onLaunchGooglePay = {
                googlePayLauncher.present(
                    currencyCode = "EUR",
                    amount = 2500L,
                )
            }
        )
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    private fun GooglePayPaymentMethodLauncherScreen(
        scaffoldState: ScaffoldState,
        enabled: Boolean,
        onLaunchGooglePay: () -> Unit
    ) {
        Scaffold(scaffoldState = scaffoldState) {
            AndroidView(
                factory = { context ->
                    GooglePayButton(context)
                },
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable(
                        enabled = enabled,
                        onClick = onLaunchGooglePay
                    )
            )
        }
    }
}
