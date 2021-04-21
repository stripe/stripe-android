package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.stripe.android.R
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.googlepay.StripeGooglePayContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.BaseAddCardFragment
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class BaseSheetViewModel<TransitionTargetType>(
    application: Application,
    internal val config: PaymentSheet.Configuration?,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO
) : AndroidViewModel(application) {
    internal val customerConfig = config?.customer

    // a fatal error
    protected val _fatal = MutableLiveData<Throwable>()

    protected val _isGooglePayReady = MutableLiveData<Boolean>()
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    protected val _launchGooglePay = MutableLiveData<Event<StripeGooglePayContract.Args>>()
    internal val launchGooglePay: LiveData<Event<StripeGooglePayContract.Args>> = _launchGooglePay

    protected val _paymentIntent = MutableLiveData<PaymentIntent?>()
    internal val paymentIntent: LiveData<PaymentIntent?> = _paymentIntent

    protected val _paymentMethods = MutableLiveData<List<PaymentMethod>>()
    internal val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    /**
     * Request to retrieve the value from the repository happens when initialize any fragment
     * and any fragment will re-update when the result comes back.
     * Represents what the user last selects (add or buy) on the
     * [PaymentOptionsActivity]/[PaymentSheetActivity], and saved/restored from the preferences.
     */
    private val _savedSelection = MutableLiveData<SavedSelection>()
    private val savedSelection: LiveData<SavedSelection> = _savedSelection

    private val _transition = MutableLiveData<Event<TransitionTargetType?>>(Event(null))
    internal val transition: LiveData<Event<TransitionTargetType?>> = _transition

    /**
     * On [BaseAddCardFragment] this is set every time the details in the add
     * card fragment is determined to be valid (not necessarily selected)
     * On [BasePaymentMethodsListFragment] this is set when a user selects one of the options
     */
    private val _selection = MutableLiveData<PaymentSelection?>()
    internal val selection: LiveData<PaymentSelection?> = _selection

    @VisibleForTesting
    internal val _processing = MutableLiveData(true)
    val processing: LiveData<Boolean> = _processing

    // a message shown to the user
    protected val _userMessage = MutableLiveData<UserMessage?>()
    internal val userMessage: LiveData<UserMessage?> = _userMessage

    /**
     * This should be initialized from the starter args, and then from that
     * point forward it will be the last valid card seen or entered in the add card view.
     * In contrast to selection, this field will not be updated by the list fragment. On the
     * Payment Sheet it is used to save a new card that is added for when you go back to the list
     * and reopen the card view. It is used on the Payment Options sheet similar to what is
     * described above, and when you have an unsaved card.
     */
    abstract var newCard: PaymentSelection.New.Card?

    abstract fun onFatal(throwable: Throwable)

    val ctaEnabled: LiveData<Boolean> = processing.switchMap { isProcessing ->
        selection.switchMap { paymentSelection ->
            MutableLiveData(!isProcessing && paymentSelection != null)
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

    fun transitionTo(target: TransitionTargetType) {
        _userMessage.value = null
        _transition.postValue(Event(target))
    }

    fun onApiError(errorMessage: String?) {
        _userMessage.value = errorMessage?.let { UserMessage.Error(it) }
        _processing.value = false
    }

    fun onApiError(throwable: Throwable) {
        when (throwable) {
            is APIConnectionException -> {
                onApiError(
                    getApplication<Application>().resources.getString(
                        R.string.stripe_failure_connection_error
                    )
                )
            }
            else -> {
                onApiError(throwable.localizedMessage)
            }
        }
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

    abstract fun onUserCancel()

    sealed class UserMessage {
        abstract val message: String

        data class Error(
            override val message: String
        ) : UserMessage()
    }

    /**
     * Used as a wrapper for data that is exposed via a LiveData that represents an event.
     * From https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
     * TODO(brnunes): Migrate to Flows once stable: https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
     */
    class Event<out T>(private val content: T) {

        var hasBeenHandled = false
            private set // Allow external read but not write

        /**
         * Returns the content and prevents its use again.
         */
        fun getContentIfNotHandled(): T? {
            return if (hasBeenHandled) {
                null
            } else {
                hasBeenHandled = true
                content
            }
        }

        /**
         * Returns the content, even if it's already been handled.
         */
        @TestOnly
        fun peekContent(): T = content
    }
}
