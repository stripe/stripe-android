package com.stripe.android.paymentsheet.example.playground.embedded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.rememberEmbeddedPaymentElement
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.paymentsheet.example.playground.activity.CustomPaymentMethodActivity
import com.stripe.android.paymentsheet.example.playground.activity.FawryActivity
import com.stripe.android.paymentsheet.example.playground.network.PlaygroundRequester
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.DropdownSetting
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedViewDisplaysMandateSettingDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.samples.ui.shared.BuyButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalEmbeddedPaymentElementApi::class,
    ExperimentalCustomPaymentMethodsApi::class,
    ExperimentalAnalyticEventCallbackApi::class,
)
internal class EmbeddedPlaygroundActivity :
    AppCompatActivity(),
    ConfirmCustomPaymentMethodCallback,
    ExternalPaymentMethodConfirmHandler,
    AnalyticEventCallback {
    companion object {
        private const val PLAYGROUND_STATE_KEY = "playgroundState"
        const val EMBEDDED_PAYMENT_ELEMENT_STATE_KEY = "EMBEDDED_PAYMENT_ELEMENT_STATE_KEY"

        fun create(
            context: Context,
            playgroundState: PlaygroundState.Payment,
            embeddedPaymentElementState: EmbeddedPaymentElement.State? = null,
        ): Intent {
            return Intent(context, EmbeddedPlaygroundActivity::class.java).apply {
                putExtra(PLAYGROUND_STATE_KEY, playgroundState)
                putExtra(EMBEDDED_PAYMENT_ELEMENT_STATE_KEY, embeddedPaymentElementState)
            }
        }
    }

    private val viewModel: EmbeddedPlaygroundViewModel by viewModels()
    private lateinit var playgroundState: PlaygroundState.Payment
    private lateinit var playgroundSettings: PlaygroundSettings
    private lateinit var embeddedPaymentElement: EmbeddedPaymentElement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialPlaygroundState = getPlaygroundState(savedInstanceState).validate()
        if (initialPlaygroundState == null) {
            finish()
            return
        }
        this.playgroundState = initialPlaygroundState
        this.playgroundSettings = initialPlaygroundState.snapshot.playgroundSettings()

        val embeddedBuilder = EmbeddedPaymentElement.Builder(
            createIntentCallback = { _, _ ->
                val playgroundState = playgroundState
                PlaygroundRequester(playgroundState.snapshot, applicationContext).fetch().fold(
                    onSuccess = { state ->
                        val clientSecret = requireNotNull(state.asPaymentState()).clientSecret
                        CreateIntentResult.Success(clientSecret)
                    },
                    onFailure = { exception ->
                        CreateIntentResult.Failure(IllegalStateException(exception))
                    },
                )
            },
            resultCallback = ::handleEmbeddedResult,
        )
            .confirmCustomPaymentMethodCallback(this)
            .externalPaymentMethodConfirmHandler(this)
            .analyticEventCallback(this)
        val embeddedViewDisplaysMandateText =
            initialPlaygroundState.snapshot[EmbeddedViewDisplaysMandateSettingDefinition]
        setContent {
            embeddedPaymentElement = rememberEmbeddedPaymentElement(embeddedBuilder)

            var loadingState by remember {
                mutableStateOf(LoadingState.Loading)
            }

            val coroutineScope = rememberCoroutineScope()

            fun configure() = coroutineScope.launch {
                loadingState = LoadingState.Loading
                val result = embeddedPaymentElement.configure(
                    intentConfiguration = playgroundState.intentConfiguration(),
                    configuration = playgroundState.embeddedConfiguration(),
                )
                loadingState = when (result) {
                    is EmbeddedPaymentElement.ConfigureResult.Failed -> LoadingState.Failed
                    is EmbeddedPaymentElement.ConfigureResult.Succeeded -> LoadingState.Complete
                }
            }

            LaunchedEffect(embeddedPaymentElement) {
                val embeddedPaymentElementState = getEmbeddedPaymentElementState(savedInstanceState)
                if (embeddedPaymentElementState != null) {
                    embeddedPaymentElement.state = embeddedPaymentElementState
                    loadingState = LoadingState.Complete
                } else {
                    configure()
                }
            }

            BottomSheetContent(
                loadingState = loadingState,
                configure = ::configure,
                embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            )
        }

        setupBackPressedCallback()
    }

    @Composable
    private fun BottomSheetContent(
        loadingState: LoadingState,
        configure: () -> Unit,
        embeddedViewDisplaysMandateText: Boolean,
    ) {
        PlaygroundTheme(
            content = {
                loadingState.Content(embeddedPaymentElement = embeddedPaymentElement, retry = configure)
            },
            bottomBarContent = {
                val selectedPaymentOption by embeddedPaymentElement.paymentOption.collectAsState()

                selectedPaymentOption?.let { selectedPaymentOption ->
                    EmbeddedContentWithSelectedPaymentOption(
                        selectedPaymentOption = selectedPaymentOption,
                        embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                    )
                }

                ModeUi(configure = configure)
            }
        )
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    setResult(
                        RESULT_CANCELED,
                        Intent().apply {
                            putExtra(EMBEDDED_PAYMENT_ELEMENT_STATE_KEY, embeddedPaymentElement.state)
                        }
                    )
                    finish()
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(PLAYGROUND_STATE_KEY, playgroundState)
    }

    @Composable
    private fun ModeUi(configure: () -> Unit) {
        val configurationData by playgroundSettings.configurationData.collectAsState()

        val options = remember(configurationData) {
            CheckoutModeSettingsDefinition.createOptions(configurationData)
        }
        var value by rememberSaveable {
            mutableStateOf(playgroundSettings.snapshot()[CheckoutModeSettingsDefinition])
        }

        DropdownSetting<CheckoutMode>(
            name = "Mode",
            options = options,
            value = value,
            onOptionChanged = {
                value = it
                playgroundSettings[CheckoutModeSettingsDefinition] = it
                playgroundState = playgroundState.copy(amount = 5099, snapshot = playgroundSettings.snapshot())
                configure()
            },
        )
    }

    @Composable
    private fun EmbeddedContentWithSelectedPaymentOption(
        selectedPaymentOption: EmbeddedPaymentElement.PaymentOptionDisplayData,
        embeddedViewDisplaysMandateText: Boolean,
    ) {
        val confirming by viewModel.confirming.collectAsState()
        PaymentMethodSelector(
            isEnabled = !confirming,
            paymentMethodLabel = selectedPaymentOption.label,
            paymentMethodPainter = selectedPaymentOption.iconPainter,
            clickable = false,
            onClick = { },
        )

        if (!embeddedViewDisplaysMandateText) {
            selectedPaymentOption.mandateText?.let { mandateText ->
                Text(mandateText)
            }
        }

        BuyButton(buyButtonEnabled = !confirming) {
            viewModel.setConfirming(true)
            embeddedPaymentElement.confirm()
        }

        Button(
            onClick = {
                embeddedPaymentElement.clearPaymentOption()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !confirming,
        ) {
            Text("Clear selected")
        }
    }

    override fun confirmExternalPaymentMethod(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails
    ) {
        startActivity(
            Intent().setClass(
                this,
                FawryActivity::class.java
            ).putExtra(FawryActivity.EXTRA_EXTERNAL_PAYMENT_METHOD_TYPE, externalPaymentMethodType)
                .putExtra(FawryActivity.EXTRA_BILLING_DETAILS, billingDetails)
        )
    }

    private fun handleEmbeddedResult(result: EmbeddedPaymentElement.Result) {
        viewModel.setConfirming(false)
        when (result) {
            is EmbeddedPaymentElement.Result.Canceled -> {
                Log.d("EmbeddedPlayground", "Canceled")
                Toast.makeText(this, "Payment Canceled.", Toast.LENGTH_LONG).show()
            }
            is EmbeddedPaymentElement.Result.Completed -> {
                Log.d("EmbeddedPlayground", "Complete")
                setResult(RESULT_OK)
                finish()
            }
            is EmbeddedPaymentElement.Result.Failed -> {
                Log.e("EmbeddedPlayground", "Failed", result.error)
                Toast.makeText(this, "Payment Failed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getPlaygroundState(savedInstanceState: Bundle?): PlaygroundState.Payment? {
        return savedInstanceState?.getParcelable<PlaygroundState.Payment?>(PLAYGROUND_STATE_KEY)
            ?: intent.getParcelableExtra<PlaygroundState.Payment?>(PLAYGROUND_STATE_KEY)
    }

    @Suppress("DEPRECATION")
    private fun getEmbeddedPaymentElementState(savedInstanceState: Bundle?): EmbeddedPaymentElement.State? {
        if (savedInstanceState != null) {
            return null // Only set the state the first time.
        }
        return intent.getParcelableExtra<EmbeddedPaymentElement.State?>(EMBEDDED_PAYMENT_ELEMENT_STATE_KEY)
    }

    private fun PlaygroundState.Payment?.validate(): PlaygroundState.Payment? {
        if (this == null || integrationType != PlaygroundConfigurationData.IntegrationType.Embedded) {
            return null
        }
        return this
    }

    private enum class LoadingState {
        Loading {
            @Composable
            override fun Content(embeddedPaymentElement: EmbeddedPaymentElement, retry: () -> Unit) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(20.dp)
                            .width(64.dp)
                            .align(Alignment.Center),
                    )
                }
            }
        },
        Complete {
            @Composable
            override fun Content(embeddedPaymentElement: EmbeddedPaymentElement, retry: () -> Unit) {
                embeddedPaymentElement.Content()
            }
        },
        Failed {
            @Composable
            override fun Content(embeddedPaymentElement: EmbeddedPaymentElement, retry: () -> Unit) {
                Button(onClick = retry, Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
            }
        };

        @Composable
        abstract fun Content(embeddedPaymentElement: EmbeddedPaymentElement, retry: () -> Unit)
    }

    override fun onConfirmCustomPaymentMethod(
        customPaymentMethod: PaymentSheet.CustomPaymentMethod,
        billingDetails: PaymentMethod.BillingDetails
    ) {
        startActivity(
            Intent().setClass(
                this,
                CustomPaymentMethodActivity::class.java
            )
                .putExtra(CustomPaymentMethodActivity.EXTRA_CUSTOM_PAYMENT_METHOD_TYPE, customPaymentMethod)
                .putExtra(CustomPaymentMethodActivity.EXTRA_BILLING_DETAILS, billingDetails)
        )
    }

    override fun onEvent(event: AnalyticEvent) {
        Log.d("AnalyticEvent", "Event: $event")
    }
}
