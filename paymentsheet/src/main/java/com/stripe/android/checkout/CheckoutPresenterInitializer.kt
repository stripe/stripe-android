package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import javax.inject.Inject

internal class CheckoutPresenterInitializer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val sheetStateHolder: SheetStateHolder,
) {
    fun initialize() {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)

        sheetStateHolder.sheetLauncher = sheetLauncher

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    sheetStateHolder.sheetLauncher = null
                }
            }
        )
    }
}
