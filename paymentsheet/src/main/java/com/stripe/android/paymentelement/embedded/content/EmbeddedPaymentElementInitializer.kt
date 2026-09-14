package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.previouslySentDeepLinkEvent
import javax.inject.Inject

@EmbeddedPaymentElementScope
internal class EmbeddedPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val sheetStateHolder: SheetStateHolder,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
) {
    fun initialize(applicationIsTaskOwner: Boolean) {
        if (!applicationIsTaskOwner && !savedStateHandle.previouslySentDeepLinkEvent) {
            eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()
            savedStateHandle.previouslySentDeepLinkEvent = true
        }

        sheetStateHolder.sheetLauncher = sheetLauncher

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    PaymentElementCallbackReferences.remove(paymentElementCallbackIdentifier)
                    sheetStateHolder.sheetLauncher = null
                }
            }
        )
    }
}
