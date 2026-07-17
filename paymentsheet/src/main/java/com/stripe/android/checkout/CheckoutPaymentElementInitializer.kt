package com.stripe.android.checkout

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.injection.CheckoutPresenterScope
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetAnalyticsListener.Companion.PREVIOUSLY_SENT_DEEP_LINK_EVENT
import javax.inject.Inject

/**
 * Binds the activity-scoped [EmbeddedSheetLauncher] onto the (singleton) [EmbeddedContentHelper]
 * facade so checkout's content interactors and `presentPaymentOptions()` can launch sheets, and
 * clears it (plus the callback references) when the presenter's activity is destroyed. Mirrors
 * [com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementInitializer].
 */
@CheckoutPresenterScope
internal class CheckoutPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
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
