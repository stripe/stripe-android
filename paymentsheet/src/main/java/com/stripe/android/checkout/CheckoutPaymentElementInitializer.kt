package com.stripe.android.checkout

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import javax.inject.Inject

/**
 * Wires the activity-scoped [EmbeddedSheetLauncher] into the singleton [EmbeddedContentHelper] when a
 * presenter is created, and clears it when the hosting activity is destroyed. Mirrors
 * [com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementInitializer].
 */
@EmbeddedPaymentElementScope
internal class CheckoutPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
    private val lifecycleOwner: LifecycleOwner,
) {
    fun initialize() {
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
