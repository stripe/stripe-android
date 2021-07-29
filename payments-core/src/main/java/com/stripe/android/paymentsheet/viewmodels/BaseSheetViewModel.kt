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
import com.stripe.android.googlepaylauncher.StripeGooglePayContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
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
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    // a fatal error
    protected val _fatal = MutableLiveData<Throwable>()

    protected val _isGooglePayReady = MutableLiveData<Boolean>()
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    protected val _launchGooglePay = MutableLiveData<Event<StripeGooglePayContract.Args>>()
    internal val launchGooglePay: LiveData<Event<StripeGooglePayContract.Args>> = _launchGooglePay

    private val _stripeIntent = MutableLiveData<StripeIntent?>()
    internal val stripeIntent: LiveData<StripeIntent?> = _stripeIntent

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
     * On [CardDataCollectionFragment] this is set every time the details in the add
     * card fragment is determined to be valid (not necessarily selected)
     * On [BasePaymentMethodsListFragment] this is set when a user selects one of the options
     */
    private val _selection = MutableLiveData<PaymentSelection?>()
    internal val selection: LiveData<PaymentSelection?> = _selection

    @VisibleForTesting
    internal val _processing = MutableLiveData(true)
    val processing: LiveData<Boolean> = _processing

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

    val userCanChooseToSaveCard: Boolean
        get() = customerConfig != null && stripeIntent.value is PaymentIntent

    init {
        fetchSavedSelection()
    }

    val fragmentConfig = MediatorLiveData<FragmentConfig?>().also { configLiveData ->
        listOf(
            savedSelection,
            stripeIntent,
            paymentMethods,
            isGooglePayReady
        ).forEach { source ->
            configLiveData.addSource(source) {
                configLiveData.value = createFragmentConfig()
            }
        }
    }.distinctUntilChanged()

    private fun createFragmentConfig(): FragmentConfig? {
        val stripeIntentValue = stripeIntent.value
        val paymentMethodsValue = paymentMethods.value
        val isGooglePayReadyValue = isGooglePayReady.value
        val savedSelectionValue = savedSelection.value

        return if (
            stripeIntentValue != null &&
            paymentMethodsValue != null &&
            isGooglePayReadyValue != null &&
            savedSelectionValue != null
        ) {
            FragmentConfig(
                stripeIntent = stripeIntentValue,
                paymentMethods = paymentMethodsValue,
                isGooglePayReady = isGooglePayReadyValue,
                savedSelection = savedSelectionValue
            )
        } else {
            null
        }
    }

    open fun transitionTo(target: TransitionTargetType) {
        _transition.postValue(Event(target))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun setStripeIntent(stripeIntent: StripeIntent?) {
        _stripeIntent.value = stripeIntent

        if (stripeIntent != null && getSupportedPaymentMethods().isEmpty()) {
            onFatal(
                IllegalArgumentException(
                    "None of the requested payment methods" +
                        " (${stripeIntent.paymentMethodTypes})" +
                        " match the supported payment types" +
                        " (${SupportedPaymentMethod.values().toList()})"
                )
            )
        }
    }

    fun getSupportedPaymentMethods(): List<SupportedPaymentMethod> {
        stripeIntent.value?.let { stripeIntent ->
            return stripeIntent.paymentMethodTypes.mapNotNull {
                SupportedPaymentMethod.fromCode(it)
            }.filter { it == SupportedPaymentMethod.Card }
        }

        return emptyList()
    }

    fun updateSelection(selection: PaymentSelection?) {
        _selection.value = selection
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

    data class UserErrorMessage(val message: String)

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
