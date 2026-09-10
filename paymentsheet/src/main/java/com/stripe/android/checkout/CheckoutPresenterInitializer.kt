package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.parseAppearance
import javax.inject.Inject

internal class CheckoutPresenterInitializer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val embeddedContentHelper: EmbeddedContentHelper,
    private val sheetStateHolder: SheetStateHolder,
    private val stateHolder: CheckoutControllerStateHolder,
) {
    fun initialize() {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)

        sheetStateHolder.sheetLauncher = sheetLauncher
        sheetStateHolder.embeddedContentHelper = embeddedContentHelper
        stateHolder.state?.embeddedConfiguration?.appearance?.parseAppearance()

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    sheetStateHolder.sheetLauncher = null
                    sheetStateHolder.embeddedContentHelper = null
                }
            }
        )
    }
}
