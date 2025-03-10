package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.confirmation.cpms.CustomPaymentMethodProxyActivity
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import javax.inject.Inject

@EmbeddedPaymentElementScope
@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal class EmbeddedPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
    private val lifecycleOwner: LifecycleOwner,
) {
    fun initialize() {
        contentHelper.setSheetLauncher(sheetLauncher)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    IntentConfirmationInterceptor.createIntentCallback = null
                    ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null
                    CustomPaymentMethodProxyActivity.customPaymentMethodConfirmHandler = null
                    contentHelper.clearSheetLauncher()
                }
            }
        )
    }
}
