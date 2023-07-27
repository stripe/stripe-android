package com.stripe.android

import android.app.Application
import androidx.annotation.IntRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.StripeError
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.utils.requireApplication
import com.stripe.android.view.PaymentMethodsActivityStarter

internal class PaymentSessionViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    paymentSessionData: PaymentSessionData,
    private val customerSession: CustomerSession,
    private val paymentSessionPrefs: PaymentSessionPrefs = PaymentSessionPrefs.Default(application)
) : AndroidViewModel(application) {

    var paymentSessionData: PaymentSessionData = paymentSessionData
        set(value) {
            if (value != field) {
                field = value
                savedStateHandle.set(KEY_PAYMENT_SESSION_DATA, value)
                _paymentSessionDataLiveData.value = value
            }
        }

    private val _paymentSessionDataLiveData = MutableLiveData<PaymentSessionData>()
    val paymentSessionDataLiveData: LiveData<PaymentSessionData> = _paymentSessionDataLiveData

    init {
        // read from saved state handle
        savedStateHandle.get<PaymentSessionData>(KEY_PAYMENT_SESSION_DATA)?.let {
            this.paymentSessionData = it
        }
    }

    private val _networkState: MutableLiveData<NetworkState> = MutableLiveData()
    internal val networkState: LiveData<NetworkState> = _networkState

    @JvmSynthetic
    fun updateCartTotal(@IntRange(from = 0) cartTotal: Long) {
        paymentSessionData = paymentSessionData.copy(cartTotal = cartTotal)
    }

    fun clearPaymentMethod() {
        paymentSessionData = paymentSessionData.copy(paymentMethod = null)
    }

    @JvmSynthetic
    fun onCompleted() {
    }

    @JvmSynthetic
    fun fetchCustomer(isInitialFetch: Boolean = false): LiveData<FetchCustomerResult> {
        _networkState.value = NetworkState.Active

        val resultData: MutableLiveData<FetchCustomerResult> = MutableLiveData()
        customerSession.retrieveCurrentCustomer(
            productUsage = setOf(PaymentSession.PRODUCT_TOKEN),
            listener = object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                    onCustomerRetrieved(
                        customer.id,
                        isInitialFetch
                    ) {
                        _networkState.value = NetworkState.Inactive
                        resultData.value = FetchCustomerResult.Success
                    }
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                    _networkState.value = NetworkState.Inactive
                    resultData.value = FetchCustomerResult.Error(
                        errorCode,
                        errorMessage,
                        stripeError
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
            paymentSessionPrefs.getPaymentMethod(customerId)?.let {
                if (it is PaymentSessionPrefs.SelectedPaymentMethod.GooglePay) {
                    paymentSessionData = paymentSessionData.copy(
                        useGooglePay = true
                    )
                    onComplete()
                } else {
                    fetchCustomerPaymentMethod(it.stringValue) { paymentMethod ->
                        paymentMethod?.let {
                            paymentSessionData = paymentSessionData.copy(
                                paymentMethod = it
                            )
                        }
                        onComplete()
                    }
                }
            } ?: onComplete()
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
    fun getSelectedPaymentMethod(
        userSelectedPaymentMethodId: String? = null,
    ): PaymentSessionPrefs.SelectedPaymentMethod? {
        return if (paymentSessionData.useGooglePay) {
            null
        } else {
            PaymentSessionPrefs.SelectedPaymentMethod.fromString(userSelectedPaymentMethodId)
                ?: if (paymentSessionData.paymentMethod != null) {
                    PaymentSessionPrefs.SelectedPaymentMethod.fromString(
                        paymentSessionData.paymentMethod?.id
                    )
                } else {
                    customerSession.cachedCustomer?.id?.let { customerId ->
                        paymentSessionPrefs.getPaymentMethod(customerId)
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
            val selectedPaymentMethod = if (useGooglePay) {
                PaymentSessionPrefs.SelectedPaymentMethod.GooglePay
            } else {
                paymentMethod?.id?.let {
                    PaymentSessionPrefs.SelectedPaymentMethod.Saved(
                        it
                    )
                }
            }
            paymentSessionPrefs.savePaymentMethod(
                customerId,
                selectedPaymentMethod
            )
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
        _paymentSessionDataLiveData.value = paymentSessionData
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
        private val paymentSessionData: PaymentSessionData,
        private val customerSession: CustomerSession
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return PaymentSessionViewModel(
                extras.requireApplication(),
                extras.createSavedStateHandle(),
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
