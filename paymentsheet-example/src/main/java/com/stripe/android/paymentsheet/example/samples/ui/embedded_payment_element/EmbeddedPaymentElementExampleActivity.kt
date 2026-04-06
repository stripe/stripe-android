package com.stripe.android.paymentsheet.example.samples.ui.embedded_payment_element

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.model.toIntentConfiguration
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CompletedPaymentAlertDialog
import com.stripe.android.paymentsheet.example.samples.ui.shared.ErrorScreen
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.samples.ui.shared.Receipt

internal class EmbeddedPaymentElementExampleActivity : AppCompatActivity() {

    private val snackbar by lazy {
        Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(Color.BLACK)
            .setTextColor(Color.WHITE)
    }

    private val viewModel by viewModels<EmbeddedPaymentElementExampleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PaymentSheetExampleTheme {
                EmbeddedPaymentElementScreen(
                    viewModel = viewModel,
                    snackbar = snackbar,
                    onFinish = ::finish,
                )
            }
        }
    }
}

@Composable
private fun EmbeddedPaymentElementScreen(
    viewModel: EmbeddedPaymentElementExampleViewModel,
    snackbar: Snackbar,
    onFinish: () -> Unit,
) {
    val embeddedBuilder = remember {
        EmbeddedPaymentElement.Builder(
            createIntentCallback = viewModel::createAndConfirmIntent,
            resultCallback = viewModel::handleResult,
        )
    }

    val embeddedPaymentElement = rememberEmbeddedPaymentElement(embeddedBuilder)

    val uiState by viewModel.state.collectAsState()
    val selectedPaymentOption by embeddedPaymentElement.paymentOption.collectAsState()
    val paymentMethodLabel = determinePaymentMethodLabel(uiState, selectedPaymentOption)

    if (uiState.cartState.total != null) {
        LaunchedEffect(uiState.cartState) {
            val result = embeddedPaymentElement.configure(
                intentConfiguration = uiState.cartState.toIntentConfiguration(),
                configuration = uiState.embeddedConfig,
            )
            val error = (result as? EmbeddedPaymentElement.ConfigureResult.Failed)?.error
            viewModel.handleConfigured(error)
        }
    }

    uiState.status?.let { status ->
        if (uiState.didComplete) {
            CompletedPaymentAlertDialog(onDismiss = onFinish)
        } else {
            LaunchedEffect(status) {
                snackbar.setText(status).show()
                viewModel.statusDisplayed()
            }
        }
    }

    EmbeddedPaymentElementContent(
        uiState = uiState,
        selectedPaymentOption = selectedPaymentOption,
        paymentMethodLabel = paymentMethodLabel,
        embeddedPaymentElement = embeddedPaymentElement,
        viewModel = viewModel,
    )
}

@Composable
private fun EmbeddedPaymentElementContent(
    uiState: EmbeddedPaymentElementExampleViewState,
    selectedPaymentOption: EmbeddedPaymentElement.PaymentOptionDisplayData?,
    paymentMethodLabel: String,
    embeddedPaymentElement: EmbeddedPaymentElement,
    viewModel: EmbeddedPaymentElementExampleViewModel,
) {
    Box(
        modifier = Modifier.padding(
            paddingValues = WindowInsets.systemBars.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            ).asPaddingValues()
        ),
    ) {
        if (uiState.isError) {
            ErrorScreen(onRetry = viewModel::retry)
        } else {
            Receipt(
                isLoading = uiState.isProcessing,
                cartState = uiState.cartState,
            ) {
                PaymentMethodSelector(
                    isEnabled = uiState.isPaymentMethodButtonEnabled,
                    paymentMethodLabel = paymentMethodLabel,
                    paymentMethodPainter = selectedPaymentOption?.iconPainter,
                    onClick = embeddedPaymentElement::presentPaymentOptions,
                )

                selectedPaymentOption?.mandateText?.let { mandateText ->
                    Text(mandateText)
                }

                BuyButton(
                    buyButtonEnabled = selectedPaymentOption != null && !uiState.isProcessing,
                    onClick = {
                        viewModel.handleBuyButtonPressed()
                        embeddedPaymentElement.confirm()
                    }
                )
            }
        }
    }
}

@Composable
private fun determinePaymentMethodLabel(
    uiState: EmbeddedPaymentElementExampleViewState,
    selectedPaymentOption: EmbeddedPaymentElement.PaymentOptionDisplayData?,
): String {
    val resources = LocalResources.current
    return remember(uiState, selectedPaymentOption) {
        if (selectedPaymentOption?.label != null) {
            selectedPaymentOption.label
        } else if (!uiState.isProcessing) {
            resources.getString(R.string.select)
        } else {
            resources.getString(R.string.loading)
        }
    }
}
