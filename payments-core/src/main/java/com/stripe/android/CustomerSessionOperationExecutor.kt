package com.stripe.android

import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CustomerSessionOperationExecutor(
    private val stripeRepository: StripeRepository,
    private val publishableKey: String,
    private val stripeAccountId: String?,
    private val listeners: MutableMap<String, CustomerSession.RetrievalListener?>,
    private val onCustomerUpdated: (Customer) -> Unit
) {
    @JvmSynthetic
    internal suspend fun execute(
        ephemeralKey: EphemeralKey,
        operation: EphemeralOperation
    ) {
        when (operation) {
            is EphemeralOperation.RetrieveKey -> {
                val result = runCatching {
                    requireNotNull(
                        retrieveCustomerWithKey(ephemeralKey, operation.productUsage)
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    onCustomerRetrieved(operation, result)
                }
            }
            is EphemeralOperation.Customer.AddSource -> {
                val result = runCatching {
                    requireNotNull(
                        stripeRepository.addCustomerSource(
                            ephemeralKey.objectId,
                            publishableKey,
                            operation.productUsage,
                            operation.sourceId,
                            operation.sourceType,
                            ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                        )
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    val listener: CustomerSession.SourceRetrievalListener? = getListener(operation.id)
                    result.fold(
                        onSuccess = { source ->
                            listener?.onSourceRetrieved(source)
                        },
                        onFailure = {
                            onError(listener, it)
                        }
                    )
                }
            }
            is EphemeralOperation.Customer.DeleteSource -> {
                val result = runCatching {
                    requireNotNull(
                        stripeRepository.deleteCustomerSource(
                            ephemeralKey.objectId,
                            publishableKey,
                            operation.productUsage,
                            operation.sourceId,
                            ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                        )
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    val listener: CustomerSession.SourceRetrievalListener? = getListener(operation.id)
                    result.fold(
                        onSuccess = { source ->
                            listener?.onSourceRetrieved(source)
                        },
                        onFailure = {
                            onError(listener, it)
                        }
                    )
                }
            }
            is EphemeralOperation.Customer.AttachPaymentMethod -> {
                val result = runCatching {
                    requireNotNull(
                        stripeRepository.attachPaymentMethod(
                            ephemeralKey.objectId,
                            publishableKey,
                            operation.productUsage,
                            operation.paymentMethodId,
                            ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                        )
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    val listener: CustomerSession.PaymentMethodRetrievalListener? = getListener(operation.id)
                    result.fold(
                        onSuccess = { paymentMethod ->
                            listener?.onPaymentMethodRetrieved(paymentMethod)
                        },
                        onFailure = {
                            onError(listener, it)
                        }
                    )
                }
            }
            is EphemeralOperation.Customer.DetachPaymentMethod -> {
                val result = runCatching {
                    requireNotNull(
                        stripeRepository.detachPaymentMethod(
                            publishableKey,
                            operation.productUsage,
                            operation.paymentMethodId,
                            ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                        )
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    val listener: CustomerSession.PaymentMethodRetrievalListener? = getListener(operation.id)
                    result.fold(
                        onSuccess = { paymentMethod ->
                            listener?.onPaymentMethodRetrieved(paymentMethod)
                        },
                        onFailure = {
                            onError(listener, it)
                        }
                    )
                }
            }
            is EphemeralOperation.Customer.GetPaymentMethods -> {
                val result = runCatching {
                    stripeRepository.getPaymentMethods(
                        ListPaymentMethodsParams(
                            customerId = ephemeralKey.objectId,
                            paymentMethodType = operation.type,
                            limit = operation.limit,
                            endingBefore = operation.endingBefore,
                            startingAfter = operation.startingAfter
                        ),
                        publishableKey,
                        operation.productUsage,
                        ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                    )
                }
                withContext(Dispatchers.Main) {
                    val listener: CustomerSession.PaymentMethodsRetrievalListener? = getListener(operation.id)
                    result.fold(
                        onSuccess = { paymentMethods ->
                            listener?.onPaymentMethodsRetrieved(paymentMethods)
                        },
                        onFailure = {
                            onError(listener, it)
                        }
                    )
                }
            }
            is EphemeralOperation.Customer.UpdateDefaultSource -> {
                val result = runCatching {
                    requireNotNull(
                        stripeRepository.setDefaultCustomerSource(
                            ephemeralKey.objectId,
                            publishableKey,
                            operation.productUsage,
                            operation.sourceId,
                            operation.sourceType,
                            ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                        )
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    onCustomerRetrieved(operation, result)
                }
            }
            is EphemeralOperation.Customer.UpdateShipping -> {
                val result = runCatching {
                    requireNotNull(
                        stripeRepository.setCustomerShippingInfo(
                            ephemeralKey.objectId,
                            publishableKey,
                            operation.productUsage,
                            operation.shippingInformation,
                            ApiRequest.Options(ephemeralKey.secret, stripeAccountId)
                        )
                    ) {
                        REQUIRED_ERROR
                    }
                }
                withContext(Dispatchers.Main) {
                    onCustomerRetrieved(operation, result)
                }
            }
        }
    }

    private fun onCustomerRetrieved(
        operation: EphemeralOperation,
        result: Result<Customer>
    ) {
        val listener: CustomerSession.CustomerRetrievalListener? = getListener(operation.id)
        result.fold(
            onSuccess = { customer ->
                onCustomerUpdated(customer)
                listener?.onCustomerRetrieved(customer)
            },
            onFailure = {
                onError(listener, it)
            }
        )
    }

    private fun onError(
        listener: CustomerSession.RetrievalListener?,
        error: Throwable
    ) {
        when (error) {
            is StripeException -> {
                listener?.onError(
                    error.statusCode,
                    error.message.orEmpty(),
                    error.stripeError
                )
            }
            else -> {
                listener?.onError(
                    0,
                    error.message.orEmpty(),
                    null
                )
            }
        }
    }

    private fun <L : CustomerSession.RetrievalListener?> getListener(operationId: String): L? {
        return listeners.remove(operationId) as L?
    }

    /**
     * Fetch a [Customer]. If the provided key is expired, this method **does not** update the key.
     * Use [createUpdateCustomer] to validate the key before refreshing the customer.
     *
     * @param key the [EphemeralKey] used for this access
     * @return a [Customer] if one can be found with this key, or `null` if one cannot.
     */
    @Throws(StripeException::class)
    private suspend fun retrieveCustomerWithKey(
        key: EphemeralKey,
        productUsage: Set<String>
    ): Customer? {
        return stripeRepository.retrieveCustomer(
            key.objectId,
            productUsage,
            ApiRequest.Options(key.secret, stripeAccountId)
        )
    }

    private companion object {
        private const val REQUIRED_ERROR = "API request returned an invalid response."
    }
}
