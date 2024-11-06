package com.stripe.android

import android.content.Context
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import com.stripe.android.Stripe.Companion.appInfo
import com.stripe.android.core.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.Source.SourceType
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Represents a logged-in session of a single Customer.
 *
 * See [Creating ephemeral keys](https://stripe.com/docs/mobile/android/standard#creating-ephemeral-keys)
 */
@Deprecated(BASIC_INTEGRATION_DEPRECATION_WARNING)
class CustomerSession @VisibleForTesting internal constructor(
    stripeRepository: StripeRepository,
    publishableKey: String,
    stripeAccountId: String?,
    private val workContext: CoroutineContext = createCoroutineDispatcher(),
    private val operationIdFactory: OperationIdFactory = StripeOperationIdFactory(),
    private val timeSupplier: TimeSupplier = { Calendar.getInstance().timeInMillis },
    ephemeralKeyManagerFactory: EphemeralKeyManager.Factory
) {
    @JvmSynthetic
    internal var customerCacheTime: Long = 0

    @JvmSynthetic
    internal var customer: Customer? = null

    private val listeners: MutableMap<String, RetrievalListener?> = mutableMapOf()
    private val operationExecutor = CustomerSessionOperationExecutor(
        stripeRepository,
        publishableKey,
        stripeAccountId,
        listeners
    ) { customer ->
        this.customer = customer
        customerCacheTime = timeSupplier()
    }

    private val ephemeralKeyManager: EphemeralKeyManager = ephemeralKeyManagerFactory.create(
        object : EphemeralKeyManager.KeyManagerListener {
            override fun onKeyUpdate(ephemeralKey: EphemeralKey, operation: EphemeralOperation) {
                CoroutineScope(workContext).launch {
                    operationExecutor.execute(ephemeralKey, operation)
                }
            }

            override fun onKeyError(
                operationId: String,
                errorCode: Int,
                errorMessage: String,
                throwable: Throwable
            ) {
                when (val listener = listeners.remove(operationId)) {
                    is RetrievalWithExceptionListener -> listener.onError(
                        errorCode,
                        errorMessage,
                        null,
                        throwable
                    )
                    is RetrievalListener -> listener.onError(
                        errorCode,
                        errorMessage,
                        null
                    )
                    else -> Unit
                }
            }
        }
    )

    /**
     * Retrieve the current [Customer]. If [customer] is not stale, this returns immediately with
     * the cache. If not, it fetches a new value and returns that to the listener.
     *
     * @param listener a [CustomerRetrievalListener] to invoke with the result of getting the
     * customer, either from the cache or from the server
     */
    fun retrieveCurrentCustomer(listener: CustomerRetrievalListener) {
        retrieveCurrentCustomer(emptySet(), listener)
    }

    @JvmSynthetic
    internal fun retrieveCurrentCustomer(
        productUsage: Set<String>,
        listener: CustomerRetrievalListener
    ) {
        cachedCustomer?.let {
            listener.onCustomerRetrieved(it)
        } ?: updateCurrentCustomer(productUsage, listener)
    }

    /**
     * Force an update of the current customer, regardless of how much time has passed.
     *
     * @param listener a [CustomerRetrievalListener] to invoke with the result of getting
     * the customer from the server
     */
    fun updateCurrentCustomer(listener: CustomerRetrievalListener) {
        updateCurrentCustomer(emptySet(), listener)
    }

    @JvmSynthetic
    internal fun updateCurrentCustomer(
        productUsage: Set<String>,
        listener: CustomerRetrievalListener
    ) {
        customer = null
        startOperation(
            EphemeralOperation.RetrieveKey(
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    /**
     * A cached [Customer], or `null` if the current customer has expired.
     */
    val cachedCustomer: Customer?
        get() {
            return customer.takeIf { canUseCachedCustomer }
        }

    /**
     * Add the Source to the current customer.
     *
     * @param sourceId the ID of the source to be added
     * @param listener a [SourceRetrievalListener] called when the API call completes
     * with the added [Source].
     */
    fun addCustomerSource(
        sourceId: String,
        @SourceType sourceType: String,
        listener: SourceRetrievalListener
    ) {
        addCustomerSource(sourceId, sourceType, emptySet(), listener)
    }

    @JvmSynthetic
    internal fun addCustomerSource(
        sourceId: String,
        @SourceType sourceType: String,
        productUsage: Set<String>,
        listener: SourceRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.AddSource(
                sourceId = sourceId,
                sourceType = sourceType,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    /**
     * Delete the Source from the current customer.
     *
     * @param sourceId the ID of the source to be deleted
     * @param listener a [SourceRetrievalListener] called when the API call completes
     * with the added [Source].
     */
    fun deleteCustomerSource(
        sourceId: String,
        listener: SourceRetrievalListener
    ) {
        deleteCustomerSource(sourceId, emptySet(), listener)
    }

    @JvmSynthetic
    internal fun deleteCustomerSource(
        sourceId: String,
        productUsage: Set<String>,
        listener: SourceRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.DeleteSource(
                sourceId = sourceId,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    /**
     * Attaches a PaymentMethod to a customer.
     *
     * @param paymentMethodId the ID of the payment method to be attached
     * @param listener a [PaymentMethodRetrievalListener] called when the API call
     * completes with the attached [PaymentMethod].
     */
    fun attachPaymentMethod(
        paymentMethodId: String,
        listener: PaymentMethodRetrievalListener
    ) {
        attachPaymentMethod(paymentMethodId, emptySet(), listener)
    }

    @JvmSynthetic
    internal fun attachPaymentMethod(
        paymentMethodId: String,
        productUsage: Set<String>,
        listener: PaymentMethodRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.AttachPaymentMethod(
                paymentMethodId = paymentMethodId,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    /**
     * Detaches a PaymentMethod from a customer.
     *
     * @param paymentMethodId the ID of the payment method to be detached
     * @param listener a [PaymentMethodRetrievalListener] called when the API call
     * completes with the detached [PaymentMethod].
     */
    fun detachPaymentMethod(
        paymentMethodId: String,
        listener: PaymentMethodRetrievalListener
    ) {
        detachPaymentMethod(paymentMethodId, emptySet(), listener)
    }

    @JvmSynthetic
    internal fun detachPaymentMethod(
        paymentMethodId: String,
        productUsage: Set<String>,
        listener: PaymentMethodRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.DetachPaymentMethod(
                paymentMethodId = paymentMethodId,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    /**
     * Retrieves all of the customer's PaymentMethod objects, filtered by a [PaymentMethod.Type].
     *
     * See [List a Customer's PaymentMethods](https://stripe.com/docs/api/payment_methods/list)
     *
     * @param paymentMethodType the [PaymentMethod.Type] to filter by
     * @param listener a [PaymentMethodRetrievalListener] called when the API call
     * completes with a list of [PaymentMethod] objects
     *
     * @param limit Optional. A limit on the number of objects to be returned. Limit can range
     * between 1 and 100, and the default is 10.
     * @param endingBefore Optional. A cursor for use in pagination. `ending_before` is an object
     * ID that defines your place in the list. For instance, if you make a list request and receive
     * 100 objects, starting with `obj_bar`, your subsequent call can include
     * `ending_before=obj_bar` in order to fetch the previous page of the list.
     * @param startingAfter Optional. A cursor for use in pagination. `starting_after` is an object
     * ID that defines your place in the list. For instance, if you make a list request and receive
     * 100 objects, ending with `obj_foo`, your subsequent call can include `starting_after=obj_foo`
     * in order to fetch the next page of the list.
     */
    @JvmOverloads
    fun getPaymentMethods(
        paymentMethodType: PaymentMethod.Type,
        @IntRange(from = 1, to = 100) limit: Int?,
        endingBefore: String? = null,
        startingAfter: String? = null,
        listener: PaymentMethodsRetrievalListener
    ) {
        getPaymentMethods(
            paymentMethodType = paymentMethodType,
            limit = limit,
            endingBefore = endingBefore,
            startingAfter = startingAfter,
            productUsage = emptySet(),
            listener = listener
        )
    }

    @JvmSynthetic
    internal fun getPaymentMethods(
        paymentMethodType: PaymentMethod.Type,
        @IntRange(from = 1, to = 100) limit: Int? = null,
        endingBefore: String? = null,
        startingAfter: String? = null,
        productUsage: Set<String>,
        listener: PaymentMethodsRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.GetPaymentMethods(
                type = paymentMethodType,
                limit = limit,
                endingBefore = endingBefore,
                startingAfter = startingAfter,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    fun getPaymentMethods(
        paymentMethodType: PaymentMethod.Type,
        listener: PaymentMethodsRetrievalListener
    ) {
        getPaymentMethods(
            paymentMethodType = paymentMethodType,
            productUsage = emptySet(),
            listener = listener
        )
    }

    /**
     * Set the shipping information on the current customer.
     *
     * @param shippingInformation the data to be set
     */
    fun setCustomerShippingInformation(
        shippingInformation: ShippingInformation,
        listener: CustomerRetrievalListener
    ) {
        setCustomerShippingInformation(shippingInformation, emptySet(), listener)
    }

    @JvmSynthetic
    internal fun setCustomerShippingInformation(
        shippingInformation: ShippingInformation,
        productUsage: Set<String>,
        listener: CustomerRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.UpdateShipping(
                shippingInformation = shippingInformation,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    /**
     * Set the default Source of the current customer.
     *
     * @param sourceId the ID of the source to be set
     * @param listener a [CustomerRetrievalListener] called when the API call
     * completes with the updated customer
     */
    fun setCustomerDefaultSource(
        sourceId: String,
        @SourceType sourceType: String,
        listener: CustomerRetrievalListener
    ) {
        setCustomerDefaultSource(sourceId, sourceType, emptySet(), listener)
    }

    @JvmSynthetic
    internal fun setCustomerDefaultSource(
        sourceId: String,
        @SourceType sourceType: String,
        productUsage: Set<String>,
        listener: CustomerRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.UpdateDefaultSource(
                sourceId = sourceId,
                sourceType = sourceType,
                id = operationIdFactory.create(),
                productUsage = productUsage
            ),
            listener
        )
    }

    private fun startOperation(
        operation: EphemeralOperation,
        listener: RetrievalListener?
    ) {
        listeners[operation.id] = listener
        ephemeralKeyManager.retrieveEphemeralKey(operation)
    }

    private val canUseCachedCustomer: Boolean
        get() {
            return customer != null &&
                timeSupplier() - customerCacheTime < CUSTOMER_CACHE_DURATION_MILLISECONDS
        }

    @JvmSynthetic
    internal fun cancel() {
        listeners.clear()
        workContext.cancelChildren()
    }

    private fun <L : RetrievalListener?> getListener(operationId: String): L? {
        return listeners.remove(operationId) as L?
    }

    interface CustomerRetrievalListener : RetrievalListener {
        fun onCustomerRetrieved(customer: Customer)
    }

    interface SourceRetrievalListener : RetrievalListener {
        fun onSourceRetrieved(source: Source)
    }

    interface PaymentMethodRetrievalListener : RetrievalListener {
        fun onPaymentMethodRetrieved(paymentMethod: PaymentMethod)
    }

    interface PaymentMethodsRetrievalListener : RetrievalListener {
        fun onPaymentMethodsRetrieved(paymentMethods: List<PaymentMethod>)
    }

    internal interface PaymentMethodsRetrievalWithExceptionListener :
        PaymentMethodsRetrievalListener,
        RetrievalWithExceptionListener

    interface RetrievalListener {
        fun onError(
            errorCode: Int,
            errorMessage: String,
            stripeError: StripeError?
        )
    }

    internal interface RetrievalWithExceptionListener : RetrievalListener {
        fun onError(
            errorCode: Int,
            errorMessage: String,
            stripeError: StripeError?,
            throwable: Throwable
        )

        override fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?) {
            onError(errorCode, errorMessage, stripeError, Exception(errorMessage))
        }
    }

    companion object {
        // The maximum number of active threads we support
        private const val THREAD_POOL_SIZE = 3

        // Sets the amount of time an idle thread waits before terminating
        private const val KEEP_ALIVE_TIME = 2

        // Sets the Time Unit to seconds
        private val KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS

        private val CUSTOMER_CACHE_DURATION_MILLISECONDS = TimeUnit.MINUTES.toMillis(1)

        /**
         * Create a CustomerSession with the provided [EphemeralKeyProvider].
         *
         * You must call [PaymentConfiguration.init] with your publishable key
         * before calling this method.
         *
         * @param context The application context
         * @param ephemeralKeyProvider An [EphemeralKeyProvider] used to retrieve
         * [EphemeralKey] ephemeral keys
         * @param shouldPrefetchEphemeralKey If true, will immediately fetch an ephemeral key using
         * {@param ephemeralKeyProvider}. Otherwise, will only fetch
         * an ephemeral key when needed.
         */
        @JvmStatic
        @JvmOverloads
        fun initCustomerSession(
            context: Context,
            ephemeralKeyProvider: EphemeralKeyProvider,
            shouldPrefetchEphemeralKey: Boolean = true
        ) {
            val operationIdFactory = StripeOperationIdFactory()
            val timeSupplier = { Calendar.getInstance().timeInMillis }
            val ephemeralKeyManagerFactory = EphemeralKeyManager.Factory.Default(
                keyProvider = ephemeralKeyProvider,
                shouldPrefetchEphemeralKey = shouldPrefetchEphemeralKey,
                operationIdFactory = operationIdFactory,
                timeSupplier = timeSupplier
            )

            val config = PaymentConfiguration.getInstance(context)

            instance = CustomerSession(
                StripeApiRepository(context, { config.publishableKey }, appInfo),
                config.publishableKey,
                config.stripeAccountId,
                createCoroutineDispatcher(),
                operationIdFactory,
                timeSupplier,
                ephemeralKeyManagerFactory
            )
        }

        @JvmSynthetic
        internal var instance: CustomerSession? = null

        /**
         * Gets the singleton instance of [CustomerSession]. If the session has not been
         * initialized, this will throw a [RuntimeException].
         *
         * @return the singleton [CustomerSession] instance.
         */
        @JvmStatic
        fun getInstance(): CustomerSession {
            return checkNotNull(instance) {
                "Attempted to get instance of CustomerSession without initialization."
            }
        }

        /**
         * End the singleton instance of a [CustomerSession].
         * Calls to [getInstance] will throw an [IllegalStateException]
         * after this call, until the user calls
         * [initCustomerSession] again.
         */
        @JvmStatic
        fun endCustomerSession() {
            clearInstance()
        }

        @VisibleForTesting
        @JvmSynthetic
        internal fun clearInstance() {
            cancelCallbacks()
            instance = null
        }

        /**
         * Cancel any in-flight [CustomerSession] operations.
         * Their callback listeners will not be called.
         *
         * It will not clear the singleton [CustomerSession] instance.
         *
         * It is not necessary to call [initCustomerSession] after calling [cancelCallbacks].
         */
        @JvmStatic
        fun cancelCallbacks() {
            instance?.cancel()
        }

        private fun createCoroutineDispatcher(): CoroutineContext {
            return ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                KEEP_ALIVE_TIME.toLong(),
                KEEP_ALIVE_TIME_UNIT,
                LinkedBlockingQueue()
            ).asCoroutineDispatcher()
        }
    }
}

internal typealias TimeSupplier = () -> Long
