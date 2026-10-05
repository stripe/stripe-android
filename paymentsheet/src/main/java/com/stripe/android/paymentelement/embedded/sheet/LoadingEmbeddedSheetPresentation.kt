package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.stripe.android.common.ui.BottomSheetLoadingIndicator
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode

internal class LoadingEmbeddedSheetPresentation(
    private val activity: EmbeddedSheetActivity,
    private val args: EmbeddedActivityArgs,
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
            customerState = args.customerState,
            linkAccountInfo = args.linkAccountInfo,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
    }

    object Factory : EmbeddedSheetPresentation.Factory {
        override fun create(
            activity: EmbeddedSheetActivity,
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): LoadingEmbeddedSheetPresentation {
            return LoadingEmbeddedSheetPresentation(
                activity = activity,
                args = args,
            )
        }
    }
}

internal const val EMBEDDED_SHEET_LOADING_TEST_TAG = "embedded_sheet_loading"
