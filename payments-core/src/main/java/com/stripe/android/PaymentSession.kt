package com.stripe.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.annotation.IntRange
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.PaymentSession.PaymentSessionListener
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentFlowActivityStarter
import com.stripe.android.view.PaymentMethodsActivity
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.coroutines.launch

/**
 * Represents a single start-to-finish payment operation.
 *
 * See [Using Android basic integration](https://stripe.com/docs/mobile/android/basic) for more
 * information.
 *
 * If [PaymentSessionConfig.shouldPrefetchCustomer] is `true`, and the customer has previously
 * selected a payment method, [PaymentSessionData.paymentMethod] will be updated with the
 * payment method and [PaymentSessionListener.onPaymentSessionDataChanged] will be called.
 */
@Deprecated(
    "Please use Mobile Payment Element instead. If you already using PaymentSession, learn how to migrate here:" +
        " https://docs.stripe.com/payments/mobile/migrating-to-mobile-payment-element-from-basic-integration"
)
class PaymentSession @VisibleForTesting internal constructor(
    private val context: Context,
    viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
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
                paymentSessionData,
                customerSession
            )
        )[PaymentSessionViewModel::class.java]

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

        lifecycleOwner.lifecycleScope.launch {
            viewModel.networkState.collect { networkState ->
                listener?.onCommunicatingStateChanged(
                    when (networkState) {
                        PaymentSessionViewModel.NetworkState.Active -> true
                        PaymentSessionViewModel.NetworkState.Inactive -> false
                    }
                )
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            viewModel.paymentSessionDataStateFlow.collect { sessionData ->
                listener?.onPaymentSessionDataChanged(sessionData)
            }
        }
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
    fun handlePaymentData(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (data == null || !isValidRequestCode(requestCode)) {
            return false
        }

        return when (resultCode) {
            Activity.RESULT_CANCELED -> {
                if (requestCode == PaymentMethodsActivityStarter.REQUEST_CODE) {
                    // If resultCode of `PaymentMethodsActivity` is `Activity.RESULT_CANCELED`,
                    // the user tapped back via the toolbar or device back button.
                    onPaymentMethodResult(data)
                } else {
                    fetchCustomer()
                }
                false
            }

            Activity.RESULT_OK -> when (requestCode) {
                PaymentMethodsActivityStarter.REQUEST_CODE -> {
                    onPaymentMethodResult(data)
                    true
                }

                PaymentFlowActivityStarter.REQUEST_CODE -> {
                    data.getParcelableExtra<PaymentSessionData>(EXTRA_PAYMENT_SESSION_DATA)?.let {
                        viewModel.onPaymentFlowResult(it)
                    }
                    true
                }

                else -> {
                    false
                }
            }

            else -> false
        }
    }

    private fun onPaymentMethodResult(data: Intent) {
        viewModel.onPaymentMethodResult(PaymentMethodsActivityStarter.Result.fromIntent(data))
    }

    /**
     * Initialize the [PaymentSession] with a [PaymentSessionListener] to be notified of
     * data changes. The reference to the [listener] will be released when the host (i.e.
     * `Activity` or `Fragment`) is destroyed.
     *
     * The [listener] will be immediately called with the current [PaymentSessionData].
     *
     * If the [PaymentSessionConfig.shouldPrefetchCustomer] is true, a new `Customer` instance
     * will be fetched.
     *
     * @param listener a [PaymentSessionListener]
     */
    fun init(
        listener: PaymentSessionListener
    ) {
        this.listener = listener

        viewModel.onListenerAttached()

        if (config.shouldPrefetchCustomer) {
            fetchCustomer(isInitialFetch = true)
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
     *  3. If the instance's [PaymentSessionPrefs.getPaymentMethod] is non-null, use that
     *  4. Otherwise, choose the most recently added Payment Method
     *
     * @param selectedPaymentMethodId if non-null, the ID of the Payment Method that should be
     * initially selected on the Payment Method selection screen
     */
    fun presentPaymentMethodSelection(selectedPaymentMethodId: String? = null) {
        val selection = viewModel.getSelectedPaymentMethod(selectedPaymentMethodId)
        val useGooglePay = if (selection is PaymentSessionPrefs.SelectedPaymentMethod.GooglePay) {
            true
        } else {
            viewModel.paymentSessionData.useGooglePay
        }
        paymentMethodsActivityStarter.startForResult(
            PaymentMethodsActivityStarter.Args.Builder()
                .setInitialPaymentMethodId(selection?.stringValue)
                .setPaymentMethodsFooter(config.paymentMethodsFooterLayoutId)
                .setAddPaymentMethodFooter(config.addPaymentMethodFooterLayoutId)
                .setIsPaymentSessionActive(true)
                .setPaymentConfiguration(PaymentConfiguration.getInstance(context))
                .setPaymentMethodTypes(config.paymentMethodTypes)
                .setShouldShowGooglePay(config.shouldShowGooglePay)
                .setWindowFlags(config.windowFlags)
                .setBillingAddressFields(config.billingAddressFields)
                .setUseGooglePay(useGooglePay)
                .setCanDeletePaymentMethods(config.canDeletePaymentMethods)
                .build()
        )
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
                paymentSessionData = viewModel.paymentSessionData,
                isPaymentSessionActive = true,
                windowFlags = config.windowFlags
            )
        )
    }

    /**
     * Clear the payment method associated with this [PaymentSession] in [PaymentSessionData].
     *
     * Will trigger a call to [PaymentSessionListener.onPaymentSessionDataChanged].
     */
    fun clearPaymentMethod() {
        viewModel.clearPaymentMethod()
    }

    private fun fetchCustomer(isInitialFetch: Boolean = false) {
        lifecycleOwner.lifecycleScope.launch {
            val result = viewModel.fetchCustomer(isInitialFetch)

            if (result is PaymentSessionViewModel.FetchCustomerResult.Error) {
                listener?.onError(result.errorCode, result.errorMessage)
            }
        }
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

    internal companion object {
        internal const val PRODUCT_TOKEN: String = "PaymentSession"

        internal const val EXTRA_PAYMENT_SESSION_DATA: String = "extra_payment_session_data"

        private fun isValidRequestCode(
            requestCode: Int
        ) = VALID_REQUEST_CODES.contains(requestCode)

        private val VALID_REQUEST_CODES = setOf(
            PaymentMethodsActivityStarter.REQUEST_CODE,
            PaymentFlowActivityStarter.REQUEST_CODE
        )
    }
}
