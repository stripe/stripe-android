@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentsheet.example.playground

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressLauncher
import com.stripe.android.paymentsheet.addresselement.rememberAddressLauncher
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceBottomSheetDialogFragment
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import com.stripe.android.paymentsheet.example.playground.activity.FawryActivity
import com.stripe.android.paymentsheet.example.playground.activity.QrCodeActivity
import com.stripe.android.paymentsheet.example.playground.activity.getEmbeddedAppearance
import com.stripe.android.paymentsheet.example.playground.embedded.EmbeddedPlaygroundOneStepContract
import com.stripe.android.paymentsheet.example.playground.embedded.EmbeddedPlaygroundTwoStepContract
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedAppearanceSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedTwoStepSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.SettingsUi
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.CHECKOUT_TEST_TAG
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class PaymentSheetPlaygroundActivity : AppCompatActivity(), ExternalPaymentMethodConfirmHandler {
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

    private lateinit var embeddedPaymentElement: EmbeddedPaymentElement

    private val embeddedPlaygroundOneStepLauncher = registerForActivityResult(
        EmbeddedPlaygroundOneStepContract()
    ) { success ->
        viewModel.onEmbeddedResult(success)
    }

    private val embeddedPlaygroundTwoStepLauncher = registerForActivityResult(
        EmbeddedPlaygroundTwoStepContract()
    ) { result ->
        when (result) {
            EmbeddedPlaygroundTwoStepContract.Result.Cancelled -> Unit
            EmbeddedPlaygroundTwoStepContract.Result.Complete -> viewModel.onEmbeddedResult(true)
            is EmbeddedPlaygroundTwoStepContract.Result.Updated -> {
                embeddedPaymentElement.state = result.embeddedPaymentElementState
            }
        }
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val paymentSheet = PaymentSheet.Builder(viewModel::onPaymentSheetResult)
                .externalPaymentMethodConfirmHandler(this)
                .createIntentCallback(viewModel::createIntentCallback)
                .build()
            val flowController = PaymentSheet.FlowController.Builder(
                viewModel::onPaymentSheetResult,
                viewModel::onPaymentOptionSelected
            )
                .externalPaymentMethodConfirmHandler(this)
                .createIntentCallback(viewModel::createIntentCallback)
                .build()
            val embeddedPaymentElementBuilder = remember {
                EmbeddedPaymentElement.Builder(
                    viewModel::createIntentCallback,
                    viewModel::onEmbeddedResult,
                )
            }
            embeddedPaymentElement = rememberEmbeddedPaymentElement(embeddedPaymentElementBuilder)

            val addressLauncher = rememberAddressLauncher(
                callback = viewModel::onAddressLauncherResult
            )

            val playgroundSettings: PlaygroundSettings? by viewModel.playgroundSettingsFlow.collectAsState()
            val localPlaygroundSettings = playgroundSettings ?: return@setContent

            val playgroundState by viewModel.state.collectAsState()
            var showCustomEndpointDialog by remember { mutableStateOf(false) }
            val endpoint = playgroundState?.endpoint

            val customerPlaygroundState = playgroundState?.asCustomerState()
            val customerSheet = if (customerPlaygroundState?.isUsingCustomerSession == true) {
                val customerSessionProvider = remember(customerPlaygroundState) {
                    viewModel.createCustomerSessionProvider(customerPlaygroundState)
                }

                rememberCustomerSheet(
                    customerSessionProvider = customerSessionProvider,
                    callback = viewModel::onCustomerSheetCallback
                )
            } else {
                val adapter = remember(playgroundState) {
                    viewModel.createCustomerAdapter(playgroundState)
                }

                rememberCustomerSheet(
                    customerAdapter = adapter,
                    callback = viewModel::onCustomerSheetCallback
                )
            }

            if (showCustomEndpointDialog) {
                CustomEndpointDialog(
                    currentUrl = endpoint,
                    onConfirm = { backendUrl ->
                        viewModel.onCustomUrlUpdated(backendUrl)
                        showCustomEndpointDialog = false
                    },
                    onDismiss = {
                        showCustomEndpointDialog = false
                    }
                )
            }

            PlaygroundTheme(
                content = {
                    playgroundState?.asPaymentState()?.endpoint?.let { customEndpoint ->
                        Text(
                            text = "Using $customEndpoint",
                            modifier = Modifier
                                .clickable { showCustomEndpointDialog = true }
                                .padding(bottom = 16.dp),
                        )
                    }

                    playgroundState?.asPaymentState()?.stripeIntentId?.let { stripeIntentId ->
                        Text(
                            text = stripeIntentId,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

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
                                customerSheet = customerSheet,
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
        val settings = viewModel.playgroundSettingsFlow.collectAsState().value
        val embeddedAppearance = settings?.get(EmbeddedAppearanceSettingsDefinition)?.collectAsState()?.value
        supportFragmentManager.setFragmentResultListener(
            AppearanceBottomSheetDialogFragment.REQUEST_KEY,
            this@PaymentSheetPlaygroundActivity
        ) { _, bundle ->
            viewModel.updateEmbeddedAppearance(
                EmbeddedAppearanceSettingsDefinition,
                bundle.getEmbeddedAppearance()
            )
        }
        Button(
            onClick = {
                val bottomSheet = AppearanceBottomSheetDialogFragment.newInstance()
                bottomSheet.arguments = Bundle().apply {
                    putParcelable(AppearanceBottomSheetDialogFragment.EMBEDDED_KEY, embeddedAppearance)
                }
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
                viewModel.prepare(
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
        customerSheet: CustomerSheet,
        addressLauncher: AddressLauncher
    ) {
        if (playgroundState == null) {
            return
        }
        playgroundState.snapshot.setValues()

        when (playgroundState) {
            is PlaygroundState.Payment -> {
                if (playgroundState.displaysShippingAddressButton()) {
                    ShippingAddressButton(
                        addressLauncher = addressLauncher,
                        playgroundState = playgroundState,
                    )
                }

                when (playgroundState.integrationType) {
                    PlaygroundConfigurationData.IntegrationType.PaymentSheet -> {
                        PaymentSheetUi(
                            paymentSheet = paymentSheet,
                            playgroundState = playgroundState,
                        )
                    }

                    PlaygroundConfigurationData.IntegrationType.FlowController -> {
                        FlowControllerUi(
                            flowController = flowController,
                            playgroundState = playgroundState,
                        )
                    }

                    PlaygroundConfigurationData.IntegrationType.Embedded -> {
                        EmbeddedUi(
                            playgroundState = playgroundState,
                        )
                    }

                    else -> Unit
                }
            }
            is PlaygroundState.Customer -> CustomerSheetUi(
                customerSheet = customerSheet,
                playgroundState = playgroundState,
            )
        }
    }

    @Composable
    fun PaymentSheetUi(
        paymentSheet: PaymentSheet,
        playgroundState: PlaygroundState.Payment,
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
        playgroundState: PlaygroundState.Payment,
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
            paymentMethodPainter = flowControllerState.paymentMethodPainter(),
            onClick = flowController::presentPaymentOptions
        )
        BuyButton(
            buyButtonEnabled = flowControllerState?.selectedPaymentOption != null,
            onClick = flowController::confirm
        )
    }

    @Composable
    fun EmbeddedUi(
        playgroundState: PlaygroundState.Payment,
    ) {
        val isTwoStep = remember(playgroundState) {
            playgroundState.snapshot[EmbeddedTwoStepSettingsDefinition]
        }
        var hasConfigured: Boolean by remember { mutableStateOf(false) }
        LaunchedEffect(isTwoStep) {
            if (isTwoStep) {
                val configureResult = embeddedPaymentElement.configure(
                    intentConfiguration = playgroundState.intentConfiguration(),
                    configuration = playgroundState.embeddedConfiguration(),
                )
                hasConfigured = configureResult is EmbeddedPaymentElement.ConfigureResult.Succeeded
            }
        }

        if (isTwoStep) {
            val paymentOption by embeddedPaymentElement.paymentOption.collectAsState()
            PaymentMethodSelector(
                isEnabled = hasConfigured,
                paymentMethodLabel = if (hasConfigured) {
                    paymentOption?.label ?: "Select"
                } else {
                    "Loading"
                },
                paymentMethodPainter = paymentOption?.iconPainter,
                onClick = {
                    embeddedPlaygroundTwoStepLauncher.launch(
                        EmbeddedPlaygroundTwoStepContract.Args(
                            playgroundState,
                            requireNotNull(embeddedPaymentElement.state),
                        )
                    )
                }
            )
        }

        Button(
            onClick = {
                if (isTwoStep) {
                    embeddedPaymentElement.confirm()
                } else {
                    embeddedPlaygroundOneStepLauncher.launch(playgroundState)
                }
            },
            enabled = if (isTwoStep) hasConfigured else true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CHECKOUT_TEST_TAG),
        ) {
            Text("Checkout")
        }
    }

    @Composable
    fun CustomerSheetUi(
        customerSheet: CustomerSheet,
        playgroundState: PlaygroundState.Customer,
    ) {
        val customerSheetState by viewModel.customerSheetState.collectAsState()

        LaunchedEffect(playgroundState, customerSheetState) {
            customerSheet.configure(
                configuration = playgroundState.customerSheetConfiguration(),
            )

            if (customerSheetState?.shouldFetchPaymentOption == true) {
                fetchOption(customerSheet).onSuccess { option ->
                    viewModel.customerSheetState.emit(
                        CustomerSheetState(
                            selectedPaymentOption = option,
                            shouldFetchPaymentOption = false
                        )
                    )
                }.onFailure { exception ->
                    viewModel.status.emit(
                        StatusMessage(
                            message = "Failed to retrieve payment options:\n${exception.message}"
                        )
                    )
                }
            }
        }

        customerSheetState?.let { state ->
            if (state.shouldFetchPaymentOption) {
                return
            }

            PaymentMethodSelector(
                isEnabled = true,
                paymentMethodLabel = customerSheetState.paymentMethodLabel(),
                paymentMethodPainter = customerSheetState.paymentMethodPainter(),
                onClick = customerSheet::present
            )
        }
    }

    @Composable
    private fun ShippingAddressButton(
        addressLauncher: AddressLauncher,
        playgroundState: PlaygroundState.Payment,
    ) {
        val context = LocalContext.current
        Button(
            onClick = {
                val configuration = AddressLauncher.Configuration.Builder()
                    .googlePlacesApiKey(Settings(context).googlePlacesApiKey)
                    .appearance(AppearanceStore.state)
                    .build()
                addressLauncher.present(playgroundState.clientSecret, configuration)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set Shipping Address")
        }
    }

    private fun presentPaymentSheet(paymentSheet: PaymentSheet, playgroundState: PlaygroundState.Payment) {
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
                intentConfiguration = playgroundState.intentConfiguration(),
                configuration = playgroundState.paymentSheetConfiguration(),
            )
        }
    }

    private fun configureFlowController(
        flowController: PaymentSheet.FlowController,
        playgroundState: PlaygroundState.Payment,
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
                intentConfiguration = playgroundState.intentConfiguration(),
                configuration = playgroundState.paymentSheetConfiguration(),
                callback = viewModel::onFlowControllerConfigured,
            )
        }
    }

    private suspend fun fetchOption(
        customerSheet: CustomerSheet
    ): Result<PaymentOption?> = withContext(Dispatchers.IO) {
        when (val result = customerSheet.retrievePaymentOptionSelection()) {
            is CustomerSheetResult.Selected -> Result.success(result.selection?.paymentOption)
            is CustomerSheetResult.Failed -> Result.failure(result.exception)
            is CustomerSheetResult.Canceled -> Result.success(null)
        }
    }

    override fun confirmExternalPaymentMethod(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails
    ) {
        this.startActivity(
            Intent().setClass(
                this,
                FawryActivity::class.java
            )
                .putExtra(FawryActivity.EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE, externalPaymentMethodType)
                .putExtra(FawryActivity.EXTRA_BILLING_DETAILS, billingDetails)
        )
    }
}

const val RELOAD_TEST_TAG = "RELOAD"
private const val PLAYGROUND_BOTTOM_BAR_LABEL = "PlaygroundBottomBar"
