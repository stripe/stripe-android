package com.stripe.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.ActivityStarter
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
    application: Application,
    viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    private val config: PaymentSessionConfig,
    customerSession: CustomerSession,
    private val paymentMethodsActivityStarter:
    ActivityStarter<PaymentMethodsActivity, PaymentMethodsActivityStarter.Args>,
    private val paymentFlowActivityStarter:
    ActivityStarter<PaymentFlowActivity, PaymentFlowActivityStarter.Args>,
    paymentSessionData: PaymentSessionData = PaymentSessionData(config)
) {
    internal val viewModel: PaymentSessionViewModel =
        ViewModelProvider(
            viewModelStoreOwner,
            PaymentSessionViewModel.Factory(
                application,
                savedStateRegistryOwner,
                paymentSessionData,
                customerSession
            )
        )[PaymentSessionViewModel::class.java]

    /**
     * @return the data associated with the instance of this class.
     */
    val paymentSessionData: PaymentSessionData
        get() = viewModel.paymentSessionData

    @JvmSynthetic
    internal var listener: PaymentSessionListener? = null

    private val lifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            listener = null
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * Create a PaymentSession attached to the given host Activity.
     *
     * @param activity a `ComponentActivity` from which to launch other Stripe Activities. This
     * Activity will receive results in
     * `Activity#onActivityResult(int, int, Intent)` that should be
     * passed back to this session.
     * @param config a [PaymentSessionConfig] that configures this [PaymentSession] instance
     */
    constructor(activity: ComponentActivity, config: PaymentSessionConfig) : this(
        activity.applicationContext,
        activity.application,
        activity,
        activity,
        activity,
        config,
        CustomerSession.getInstance(),
        PaymentMethodsActivityStarter(activity),
        PaymentFlowActivityStarter(activity, config)
    )

    /**
     * Create a PaymentSession attached to the given host Fragment.
     *
     * @param fragment a `Fragment` from which to launch other Stripe Activities. This
     * Fragment will receive results in `Fragment#onActivityResult(int, int, Intent)` that should be
     * passed back to this session.
     * @param config a [PaymentSessionConfig] that configures this [PaymentSession] instance
     */
    constructor(fragment: Fragment, config: PaymentSessionConfig) : this(
        fragment.requireActivity().applicationContext,
        fragment.requireActivity().application,
        fragment,
        fragment,
        fragment,
        config,
        CustomerSession.getInstance(),
        PaymentMethodsActivityStarter(fragment),
        PaymentFlowActivityStarter(fragment, config)
    )

    /**
     * Notify this payment session that it is complete
     */
    fun onCompleted() {
        viewModel.onCompleted()
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
                    viewModel.paymentSessionData = paymentSessionData
                    listener?.onPaymentSessionDataChanged(paymentSessionData)
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
        val result = PaymentMethodsActivityStarter.Result.fromIntent(data)
        persistPaymentMethodResult(
            paymentMethod = result?.paymentMethod,
            useGooglePay = result?.useGooglePay ?: false
        )
        dispatchUpdates()
    }

    private fun dispatchUpdates() {
        listener?.onPaymentSessionDataChanged(paymentSessionData)
        listener?.onCommunicatingStateChanged(false)
    }

    private fun persistPaymentMethodResult(
        paymentMethod: PaymentMethod?,
        useGooglePay: Boolean
    ) {
        viewModel.persistPaymentMethodResult(paymentMethod, useGooglePay)
    }

    /**
     * Initialize the [PaymentSession] with a [PaymentSessionListener] to be notified of
     * data changes. The reference to the [listener] will be released when the host (i.e.
     * `Activity` or `Fragment`) is destroyed.
     *
     * If the [PaymentSessionConfig.shouldPrefetchCustomer] is true, a new Customer instance
     * will be fetched. Otherwise, the [listener] will be immediately called with the current
     * [paymentSessionData].
     *
     * @param listener a [PaymentSessionListener]
     */
    fun init(
        listener: PaymentSessionListener
    ) {
        this.listener = listener

        if (config.shouldPrefetchCustomer) {
            fetchCustomer()
        } else {
            dispatchUpdates()
        }
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
                .setAddPaymentMethodFooter(config.addPaymentMethodFooterLayoutId)
                .setIsPaymentSessionActive(true)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setPaymentMethodTypes(config.paymentMethodTypes)
                .setShouldShowGooglePay(config.shouldShowGooglePay)
                .setWindowFlags(config.windowFlags)
                .setBillingAddressFields(config.billingAddressFields)
                .build()
        )
    }

    @VisibleForTesting
    internal fun getSelectedPaymentMethodId(userSelectedPaymentMethodId: String?): String? {
        return viewModel.getSelectedPaymentMethodId(userSelectedPaymentMethodId)
    }

    /**
     * Set the cart total for this PaymentSession. This should not include shipping costs.
     *
     * @param cartTotal the current total price for all non-shipping and non-tax items in
     * a customer's cart
     */
    fun setCartTotal(@IntRange(from = 0) cartTotal: Long) {
        viewModel.updateCartTotal(cartTotal)
    }

    /**
     * Launch the [PaymentFlowActivity] to allow the user to fill in payment details.
     */
    fun presentShippingFlow() {
        paymentFlowActivityStarter.startForResult(
            PaymentFlowActivityStarter.Args(
                paymentSessionConfig = config,
                paymentSessionData = paymentSessionData,
                isPaymentSessionActive = true,
                windowFlags = config.windowFlags
            )
        )
    }

    private fun fetchCustomer() {
        listener?.onCommunicatingStateChanged(true)

        viewModel.fetchCustomer().observe(lifecycleOwner, Observer {
            when (it) {
                PaymentSessionViewModel.FetchCustomerResult.Success -> {
                    dispatchUpdates()
                }
                is PaymentSessionViewModel.FetchCustomerResult.Error -> {
                    listener?.onError(it.errorCode, it.errorMessage)
                    listener?.onCommunicatingStateChanged(false)
                }
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

        private val VALID_REQUEST_CODES = setOf(
            PaymentMethodsActivityStarter.REQUEST_CODE,
            PaymentFlowActivityStarter.REQUEST_CODE
        )
    }
}
