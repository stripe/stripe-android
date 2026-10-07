package com.stripe.android.checkout

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CheckoutMandateState @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) {
    fun recordContentAccess(mandateAcknowledgementId: String) {
        savedStateHandle[CONTENT_ACCESSED_KEY] = mandateAcknowledgementId
    }

    fun hasAccessedContent(mandateAcknowledgementId: String): Boolean {
        return savedStateHandle.get<String>(CONTENT_ACCESSED_KEY) == mandateAcknowledgementId
    }

    fun recordMandateTextAccess(mandateAcknowledgementId: String, paymentMethod: PaymentMethod) {
        val paymentMethodId = paymentMethod.id ?: return
        if (paymentMethod.type == PaymentMethod.Type.SepaDebit) {
            savedStateHandle[MANDATE_TEXT_ACCESSED_KEY] = MandateTextAccess(
                mandateAcknowledgementId = mandateAcknowledgementId,
                paymentMethodId = paymentMethodId,
            )
        }
    }

    fun hasAccessedMandateText(mandateAcknowledgementId: String, paymentMethod: PaymentMethod): Boolean {
        val paymentMethodId = paymentMethod.id ?: return false
        return paymentMethod.type == PaymentMethod.Type.SepaDebit &&
            savedStateHandle.get<MandateTextAccess>(MANDATE_TEXT_ACCESSED_KEY) == MandateTextAccess(
                mandateAcknowledgementId = mandateAcknowledgementId,
                paymentMethodId = paymentMethodId,
            )
    }

    @Parcelize
    internal data class MandateTextAccess(
        val mandateAcknowledgementId: String,
        val paymentMethodId: String,
    ) : Parcelable

    private companion object {
        const val CONTENT_ACCESSED_KEY = "CheckoutMandateState_CONTENT_ACKNOWLEDGEMENT_ID"
        const val MANDATE_TEXT_ACCESSED_KEY = "CheckoutMandateState_MANDATE_TEXT_ACCESS"
    }
}
