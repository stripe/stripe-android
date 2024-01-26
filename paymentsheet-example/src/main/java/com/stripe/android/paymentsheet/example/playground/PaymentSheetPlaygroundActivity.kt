package com.stripe.android.paymentsheet.example.playground

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceBottomSheetDialogFragment
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import com.stripe.android.paymentsheet.example.playground.activity.QrCodeActivity
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.IntegrationType
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.SettingsUi
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CHECKOUT_TEST_TAG
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheetFlowController
import kotlinx.coroutines.flow.update

internal class PaymentSheetPlaygroundActivity : AppCompatActivity() {
    companion object {
        fun createTestIntent(settingsJson: String): Intent {
            return Intent(
                Intent.ACTION_VIEW,
                PaymentSheetPlaygroundUrlHelper.createUri(settingsJson)
            )
        }
    }

    val viewModel: PaymentSheetPlaygroundViewModel by viewModels {
        PaymentSheetPlaygroundViewModel.Factory(
            applicationSupplier = { application },
            uriSupplier = { intent.data },
        )
    }

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
            val addressLauncher = rememberAddressLauncher(
                callback = viewModel::onAddressLauncherResult
            )

            val playgroundSettings by viewModel.playgroundSettingsFlow.collectAsState()
            val localPlaygroundSettings = playgroundSettings ?: return@setContent

            val playgroundState by viewModel.state.collectAsState()

            PlaygroundTheme(
                content = {
                    SettingsUi(playgroundSettings = localPlaygroundSettings)

                    AppearanceButton()

                    QrCodeButton(playgroundSettings = localPlaygroundSettings)

                    ClearLinkDataButton()
                },
                bottomBarContent = {
                    ReloadButton(playgroundSettings = localPlaygroundSettings)

                    AnimatedContent(
                        label = PLAYGROUND_BOTTOM_BAR_LABEL,
                        targetState = playgroundState
                    ) { playgroundState ->
                        Column {
                            PlaygroundStateUi(
                                playgroundState = playgroundState,
                                paymentSheet = paymentSheet,
                                flowController = flowController,
                                addressLauncher = addressLauncher,
                            )
                        }
                    }
                },
            )

            val status by viewModel.status.collectAsState()
            val context = LocalContext.current
            LaunchedEffect(status) {
                if (!status?.message.isNullOrEmpty() && status?.hasBeenDisplayed == false) {
                    Toast.makeText(context, status?.message, Toast.LENGTH_LONG).show()
                }
                viewModel.status.value = status?.copy(hasBeenDisplayed = true)
            }
        }
    }

    @Composable
    private fun AppearanceButton() {
        Button(
            onClick = {
                val bottomSheet = AppearanceBottomSheetDialogFragment.newInstance()
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Change Appearance")
        }
    }

    @Composable
    private fun QrCodeButton(playgroundSettings: PlaygroundSettings) {
        val context = LocalContext.current
        Button(
            onClick = {
                context.startActivity(
                    QrCodeActivity.create(
                        context = context,
                        settingsJson = playgroundSettings.snapshot().asJsonString(),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("QR code for current settings")
        }
    }

    @Composable
    private fun ClearLinkDataButton() {
        val context = LocalContext.current
        Button(
            onClick = {
                PaymentSheet.resetCustomer(context)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear Link customer")
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RELOAD_TEST_TAG),
        ) {
            Text("Reload")
        }
    }

    @Composable
    private fun PlaygroundStateUi(
        playgroundState: PlaygroundState?,
        paymentSheet: PaymentSheet,
        flowController: PaymentSheet.FlowController,
        addressLauncher: AddressLauncher
    ) {
        if (playgroundState == null) {
            return
        }

        ShippingAddressButton(
            addressLauncher = addressLauncher,
            playgroundState = playgroundState,
        )

        when (playgroundState.integrationType) {
            IntegrationType.PaymentSheet -> {
                PaymentSheetUi(
                    paymentSheet = paymentSheet,
                    playgroundState = playgroundState,
                )
            }

            IntegrationType.FlowController -> {
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CHECKOUT_TEST_TAG),
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
        val localFlowControllerState = flowControllerState

        LaunchedEffect(localFlowControllerState) {
            if (localFlowControllerState?.shouldFetchPaymentOption == true) {
                viewModel.flowControllerState.update { previousState ->
                    previousState?.copy(
                        selectedPaymentOption = flowController.getPaymentOption(),
                        shouldFetchPaymentOption = false,
                    )
                }
            }
        }

        LaunchedEffect(localFlowControllerState?.addressDetails) {
            flowController.shippingDetails = localFlowControllerState?.addressDetails
        }

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

    @Composable
    private fun ShippingAddressButton(
        addressLauncher: AddressLauncher,
        playgroundState: PlaygroundState,
    ) {
        Button(
            onClick = {
                addressLauncher.present(playgroundState.clientSecret)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set Shipping Address")
        }
    }

    private fun presentPaymentSheet(paymentSheet: PaymentSheet, playgroundState: PlaygroundState) {
        if (playgroundState.initializationType == InitializationType.Normal) {
            if (playgroundState.checkoutMode == CheckoutMode.SETUP) {
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
        if (playgroundState.initializationType == InitializationType.Normal) {
            if (playgroundState.checkoutMode == CheckoutMode.SETUP) {
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

@Composable
private fun PlaygroundTheme(
    content: @Composable ColumnScope.() -> Unit,
    bottomBarContent: @Composable ColumnScope.() -> Unit,
) {
    val colors = if (isSystemInDarkTheme() || AppearanceStore.forceDarkMode) {
        darkColors()
    } else {
        lightColors()
    }
    MaterialTheme(
        typography = MaterialTheme.typography.copy(
            body1 = MaterialTheme.typography.body1.copy(fontSize = 14.sp)
        ),
        colors = colors,
    ) {
        Surface(
            color = MaterialTheme.colors.background,
        ) {
            Scaffold(
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.surface)
                            .animateContentSize()
                    ) {
                        Divider()
                        Column(
                            content = bottomBarContent,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                },
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                            .padding(16.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

const val RELOAD_TEST_TAG = "RELOAD"
private const val PLAYGROUND_BOTTOM_BAR_LABEL = "PlaygroundBottomBar"
