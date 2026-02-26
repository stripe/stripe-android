@file:OptIn(ExperimentalMaterial3Api::class)

package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
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
            ctaText = args.ctaText,
            stripeAccountId = args.stripeAccountId,
            qrCodeUrl = args.qrCodeUrl,
        )
    }

    private val viewModel by viewModels<PollingViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if required args are present, finish gracefully if not
        if (!hasRequiredArgs()) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            StripeTheme {
                val uiState by viewModel.uiState.collectAsState()

                val state = rememberStripeBottomSheetState(
                    confirmValueChange = { proposedValue ->
                        if (proposedValue == SheetValue.Hidden) {
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

                ElementsBottomSheetLayout(
                    state = state,
                    onDismissed = { /* Not applicable here */ },
                ) {
                    val qrCodeUrl = args.qrCodeUrl
                    if (uiState.shouldShowQrCode && qrCodeUrl != null) {
                        QrCodeWebView(
                            url = qrCodeUrl,
                            clientSecret = args.clientSecret,
                            onClose = viewModel::hideQrCode,
                        )
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

    private fun hasRequiredArgs(): Boolean {
        return PollingContract.Args.fromIntent(intent) != null
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }
}
