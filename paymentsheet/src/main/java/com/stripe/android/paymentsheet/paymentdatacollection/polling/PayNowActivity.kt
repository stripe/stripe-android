package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.seconds

internal class PayNowActivity : AppCompatActivity() {

    private val args: PayNowContract.Args by lazy {
        requireNotNull(PayNowContract.Args.fromIntent(intent))
    }

    private val _shouldShowWebView = MutableStateFlow(true)

    internal var viewModelFactory: ViewModelProvider.Factory = PollingViewModel.Factory {
        PollingViewModel.Args(
            clientSecret = args.clientSecret,
            timeLimit = args.timeLimitInSeconds.seconds,
            initialDelay = args.initialDelayInSeconds.seconds,
            maxAttempts = args.maxAttempts,
            ctaText = args.ctaText,
            stripeAccountId = args.stripeAccountId,
        )
    }

    private val viewModel by viewModels<PollingViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StripeTheme {
                val uiState by viewModel.uiState.collectAsState()
                // TODO: move this into uiState?
                val shouldShowWebView by _shouldShowWebView.collectAsState()

                val state = rememberStripeBottomSheetState(
                    confirmValueChange = { proposedValue ->
                        if (proposedValue == ModalBottomSheetValue.Hidden) {
                            uiState.pollingState != PollingState.Active
                        } else {
                            true
                        }
                    }
                )

                BackHandler(enabled = true) {
                    if (uiState.pollingState == PollingState.Failed) {
                        _shouldShowWebView.value = false
                        viewModel.handleCancel()
                    } else {
                        // Ignore back presses while polling for the result
                    }
                }

                LaunchedEffect(uiState.pollingState) {
                    val pollingState = uiState.pollingState
                    if (pollingState == PollingState.Failed) {
                        _shouldShowWebView.value = false
                    }
                    val result = uiState.pollingState.toFlowResult(
                        clientSecret = args.clientSecret,
                        stripeAccountId = args.stripeAccountId,
                    )
                    if (result != null) {
                        _shouldShowWebView.value = false
                        state.hide()
                        finishWithResult(result)
                    }
                }

                ElementsBottomSheetLayout(
                    state = state,
                    onDismissed = { /* Not applicable here */ },
                ) {
                    if (shouldShowWebView) {
                        PayNowWebView(args.qrCodeUrl) {
                            _shouldShowWebView.value = false
                        }
                    } else {
                        PollingScreen(viewModel)
                    }
                }
            }
        }
    }

    private fun finishWithResult(result: PaymentFlowResult.Unvalidated) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}