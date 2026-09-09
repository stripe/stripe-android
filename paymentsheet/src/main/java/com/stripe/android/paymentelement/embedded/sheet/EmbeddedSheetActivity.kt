package com.stripe.android.paymentelement.embedded.sheet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut

@OptIn(ExperimentalMaterialApi::class)
internal class EmbeddedSheetActivity : AppCompatActivity() {
    private val args: EmbeddedActivityArgs? by lazy {
        EmbeddedActivityArgs.fromIntent(intent)
    }

    private val presentationDelegate = lazy {
        val args = requireNotNull(args)
        EmbeddedSheetViewModel.Factory { args }.createReadyPresentation(
            activity = this,
            args = args,
            activityResultCaller = this,
        )
    }
    private val presentation by presentationDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityArgs = args ?: run {
            finish()
            return
        }

        renderEdgeToEdge()
        presentation.register()
        setContent {
            PaymentElementTheme(appearance = activityArgs.configuration.appearance) {
                val bottomSheetState = rememberStripeBottomSheetState(
                    confirmValueChange = { presentation.canDismiss() },
                )
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = presentation::onDismissed,
                ) {
                    presentation.Content()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (presentationDelegate.isInitialized()) {
            presentation.onDestroy()
        }
    }
}
