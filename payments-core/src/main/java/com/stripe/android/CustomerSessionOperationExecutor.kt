package com.stripe.android

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
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
                val result = retrieveCustomerWithKey(ephemeralKey, operation.productUsage)
                withContext(Dispatchers.Main) {
                    onCustomerRetrieved(operation, result)
                }
            }
            is EphemeralOperation.Customer.AddSource -> {
                val result = stripeRepository.addCustomerSource(
                    customerId = ephemeralKey.objectId,
                    publishableKey = publishableKey,
                    productUsageTokens = operation.productUsage,
                    sourceId = operation.sourceId,
                    sourceType = operation.sourceType,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

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
                val result = stripeRepository.deleteCustomerSource(
                    customerId = ephemeralKey.objectId,
                    publishableKey = publishableKey,
                    productUsageTokens = operation.productUsage,
                    sourceId = operation.sourceId,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

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
                val result = stripeRepository.attachPaymentMethod(
                    customerId = ephemeralKey.objectId,
                    productUsageTokens = operation.productUsage,
                    paymentMethodId = operation.paymentMethodId,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

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
                val result = stripeRepository.detachPaymentMethod(
                    productUsageTokens = operation.productUsage,
                    paymentMethodId = operation.paymentMethodId,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

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
                val result = stripeRepository.getPaymentMethods(
                    listPaymentMethodsParams = ListPaymentMethodsParams(
                        customerId = ephemeralKey.objectId,
                        paymentMethodType = operation.type,
                        limit = operation.limit,
                        endingBefore = operation.endingBefore,
                        startingAfter = operation.startingAfter,
                    ),
                    productUsageTokens = operation.productUsage,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

                withContext(Dispatchers.Main) {
                    val listener = getListener<CustomerSession.PaymentMethodsRetrievalListener>(operation.id)

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
                val result = stripeRepository.setDefaultCustomerSource(
                    customerId = ephemeralKey.objectId,
                    publishableKey = publishableKey,
                    productUsageTokens = operation.productUsage,
                    sourceId = operation.sourceId,
                    sourceType = operation.sourceType,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

                withContext(Dispatchers.Main) {
                    onCustomerRetrieved(operation, result)
                }
            }
            is EphemeralOperation.Customer.UpdateShipping -> {
                val result = stripeRepository.setCustomerShippingInfo(
                    customerId = ephemeralKey.objectId,
                    publishableKey = publishableKey,
                    productUsageTokens = operation.productUsage,
                    shippingInformation = operation.shippingInformation,
                    requestOptions = ApiRequest.Options(ephemeralKey.secret, stripeAccountId),
                )

                withContext(Dispatchers.Main) {
                    onCustomerRetrieved(operation, result)
                }
            }
            else -> {}
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
        val (statusCode, message, stripeError) = when (error) {
            is StripeException -> Triple(
                error.statusCode,
                error.message.orEmpty(),
                error.stripeError
            )
            else -> Triple(
                0,
                error.message.orEmpty(),
                null
            )
        }

        when (listener) {
            is CustomerSession.RetrievalWithExceptionListener -> listener.onError(
                statusCode,
                message,
                stripeError,
                error,
            )
            is CustomerSession.RetrievalListener -> listener.onError(
                statusCode,
                message,
                stripeError,
            )
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
    private suspend fun retrieveCustomerWithKey(
        key: EphemeralKey,
        productUsage: Set<String>
    ): Result<Customer> {
        return stripeRepository.retrieveCustomer(
            key.objectId,
            productUsage,
            ApiRequest.Options(key.secret, stripeAccountId)
        )
    }
}
