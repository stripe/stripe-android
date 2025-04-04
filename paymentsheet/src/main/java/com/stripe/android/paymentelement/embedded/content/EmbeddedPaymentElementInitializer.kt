package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetAnalyticsListener.Companion.PREVIOUSLY_SENT_DEEP_LINK_EVENT
import javax.inject.Inject

@EmbeddedPaymentElementScope
internal class EmbeddedPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateHandle: MainThreadSavedStateHandle,
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

        contentHelper.setSheetLauncher(sheetLauncher)

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    PaymentElementCallbackReferences.remove(paymentElementCallbackIdentifier)
                    contentHelper.clearSheetLauncher()
                }
            }
        )
    }
}
