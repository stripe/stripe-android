package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetAnalyticsListener.Companion.PREVIOUSLY_SENT_DEEP_LINK_EVENT
import javax.inject.Inject

@EmbeddedPaymentElementScope
internal class EmbeddedPaymentElementInitializer @Inject constructor(
    private val sheetLauncher: EmbeddedSheetLauncher,
    private val contentHelper: EmbeddedContentHelper,
    private val lifecycleOwner: LifecycleOwner,
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
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
                    IntentConfirmationInterceptor.createIntentCallback = null
                    ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null
                    contentHelper.clearSheetLauncher()
                }
            }
        )
    }
}
