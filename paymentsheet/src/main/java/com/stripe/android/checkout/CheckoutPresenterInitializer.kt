package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentsheet.parseAppearance
import javax.inject.Inject

internal class CheckoutPresenterInitializer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val sheetStateHolder: SheetStateHolder,
    private val stateHolder: CheckoutControllerStateHolder,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
) {
    fun initialize() {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)

        sheetStateHolder.sheetLauncher = sheetLauncher
        stateHolder.state?.embeddedConfiguration?.appearance?.parseAppearance()

        (lifecycleOwner as? PresenterLifecycleOwner)?.addControllerDestroyListener {
            EmbeddedSheetActivity.dismiss(paymentElementCallbackIdentifier)
            sheetStateHolder.sheetIsOpen = false
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (sheetStateHolder.sheetLauncher === sheetLauncher) {
                        sheetStateHolder.sheetLauncher = null
                    }
                }
            }
        )
    }
}
