package com.stripe.android.paymentelement.embedded.sheet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.BundleCompat
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.toArgs
import com.stripe.android.paymentelement.embedded.toState
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut

@OptIn(ExperimentalMaterialApi::class)
internal class EmbeddedSheetActivity : AppCompatActivity() {
    private var coordinator: EmbeddedSheetActivityCoordinator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityArgs = savedInstanceState?.let {
            BundleCompat.getParcelable(it, STATE_ACTIVITY_ARGS, EmbeddedActivityArgs::class.java)
        } ?: EmbeddedActivityArgs.fromIntent(intent)
        val activityState = activityArgs?.toState()
        if (activityState == null) {
            finish()
            return
        }

        renderEdgeToEdge()
        val coordinator = EmbeddedSheetActivityCoordinator(
            activity = this,
            initialState = activityState,
            presentationFactory = EmbeddedSheetPresentation,
        )
        this.coordinator = coordinator
        coordinator.register()
        setContent {
            PaymentElementTheme(appearance = coordinator.currentState.appearance) {
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { coordinator.canDismiss() },
                )
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = coordinator::onDismissed,
                ) {
                    coordinator.Content()
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

    override fun onSaveInstanceState(outState: Bundle) {
        coordinator?.currentState?.let { outState.putParcelable(STATE_ACTIVITY_ARGS, it.toArgs()) }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinator?.onDestroy()
    }

    private companion object {
        const val STATE_ACTIVITY_ARGS = "embedded_sheet_activity_args"
    }
}
