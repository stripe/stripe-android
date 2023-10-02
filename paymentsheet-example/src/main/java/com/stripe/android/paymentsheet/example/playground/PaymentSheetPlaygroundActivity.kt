package com.stripe.android.paymentsheet.example.playground

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.IntegrationTypeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.SettingsUi
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController

internal class PaymentSheetPlaygroundActivity : AppCompatActivity() {
    private val viewModel: PaymentSheetPlaygroundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val paymentSheet = rememberPaymentSheet(
                paymentResultCallback = viewModel::onPaymentSheetResult,
                createIntentCallback = viewModel::createIntentCallback
            )
            val flowController = rememberPaymentSheetFlowController(
                paymentOptionCallback = viewModel::onPaymentOptionSelected,
                paymentResultCallback = viewModel::onPaymentSheetResult,
                createIntentCallback = viewModel::createIntentCallback
            )

            val playgroundSettings by viewModel.playgroundSettingsFlow.collectAsState()
            val localPlaygroundSettings = playgroundSettings ?: return@setContent

            MaterialTheme(
                typography = MaterialTheme.typography.copy(
                    body1 = MaterialTheme.typography.body1.copy(fontSize = 14.sp)
                )
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    SettingsUi(playgroundSettings = localPlaygroundSettings)
                    ReloadButton(playgroundSettings = localPlaygroundSettings)

                    val playgroundState by viewModel.state.collectAsState()
                    PlaygroundStateUi(
                        playgroundState = playgroundState,
                        paymentSheet = paymentSheet,
                        flowController = flowController,
                    )

                    val status by viewModel.status.collectAsState()
                    val context = LocalContext.current
                    LaunchedEffect(status) {
                        if (!status.isNullOrEmpty()) {
                            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReloadButton(playgroundSettings: PlaygroundSettings) {
        Button(
            onClick = {
                viewModel.prepareCheckout(
                    playgroundSettings = playgroundSettings,
                )
            },
        ) {
            Text("Reload")
        }
    }

    @Composable
    private fun PlaygroundStateUi(
        playgroundState: PlaygroundState?,
        paymentSheet: PaymentSheet,
        flowController: PaymentSheet.FlowController
    ) {
        if (playgroundState == null) {
            return
        }

        when (playgroundState.integrationType) {
            IntegrationTypeSettingsDefinition.IntegrationType.PaymentSheet -> {
                PaymentSheetUi(
                    paymentSheet = paymentSheet,
                    playgroundState = playgroundState,
                )
            }

            IntegrationTypeSettingsDefinition.IntegrationType.FlowController -> {
                FlowControllerUi(
                    flowController = flowController,
                    playgroundState = playgroundState,
                )
            }
        }
    }

    @Composable
    fun PaymentSheetUi(
        paymentSheet: PaymentSheet,
        playgroundState: PlaygroundState,
    ) {
        Button(
            onClick = {
                presentPaymentSheet(paymentSheet, playgroundState)
            }
        ) {
            Text("Checkout")
        }
    }

    @Composable
    fun FlowControllerUi(
        flowController: PaymentSheet.FlowController,
        playgroundState: PlaygroundState,
    ) {
        LaunchedEffect(playgroundState) {
            configureFlowController(
                flowController = flowController,
                playgroundState = playgroundState,
            )
        }

        val flowControllerState by viewModel.flowControllerState.collectAsState()

        PaymentMethodSelector(
            isEnabled = flowControllerState != null,
            paymentMethodLabel = flowControllerState.paymentMethodLabel(),
            paymentMethodIcon = flowControllerState.paymentMethodIcon(),
            onClick = flowController::presentPaymentOptions
        )
        BuyButton(
            buyButtonEnabled = flowControllerState?.selectedPaymentOption != null,
            onClick = flowController::confirm
        )
    }

    private fun presentPaymentSheet(paymentSheet: PaymentSheet, playgroundState: PlaygroundState) {
        if (playgroundState.initializationType == InitializationTypeSettingsDefinition.InitializationType.Normal) {
            if (playgroundState.checkoutMode == CheckoutModeSettingsDefinition.CheckoutMode.SETUP) {
                paymentSheet.presentWithSetupIntent(
                    setupIntentClientSecret = playgroundState.clientSecret,
                    configuration = playgroundState.paymentSheetConfiguration()
                )
            } else {
                paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret = playgroundState.clientSecret,
                    configuration = playgroundState.paymentSheetConfiguration()
                )
            }
        } else {
            paymentSheet.presentWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = playgroundState.checkoutMode.intentConfigurationMode(playgroundState),
                    paymentMethodTypes = playgroundState.paymentMethodTypes,
                ),
                configuration = playgroundState.paymentSheetConfiguration(),
            )
        }
    }

    private fun configureFlowController(
        flowController: PaymentSheet.FlowController,
        playgroundState: PlaygroundState,
    ) {
        if (playgroundState.initializationType == InitializationTypeSettingsDefinition.InitializationType.Normal) {
            if (playgroundState.checkoutMode == CheckoutModeSettingsDefinition.CheckoutMode.SETUP) {
                flowController.configureWithSetupIntent(
                    setupIntentClientSecret = playgroundState.clientSecret,
                    configuration = playgroundState.paymentSheetConfiguration(),
                    callback = viewModel::onFlowControllerConfigured,
                )
            } else {
                flowController.configureWithPaymentIntent(
                    paymentIntentClientSecret = playgroundState.clientSecret,
                    configuration = playgroundState.paymentSheetConfiguration(),
                    callback = viewModel::onFlowControllerConfigured,
                )
            }
        } else {
            flowController.configureWithIntentConfiguration(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = playgroundState.checkoutMode.intentConfigurationMode(playgroundState),
                    paymentMethodTypes = playgroundState.paymentMethodTypes,
                ),
                configuration = playgroundState.paymentSheetConfiguration(),
                callback = viewModel::onFlowControllerConfigured,
            )
        }
    }
}
