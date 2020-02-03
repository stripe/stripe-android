package com.stripe.android

import android.os.Handler
import android.util.Pair
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source

/**
 * Class that creates the [Runnable] task for the [CustomerSession] operation.
 */
internal class CustomerSessionRunnableFactory(
    private val stripeRepository: StripeRepository,
    private val handler: Handler,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val productUsage: CustomerSessionProductUsage
) {
    enum class ResultType {
        Error,
        CustomerRetrieved,
        SourceRetrieved,
        PaymentMethod, // single
        PaymentMethods, // multiple
        ShippingInfo
    }

    internal fun create(
        ephemeralKey: EphemeralKey,
        operationId: String,
        actionString: String?,
        arguments: Map<String, Any>?
    ): Runnable? {
        return if (actionString == null) {
            createUpdateCustomerRunnable(ephemeralKey, operationId)
        } else if (arguments == null) {
            return null
        } else if (CustomerSession.ACTION_ADD_SOURCE == actionString &&
            arguments.containsKey(CustomerSession.KEY_SOURCE) &&
            arguments.containsKey(CustomerSession.KEY_SOURCE_TYPE)) {
            createAddCustomerSourceRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_SOURCE] as String,
                arguments[CustomerSession.KEY_SOURCE_TYPE] as String,
                operationId
            )
        } else if (CustomerSession.ACTION_DELETE_SOURCE == actionString &&
            arguments.containsKey(CustomerSession.KEY_SOURCE)) {
            createDeleteCustomerSourceRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_SOURCE] as String,
                operationId
            )
        } else if (CustomerSession.ACTION_ATTACH_PAYMENT_METHOD ==
            actionString && arguments.containsKey(CustomerSession.KEY_PAYMENT_METHOD)) {
            createAttachPaymentMethodRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_PAYMENT_METHOD] as String,
                operationId
            )
        } else if (CustomerSession.ACTION_DETACH_PAYMENT_METHOD == actionString &&
            arguments.containsKey(CustomerSession.KEY_PAYMENT_METHOD)) {
            createDetachPaymentMethodRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_PAYMENT_METHOD] as String,
                operationId
            )
        } else if (CustomerSession.ACTION_GET_PAYMENT_METHODS == actionString) {
            createGetPaymentMethodsRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_PAYMENT_METHOD_TYPE] as String,
                operationId
            )
        } else if (CustomerSession.ACTION_SET_DEFAULT_SOURCE == actionString &&
            arguments.containsKey(CustomerSession.KEY_SOURCE) &&
            arguments.containsKey(CustomerSession.KEY_SOURCE_TYPE)) {
            createSetCustomerSourceDefaultRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_SOURCE] as String,
                arguments[CustomerSession.KEY_SOURCE_TYPE] as String,
                operationId
            )
        } else if (CustomerSession.ACTION_SET_CUSTOMER_SHIPPING_INFO == actionString &&
            arguments.containsKey(CustomerSession.KEY_SHIPPING_INFO)) {
            createSetCustomerShippingInformationRunnable(
                ephemeralKey,
                arguments[CustomerSession.KEY_SHIPPING_INFO] as ShippingInformation,
                operationId
            )
        } else {
            // unsupported operation
            null
        }
    }

    private fun createAddCustomerSourceRunnable(
        key: EphemeralKey,
        sourceId: String,
        sourceType: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Source>(
            handler,
            ResultType.SourceRetrieved,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Source? {
                return stripeRepository.addCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    sourceId,
                    sourceType,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createDeleteCustomerSourceRunnable(
        key: EphemeralKey,
        sourceId: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Source>(
            handler,
            ResultType.SourceRetrieved,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Source? {
                return stripeRepository.deleteCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    sourceId,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createAttachPaymentMethodRunnable(
        key: EphemeralKey,
        paymentMethodId: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<PaymentMethod>(
            handler,
            ResultType.PaymentMethod,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): PaymentMethod? {
                return stripeRepository.attachPaymentMethod(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    paymentMethodId,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createDetachPaymentMethodRunnable(
        key: EphemeralKey,
        paymentMethodId: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<PaymentMethod>(
            handler,
            ResultType.PaymentMethod,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): PaymentMethod? {
                return stripeRepository.detachPaymentMethod(
                    publishableKey,
                    productUsage.get(),
                    paymentMethodId,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createGetPaymentMethodsRunnable(
        key: EphemeralKey,
        paymentMethodType: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<List<PaymentMethod>>(
            handler,
            ResultType.PaymentMethods,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): List<PaymentMethod> {
                return stripeRepository.getPaymentMethods(
                    key.objectId,
                    paymentMethodType,
                    publishableKey,
                    productUsage.get(),
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createSetCustomerSourceDefaultRunnable(
        key: EphemeralKey,
        sourceId: String,
        sourceType: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(
            handler,
            ResultType.CustomerRetrieved,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return stripeRepository.setDefaultCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    sourceId,
                    sourceType,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createSetCustomerShippingInformationRunnable(
        key: EphemeralKey,
        shippingInformation: ShippingInformation,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(
            handler,
            ResultType.ShippingInfo,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return stripeRepository.setCustomerShippingInfo(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    shippingInformation,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createUpdateCustomerRunnable(
        key: EphemeralKey,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(
            handler,
            ResultType.CustomerRetrieved,
            operationId
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return retrieveCustomerWithKey(key)
            }
        }
    }

    /**
     * Fetch a [Customer]. If the provided key is expired, this method **does not** update the key.
     * Use [createUpdateCustomerRunnable] to validate the key before refreshing the customer.
     *
     * @param key the [EphemeralKey] used for this access
     * @return a [Customer] if one can be found with this key, or `null` if one cannot.
     */
    @Throws(StripeException::class)
    private fun retrieveCustomerWithKey(key: EphemeralKey): Customer? {
        return stripeRepository.retrieveCustomer(
            key.objectId,
            ApiRequest.Options(key.secret, stripeAccountId)
        )
    }

    private abstract class CustomerSessionRunnable<T>(
        private val handler: Handler,
        private val resultType: ResultType,
        private val operationId: String
    ) : Runnable {

        /**
         * An object, [T], that will populate Message.obj
         */
        @Throws(StripeException::class)
        internal abstract fun createMessageObject(): T?

        override fun run() {
            try {
                sendMessage(createMessageObject())
            } catch (stripeEx: StripeException) {
                sendErrorMessage(stripeEx)
            }
        }

        private fun sendMessage(messageObject: T?) {
            handler.sendMessage(
                handler.obtainMessage(
                    resultType.ordinal,
                    Pair.create<String, T>(operationId, messageObject)
                )
            )
        }

        private fun sendErrorMessage(stripeEx: StripeException) {
            handler.sendMessage(
                handler.obtainMessage(
                    ResultType.Error.ordinal,
                    Pair.create(operationId, stripeEx)
                )
            )
        }
    }
}
