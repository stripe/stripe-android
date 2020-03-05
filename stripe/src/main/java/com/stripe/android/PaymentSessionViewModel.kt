package com.stripe.android

import android.app.Application
import androidx.annotation.IntRange
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter

internal class PaymentSessionViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    paymentSessionData: PaymentSessionData,
    private val customerSession: CustomerSession,
    private val paymentSessionPrefs: PaymentSessionPrefs =
        PaymentSessionPrefs.create(application.applicationContext)
) : AndroidViewModel(application) {

    var paymentSessionData: PaymentSessionData = paymentSessionData
        set(value) {
            if (value != field) {
                field = value
                savedStateHandle.set(KEY_PAYMENT_SESSION_DATA, value)
                mutablePaymentSessionDataLiveData.value = value
            }
        }

    private val mutablePaymentSessionDataLiveData: MutableLiveData<PaymentSessionData> = MutableLiveData()
    val paymentSessionDataLiveData: LiveData<PaymentSessionData> = mutablePaymentSessionDataLiveData

    init {
        customerSession.resetUsageTokens()
        customerSession.addProductUsageTokenIfValid(PaymentSession.TOKEN_PAYMENT_SESSION)

        // read from saved state handle
        savedStateHandle.get<PaymentSessionData>(KEY_PAYMENT_SESSION_DATA)?.let {
            this.paymentSessionData = it
        }
    }

    private val mutableNetworkState: MutableLiveData<NetworkState> = MutableLiveData()
    internal val networkState: LiveData<NetworkState> = mutableNetworkState

    @JvmSynthetic
    fun updateCartTotal(@IntRange(from = 0) cartTotal: Long) {
        paymentSessionData = paymentSessionData.copy(cartTotal = cartTotal)
    }

    fun clearPaymentMethod() {
        paymentSessionData = paymentSessionData.copy(paymentMethod = null)
    }

    @JvmSynthetic
    fun onCompleted() {
        customerSession.resetUsageTokens()
    }

    @JvmSynthetic
    fun fetchCustomer(isInitialFetch: Boolean = false): LiveData<FetchCustomerResult> {
        mutableNetworkState.value = NetworkState.Active

        val resultData: MutableLiveData<FetchCustomerResult> = MutableLiveData()
        customerSession.retrieveCurrentCustomer(
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    onCustomerRetrieved(
                        customer.id,
                        isInitialFetch
                    ) {
                        mutableNetworkState.value = NetworkState.Inactive
                        resultData.value = FetchCustomerResult.Success
                    }
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    mutableNetworkState.value = NetworkState.Inactive
                    resultData.value = FetchCustomerResult.Error(
                        errorCode, errorMessage, stripeError
                    )
                }
            }
        )

        return resultData
    }

    @JvmSynthetic
    internal fun onCustomerRetrieved(
        customerId: String?,
        isInitialFetch: Boolean = false,
        onComplete: () -> Unit
    ) {
        if (isInitialFetch) {
            fetchCustomerPaymentMethod(
                paymentMethodId = paymentSessionPrefs.getPaymentMethodId(customerId)
            ) { paymentMethod ->
                paymentMethod?.let {
                    paymentSessionData = paymentSessionData.copy(
                        paymentMethod = it
                    )
                }
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    private fun fetchCustomerPaymentMethod(
        paymentMethodId: String?,
        onComplete: (paymentMethod: PaymentMethod?) -> Unit
    ) {
        if (paymentMethodId != null) {
            // fetch the maximum number of payment methods and attempt to find the
            // payment method id in the list
            customerSession.getPaymentMethods(
                paymentMethodType = PaymentMethod.Type.Card,
                limit = MAX_PAYMENT_METHODS_LIMIT,
                listener = object : CustomerSession.PaymentMethodsRetrievalListener {
                    override fun onPaymentMethodsRetrieved(
                        paymentMethods: List<PaymentMethod>
                    ) {
                        onComplete(
                            paymentMethods.firstOrNull { it.id == paymentMethodId }
                        )
                    }

                    override fun onError(
                        errorCode: Int,
                        errorMessage: String,
                        stripeError: StripeError?
                    ) {
                        // if an error is encountered, treat it as a no-op
                        onComplete(null)
                    }
                }
            )
        } else {
            onComplete(null)
        }
    }

    @JvmSynthetic
    fun getSelectedPaymentMethodId(userSelectedPaymentMethodId: String? = null): String? {
        return if (paymentSessionData.useGooglePay) {
            null
        } else {
            userSelectedPaymentMethodId
                ?: if (paymentSessionData.paymentMethod != null) {
                    paymentSessionData.paymentMethod?.id
                } else {
                    customerSession.cachedCustomer?.id?.let { customerId ->
                        paymentSessionPrefs.getPaymentMethodId(customerId)
                    }
                }
        }
    }

    @JvmSynthetic
    fun onPaymentMethodResult(result: PaymentMethodsActivityStarter.Result?) {
        persistPaymentMethodResult(
            paymentMethod = result?.paymentMethod,
            useGooglePay = result?.useGooglePay ?: false
        )
    }

    private fun persistPaymentMethodResult(
        paymentMethod: PaymentMethod?,
        useGooglePay: Boolean
    ) {
        customerSession.cachedCustomer?.id?.let { customerId ->
            paymentSessionPrefs.savePaymentMethodId(customerId, paymentMethod?.id)
        }
        paymentSessionData = paymentSessionData.copy(
            paymentMethod = paymentMethod,
            useGooglePay = useGooglePay
        )
    }

    @JvmSynthetic
    fun onPaymentFlowResult(paymentSessionData: PaymentSessionData) {
        this.paymentSessionData = paymentSessionData
    }

    @JvmSynthetic
    fun onListenerAttached() {
        mutablePaymentSessionDataLiveData.value = paymentSessionData
    }

    sealed class FetchCustomerResult {
        object Success : FetchCustomerResult()

        data class Error(
            val errorCode: Int,
            val errorMessage: String,
            val stripeError: StripeError?
        ) : FetchCustomerResult()
    }

    enum class NetworkState {
        Active,
        Inactive
    }

    internal class Factory(
        private val application: Application,
        owner: SavedStateRegistryOwner,
        private val paymentSessionData: PaymentSessionData,
        private val customerSession: CustomerSession
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return PaymentSessionViewModel(
                application,
                handle,
                paymentSessionData,
                customerSession
            ) as T
        }
    }

    internal companion object {
        internal const val KEY_PAYMENT_SESSION_DATA = "key_payment_session_data"

        private const val MAX_PAYMENT_METHODS_LIMIT = 100
    }
}
