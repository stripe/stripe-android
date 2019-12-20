package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.BillingAddressFields
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentFlowActivityStarter
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import java.lang.ref.WeakReference

/**
 * Represents a single start-to-finish payment operation.
 */
class PaymentSession @VisibleForTesting internal constructor(
    private val context: Context,
    private val customerSession: CustomerSession,
    private val paymentMethodsActivityStarter:
    ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args>,
    private val paymentFlowActivityStarter:
    ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args>,
    private val paymentSessionPrefs: PaymentSessionPrefs,
    paymentSessionData: PaymentSessionData = PaymentSessionData()
) {
    /**
     * @return the data associated with the instance of this class.
     */
    var paymentSessionData: PaymentSessionData = paymentSessionData
        private set
    private var paymentSessionListener: PaymentSessionListener? = null
    private var config: PaymentSessionConfig? = null

    /**
     * Create a PaymentSession attached to the given host Activity.
     *
     * @param activity an `Activity` from which to launch other Stripe Activities. This
     * Activity will receive results in
     * `Activity#onActivityResult(int, int, Intent)` that should be
     * passed back to this session.
     */
    constructor(activity: Activity) : this(
        activity.applicationContext,
        CustomerSession.getInstance(),
        PaymentMethodsActivityStarter(activity),
        PaymentFlowActivityStarter(activity),
        PaymentSessionPrefs.create(activity)
    )

    constructor(fragment: Fragment) : this(
        fragment.requireContext().applicationContext,
        CustomerSession.getInstance(),
        PaymentMethodsActivityStarter(fragment),
        PaymentFlowActivityStarter(fragment),
        PaymentSessionPrefs.create(fragment.requireActivity())
    )

    /**
     * Notify this payment session that it is complete
     */
    fun onCompleted() {
        customerSession.resetUsageTokens()
    }

    /**
     * Method to handle Activity results from Stripe activities. Pass data here from your
     * host's `#onActivityResult(int, int, Intent)` function.
     *
     * @param requestCode the request code used to open the resulting activity
     * @param resultCode a result code representing the success of the intended action
     * @param data an [Intent] with the resulting data from the Activity
     *
     * @return `true` if the activity result was handled by this function,
     * otherwise `false`
     */
    fun handlePaymentData(requestCode: Int, resultCode: Int, data: Intent): Boolean {
        if (!VALID_REQUEST_CODES.contains(requestCode)) {
            return false
        }

        when (resultCode) {
            Activity.RESULT_CANCELED -> {
                if (requestCode == PaymentMethodsActivityStarter.REQUEST_CODE) {
                    // If resultCode of `PaymentMethodsActivity` is `Activity.RESULT_CANCELED`,
                    // the user tapped back via the toolbar or device back button.
                    onPaymentMethodResult(data)
                } else {
                    fetchCustomer()
                }
                return false
            }
            Activity.RESULT_OK -> when (requestCode) {
                PaymentMethodsActivityStarter.REQUEST_CODE -> {
                    onPaymentMethodResult(data)
                    return true
                }
                PaymentFlowActivityStarter.REQUEST_CODE -> {
                    val paymentSessionData =
                        data.getParcelableExtra(EXTRA_PAYMENT_SESSION_DATA)
                            ?: this.paymentSessionData
                    this.paymentSessionData = paymentSessionData
                    paymentSessionListener?.onPaymentSessionDataChanged(paymentSessionData)
                    return true
                }
                else -> {
                    return false
                }
            }
            else -> return false
        }
    }

    private fun onPaymentMethodResult(data: Intent) {
        val paymentMethod: PaymentMethod? =
            PaymentMethodsActivityStarter.Result.fromIntent(data)?.paymentMethod
        persistPaymentMethod(paymentMethod)
        dispatchUpdates()
    }

    private fun dispatchUpdates() {
        paymentSessionListener?.onPaymentSessionDataChanged(paymentSessionData)
        paymentSessionListener?.onCommunicatingStateChanged(false)
    }

    private fun persistPaymentMethod(paymentMethod: PaymentMethod?) {
        customerSession.cachedCustomer?.id?.let { customerId ->
            paymentSessionPrefs.saveSelectedPaymentMethodId(customerId, paymentMethod?.id)
        }
        paymentSessionData = paymentSessionData.copy(paymentMethod = paymentMethod)
    }

    /**
     * Initialize the PaymentSession with a [PaymentSessionListener] to be notified of
     * data changes.
     *
     * @param listener a [PaymentSessionListener] that will receive notifications of changes
     * in payment session status, including networking status
     * @param paymentSessionConfig a [PaymentSessionConfig] used to decide which items are
     * necessary in the PaymentSession.
     * @param savedInstanceState a `Bundle` containing the saved state of a
     * PaymentSession that was stored in [savePaymentSessionInstanceState]
     *
     * @return `true` if the PaymentSession is initialized, `false` if a state error
     * occurs. Failure can only occur if there is no initialized [CustomerSession].
     */
    @JvmOverloads
    fun init(
        listener: PaymentSessionListener,
        paymentSessionConfig: PaymentSessionConfig,
        savedInstanceState: Bundle? = null
    ): Boolean {
        // Checking to make sure that there is a valid CustomerSession -- the getInstance() call
        // will throw a runtime exception if none is ready.
        try {
            if (savedInstanceState == null) {
                customerSession.resetUsageTokens()
            }
            customerSession.addProductUsageTokenIfValid(TOKEN_PAYMENT_SESSION)
        } catch (illegalState: IllegalStateException) {
            paymentSessionListener = null
            return false
        }

        this.config = paymentSessionConfig
        paymentSessionListener = listener

        paymentSessionData = savedInstanceState?.getParcelable(STATE_PAYMENT_SESSION_DATA)
            ?: PaymentSessionData(paymentSessionConfig)

        if (paymentSessionConfig.shouldPrefetchCustomer) {
            fetchCustomer()
        }

        return true
    }

    /**
     * Launch the [PaymentMethodsActivity] to allow the user to select a payment method,
     * or to add a new one.
     *
     * The initial selected Payment Method ID uses the following logic.
     *
     *  1. If {@param userSelectedPaymentMethodId} is specified, use that
     *  2. If the instance's [PaymentSessionData.paymentMethod] is non-null, use that
     *  3. If the instance's [PaymentSessionPrefs.getSelectedPaymentMethodId] is non-null, use that
     *  4. Otherwise, choose the most recently added Payment Method
     *
     * See [getSelectedPaymentMethodId]
     *
     * @param selectedPaymentMethodId if non-null, the ID of the Payment Method that should be
     * initially selected on the Payment Method selection screen
     */
    fun presentPaymentMethodSelection(selectedPaymentMethodId: String? = null) {
        paymentMethodsActivityStarter.startForResult(
            PaymentMethodsActivityStarter.Args.Builder()
                .setInitialPaymentMethodId(
                    getSelectedPaymentMethodId(selectedPaymentMethodId))
                .setAddPaymentMethodFooter(config?.addPaymentMethodFooterLayoutId ?: 0)
                .setIsPaymentSessionActive(true)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setPaymentMethodTypes(config?.paymentMethodTypes.orEmpty())
                .setWindowFlags(config?.windowFlags)
                .setBillingAddressFields(config?.billingAddressFields ?: BillingAddressFields.None)
                .build()
        )
    }

    @VisibleForTesting
    internal fun getSelectedPaymentMethodId(userSelectedPaymentMethodId: String?): String? {
        return userSelectedPaymentMethodId
            ?: if (paymentSessionData.paymentMethod != null) {
                paymentSessionData.paymentMethod?.id
            } else {
                customerSession.cachedCustomer?.id?.let { customerId ->
                    paymentSessionPrefs.getSelectedPaymentMethodId(customerId)
                }
            }
    }

    /**
     * Save the data associated with this PaymentSession. This should be called in the host's
     * `onSaveInstanceState(Bundle)` method.
     *
     * @param outState the host activity's outgoing `Bundle`
     */
    fun savePaymentSessionInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_PAYMENT_SESSION_DATA, paymentSessionData)
    }

    /**
     * Set the cart total for this PaymentSession. This should not include shipping costs.
     *
     * @param cartTotal the current total price for all non-shipping and non-tax items in
     * a customer's cart
     */
    fun setCartTotal(@IntRange(from = 0) cartTotal: Long) {
        paymentSessionData = paymentSessionData.copy(cartTotal = cartTotal)
    }

    /**
     * Launch the [PaymentFlowActivity] to allow the user to fill in payment details.
     */
    fun presentShippingFlow() {
        paymentFlowActivityStarter.startForResult(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(config)
                .setPaymentSessionData(paymentSessionData)
                .setIsPaymentSessionActive(true)
                .setWindowFlags(config?.windowFlags)
                .build()
        )
    }

    /**
     * Should be called during the host `Activity`'s onDestroy to detach listeners.
     */
    fun onDestroy() {
        paymentSessionListener = null
    }

    private fun fetchCustomer() {
        paymentSessionListener?.onCommunicatingStateChanged(true)

        customerSession.retrieveCurrentCustomer(
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    dispatchUpdates()
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    paymentSessionListener?.onError(errorCode, errorMessage)
                    paymentSessionListener?.onCommunicatingStateChanged(false)
                }
            })
    }

    /**
     * Represents a listener for PaymentSession actions, used to update the host activity
     * when necessary.
     */
    interface PaymentSessionListener {
        /**
         * Notification method called when network communication is beginning or ending.
         *
         * @param isCommunicating `true` if communication is starting, `false` if it is stopping.
         */
        fun onCommunicatingStateChanged(isCommunicating: Boolean)

        /**
         * Notification method called when an error has occurred.
         *
         * @param errorCode a network code associated with the error
         * @param errorMessage a message associated with the error
         */
        fun onError(errorCode: Int, errorMessage: String)

        /**
         * Notification method called when the [PaymentSessionData] for this session has changed.
         *
         * @param data the updated [PaymentSessionData]
         */
        fun onPaymentSessionDataChanged(data: PaymentSessionData)
    }

    /**
     * Abstract implementation of [PaymentSessionListener] that holds a
     * [WeakReference] to an `Activity` object.
     */
    abstract class ActivityPaymentSessionListener<A : Activity>(
        activity: A
    ) : PaymentSessionListener {
        private val activityRef: WeakReference<A> = WeakReference(activity)

        protected val listenerActivity: A?
            get() = activityRef.get()
    }

    internal companion object {
        internal const val TOKEN_PAYMENT_SESSION: String = "PaymentSession"

        internal const val EXTRA_PAYMENT_SESSION_DATA: String = "extra_payment_session_data"

        private const val STATE_PAYMENT_SESSION_DATA: String = "state_payment_session_data"

        private val VALID_REQUEST_CODES = setOf(
            PaymentMethodsActivityStarter.REQUEST_CODE,
            PaymentFlowActivityStarter.REQUEST_CODE
        )
    }
}
