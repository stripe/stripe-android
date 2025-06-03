@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class, WalletsButtonPreview::class)

package com.stripe.android.paymentsheet.example.playground.wallets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.WalletsButtonPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import androidx.compose.runtime.collectAsState

internal class WalletsButtonPlaygroundActivity : AppCompatActivity() {
    companion object {
        private const val PLAYGROUND_STATE_KEY = "playgroundState"

        fun create(
            context: Context,
            playgroundState: PlaygroundState.Payment,
        ): Intent {
            return Intent(context, WalletsButtonPlaygroundActivity::class.java).apply {
                putExtra(PLAYGROUND_STATE_KEY, playgroundState)
            }
        }
    }

    val viewModel: PaymentSheetPlaygroundViewModel by viewModels {
        PaymentSheetPlaygroundViewModel.Factory(
            applicationSupplier = { application },
            uriSupplier = { intent.data },
        )
    }

    private lateinit var playgroundState: PlaygroundState.Payment
    private lateinit var playgroundSettings: PlaygroundSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialPlaygroundState = getPlaygroundState(savedInstanceState)
        if (initialPlaygroundState == null) {
            finish()
            return
        }
        this.playgroundState = initialPlaygroundState
        this.playgroundSettings = initialPlaygroundState.snapshot.playgroundSettings()

        setContent {
            val flowController = remember {
                PaymentSheet.FlowController.Builder(
                    viewModel::onPaymentSheetResult,
                    viewModel::onPaymentOptionSelected
                )
                    .createIntentCallback(viewModel::createIntentCallback)
            }.build()

            val playgroundSettings: PlaygroundSettings? by viewModel.playgroundSettingsFlow.collectAsState()
            val localPlaygroundSettings = playgroundSettings ?: return@setContent

            PlaygroundTheme(
                content = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        WalletsButtonContent(
                            playgroundState = playgroundState,
                            flowController = flowController,
                        )
                    }
                },
                bottomBarContent = { },
                topBarContent = {  },
            )

            val status by viewModel.status.collectAsState()
            val context = LocalContext.current
            LaunchedEffect(status) {
                val statusMessage = status?.toString() ?: ""
                if (statusMessage.isNotEmpty()) {
                    Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Composable
    private fun WalletsButtonContent(
        playgroundState: PlaygroundState.Payment,
        flowController: PaymentSheet.FlowController,
    ) {
        LaunchedEffect(playgroundState) {
            configureFlowController(
                flowController = flowController,
                playgroundState = playgroundState,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            flowController.WalletButtons()
        }
    }

    private fun configureFlowController(
        flowController: PaymentSheet.FlowController,
        playgroundState: PlaygroundState.Payment,
    ) {
        when {
            playgroundState.checkoutMode == CheckoutMode.SETUP -> {
                flowController.configureWithSetupIntent(
                    setupIntentClientSecret = playgroundState.clientSecret,
                    configuration = playgroundState.paymentSheetConfiguration(),
                    callback = viewModel::onFlowControllerConfigured,
                )
            }
            else -> {
                flowController.configureWithPaymentIntent(
                    paymentIntentClientSecret = playgroundState.clientSecret,
                    configuration = playgroundState.paymentSheetConfiguration(),
                    callback = viewModel::onFlowControllerConfigured,
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getPlaygroundState(savedInstanceState: Bundle?): PlaygroundState.Payment? {
        return savedInstanceState?.getParcelable<PlaygroundState.Payment?>(PLAYGROUND_STATE_KEY)
            ?: intent.getParcelableExtra<PlaygroundState.Payment?>(PLAYGROUND_STATE_KEY)
    }
}