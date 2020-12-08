package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.GooglePayRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.AddPaymentMethodConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class SheetViewModel<TransitionTargetType, ViewStateType>(
    internal val customerConfig: PaymentSheet.CustomerConfiguration?,
    private val isGooglePayEnabled: Boolean,
    private val googlePayRepository: GooglePayRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {
    // a fatal error
    private val mutableFatal = MutableLiveData<Throwable>()
    internal val fatal: LiveData<Throwable> = mutableFatal

    private val mutableIsGooglePayReady = MutableLiveData<Boolean>()
    internal val isGooglePayReady: LiveData<Boolean> = mutableIsGooglePayReady.distinctUntilChanged()

    protected val mutablePaymentIntent = MutableLiveData<PaymentIntent?>()
    internal val paymentIntent: LiveData<PaymentIntent?> = mutablePaymentIntent

    protected val mutablePaymentMethods = MutableLiveData<List<PaymentMethod>>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = mutablePaymentMethods

    private val mutableTransition = MutableLiveData<TransitionTargetType>()
    internal val transition: LiveData<TransitionTargetType> = mutableTransition

    private val mutableSelection = MutableLiveData<PaymentSelection?>()
    internal val selection: LiveData<PaymentSelection?> = mutableSelection

    private val mutableSheetMode = MutableLiveData<SheetMode>()
    val sheetMode: LiveData<SheetMode> = mutableSheetMode.distinctUntilChanged()

    protected val mutableProcessing = MutableLiveData(false)
    val processing = mutableProcessing.distinctUntilChanged()

    protected val mutableViewState = MutableLiveData<ViewStateType>(null)
    internal val viewState: LiveData<ViewStateType> = mutableViewState.distinctUntilChanged()

    // a message shown to the user
    protected val mutableUserMessage = MutableLiveData<UserMessage?>()
    internal val userMessage: LiveData<UserMessage?> = mutableUserMessage

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
                        mutableIsGooglePayReady.postValue(
                            googlePayRepository.isReady().filterNotNull().first()
                        )
                    }
                }
            } else {
                mutableIsGooglePayReady.value = false
            }
        }
    }

    fun updateMode(mode: SheetMode) {
        mutableSheetMode.postValue(mode)
    }

    fun transitionTo(target: TransitionTargetType) {
        mutableUserMessage.value = null
        mutableTransition.postValue(target)
    }

    fun onFatal(throwable: Throwable) {
        mutableFatal.postValue(throwable)
    }

    fun onApiError(errorMessage: String?) {
        mutableUserMessage.value = errorMessage?.let { UserMessage.Error(it) }
        mutableProcessing.value = false
    }

    fun updateSelection(selection: PaymentSelection?) {
        mutableSelection.value = selection
    }

    fun onBackPressed() {
        mutableUserMessage.value = null
    }

    sealed class UserMessage {
        abstract val message: String

        data class Error(
            override val message: String
        ) : UserMessage()
    }
}
