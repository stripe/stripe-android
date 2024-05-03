package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.BottomSheet
import com.stripe.android.common.ui.rememberBottomSheetState
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.utils.fadeOut
import kotlin.time.Duration.Companion.seconds

internal class PollingActivity : AppCompatActivity() {

    private val args: PollingContract.Args by lazy {
        requireNotNull(PollingContract.Args.fromIntent(intent))
    }

    internal var viewModelFactory: ViewModelProvider.Factory = PollingViewModel.Factory {
        PollingViewModel.Args(
            clientSecret = args.clientSecret,
            timeLimit = args.timeLimitInSeconds.seconds,
            initialDelay = args.initialDelayInSeconds.seconds,
            maxAttempts = args.maxAttempts,
            ctaText = args.ctaText,
        )
    }

    private val viewModel by viewModels<PollingViewModel> { viewModelFactory }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            StripeTheme {
                val uiState by viewModel.uiState.collectAsState()

                val state = rememberBottomSheetState(
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
                        viewModel.handleCancel()
                    } else {
                        // Ignore back presses while polling for the result
                    }
                }

                LaunchedEffect(uiState.pollingState) {
                    val result = uiState.pollingState.toFlowResult(args)
                    if (result != null) {
                        state.hide()
                        finishWithResult(result)
                    }
                }

                BottomSheet(
                    state = state,
                    onDismissed = { /* Not applicable here */ },
                ) {
                    PollingScreen(viewModel)
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
