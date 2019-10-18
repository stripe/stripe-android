package com.stripe.android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Pair
import androidx.annotation.IntDef
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source

/**
 * Class that creates the [Runnable] task for the [CustomerSession] operation.
 */
internal class CustomerSessionRunnableFactory constructor(
    private val stripeRepository: StripeRepository,
    private val handler: Handler,
    private val localBroadcastManager: LocalBroadcastManager,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val productUsage: CustomerSessionProductUsage
) {
    @IntDef(MessageCode.ERROR, MessageCode.CUSTOMER_RETRIEVED, MessageCode.SOURCE_RETRIEVED,
        MessageCode.PAYMENT_METHOD_RETRIEVED, MessageCode.CUSTOMER_SHIPPING_INFO_SAVED,
        MessageCode.PAYMENT_METHODS_RETRIEVED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class MessageCode {
        companion object {
            const val ERROR = 1
            const val CUSTOMER_RETRIEVED = 2
            const val SOURCE_RETRIEVED = 3
            const val PAYMENT_METHOD_RETRIEVED = 4
            const val CUSTOMER_SHIPPING_INFO_SAVED = 5
            const val PAYMENT_METHODS_RETRIEVED = 6
        }
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
        return object : CustomerSessionRunnable<Source>(handler, localBroadcastManager,
            MessageCode.SOURCE_RETRIEVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Source? {
                return stripeRepository.addCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    sourceId,
                    sourceType,
                    ApiRequest.Options.create(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createDeleteCustomerSourceRunnable(
        key: EphemeralKey,
        sourceId: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Source>(handler, localBroadcastManager,
            MessageCode.SOURCE_RETRIEVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Source? {
                return stripeRepository.deleteCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    sourceId,
                    ApiRequest.Options.create(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createAttachPaymentMethodRunnable(
        key: EphemeralKey,
        paymentMethodId: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<PaymentMethod>(handler, localBroadcastManager,
            MessageCode.PAYMENT_METHOD_RETRIEVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): PaymentMethod? {
                return stripeRepository.attachPaymentMethod(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    paymentMethodId,
                    ApiRequest.Options.create(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createDetachPaymentMethodRunnable(
        key: EphemeralKey,
        paymentMethodId: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<PaymentMethod>(handler, localBroadcastManager,
            MessageCode.PAYMENT_METHOD_RETRIEVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): PaymentMethod? {
                return stripeRepository.detachPaymentMethod(
                    publishableKey,
                    productUsage.get(),
                    paymentMethodId,
                    ApiRequest.Options.create(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createGetPaymentMethodsRunnable(
        key: EphemeralKey,
        paymentMethodType: String,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<List<PaymentMethod>>(handler,
            localBroadcastManager, MessageCode.PAYMENT_METHODS_RETRIEVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): List<PaymentMethod> {
                return stripeRepository.getPaymentMethods(
                    key.objectId,
                    paymentMethodType,
                    publishableKey,
                    productUsage.get(),
                    ApiRequest.Options.create(key.secret, stripeAccountId)
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
        return object : CustomerSessionRunnable<Customer>(handler, localBroadcastManager,
            MessageCode.CUSTOMER_RETRIEVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return stripeRepository.setDefaultCustomerSource(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    sourceId,
                    sourceType,
                    ApiRequest.Options.create(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createSetCustomerShippingInformationRunnable(
        key: EphemeralKey,
        shippingInformation: ShippingInformation,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(handler, localBroadcastManager,
            MessageCode.CUSTOMER_SHIPPING_INFO_SAVED, operationId) {
            @Throws(StripeException::class)
            public override fun createMessageObject(): Customer? {
                return stripeRepository.setCustomerShippingInfo(
                    key.objectId,
                    publishableKey,
                    productUsage.get(),
                    shippingInformation,
                    ApiRequest.Options.create(key.secret, stripeAccountId)
                )
            }
        }
    }

    private fun createUpdateCustomerRunnable(
        key: EphemeralKey,
        operationId: String
    ): Runnable {
        return object : CustomerSessionRunnable<Customer>(handler, localBroadcastManager,
            MessageCode.CUSTOMER_RETRIEVED, operationId) {
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
            ApiRequest.Options.create(key.secret, stripeAccountId)
        )
    }

    private abstract class CustomerSessionRunnable<T> constructor(
        private val handler: Handler,
        private val localBroadcastManager: LocalBroadcastManager,
        @param:MessageCode @field:MessageCode private val messageCode: Int,
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
                sendErrorIntent(stripeEx)
            }
        }

        private fun sendMessage(messageObject: T?) {
            handler.sendMessage(
                handler.obtainMessage(
                    messageCode,
                    Pair.create<String, T>(operationId, messageObject)
                )
            )
        }

        private fun sendErrorMessage(stripeEx: StripeException) {
            handler.sendMessage(
                handler.obtainMessage(
                    MessageCode.ERROR,
                    Pair.create(operationId, stripeEx)
                )
            )
        }

        private fun sendErrorIntent(exception: StripeException) {
            val bundle = Bundle()
            bundle.putSerializable(CustomerSession.EXTRA_EXCEPTION, exception)
            localBroadcastManager.sendBroadcast(
                Intent(CustomerSession.ACTION_API_EXCEPTION)
                    .putExtras(bundle)
            )
        }
    }
}
