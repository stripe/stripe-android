package com.stripe.android.view

import androidx.appcompat.app.AlertDialog
import com.stripe.android.CustomerSession
import com.stripe.android.R
import com.stripe.android.StripeError
import com.stripe.android.model.PaymentMethod

internal class DeletePaymentMethodDialogFactory internal constructor(
    private val activity: PaymentMethodsActivity,
    private val adapter: PaymentMethodsAdapter,
    private val cardDisplayTextFactory: CardDisplayTextFactory,
    private val customerSession: CustomerSession
) {
    @JvmSynthetic
    fun create(paymentMethod: PaymentMethod): AlertDialog {
        val message = paymentMethod.card?.let {
            cardDisplayTextFactory.createUnstyled(it)
        }
        return AlertDialog.Builder(activity, R.style.AlertDialogStyle)
            .setTitle(R.string.delete_payment_method_prompt_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                onDeletedPaymentMethod(paymentMethod)
            }
            .setNegativeButton(android.R.string.no) { _, _ ->
                adapter.resetPaymentMethod(paymentMethod)
            }
            .setOnCancelListener {
                adapter.resetPaymentMethod(paymentMethod)
            }
            .create()
    }

    private fun onDeletedPaymentMethod(paymentMethod: PaymentMethod) {
        adapter.deletePaymentMethod(paymentMethod)

        paymentMethod.id?.let { paymentMethodId ->
            customerSession.detachPaymentMethod(paymentMethodId, PaymentMethodDeleteListener())
        }

        activity.showSnackbar(paymentMethod, R.string.removed)
    }

    private class PaymentMethodDeleteListener : CustomerSession.PaymentMethodRetrievalListener {
        override fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod) {
        }

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
        }
    }
}
