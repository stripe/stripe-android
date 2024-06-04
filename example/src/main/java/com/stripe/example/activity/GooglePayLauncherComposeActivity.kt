package com.stripe.example.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayLauncher
import com.stripe.android.googlepaylauncher.rememberGooglePayLauncher
import kotlinx.coroutines.launch

class GooglePayLauncherComposeActivity : StripeIntentActivity() {
    private val googlePayConfig = GooglePayLauncher.Config(
        environment = GooglePayEnvironment.Test,
        merchantCountryCode = COUNTRY_CODE,
        merchantName = "Widget Store",
        billingAddressConfig = GooglePayLauncher.BillingAddressConfig(
            isRequired = true,
            format = GooglePayLauncher.BillingAddressConfig.Format.Full,
            isPhoneNumberRequired = false
        ),
        existingPaymentMethodRequired = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GooglePayLauncherScreen()
        }
    }

    @Composable
    private fun GooglePayLauncherScreen() {
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()

        var clientSecret by rememberSaveable { mutableStateOf("") }
        var googlePayReady by rememberSaveable { mutableStateOf<Boolean?>(null) }
        var completed by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (clientSecret.isBlank()) {
                viewModel.createPaymentIntent(COUNTRY_CODE).observe(
                    this@GooglePayLauncherComposeActivity
                ) { result ->
                    result.fold(
                        onSuccess = { json ->
                            clientSecret = json.getString("secret")
                        },
                        onFailure = { error ->
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    "Could not create PaymentIntent. ${error.message}"
                                )
                            }
                            completed = true
                        }
                    )
                }
            }
        }

        val googlePayLauncher = rememberGooglePayLauncher(
            config = googlePayConfig,
            readyCallback = { ready ->
                if (googlePayReady == null) {
                    googlePayReady = ready

                    if (!ready) {
                        completed = true
                    }

                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar("Google Pay ready? $ready")
                    }
                }
            },
            resultCallback = { result ->
                when (result) {
                    GooglePayLauncher.Result.Completed -> {
                        "Successfully collected payment."
                    }
                    GooglePayLauncher.Result.Canceled -> {
                        "Customer cancelled Google Pay."
                    }
                    is GooglePayLauncher.Result.Failed -> {
                        "Google Pay failed. ${result.error.message}"
                    }
                }.let {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(it)
                        completed = true
                    }
                }
            }
        )

        GooglePayLauncherScreen(
            scaffoldState = scaffoldState,
            clientSecret = clientSecret,
            googlePayReady = googlePayReady,
            completed = completed,
            onLaunchGooglePay = { googlePayLauncher.presentForPaymentIntent(it) }
        )
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    private fun GooglePayLauncherScreen(
        scaffoldState: ScaffoldState,
        clientSecret: String,
        googlePayReady: Boolean?,
        completed: Boolean,
        onLaunchGooglePay: (String) -> Unit
    ) {
        Scaffold(scaffoldState = scaffoldState) {
            Column(Modifier.fillMaxWidth()) {
                if (googlePayReady == null || clientSecret.isBlank()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                Spacer(
                    Modifier
                        .height(8.dp)
                        .fillMaxWidth()
                )

                AndroidView(
                    factory = { context ->
                        GooglePayButton(context)
                    },
                    modifier = Modifier
                        .wrapContentWidth()
                        .clickable(
                            enabled = googlePayReady == true &&
                                clientSecret.isNotBlank() && !completed,
                            onClick = {
                                onLaunchGooglePay(clientSecret)
                            }
                        )
                )
            }
        }
    }

    private companion object {
        private const val COUNTRY_CODE = "US"
    }
}
