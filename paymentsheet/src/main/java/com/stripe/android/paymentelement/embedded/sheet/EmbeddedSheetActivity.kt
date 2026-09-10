package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.utils.renderEdgeToEdge
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
internal class EmbeddedSheetActivity : AppCompatActivity() {
    private var coordinator: EmbeddedSheetActivityCoordinator? = null
    private var currentArgs: EmbeddedActivityArgs? = null

    private val sheetActivityViewModel: SheetActivityViewModel by viewModels {
        SheetActivityViewModel.Factory { requireNotNull(currentArgs) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activityArgs = savedInstanceState?.let {
            BundleCompat.getParcelable(it, STATE_ACTIVITY_ARGS, EmbeddedActivityArgs::class.java)
        } ?: EmbeddedActivityArgs.fromIntent(intent) ?: run {
            finish()
            return
        }
        currentArgs = activityArgs

        val paymentSheetArgs =
            (activityArgs.activityConfiguration as? EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet)?.args
        if (paymentSheetArgs != null && !validate(paymentSheetArgs)) {
            return
        }

        createEmbeddedSheet(activityArgs)

        if (activityArgs.shouldLoadPaymentSheet()) {
            lifecycleScope.launch {
                sheetActivityViewModel.state.collect { state ->
                    when (state) {
                        SheetActivityViewModel.State.Loading -> Unit
                        is SheetActivityViewModel.State.Ready -> {
                            currentArgs = state.args
                            coordinator?.handleLoadedArgs(state.args)
                        }
                        is SheetActivityViewModel.State.Failed -> {
                            setPaymentSheetResult(PaymentSheetResult.Failed(state.error))
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun validate(args: PaymentSheetContract.Args): Boolean {
        return try {
            args.initializationMode.validate()
            args.config.asCommonConfiguration().validate(
                initializationMode = args.initializationMode,
                isLiveMode = PaymentConfiguration.getInstance(this).isLiveMode(),
                callbackIdentifier = args.paymentElementCallbackIdentifier,
            )
            true
        } catch (e: IllegalArgumentException) {
            setPaymentSheetResult(PaymentSheetResult.Failed(e))
            finish()
            false
        }
    }

    private fun createEmbeddedSheet(args: EmbeddedActivityArgs) {
        if (coordinator != null) return

        currentArgs = args
        renderEdgeToEdge()
        val coordinator = EmbeddedSheetActivityCoordinator(
            activity = this,
            initialArgs = args,
            presentationFactory = EmbeddedSheetPresentation,
        )
        this.coordinator = coordinator
        coordinator.register()
        setContent {
            PaymentElementTheme(appearance = args.configuration.appearance) {
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
        (coordinator?.currentArgs ?: currentArgs ?: EmbeddedActivityArgs.fromIntent(intent))?.let {
            outState.putParcelable(STATE_ACTIVITY_ARGS, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        coordinator?.onDestroy()
    }

    private fun setPaymentSheetResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(PaymentSheetContract.Result(result).toBundle()),
        )
    }

    private fun EmbeddedActivityArgs.shouldLoadPaymentSheet(): Boolean {
        return launchMode is EmbeddedLaunchMode.Complete &&
            presentationState == EmbeddedActivityArgs.PresentationState.Loading &&
            activityConfiguration is EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet
    }

    private companion object {
        const val STATE_ACTIVITY_ARGS = "embedded_sheet_activity_args"
    }
}
