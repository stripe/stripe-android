package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.ui.SheetMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class SheetViewModel<TransitionTargetType>(
    internal val config: PaymentSheet.Configuration?,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO
) : ViewModel() {
    internal val customerConfig = config?.customer

    // a fatal error
    private val _fatal = MutableLiveData<Throwable>()
    internal val fatal: LiveData<Throwable> = _fatal

    protected val _isGooglePayReady = MutableLiveData<Boolean>()
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    protected val _launchGooglePay = MutableLiveData<StripeGooglePayContract.Args>()
    internal val launchGooglePay: LiveData<StripeGooglePayContract.Args> = _launchGooglePay

    protected val _paymentIntent = MutableLiveData<PaymentIntent?>()
    internal val paymentIntent: LiveData<PaymentIntent?> = _paymentIntent

    protected val _paymentMethods = MutableLiveData<List<PaymentMethod>>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    private val _savedSelection = MutableLiveData<SavedSelection>()
    private val savedSelection: LiveData<SavedSelection> = _savedSelection

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

    abstract val newCard: PaymentSelection.New.Card?

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
        fetchSavedSelection()
    }

    fun fetchFragmentConfig() = MediatorLiveData<FragmentConfig?>().also { configLiveData ->
        listOf(
            savedSelection,
            paymentIntent,
            paymentMethods,
            isGooglePayReady
        ).forEach { source ->
            configLiveData.addSource(source) {
                configLiveData.value = createFragmentConfig()
            }
        }
    }.distinctUntilChanged()

    private fun createFragmentConfig(): FragmentConfig? {
        val paymentIntentValue = paymentIntent.value
        val paymentMethodsValue = paymentMethods.value
        val isGooglePayReadyValue = isGooglePayReady.value
        val savedSelectionValue = savedSelection.value

        return if (
            paymentIntentValue != null &&
            paymentMethodsValue != null &&
            isGooglePayReadyValue != null &&
            savedSelectionValue != null
        ) {
            FragmentConfig(
                paymentIntent = paymentIntentValue,
                paymentMethods = paymentMethodsValue,
                isGooglePayReady = isGooglePayReadyValue,
                savedSelection = savedSelectionValue
            )
        } else {
            null
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

    private fun fetchSavedSelection() {
        viewModelScope.launch {
            val savedSelection = withContext(workContext) {
                prefsRepository.getSavedSelection()
            }
            _savedSelection.value = savedSelection
        }
    }

    sealed class UserMessage {
        abstract val message: String

        data class Error(
            override val message: String
        ) : UserMessage()
    }
}
