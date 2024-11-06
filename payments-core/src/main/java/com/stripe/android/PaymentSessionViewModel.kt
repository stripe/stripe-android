package com.stripe.android

import android.app.Application
import androidx.annotation.IntRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.core.StripeError
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.PaymentMethodsActivityStarter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
                _paymentSessionDataStateFlow.tryEmit(value)
            }
        }

    private val _paymentSessionDataStateFlow = MutableSharedFlow<PaymentSessionData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val paymentSessionDataStateFlow: SharedFlow<PaymentSessionData> = _paymentSessionDataStateFlow.asSharedFlow()

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        // read from saved state handle
        savedStateHandle.get<PaymentSessionData>(KEY_PAYMENT_SESSION_DATA)?.let {
            this.paymentSessionData = it
        }
    }

    private val _networkState: MutableStateFlow<NetworkState> = MutableStateFlow(NetworkState.Inactive)
    internal val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

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
    suspend fun fetchCustomer(isInitialFetch: Boolean = false): FetchCustomerResult =
        suspendCoroutine { continuation ->
            _networkState.value = NetworkState.Active

            customerSession.retrieveCurrentCustomer(
                productUsage = setOf(PaymentSession.PRODUCT_TOKEN),
                listener = object : CustomerSession.CustomerRetrievalListener {
                    override fun onCustomerRetrieved(customer: Customer) {
                        onCustomerRetrieved(
                            customer.id,
                            isInitialFetch
                        ) {
                            _networkState.value = NetworkState.Inactive
                            continuation.resume(FetchCustomerResult.Success)
                        }
                    }

                    override fun onError(
                        errorCode: Int,
                        errorMessage: String,
                        stripeError: StripeError?
                    ) {
                        _networkState.value = NetworkState.Inactive
                        continuation.resume(
                            FetchCustomerResult.Error(
                                errorCode,
                                errorMessage,
                                stripeError
                            )
                        )
                    }
                }
            )
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
        userSelectedPaymentMethodId: String? = null
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
        _paymentSessionDataStateFlow.tryEmit(paymentSessionData)
    }

    sealed class FetchCustomerResult {
        data object Success : FetchCustomerResult()

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
