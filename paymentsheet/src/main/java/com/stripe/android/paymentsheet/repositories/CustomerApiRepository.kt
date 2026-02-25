package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * A [CustomerRepository] that uses the Stripe API.
 */
@Singleton
internal class CustomerApiRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    @Named(PRODUCT_USAGE) private val productUsageTokens: Set<String> = emptySet()
) : CustomerRepository {

    override suspend fun retrieveCustomer(
        accessInfo: CustomerMetadata.AccessInfo,
    ): Customer? {
        return stripeRepository.retrieveCustomer(
            accessInfo.customerId,
            productUsageTokens,
            ApiRequest.Options(
                requireNotNull(accessInfo.ephemeralKeySecret) {
                    "Cannot retrieve customer without an ephemeral key"
                },
                lazyPaymentConfig.get().stripeAccountId
            )
        ).getOrNull()
    }

    override suspend fun getPaymentMethods(
        accessInfo: CustomerMetadata.AccessInfo,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>> = withContext(workContext) {
        val ephemeralKeySecret = requireNotNull(accessInfo.ephemeralKeySecret) {
            "Cannot get payment methods without an ephemeral key"
        }
        val requests = types.filter { paymentMethodType ->
            paymentMethodType in setOf(
                PaymentMethod.Type.Card,
                PaymentMethod.Type.USBankAccount,
                PaymentMethod.Type.SepaDebit,
            )
        }.map { paymentMethodType ->
            async {
                stripeRepository.getPaymentMethods(
                    listPaymentMethodsParams = ListPaymentMethodsParams(
                        customerId = accessInfo.customerId,
                        limit = 100,
                        paymentMethodType = paymentMethodType,
                    ),
                    productUsageTokens = productUsageTokens,
                    requestOptions = ApiRequest.Options(
                        apiKey = ephemeralKeySecret,
                        stripeAccount = lazyPaymentConfig.get().stripeAccountId,
                    ),
                ).onFailure {
                    logger.error("Failed to retrieve payment methods.", it)
                    errorReporter.report(
                        ErrorReporter.ExpectedErrorEvent.GET_SAVED_PAYMENT_METHODS_FAILURE,
                        StripeException.create(it)
                    )
                }.onSuccess {
                    errorReporter.report(ErrorReporter.SuccessEvent.GET_SAVED_PAYMENT_METHODS_SUCCESS)
                }
            }
        }

        val paymentMethods = mutableListOf<PaymentMethod>()
        requests.awaitAll().forEach {
            it.fold(
                onFailure = {
                    if (!silentlyFail) {
                        return@withContext Result.failure(it)
                    }
                },
                onSuccess = { customerPaymentMethods ->
                    paymentMethods.addAll(filterPaymentMethods(customerPaymentMethods))
                }
            )
        }

        Result.success(paymentMethods)
    }

    override suspend fun detachPaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean
    ): Result<PaymentMethod> {
        val ephemeralKeySecret = requireNotNull(accessInfo.ephemeralKeySecret) {
            "Cannot detach payment method without an ephemeral key"
        }
        val result = if (canRemoveDuplicates) {
            detachPaymentMethodAndDuplicates(
                accessInfo,
                paymentMethodId
            )
        } else {
            val clientSecret = accessInfo.customerSessionClientSecret
            if (clientSecret != null) {
                stripeRepository.detachPaymentMethod(
                    customerSessionClientSecret = clientSecret,
                    productUsageTokens = productUsageTokens,
                    paymentMethodId = paymentMethodId,
                    requestOptions = ApiRequest.Options(
                        apiKey = ephemeralKeySecret,
                        stripeAccount = lazyPaymentConfig.get().stripeAccountId,
                    )
                )
            } else {
                stripeRepository.detachPaymentMethod(
                    productUsageTokens = productUsageTokens,
                    paymentMethodId = paymentMethodId,
                    requestOptions = ApiRequest.Options(
                        apiKey = ephemeralKeySecret,
                        stripeAccount = lazyPaymentConfig.get().stripeAccountId,
                    )
                )
            }
        }

        return result.onFailure {
            logger.error("Failed to detach payment method $paymentMethodId.", it)
        }
    }

    override suspend fun attachPaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String
    ): Result<PaymentMethod> =
        stripeRepository.attachPaymentMethod(
            customerId = accessInfo.customerId,
            productUsageTokens = productUsageTokens,
            paymentMethodId = paymentMethodId,
            requestOptions = ApiRequest.Options(
                apiKey = requireNotNull(accessInfo.ephemeralKeySecret) {
                    "Cannot attach payment method without an ephemeral key"
                },
                stripeAccount = lazyPaymentConfig.get().stripeAccountId,
            )
        ).onFailure {
            logger.error("Failed to attach payment method $paymentMethodId.", it)
        }

    override suspend fun updatePaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod> =
        stripeRepository.updatePaymentMethod(
            paymentMethodId = paymentMethodId,
            paymentMethodUpdateParams = params,
            options = ApiRequest.Options(
                apiKey = requireNotNull(accessInfo.ephemeralKeySecret) {
                    "Cannot update payment method without an ephemeral key"
                },
                stripeAccount = lazyPaymentConfig.get().stripeAccountId,
            )
        ).onFailure {
            logger.error("Failed to update payment method $paymentMethodId.", it)
        }

    override suspend fun setDefaultPaymentMethod(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String?
    ): Result<Customer> = stripeRepository.setDefaultPaymentMethod(
        paymentMethodId = paymentMethodId,
        customerId = accessInfo.customerId,
        options = ApiRequest.Options(
            apiKey = requireNotNull(accessInfo.ephemeralKeySecret) {
                "Cannot set default payment method without an ephemeral key"
            },
            stripeAccount = lazyPaymentConfig.get().stripeAccountId,
        )
    )

    private fun filterPaymentMethods(allPaymentMethods: List<PaymentMethod>): List<PaymentMethod> {
        val paymentMethods = mutableListOf<PaymentMethod>()

        val linkPaymentMethods = getLinkPaymentMethods(allPaymentMethods)

        paymentMethods.addAll(linkPaymentMethods)

        val walletTypesToRemove = setOf(
            Wallet.Type.ApplePay,
            Wallet.Type.GooglePay,
            Wallet.Type.SamsungPay,
            Wallet.Type.Link,
        )
        paymentMethods.addAll(
            allPaymentMethods.filter { paymentMethod ->
                val isCardWithWallet = paymentMethod.type == PaymentMethod.Type.Card &&
                    walletTypesToRemove.contains(paymentMethod.card?.wallet?.walletType)
                !isCardWithWallet
            }
        )

        return paymentMethods
    }

    private fun getLinkPaymentMethods(paymentMethods: List<PaymentMethod>): List<PaymentMethod> {
        return paymentMethods.filter { paymentMethod ->
            paymentMethod.type == PaymentMethod.Type.Card &&
                paymentMethod.card?.wallet?.walletType == Wallet.Type.Link
        }.distinctBy { paymentMethod ->
            val card = paymentMethod.card

            "${card?.last4}-${card?.expiryMonth}-${card?.expiryYear}-${card?.brand?.code}"
        }
    }

    /**
     * Removes the provided saved payment method alongside any duplicate stored payment methods. This logic removes
     * removes all the duplicates first before attempting to remove the requested payment method. We will only return
     * the result of the requested payment method since it is the main payment method that we are trying to remove.
     *
     * This function should eventually be replaced by an endpoint that does this logic in the backend.
     *
     * @param accessInfo authentication information that can perform detaching operations
     * @param paymentMethodId the id of the payment method to remove and to compare with for stored duplicates
     *
     * @return a result containing the requested payment method to remove
     */
    private suspend fun CustomerRepository.detachPaymentMethodAndDuplicates(
        accessInfo: CustomerMetadata.AccessInfo,
        paymentMethodId: String,
    ): Result<PaymentMethod> = with(CoroutineScope(workContext)) {
        val paymentMethods = getPaymentMethods(
            accessInfo = accessInfo,
            // We only support removing duplicate cards.
            types = listOf(PaymentMethod.Type.Card),
            silentlyFail = false,
        ).getOrElse {
            return Result.failure(it)
        }

        val requestedPaymentMethodToRemove = paymentMethods.find { paymentMethod ->
            paymentMethod.id == paymentMethodId
        } ?: run {
            /*
             * If we don't find the requested payment method in the retrieved list, attempt remove it anyways. It
             * could be that the payment method is not a card but a saved US Bank Account or SEPA Debit PM.
             */
            return@with detachPaymentMethod(
                accessInfo = accessInfo,
                paymentMethodId = paymentMethodId,
                canRemoveDuplicates = false,
            )
        }

        /*
         * Find all duplicate payment methods except for the original payment method we are attempting to remove. The
         * original payment method will be removed last.
         */
        val paymentMethodsToRemove = paymentMethods.filter { paymentMethod ->
            paymentMethod.type == PaymentMethod.Type.Card &&
                paymentMethod.card?.fingerprint == requestedPaymentMethodToRemove.card?.fingerprint &&
                paymentMethod.id != paymentMethodId
        }

        val failureResults = mutableListOf<DuplicatePaymentMethodDetachFailureException.DuplicateDetachFailure>()

        /*
         * Removes all the payment methods asynchronously, improving the overall performance of this function.
         */
        val paymentMethodAsyncRemovals = paymentMethodsToRemove.map { paymentMethod ->
            async {
                detachPaymentMethod(
                    accessInfo = accessInfo,
                    paymentMethodId = paymentMethod.id,
                    canRemoveDuplicates = false,
                ).onFailure { exception ->
                    failureResults.add(
                        DuplicatePaymentMethodDetachFailureException.DuplicateDetachFailure(
                            paymentMethodId = paymentMethod.id,
                            exception = exception,
                        )
                    )
                }
            }
        }

        paymentMethodAsyncRemovals.awaitAll()

        if (failureResults.isNotEmpty()) {
            return Result.failure(DuplicatePaymentMethodDetachFailureException(failureResults))
        }

        // Remove the original payment method
        return detachPaymentMethod(
            accessInfo = accessInfo,
            paymentMethodId = paymentMethodId,
            canRemoveDuplicates = false,
        )
    }
}
