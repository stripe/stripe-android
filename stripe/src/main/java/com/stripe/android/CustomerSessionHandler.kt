package com.stripe.android

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Pair
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.Source

internal class CustomerSessionHandler(
    private val listener: Listener
) : Handler(Looper.getMainLooper()) {

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        val messageData = msg.obj as Pair<String, Any>
        val operationId = messageData.first
        val obj = messageData.second

        when (msg.what) {
            CustomerSessionRunnableFactory.MessageCode.CUSTOMER_RETRIEVED -> {
                listener.onCustomerRetrieved(obj as Customer, operationId)
            }
            CustomerSessionRunnableFactory.MessageCode.SOURCE_RETRIEVED -> {
                listener.onSourceRetrieved(obj as Source, operationId)
            }
            CustomerSessionRunnableFactory.MessageCode.PAYMENT_METHOD_RETRIEVED -> {
                listener.onPaymentMethodRetrieved(obj as PaymentMethod, operationId)
            }
            CustomerSessionRunnableFactory.MessageCode.CUSTOMER_SHIPPING_INFO_SAVED -> {
                listener.onCustomerShippingInfoSaved(obj as Customer)
            }
            CustomerSessionRunnableFactory.MessageCode.PAYMENT_METHODS_RETRIEVED -> {
                listener.onPaymentMethodsRetrieved(obj as List<PaymentMethod>, operationId)
            }
            CustomerSessionRunnableFactory.MessageCode.ERROR -> {
                if (obj is StripeException) {
                    listener.onError(obj, operationId)
                }
            }
            else -> {
            }
        }
    }

    internal interface Listener {
        fun onCustomerRetrieved(customer: Customer?, operationId: String)

        fun onSourceRetrieved(source: Source?, operationId: String)

        fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod?, operationId: String)

        fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>, operationId: String)

        fun onCustomerShippingInfoSaved(customer: Customer?)

        fun onError(exception: StripeException, operationId: String)
    }
}
