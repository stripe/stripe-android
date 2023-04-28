package com.stripe.android.paymentsheet.example.samples.ui.complete_flow

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt
import com.stripe.android.paymentsheet.example.samples.ui.shared.ShippingAddressButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.ShippingAddressLabel
import com.stripe.android.uicore.R as UiCoreR

internal class CompleteFlowActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<CompleteFlowViewModel>()

    private lateinit var paymentSheet: PaymentSheet
    private lateinit var addressLauncher: AddressLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentSheet = PaymentSheet(
            activity = this,
            callback = viewModel::handlePaymentSheetResult,
        )

        addressLauncher = AddressLauncher(
            activity = this,
            callback = viewModel::handleAddressResult
        )

        setContent {
            PaymentSheetExampleTheme {
                val uiState by viewModel.state.collectAsState()
                val shippingAddress = uiState.paymentInfo?.shippingDetails

                uiState.paymentInfo?.let { paymentInfo ->
                    LaunchedEffect(paymentInfo) {
                        presentPaymentSheet(paymentInfo)
                    }
                }

                uiState.status?.let { status ->
                    if (uiState.didComplete) {
                        CompletedPaymentAlertDialog(
                            onDismiss = ::finish
                        )
                    } else {
                        LaunchedEffect(status) {
                            snackbar.setText(status).show()
                            viewModel.statusDisplayed()
                        }
                    }
                }

                Receipt(
                    isLoading = uiState.isProcessing,
                    cartState = uiState.cartState,
                ) {
                    if (shippingAddress == null) {
                        ShippingAddressButton(
                            addressButtonEnabled = !uiState.isProcessing,
                            onClick = ::presentAddressSheet
                        )
                    } else {
                        ShippingAddressLabel(address = shippingAddress)
                    }
                    BuyButton(
                        buyButtonEnabled = !uiState.isProcessing,
                        onClick = viewModel::checkout,
                    )
                }
            }
        }
    }

    private fun presentPaymentSheet(paymentInfo: CompleteFlowViewState.PaymentInfo) {
        if (!paymentInfo.shouldPresent) {
            return
        }

        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret = paymentInfo.clientSecret,
            configuration = paymentInfo.paymentSheetConfig,
        )

        viewModel.paymentSheetPresented()
    }

    private fun presentAddressSheet() {
        addressLauncher.present(
            publishableKey = PaymentConfiguration.getInstance(this).publishableKey,
            configuration = AddressLauncher.Configuration(
                additionalFields = AddressLauncher.AdditionalFieldsConfiguration(
                    checkboxLabel = resources.getString(
                        UiCoreR.string.stripe_billing_same_as_shipping
                    ),
                )
            )
        )
    }
}
