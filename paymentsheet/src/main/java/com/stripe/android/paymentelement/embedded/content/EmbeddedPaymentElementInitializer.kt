package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.callbacks.PAYMENT_ELEMENT_CALLBACK_INSTANCE_ID
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackStorage
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import javax.inject.Inject
import javax.inject.Named

@EmbeddedPaymentElementScope
internal class EmbeddedPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
    private val lifecycleOwner: LifecycleOwner,
    @Named(PAYMENT_ELEMENT_CALLBACK_INSTANCE_ID) private val instanceId: String,
) {
    fun initialize() {
        contentHelper.setSheetLauncher(sheetLauncher)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    PaymentElementCallbackStorage.remove(instanceId)
                    contentHelper.clearSheetLauncher()
                }
            }
        )
    }
}
