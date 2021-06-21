package com.stripe.android.view

import com.stripe.android.model.PaymentMethod

internal class SwipeToDeleteCallbackListener internal constructor(
    private val deletePaymentMethodDialogFactory: DeletePaymentMethodDialogFactory
) : PaymentMethodSwipeCallback.Listener {

    override fun onSwiped(paymentMethod: PaymentMethod) {
        deletePaymentMethodDialogFactory
            .create(paymentMethod)
            .show()
    }
}
