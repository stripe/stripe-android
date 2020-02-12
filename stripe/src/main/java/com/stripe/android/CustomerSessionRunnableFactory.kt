package com.stripe.android

import android.os.Handler
import android.util.Pair
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
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

    @JvmSynthetic
    internal fun create(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation
    ): Runnable? {
        return when (operation) {
            is EphemeralOperation.RetrieveKey -> {
                createUpdateCustomerRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.AddSource -> {
                createAddCustomerSourceRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.DeleteSource -> {
                createDeleteCustomerSourceRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.AttachPaymentMethod -> {
                createAttachPaymentMethodRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.DetachPaymentMethod -> {
                createDetachPaymentMethodRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.GetPaymentMethods -> {
                createGetPaymentMethodsRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.UpdateDefaultSource -> {
                createSetCustomerSourceDefaultRunnable(
                    ephemeralKey,
                    operation
                )
            }
            is EphemeralOperation.Customer.UpdateShipping -> {
                createSetCustomerShippingInformationRunnable(
                    ephemeralKey,
                    operation
                )
            }
            else -> null
        }
    }

    private fun createAddCustomerSourceRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.AddSource
    ): Runnable {
        return object : CustomerSessionRunnable<Source>(
            handler,
            ResultType.SourceRetrieved,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Source? {
                return stripeRepository.addCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    operation.sourceId,
                    operation.sourceType,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createDeleteCustomerSourceRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.DeleteSource
    ): Runnable {
        return object : CustomerSessionRunnable<Source>(
            handler,
            ResultType.SourceRetrieved,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Source? {
                return stripeRepository.deleteCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    operation.sourceId,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createAttachPaymentMethodRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.AttachPaymentMethod
    ): Runnable {
        return object : CustomerSessionRunnable<PaymentMethod>(
            handler,
            ResultType.PaymentMethod,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): PaymentMethod? {
                return stripeRepository.attachPaymentMethod(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    operation.paymentMethodId,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createDetachPaymentMethodRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.DetachPaymentMethod
    ): Runnable {
        return object : CustomerSessionRunnable<PaymentMethod>(
            handler,
            ResultType.PaymentMethod,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): PaymentMethod? {
                return stripeRepository.detachPaymentMethod(
                    publishableKey,
                    productUsage.get(),
                    operation.paymentMethodId,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createGetPaymentMethodsRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.GetPaymentMethods
    ): Runnable {
        return object : CustomerSessionRunnable<List<PaymentMethod>>(
            handler,
            ResultType.PaymentMethods,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): List<PaymentMethod> {
                return stripeRepository.getPaymentMethods(
                    ListPaymentMethodsParams(
                        customerId = key.objectId,
                        paymentMethodType = operation.type,
                        limit = operation.limit,
                        endingBefore = operation.endingBefore,
                        startingAfter = operation.startingAfter
                    ),
                    publishableKey,
                    productUsage.get(),
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createSetCustomerSourceDefaultRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.UpdateDefaultSource
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(
            handler,
            ResultType.CustomerRetrieved,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return stripeRepository.setDefaultCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    operation.sourceId,
                    operation.sourceType,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createSetCustomerShippingInformationRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.Customer.UpdateShipping
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(
            handler,
            ResultType.ShippingInfo,
            operation.id
        ) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return stripeRepository.setCustomerShippingInfo(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    operation.shippingInformation,
                    ApiRequest.Options(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createUpdateCustomerRunnable(
        key: EphemeralKey,
        operation: EphemeralOperation.RetrieveKey
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(
            handler,
            ResultType.CustomerRetrieved,
            operation.id
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
