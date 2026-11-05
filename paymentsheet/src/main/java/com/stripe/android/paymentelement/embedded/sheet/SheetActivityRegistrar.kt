package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.isFlowController
import com.stripe.android.paymentsheet.addresselement.AutocompleteActivityLauncher
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal interface SheetActivityRegistrar {
    fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    )
}

@Singleton
internal class DefaultSheetActivityRegistrar @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val tapToAddHelper: TapToAddHelper,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val launchMode: EmbeddedLaunchMode,
    private val linkLauncher: Provider<SheetLinkPaymentMethodLauncher>,
    private val autocompleteLauncher: Provider<AutocompleteActivityLauncher>,
) : SheetActivityRegistrar {

    private var isBootstrapped = false

    override fun registerAndBootstrap(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
        tapToAddHelper.register(activityResultCaller, lifecycleOwner)
        if (launchMode.isFlowController) {
            linkLauncher.get().register(activityResultCaller, lifecycleOwner)
            autocompleteLauncher.get().register(activityResultCaller, lifecycleOwner)
            // FlowController bootstraps its own confirmation handler, and the sheet only selects for FlowController.
            return
        }
        if (!isBootstrapped) {
            confirmationHandler.bootstrap(paymentMethodMetadata)
            isBootstrapped = true
        }
    }
}
