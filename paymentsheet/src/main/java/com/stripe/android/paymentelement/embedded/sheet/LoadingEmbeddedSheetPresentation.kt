package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedActivityState
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode

internal class LoadingEmbeddedSheetPresentation(
    private val activity: EmbeddedSheetActivity,
    private val state: EmbeddedActivityState.LoadingPaymentOptions,
) : EmbeddedSheetPresentation {
    private var backCallback: OnBackPressedCallback? = null

    override fun register() {
        backCallback = activity.onBackPressedDispatcher.addCallback {
            activity.finishWithResult(createCancellationResult())
        }
    }

    override fun canDismiss(): Boolean {
        return true
    }

    override fun onDismissed() {
        activity.finishWithResult(createCancellationResult())
    }

    @Composable
    override fun Content() {
        BottomSheetLoadingIndicator(
            modifier = Modifier.testTag(EMBEDDED_SHEET_LOADING_TEST_TAG),
        )
    }

    override fun onDestroy() {
        backCallback?.remove()
    }

    private fun createCancellationResult(): EmbeddedActivityResult {
        return EmbeddedActivityResult.Cancelled(
            customerState = state.customerState,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
    }

    object Factory {
        fun create(
            activity: EmbeddedSheetActivity,
            state: EmbeddedActivityState.LoadingPaymentOptions,
        ): LoadingEmbeddedSheetPresentation {
            return LoadingEmbeddedSheetPresentation(
                activity = activity,
                state = state,
            )
        }
    }
}

internal const val EMBEDDED_SHEET_LOADING_TEST_TAG = "embedded_sheet_loading"
