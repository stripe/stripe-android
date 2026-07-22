package com.stripe.android.paymentelement.embedded.content

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

internal interface EmbeddedContentHelperStateHolder {
    val state: StateFlow<State?>

    fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        appearance: Embedded,
        embeddedViewDisplaysMandateText: Boolean,
        configuration: EmbeddedPaymentElement.Configuration,
    )

    fun clearEmbeddedContent()

    @Parcelize
    class State(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val appearance: Embedded,
        val embeddedViewDisplaysMandateText: Boolean,
        val configuration: EmbeddedPaymentElement.Configuration,
    ) : Parcelable

    companion object {
        const val STATE_KEY_EMBEDDED_CONTENT = "STATE_KEY_EMBEDDED_CONTENT"
    }
}

@Singleton
internal class DefaultEmbeddedContentHelperStateHolder @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
) : EmbeddedContentHelperStateHolder {

    override val state: StateFlow<EmbeddedContentHelperStateHolder.State?> =
        savedStateHandle.getStateFlow(
            key = EmbeddedContentHelperStateHolder.STATE_KEY_EMBEDDED_CONTENT,
            initialValue = null,
        )

    override fun dataLoaded(
        paymentMethodMetadata: PaymentMethodMetadata,
        appearance: Embedded,
        embeddedViewDisplaysMandateText: Boolean,
        configuration: EmbeddedPaymentElement.Configuration,
    ) {
        eventReporter.onShowNewPaymentOptions()
        val state = EmbeddedContentHelperStateHolder.State(
            paymentMethodMetadata = paymentMethodMetadata,
            appearance = appearance,
            embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            configuration = configuration,
        )
        savedStateHandle[EmbeddedContentHelperStateHolder.STATE_KEY_EMBEDDED_CONTENT] = state
    }

    override fun clearEmbeddedContent() {
        savedStateHandle[EmbeddedContentHelperStateHolder.STATE_KEY_EMBEDDED_CONTENT] = null
    }
}
