package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.analytics.EventReporter
import javax.inject.Inject

private const val PREVIOUSLY_SENT_DEEP_LINK_EVENT = "previously_sent_deep_link_event"

@EmbeddedPaymentElementScope
internal class EmbeddedPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val sheetStateHolder: SheetStateHolder,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
    @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
) {
    private var previouslySentDeepLinkEvent: Boolean
        get() = savedStateHandle[PREVIOUSLY_SENT_DEEP_LINK_EVENT] ?: false
        set(value) {
            savedStateHandle[PREVIOUSLY_SENT_DEEP_LINK_EVENT] = value
        }

    fun initialize(applicationIsTaskOwner: Boolean) {
        if (!applicationIsTaskOwner && !previouslySentDeepLinkEvent) {
            eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()
            previouslySentDeepLinkEvent = true
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
