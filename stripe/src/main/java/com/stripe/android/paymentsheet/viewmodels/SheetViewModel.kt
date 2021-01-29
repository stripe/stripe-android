package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.GooglePayRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class SheetViewModel<TransitionTargetType>(
    internal val config: PaymentSheet.Configuration?,
    private val isGooglePayEnabled: Boolean,
    private val googlePayRepository: GooglePayRepository,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {
    internal val customerConfig = config?.customer

    // a fatal error
    private val _fatal = MutableLiveData<Throwable>()
    internal val fatal: LiveData<Throwable> = _fatal

    private val _isGooglePayReady = MutableLiveData<Boolean>()
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    protected val _launchGooglePay = MutableLiveData<StripeGooglePayContract.Args>()
    internal val launchGooglePay: LiveData<StripeGooglePayContract.Args> = _launchGooglePay

    protected val _paymentIntent = MutableLiveData<PaymentIntent?>()
    internal val paymentIntent: LiveData<PaymentIntent?> = _paymentIntent

    protected val _paymentMethods = MutableLiveData<List<PaymentMethod>>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    private val _transition = MutableLiveData<TransitionTargetType?>(null)
    internal val transition: LiveData<TransitionTargetType?> = _transition

    private val _selection = MutableLiveData<PaymentSelection?>()
    internal val selection: LiveData<PaymentSelection?> = _selection

    private val _sheetMode = MutableLiveData<SheetMode>()
    val sheetMode: LiveData<SheetMode> = _sheetMode.distinctUntilChanged()

    protected val _processing = MutableLiveData(true)
    val processing: LiveData<Boolean> = _processing

    // a message shown to the user
    protected val _userMessage = MutableLiveData<UserMessage?>()
    internal val userMessage: LiveData<UserMessage?> = _userMessage

    val ctaEnabled: LiveData<Boolean> = processing.switchMap { isProcessing ->
        transition.switchMap { transitionTarget ->
            selection.switchMap { paymentSelection ->
                MutableLiveData(
                    !isProcessing && transitionTarget != null && paymentSelection != null
                )
            }
        }
    }

    init {
        fetchIsGooglePayReady()
    }

    fun fetchAddPaymentMethodConfig() = liveData {
        emitSource(
            MediatorLiveData<AddPaymentMethodConfig?>().also { configLiveData ->
                listOf(paymentIntent, paymentMethods, isGooglePayReady).forEach { source ->
                    configLiveData.addSource(source) {
                        configLiveData.value = createAddPaymentMethodConfig()
                    }
                }
            }.distinctUntilChanged()
        )
    }

    private fun createAddPaymentMethodConfig(): AddPaymentMethodConfig? {
        val paymentIntentValue = paymentIntent.value
        val paymentMethodsValue = paymentMethods.value
        val isGooglePayReadyValue = isGooglePayReady.value

        return if (
            paymentIntentValue != null &&
            paymentMethodsValue != null &&
            isGooglePayReadyValue != null
        ) {
            AddPaymentMethodConfig(
                paymentIntent = paymentIntentValue,
                paymentMethods = paymentMethodsValue,
                isGooglePayReady = isGooglePayReadyValue
            )
        } else {
            null
        }
    }

    fun fetchIsGooglePayReady() {
        if (isGooglePayReady.value == null) {
            if (isGooglePayEnabled) {
                viewModelScope.launch {
                    withContext(workContext) {
                        _isGooglePayReady.postValue(
                            googlePayRepository.isReady().first()
                        )
                    }
                }
            } else {
                _isGooglePayReady.value = false
            }
        }
    }

    fun updateMode(mode: SheetMode) {
        _sheetMode.postValue(mode)
    }

    fun transitionTo(target: TransitionTargetType) {
        _userMessage.value = null
        _transition.postValue(target)
    }

    fun onFatal(throwable: Throwable) {
        _fatal.postValue(throwable)
    }

    fun onApiError(errorMessage: String?) {
        _userMessage.value = errorMessage?.let { UserMessage.Error(it) }
        _processing.value = false
    }

    fun updateSelection(selection: PaymentSelection?) {
        _selection.value = selection
    }

    fun onBackPressed() {
        _userMessage.value = null
    }

    fun getSavedSelection() = liveData {
        emit(
            withContext(workContext) {
                prefsRepository.getSavedSelection()
            }
        )
    }

    sealed class UserMessage {
        abstract val message: String

        data class Error(
            override val message: String
        ) : UserMessage()
    }
}
