package com.stripe.android.checkout

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

/**
 * Shared by the states and display data from one configure call, and parceled with the controller state.
 * Old display data retains its own instance after reconfiguration. Acknowledgement is only read at
 * confirmation, so recording it does not need to emit a new session.
 * Identity equality ensures a fresh configuration replaces the previous state in StateFlow even
 * when both configurations have the same acknowledgement values.
 */
@Parcelize
internal class CheckoutMandateState private constructor(
    private var hasAccessedContent: Boolean,
    private var lastAccessedPaymentMethodId: String?,
    private var lastAcknowledgedPaymentMethodId: String?,
) : Parcelable {
    fun recordContentAccess() {
        hasAccessedContent = true
    }

    fun recordMandateTextAccess(paymentMethod: PaymentMethod) {
        if (paymentMethod.type == PaymentMethod.Type.SepaDebit) {
            lastAccessedPaymentMethodId = paymentMethod.id
        }
    }

    fun acknowledgeSepaMandate(paymentMethod: PaymentMethod) {
        if (paymentMethod.type == PaymentMethod.Type.SepaDebit) {
            lastAcknowledgedPaymentMethodId = paymentMethod.id
        }
    }

    fun hasAcknowledgedSepaMandate(selection: PaymentSelection): Boolean {
        if (hasAccessedContent) return true
        val paymentMethod = (selection as? PaymentSelection.Saved)?.paymentMethod ?: return false
        return paymentMethod.type == PaymentMethod.Type.SepaDebit &&
            (paymentMethod.id == lastAccessedPaymentMethodId || paymentMethod.id == lastAcknowledgedPaymentMethodId)
    }

    companion object {
        fun initial() = CheckoutMandateState(
            hasAccessedContent = false,
            lastAccessedPaymentMethodId = null,
            lastAcknowledgedPaymentMethodId = null,
        )
    }
}
