package com.stripe.android.paymentsheet.example.playground.spt

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.ExtendedLabelsInPaymentOptionPreview
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.rememberPaymentLauncher
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.data.ShopPayData
import com.stripe.android.paymentsheet.example.playground.network.SharedPaymentTokenPlaygroundRequester
import com.stripe.android.paymentsheet.example.playground.settings.WalletButtonsPlaygroundType
import com.stripe.android.paymentsheet.example.playground.settings.WalletButtonsSettingsDefinition
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBackgroundColor
import com.stripe.android.uicore.getBorderStrokeColor
import com.stripe.android.uicore.getOnBackgroundColor
import kotlinx.coroutines.launch

@OptIn(SharedPaymentTokenSessionPreview::class)
internal class SharedPaymentTokenPlaygroundActivity : AppCompatActivity() {
    @Suppress("DEPRECATION")
    private val playgroundState by lazy {
        intent.getParcelableExtra<PlaygroundState.SharedPaymentToken>(PLAYGROUND_STATE_KEY)
            ?: throw IllegalStateException("Cannot launch SPT flow without playground state!")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var confirming by remember { mutableStateOf(false) }
            var screenContent by remember { mutableStateOf(ScreenContent.Loading) }
            var paymentOption by remember { mutableStateOf<PaymentOption?>(null) }

            val requester = remember {
                SharedPaymentTokenPlaygroundRequester(
                    playgroundState.snapshot,
                    applicationContext,
                )
            }

            val paymentLauncher = rememberPlaygroundPaymentLauncher {
                confirming = it
            }

            val preparePaymentMethodHandler by rememberPreparePaymentMethodHandler(paymentLauncher, requester) {
                confirming = it
            }

            val flowController = rememberFlowControllerBuilder(preparePaymentMethodHandler) {
                paymentOption = it
            }.build()

            val configure = remember(flowController) {
                {
                    screenContent = ScreenContent.Loading

                    flowController.configureWithIntentConfiguration(
                        intentConfiguration = playgroundState.intentConfiguration(),
                        configuration = playgroundState.paymentSheetConfiguration(),
                        callback = { success, error ->
                            paymentOption = flowController.getPaymentOption()

                            Log.d("TEST", error?.message ?: "")

                            screenContent = if (success) {
                                ScreenContent.Complete
                            } else {
                                ScreenContent.Failed
                            }
                        }
                    )
                }
            }

            LaunchedEffect(Unit) {
                configure()
            }

            StripeTheme {
                screenContent.Content(
                    ScreenContent.Parameters(
                        paymentOption = paymentOption,
                        flowController = flowController,
                        showWalletButtons =
                        playgroundState.snapshot[WalletButtonsSettingsDefinition]
                            != WalletButtonsPlaygroundType.Disabled,
                        confirming = confirming,
                        confirm = {
                            confirming = true
                            flowController.confirm()
                        },
                        retry = configure
                    )
                )
            }
        }
    }

    @Composable
    private fun rememberPlaygroundPaymentLauncher(
        onConfirmingChanged: (Boolean) -> Unit
    ): PaymentLauncher {
        return rememberPaymentLauncher(
            publishableKey = SharedPaymentTokenPlaygroundRequester.PUBLISHABLE_KEY,
            stripeAccountId = null,
        ) { result ->
            onConfirmingChanged(false)

            when (result) {
                is PaymentResult.Completed -> {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is PaymentResult.Failed -> {
                    Toast.makeText(
                        applicationContext,
                        "Failed next action: ${result.throwable.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is PaymentResult.Canceled -> {
                    Toast.makeText(
                        applicationContext,
                        "Canceled",
                        Toast.LENGTH_LONG
                    ).show()

                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    @OptIn(ShopPayPreview::class)
    @Composable
    private fun rememberFlowControllerBuilder(
        preparePaymentMethodHandler: PreparePaymentMethodHandler,
        onPaymentOptionChanged: (PaymentOption?) -> Unit
    ): PaymentSheet.FlowController.Builder {
        return remember {
            PaymentSheet.FlowController.Builder(
                paymentOptionCallback = { result ->
                    onPaymentOptionChanged(result)
                },
                resultCallback = {}
            ).shopPayHandlers(ShopPayData.shopPayHandlers())
                .preparePaymentMethodHandler { paymentMethod, shippingAddress ->
                    preparePaymentMethodHandler.onPreparePaymentMethod(paymentMethod, shippingAddress)
                }
        }
    }

    @Composable
    private fun rememberPreparePaymentMethodHandler(
        paymentLauncher: PaymentLauncher,
        requester: SharedPaymentTokenPlaygroundRequester,
        onConfirmingChanged: (Boolean) -> Unit
    ): State<PreparePaymentMethodHandler> {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        return rememberUpdatedState(
            remember(coroutineScope, requester, context) {
                PreparePaymentMethodHandler { paymentMethod, address ->
                    onConfirmingChanged(false)

                    coroutineScope.launch {
                        requester.spt(paymentMethod).onSuccess {
                            it?.let {
                                paymentLauncher.handleNextActionForHashedPaymentIntent(it)
                            } ?: run {
                                onConfirmingChanged(false)

                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }.onFailure {
                            onConfirmingChanged(false)

                            Toast.makeText(
                                applicationContext,
                                "Failed to create SPT: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            }
        )
    }

    private enum class ScreenContent {
        Loading {
            @Composable
            override fun Content(
                parameters: Parameters
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(20.dp)
                            .width(64.dp)
                            .align(Alignment.TopCenter),
                    )
                }
            }
        },
        Complete {
            @OptIn(WalletButtonsPreview::class)
            @Composable
            override fun Content(
                parameters: Parameters
            ) {
                val flowController = parameters.flowController

                val horizontalModifier = Modifier.padding(horizontal = 12.dp)

                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (parameters.showWalletButtons) {
                        Box(modifier = horizontalModifier) {
                            flowController.WalletButtons()
                        }
                    }

                    PrimaryButton(
                        label = "Pay another way",
                        enabled = !parameters.confirming,
                        modifier = horizontalModifier
                    ) {
                        flowController.presentPaymentOptions()
                    }

                    parameters.paymentOption?.let {
                        Divider()

                        Summary(
                            option = it,
                            modifier = horizontalModifier
                        )

                        Divider()

                        PrimaryButton(
                            label = "Confirm",
                            enabled = !parameters.confirming,
                            modifier = horizontalModifier
                        ) {
                            parameters.confirm()
                        }
                    }
                }
            }

            @Composable
            private fun PrimaryButton(
                label: String,
                enabled: Boolean,
                modifier: Modifier = Modifier,
                onClick: () -> Unit
            ) {
                val context = LocalContext.current

                val primaryButtonStyle = StripeTheme.primaryButtonStyle

                TextButton(
                    onClick = onClick,
                    modifier = modifier
                        .fillMaxWidth()
                        .defaultMinSize(
                            minHeight = primaryButtonStyle.shape.height.dp
                        ),
                    enabled = enabled,
                    shape = RoundedCornerShape(primaryButtonStyle.shape.cornerRadius.dp),
                    border = BorderStroke(
                        primaryButtonStyle.shape.borderStrokeWidth.dp,
                        Color(primaryButtonStyle.getBorderStrokeColor(context))
                    ),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(primaryButtonStyle.getBackgroundColor(context)),
                        disabledBackgroundColor = Color(primaryButtonStyle.getBackgroundColor(context)),
                    ),
                ) {
                    Text(
                        text = label,
                        color = Color(primaryButtonStyle.getOnBackgroundColor(context))
                            .copy(alpha = LocalContentColor.current.alpha),
                        style = TextStyle(
                            fontFamily = primaryButtonStyle.typography.fontFamily?.let {
                                FontFamily(Font(it))
                            } ?: FontFamily.Default,
                            fontSize = primaryButtonStyle.typography.fontSize,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }

            @OptIn(ExtendedLabelsInPaymentOptionPreview::class)
            @Composable
            private fun Summary(
                option: PaymentOption,
                modifier: Modifier = Modifier,
            ) {
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        option.iconPainter,
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = option.labels.label,
                            style = MaterialTheme.typography.h5,
                            color = MaterialTheme.colors.onBackground,
                        )

                        option.labels.sublabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onBackground,
                            )
                        }
                    }
                }
            }
        },
        Failed {
            @Composable
            override fun Content(
                parameters: Parameters,
            ) {
                Button(onClick = parameters.retry, Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
            }
        };

        @Composable
        abstract fun Content(
            parameters: Parameters,
        )

        @Immutable
        class Parameters(
            val confirming: Boolean,
            val paymentOption: PaymentOption?,
            val flowController: PaymentSheet.FlowController,
            val showWalletButtons: Boolean,
            val confirm: () -> Unit,
            val retry: () -> Unit,
        )
    }

    internal companion object {
        private const val PLAYGROUND_STATE_KEY = "PLAYGROUND_STATE"

        fun create(
            context: Context,
            playgroundState: PlaygroundState.SharedPaymentToken,
        ): Intent {
            return Intent(context, SharedPaymentTokenPlaygroundActivity::class.java).apply {
                putExtra(PLAYGROUND_STATE_KEY, playgroundState)
            }
        }
    }
}
