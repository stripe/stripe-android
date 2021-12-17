package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.CardDataCollectionFragment
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class BaseSheetViewModel<TransitionTargetType>(
    application: Application,
    internal val config: PaymentSheet.Configuration?,
    internal val eventReporter: EventReporter,
    protected val customerRepository: CustomerRepository,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO,
    protected val logger: Logger,
    @InjectorKey val injectorKey: String,
    resourceRepository: ResourceRepository,
    val handle: SavedStateHandle? = null
) : AndroidViewModel(application) {
    internal val customerConfig = config?.customer
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    // a fatal error
    protected val _fatal = MutableLiveData<Throwable>()

    @VisibleForTesting
    internal val _isGooglePayReady = MutableLiveData<Boolean>()
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    private val _isResourceRepositoryReady = MutableLiveData<Boolean>()
    internal val isResourceRepositoryReady: LiveData<Boolean> =
        _isResourceRepositoryReady.distinctUntilChanged()

    private val _stripeIntent = handle!!.getLiveData<StripeIntent>(SAVE_STRIPE_INTENT)
    internal val stripeIntent: LiveData<StripeIntent?> = _stripeIntent

    internal var supportedPaymentMethods = emptyList<SupportedPaymentMethod>()

    @VisibleForTesting
    internal val _paymentMethods = handle!!.getLiveData<List<PaymentMethod>>(SAVE_PAYMENT_METHODS)

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    @VisibleForTesting
    internal val _amount = handle!!.getLiveData<Amount>(SAVE_AMOUNT)
    internal val amount: LiveData<Amount> = _amount

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

    @VisibleForTesting
    internal val _liveMode = MutableLiveData<Boolean>()
    internal val liveMode: LiveData<Boolean> = _liveMode

    /**
     * On [CardDataCollectionFragment] this is set every time the details in the add
     * card fragment is determined to be valid (not necessarily selected)
     * On [BasePaymentMethodsListFragment] this is set when a user selects one of the options
     */
    private val _selection = handle!!.getLiveData<PaymentSelection>(SAVE_SELECTION)
    internal val selection: LiveData<PaymentSelection?> = _selection

    private val editing = MutableLiveData(false)

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

    val ctaEnabled = MediatorLiveData<Boolean>().apply {
        listOf(
            processing,
            selection,
            editing
        ).forEach { source ->
            addSource(source) {
                value = processing.value != true &&
                    selection.value != null &&
                    editing.value != true
            }
        }
    }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            val savedSelection = withContext(workContext) {
                prefsRepository.getSavedSelection(isGooglePayReady.asFlow().first())
            }
            _savedSelection.value = savedSelection
        }

        viewModelScope.launch {
            resourceRepository.waitUntilLoaded()
            _isResourceRepositoryReady.value = true
        }
    }

    val fragmentConfigEvent = MediatorLiveData<FragmentConfig?>().apply {
        listOf(
            savedSelection,
            stripeIntent,
            paymentMethods,
            isGooglePayReady,
            isResourceRepositoryReady
        ).forEach { source ->
            addSource(source) {
                value = createFragmentConfig()
            }
        }
    }.distinctUntilChanged().map {
        Event(it)
    }

    private fun createFragmentConfig(): FragmentConfig? {
        val stripeIntentValue = stripeIntent.value
        val isGooglePayReadyValue = isGooglePayReady.value
        val isResourceRepositoryReadyValue = isResourceRepositoryReady.value
        val savedSelectionValue = savedSelection.value
        // List of Payment Methods is not passed in the config but we still wait for it to be loaded
        // before adding the Fragment.
        val paymentMethodsValue = paymentMethods.value

        return if (
            stripeIntentValue != null &&
            paymentMethodsValue != null &&
            isGooglePayReadyValue != null &&
            isResourceRepositoryReadyValue != null &&
            savedSelectionValue != null
        ) {
            FragmentConfig(
                stripeIntent = stripeIntentValue,
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
        handle?.set(SAVE_STRIPE_INTENT, stripeIntent)

        /**
         * The settings of values in this function is so that
         * they will be ready in the onViewCreated method of
         * the [BaseAddPaymentMethodFragment]
         */

        supportedPaymentMethods = SupportedPaymentMethod.getPMsToAdd(stripeIntent, config)

        if (stripeIntent != null && supportedPaymentMethods.isEmpty()) {
            onFatal(
                IllegalArgumentException(
                    "None of the requested payment methods" +
                        " (${stripeIntent.paymentMethodTypes})" +
                        " match the supported payment types" +
                        " (${SupportedPaymentMethod.values().toList()})"
                )
            )
        }

        if (stripeIntent is PaymentIntent) {
            runCatching {
                handle!!.set(SAVE_AMOUNT, Amount(
                        requireNotNull(stripeIntent.amount),
                        requireNotNull(stripeIntent.currency)
                    )
                )
            }.onFailure {
                onFatal(
                    IllegalStateException("PaymentIntent must contain amount and currency.")
                )
            }
        }

        if (stripeIntent != null) {
            _liveMode.postValue(stripeIntent.isLiveMode)
            warnUnactivatedIfNeeded(stripeIntent.unactivatedPaymentMethods)
        }
    }

    private fun warnUnactivatedIfNeeded(unactivatedPaymentMethodTypes: List<String>) {
        if (unactivatedPaymentMethodTypes.isEmpty()) {
            return
        }

        val message = "[Stripe SDK] Warning: Your Intent contains the following payment method " +
            "types which are activated for test mode but not activated for " +
            "live mode: $unactivatedPaymentMethodTypes. These payment method types will not be " +
            "displayed in live mode until they are activated. To activate these payment method " +
            "types visit your Stripe dashboard." +
            "More information: https://support.stripe.com/questions/activate-a-new-payment-method"

        logger.warning(message)
    }

    fun updateSelection(selection: PaymentSelection?) {
        handle!!.set(SAVE_SELECTION, selection)
    }

    fun setEditing(isEditing: Boolean) {
        editing.value = isEditing
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) = runBlocking {
        launch {
            paymentMethod.id?.let { paymentMethodId ->
                handle?.set(SAVE_PAYMENT_METHODS, _paymentMethods.value?.filter {
                    it.id != paymentMethodId
                }
                )

                customerConfig?.let {
                    customerRepository.detachPaymentMethod(
                        it,
                        paymentMethodId
                    )
                }
            }
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

    companion object {
        internal const val SAVE_STRIPE_INTENT = "stripe_intent"
        internal const val SAVE_PAYMENT_METHODS = "customer_payment_methods"
        internal const val SAVE_AMOUNT = "amount"
        internal const val SAVE_SELECTION = "selection"
    }
}
