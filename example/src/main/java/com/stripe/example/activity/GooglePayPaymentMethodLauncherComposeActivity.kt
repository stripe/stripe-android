package com.stripe.example.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.googlepaylauncher.GooglePayDynamicUpdateHandler
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayReceivedUpdate
import com.stripe.android.googlepaylauncher.GooglePayUpdate
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
        existingPaymentMethodRequired = false,
        shippingAddressParameters = GooglePayJsonFactory.ShippingAddressParameters(
            isRequired = true,
        ),
        shippingOptionParameters = GooglePayJsonFactory.ShippingOptionParameters(
            shippingOptions = listOf(
                GooglePayJsonFactory.ShippingOptionParameters.SelectionOption(
                    id = "id_2",
                    label = "Regular $10",
                    description = null
                )
            ),
            defaultSelectedOptionId = "id_2"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

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
            },
            updateHandler = remember {
                object : GooglePayDynamicUpdateHandler {
                    var previousCountry: String? = null

                    override fun handle(
                        receivedUpdate: GooglePayReceivedUpdate,
                        onUpdate: (update: GooglePayUpdate) -> Unit
                    ) {
                        if (receivedUpdate.address?.countryCode == "CA") {
                            val options = listOf(
                                GooglePayUpdate.ShippingOptionParameters.SelectionOption(
                                    id = "id_1",
                                    label = "Canada Special $5",
                                    description = "Canadians get cheaper shipping"
                                ),
                                GooglePayUpdate.ShippingOptionParameters.SelectionOption(
                                    id = "id_2",
                                    label = "Regular $10",
                                    description = null
                                )
                            )

                            val selectedOption = if (previousCountry != "CA") {
                                options[0].id
                            } else {
                                receivedUpdate.shippingOption?.id ?: options[0].id
                            }

                            onUpdate(
                                GooglePayUpdate(
                                    transactionInfo = GooglePayUpdate.TransactionInfo(
                                        currencyCode = "EUR",
                                        totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                                        checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
                                        totalPrice = getTotalPrice(selectedOption).toString(),
                                    ),
                                    shippingOptionParameters = GooglePayUpdate.ShippingOptionParameters(
                                        options = options,
                                        selectedOptionId = selectedOption,
                                    )
                                )
                            )

                            previousCountry = "CA"
                        } else {
                            val options = listOf(
                                GooglePayUpdate.ShippingOptionParameters.SelectionOption(
                                    id = "id_2",
                                    label = "Regular $10",
                                    description = null,
                                )
                            )

                            val selectedOption = receivedUpdate.shippingOption?.id.takeUnless { it == "id_1" }
                                ?: options[0].id

                            onUpdate(
                                GooglePayUpdate(
                                    transactionInfo = GooglePayUpdate.TransactionInfo(
                                        currencyCode = "EUR",
                                        totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                                        checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default,
                                        totalPrice = getTotalPrice(selectedOption).toString(),
                                    ),
                                    shippingOptionParameters = GooglePayUpdate.ShippingOptionParameters(
                                        options = options,
                                        selectedOptionId = receivedUpdate.shippingOption?.id.takeUnless { it == "id_1" }
                                    )
                                )
                            )

                            previousCountry = "CA"
                        }
                    }
                }
            }
        )

        GooglePayPaymentMethodLauncherScreen(
            enabled = enabled,
            onLaunchGooglePay = {
                googlePayLauncher.present(
                    currencyCode = "EUR",
                    amount = 2500L,
                )
            }
        )
    }

    private fun getTotalPrice(selectedOption: String?) = when (selectedOption) {
        "id_1" -> 30.00
        "id_2" -> 35.00
        else -> 25.00
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    private fun GooglePayPaymentMethodLauncherScreen(
        enabled: Boolean,
        onLaunchGooglePay: () -> Unit
    ) {
        Box(Modifier.padding(vertical = 50.dp)) {
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
