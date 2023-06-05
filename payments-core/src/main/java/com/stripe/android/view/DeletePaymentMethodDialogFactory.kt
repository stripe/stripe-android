package com.stripe.android.view

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.stripe.android.CustomerSession
import com.stripe.android.R
import com.stripe.android.core.StripeError
import com.stripe.android.model.PaymentMethod

internal class DeletePaymentMethodDialogFactory internal constructor(
    private val context: Context,
    private val adapter: PaymentMethodsAdapter,
    private val cardDisplayTextFactory: CardDisplayTextFactory,
    private val customerSession: Result<CustomerSession>,
    private val productUsage: Set<String>,
    private val onDeletedPaymentMethodCallback: (PaymentMethod) -> Unit
) {
    @JvmSynthetic
    fun create(paymentMethod: PaymentMethod): AlertDialog {
        val message = paymentMethod.card?.let {
            cardDisplayTextFactory.createUnstyled(it)
        }
        return AlertDialog.Builder(context, R.style.StripeAlertDialogStyle)
            .setTitle(R.string.stripe_delete_payment_method_prompt_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onDeletedPaymentMethod(paymentMethod)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                adapter.resetPaymentMethod(paymentMethod)
            }
            .setOnCancelListener {
                adapter.resetPaymentMethod(paymentMethod)
            }
            .create()
    }

    @JvmSynthetic
    internal fun onDeletedPaymentMethod(paymentMethod: PaymentMethod) {
        adapter.deletePaymentMethod(paymentMethod)

        paymentMethod.id?.let { paymentMethodId ->
            customerSession.getOrNull()?.detachPaymentMethod(
                paymentMethodId = paymentMethodId,
                productUsage = productUsage,
                listener = PaymentMethodDeleteListener()
            )
        }

        onDeletedPaymentMethodCallback(paymentMethod)
    }

    private class PaymentMethodDeleteListener : CustomerSession.PaymentMethodRetrievalListener {
        override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
        }
    }
}
