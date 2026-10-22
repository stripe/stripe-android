package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.injection.CheckoutPresenterScope
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.parseAppearance
import dagger.Lazy
import javax.inject.Inject

@OptIn(CheckoutSessionPreview::class)
@CheckoutPresenterScope
internal class CheckoutPresenterInitializer @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val shippingAddressElementProvider: Lazy<ShippingAddressElement>,
    private val sheetStateHolder: SheetStateHolder,
    private val stateHolder: CheckoutControllerStateHolder,
) {
    fun initialize() {
        confirmationHandler.register(activityResultCaller, lifecycleOwner)

        sheetStateHolder.sheetLauncher = sheetLauncher
        if (stateHolder.state?.configuration?.shippingAddressElementConfiguration != null) {
            shippingAddressElementProvider.get()
        }
        stateHolder.state?.embeddedConfiguration?.appearance?.parseAppearance()

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    sheetStateHolder.sheetLauncher = null
                }
            }
        )
    }
}
