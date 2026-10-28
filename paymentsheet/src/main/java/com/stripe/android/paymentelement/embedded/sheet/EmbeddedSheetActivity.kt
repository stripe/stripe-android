package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.BundleCompat
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut

@OptIn(ExperimentalMaterialApi::class)
internal class EmbeddedSheetActivity : AppCompatActivity() {
    private var coordinator: EmbeddedSheetActivityCoordinator? = null
    private var pendingResult by mutableStateOf<EmbeddedActivityResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityArgs = savedInstanceState?.let {
            BundleCompat.getParcelable(it, STATE_ACTIVITY_ARGS, EmbeddedActivityArgs::class.java)
        } ?: EmbeddedActivityArgs.fromIntent(intent) ?: run {
            finish()
            return
        }

        pendingResult = savedInstanceState?.let {
            BundleCompat.getParcelable(it, STATE_PENDING_RESULT, EmbeddedActivityResult::class.java)
        }
        pendingResult?.let(::setActivityResult)

        renderEdgeToEdge()
        val coordinator = EmbeddedSheetActivityCoordinator(
            activity = this,
            initialArgs = activityArgs,
            presentationFactory = EmbeddedSheetPresentation,
        )
        this.coordinator = coordinator
        coordinator.register()
        setContent {
            PaymentElementTheme(appearance = activityArgs.configuration.appearance) {
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { pendingResult != null || coordinator.canDismiss() },
                )
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = coordinator::onDismissed,
                ) {
                    coordinator.Content()
                }
                LaunchedEffect(pendingResult) {
                    if (pendingResult != null) {
                        bottomSheetState.hide()
                        finish()
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
    }

    internal fun handleNewIntent(intent: Intent) {
        coordinator?.handleNewIntent(intent)
    }

    internal fun finishWithResult(result: EmbeddedActivityResult) {
        if (pendingResult != null || isFinishing) {
            return
        }
        setActivityResult(result)
        if (result.launchMode == EmbeddedLaunchMode.PaymentOptions) {
            pendingResult = result
        } else {
            finish()
        }
    }

    private fun setActivityResult(result: EmbeddedActivityResult) {
        setResult(Activity.RESULT_OK, EmbeddedActivityResult.toIntent(intent, result))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        EmbeddedActivityArgs.fromIntent(intent)?.let {
            outState.putParcelable(STATE_ACTIVITY_ARGS, it)
        }
        outState.putParcelable(STATE_PENDING_RESULT, pendingResult)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinator?.onDestroy()
    }

    private companion object {
        const val STATE_ACTIVITY_ARGS = "embedded_sheet_activity_args"
        const val STATE_PENDING_RESULT = "embedded_sheet_pending_result"
    }
}
