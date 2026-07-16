package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import javax.inject.Inject

/**
 * Attaches the [EmbeddedSheetLauncher] to the (singleton) [EmbeddedContentHelper] so clicks in the
 * checkout payment element launch the form/manage/payment-options sheets, and detaches it when the
 * hosting activity is destroyed so a launcher bound to a dead activity is never used. Mirrors the
 * embedded flow's `EmbeddedPaymentElementInitializer`.
 */
internal class CheckoutPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val confirmationHandler: ConfirmationHandler,
) {
    fun initialize() {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)
        contentHelper.setSheetLauncher(sheetLauncher)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    contentHelper.clearSheetLauncher()
                }
            }
        )
    }
}
