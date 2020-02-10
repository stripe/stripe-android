package com.stripe.android

import android.app.Activity
import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.PaymentConfiguration.Companion.getInstance
import com.stripe.android.Stripe.Companion.appInfo
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.Source.SourceType
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Represents a logged-in session of a single Customer.
 *
 * See [Creating ephemeral keys](https://stripe.com/docs/mobile/android/standard#creating-ephemeral-keys)
 */
class CustomerSession @VisibleForTesting internal constructor(
    context: Context,
    keyProvider: EphemeralKeyProvider,
    private val threadPoolExecutor: ThreadPoolExecutor,
    stripeRepository: StripeRepository,
    publishableKey: String,
    stripeAccountId: String?,
    shouldPrefetchEphemeralKey: Boolean,
    private val timeSupplier: TimeSupplier = { Calendar.getInstance().timeInMillis }
) {
    @JvmSynthetic
    internal var customerCacheTime: Long = 0
    @JvmSynthetic
    internal var customer: Customer? = null

    private val operationIdFactory = StripeOperationIdFactory()
    private val productUsage = CustomerSessionProductUsage()
    private val listeners: MutableMap<String, RetrievalListener?> = mutableMapOf()
    private val ephemeralKeyManager: EphemeralKeyManager

    init {
        val keyManagerListener = CustomerSessionEphemeralKeyManagerListener(
            CustomerSessionRunnableFactory(
                stripeRepository,
                createHandler(),
                publishableKey,
                stripeAccountId,
                productUsage
            ),
            threadPoolExecutor,
            listeners,
            productUsage
        )
        ephemeralKeyManager = EphemeralKeyManager(
            keyProvider,
            keyManagerListener,
            operationIdFactory,
            shouldPrefetchEphemeralKey,
            timeSupplier
        )
    }

    @VisibleForTesting
    internal val productUsageTokens: Set<String>
        get() {
            return productUsage.get()
        }

    private constructor(
        context: Context,
        keyProvider: EphemeralKeyProvider,
        appInfo: AppInfo?,
        publishableKey: String,
        stripeAccountId: String?,
        shouldPrefetchEphemeralKey: Boolean
    ) : this(
        context, keyProvider, createThreadPoolExecutor(),
        StripeApiRepository(context, appInfo), publishableKey, stripeAccountId,
        shouldPrefetchEphemeralKey
    )

    private fun createHandler(): Handler {
        return CustomerSessionHandler(object : CustomerSessionHandler.Listener {
            override fun onCustomerRetrieved(
                customer: Customer?,
                operationId: String
            ) {
                this@CustomerSession.customer = customer
                customerCacheTime = timeSupplier()
                val listener: CustomerRetrievalListener? = getListener(operationId)
                if (customer != null) {
                    listener?.onCustomerRetrieved(customer)
                }
            }

            override fun onSourceRetrieved(
                source: Source?,
                operationId: String
            ) {
                val listener: SourceRetrievalListener? = getListener(operationId)
                if (source != null) {
                    listener?.onSourceRetrieved(source)
                }
            }

            override fun onPaymentMethodRetrieved(
                paymentMethod: PaymentMethod?,
                operationId: String
            ) {
                val listener: PaymentMethodRetrievalListener? = getListener(operationId)
                if (paymentMethod != null) {
                    listener?.onPaymentMethodRetrieved(paymentMethod)
                }
            }

            override fun onPaymentMethodsRetrieved(
                paymentMethods: List<PaymentMethod>,
                operationId: String
            ) {
                val listener: PaymentMethodsRetrievalListener? = getListener(operationId)
                listener?.onPaymentMethodsRetrieved(paymentMethods)
            }

            override fun onCustomerShippingInfoSaved(
                customer: Customer?,
                operationId: String
            ) {
                this@CustomerSession.customer = customer
                val listener: CustomerRetrievalListener? = getListener(operationId)
                if (customer != null) {
                    listener?.onCustomerRetrieved(customer)
                }
            }

            override fun onError(
                exception: StripeException,
                operationId: String
            ) {
                handleRetrievalError(operationId, exception)
            }
        })
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @JvmSynthetic
    internal fun addProductUsageTokenIfValid(token: String) {
        productUsage.add(token)
    }

    /**
     * Retrieve the current [Customer]. If [customer] is not stale, this returns immediately with
     * the cache. If not, it fetches a new value and returns that to the listener.
     *
     * @param listener a [CustomerRetrievalListener] to invoke with the result of getting the
     * customer, either from the cache or from the server
     */
    fun retrieveCurrentCustomer(listener: CustomerRetrievalListener) {
        cachedCustomer?.let {
            listener.onCustomerRetrieved(it)
        } ?: updateCurrentCustomer(listener)
    }

    /**
     * Force an update of the current customer, regardless of how much time has passed.
     *
     * @param listener a [CustomerRetrievalListener] to invoke with the result of getting
     * the customer from the server
     */
    fun updateCurrentCustomer(listener: CustomerRetrievalListener) {
        customer = null
        startOperation(
            EphemeralOperation.RetrieveKey(operationIdFactory.create()),
            listener
        )
    }

    /**
     * Gets a cached customer, or `null` if the current customer has expired.
     *
     * @return the current value of [customer], or `null` if the customer object is
     * expired.
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
        startOperation(
            EphemeralOperation.Customer.AddSource(
                sourceId = sourceId,
                sourceType = sourceType,
                id = operationIdFactory.create()
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
        startOperation(
            EphemeralOperation.Customer.DeleteSource(
                sourceId = sourceId,
                id = operationIdFactory.create()
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
        startOperation(
            EphemeralOperation.Customer.AttachPaymentMethod(
                paymentMethodId = paymentMethodId,
                id = operationIdFactory.create()
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
        startOperation(
            EphemeralOperation.Customer.DetachPaymentMethod(
                paymentMethodId = paymentMethodId,
                id = operationIdFactory.create()
            ),
            listener
        )
    }

    /**
     * Retrieves all of the customer's PaymentMethod objects,
     * filtered by a [PaymentMethod.Type].
     *
     * @param paymentMethodType the [PaymentMethod.Type] to filter by
     * @param listener a [PaymentMethodRetrievalListener] called when the API call
     * completes with a list of [PaymentMethod] objects
     */
    fun getPaymentMethods(
        paymentMethodType: PaymentMethod.Type,
        listener: PaymentMethodsRetrievalListener
    ) {
        startOperation(
            EphemeralOperation.Customer.PaymentMethods(
                type = paymentMethodType,
                id = operationIdFactory.create()
            ),
            listener
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
        startOperation(
            EphemeralOperation.Customer.UpdateShipping(
                shippingInformation = shippingInformation,
                id = operationIdFactory.create()
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
        startOperation(
            EphemeralOperation.Customer.UpdateDefaultSource(
                sourceId = sourceId,
                sourceType = sourceType,
                id = operationIdFactory.create()
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

    @JvmSynthetic
    internal fun resetUsageTokens() {
        productUsage.reset()
    }

    private val canUseCachedCustomer: Boolean
        get() {
            return customer != null &&
                timeSupplier() - customerCacheTime < CUSTOMER_CACHE_DURATION_MILLISECONDS
        }

    private fun handleRetrievalError(
        operationId: String,
        exception: StripeException
    ) {
        listeners.remove(operationId)?.let { listener ->
            val message = exception.localizedMessage.orEmpty()
            listener.onError(
                exception.statusCode,
                message,
                exception.stripeError
            )
        }
        resetUsageTokens()
    }

    private fun <L : RetrievalListener?> getListener(operationId: String): L? {
        return listeners.remove(operationId) as L?
    }

    abstract class ActivityCustomerRetrievalListener<A : Activity?>(activity: A) : CustomerRetrievalListener {
        private val activityRef: WeakReference<A> = WeakReference(activity)
        protected val activity: A?
            get() = activityRef.get()
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

    interface RetrievalListener {
        fun onError(errorCode: Int, errorMessage: String, stripeError: StripeError?)
    }

    /**
     * Abstract implementation of [PaymentMethodsRetrievalListener] that holds a
     * [WeakReference] to an `Activity` object.
     */
    abstract class ActivityPaymentMethodsRetrievalListener<A : Activity?>(activity: A) : PaymentMethodsRetrievalListener {
        private val activityRef: WeakReference<A> = WeakReference(activity)
        protected val activity: A?
            get() = activityRef.get()
    }

    /**
     * Abstract implementation of [SourceRetrievalListener] that holds a
     * [WeakReference] to an `Activity` object.
     */
    abstract class ActivitySourceRetrievalListener<A : Activity?>(activity: A) : SourceRetrievalListener {
        private val activityRef: WeakReference<A> = WeakReference(activity)
        protected val activity: A?
            get() = activityRef.get()
    }

    /**
     * Abstract implementation of [PaymentMethodRetrievalListener] that holds a
     * [WeakReference] to an `Activity` object.
     */
    abstract class ActivityPaymentMethodRetrievalListener<A : Activity?>(activity: A) : PaymentMethodRetrievalListener {
        private val activityRef: WeakReference<A> = WeakReference(activity)
        protected val activity: A?
            get() = activityRef.get()
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
         * @param stripeAccountId An optional Stripe Connect account to associate with Customer-related
         * Stripe API Requests. See [Stripe].
         * @param shouldPrefetchEphemeralKey If true, will immediately fetch an ephemeral key using
         * {@param ephemeralKeyProvider}. Otherwise, will only fetch
         * an ephemeral key when needed.
         */
        @JvmStatic
        @JvmOverloads
        fun initCustomerSession(
            context: Context,
            ephemeralKeyProvider: EphemeralKeyProvider,
            stripeAccountId: String? = null,
            shouldPrefetchEphemeralKey: Boolean = true
        ) {
            instance = CustomerSession(context, ephemeralKeyProvider, appInfo,
                getInstance(context).publishableKey, stripeAccountId, shouldPrefetchEphemeralKey)
        }

        /**
         * See [initCustomerSession]
         */
        @JvmStatic
        fun initCustomerSession(
            context: Context,
            ephemeralKeyProvider: EphemeralKeyProvider,
            shouldPrefetchEphemeralKey: Boolean
        ) {
            initCustomerSession(
                context, ephemeralKeyProvider, null, shouldPrefetchEphemeralKey
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
            instance?.listeners?.clear()
            cancelCallbacks()
            instance = null
        }

        /**
         * End any async calls in process and will not invoke callback listeners.
         * It will not clear the singleton instance of a [CustomerSession] so it can be
         * safely used when a view is being removed/destroyed to avoid null pointer exceptions
         * due to async operation delay.
         *
         * No need to call [initCustomerSession] again after this operation.
         */
        @JvmStatic
        fun cancelCallbacks() {
            instance?.threadPoolExecutor?.shutdownNow()
        }

        private fun createThreadPoolExecutor(): ThreadPoolExecutor {
            return ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                KEEP_ALIVE_TIME.toLong(),
                KEEP_ALIVE_TIME_UNIT,
                LinkedBlockingQueue())
        }
    }
}

internal typealias TimeSupplier = () -> Long
