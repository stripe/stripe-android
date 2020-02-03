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

        when (CustomerSessionRunnableFactory.ResultType.values()[msg.what]) {
            CustomerSessionRunnableFactory.ResultType.CustomerRetrieved -> {
                listener.onCustomerRetrieved(obj as Customer, operationId)
            }
            CustomerSessionRunnableFactory.ResultType.SourceRetrieved -> {
                listener.onSourceRetrieved(obj as Source, operationId)
            }
            CustomerSessionRunnableFactory.ResultType.PaymentMethod -> {
                listener.onPaymentMethodRetrieved(obj as PaymentMethod, operationId)
            }
            CustomerSessionRunnableFactory.ResultType.ShippingInfo -> {
                listener.onCustomerShippingInfoSaved(obj as Customer, operationId)
            }
            CustomerSessionRunnableFactory.ResultType.PaymentMethods -> {
                listener.onPaymentMethodsRetrieved(obj as List<PaymentMethod>, operationId)
            }
            CustomerSessionRunnableFactory.ResultType.Error -> {
                if (obj is StripeException) {
                    listener.onError(obj, operationId)
                }
            }
        }
    }

    internal interface Listener {
        fun onCustomerRetrieved(customer: Customer?, operationId: String)

        fun onSourceRetrieved(source: Source?, operationId: String)

        fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod?, operationId: String)

        fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>, operationId: String)

        fun onCustomerShippingInfoSaved(customer: Customer?, operationId: String)

        fun onError(exception: StripeException, operationId: String)
    }
}
