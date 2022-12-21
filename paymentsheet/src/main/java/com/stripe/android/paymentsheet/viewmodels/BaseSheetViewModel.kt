package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
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
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSelectionRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
@Suppress("TooManyFunctions")
internal abstract class BaseSheetViewModel(
    application: Application,
    internal val config: PaymentSheet.Configuration?,
    internal val eventReporter: EventReporter,
    protected val customerRepository: CustomerRepository,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO,
    protected val logger: Logger,
    @InjectorKey val injectorKey: String,
    val lpmResourceRepository: ResourceRepository<LpmRepository>,
    val addressResourceRepository: ResourceRepository<AddressRepository>,
    val savedStateHandle: SavedStateHandle,
    val linkHandler: LinkHandler,
) : AndroidViewModel(application), PaymentSelectionRepository {
    /**
     * This ViewModel exists during the whole user flow, and needs to share the Dagger dependencies
     * with the other, screen-specific ViewModels. So it holds a reference to the injector which is
     * passed as a parameter to the other ViewModel factories.
     */
    lateinit var injector: NonFallbackInjector

    internal val customerConfig = config?.customer
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    // a fatal error
    protected val _fatal = MutableLiveData<Throwable>()

    @VisibleForTesting
    internal val _isGooglePayReady = savedStateHandle.getLiveData<Boolean>(SAVE_GOOGLE_PAY_READY)
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    // Don't save the resource repository state because it must be re-initialized
    // with the save server specs when reconstructed.
    private var _isResourceRepositoryReady = MutableLiveData<Boolean>(null)

    internal val isResourceRepositoryReady: LiveData<Boolean?> =
        _isResourceRepositoryReady.distinctUntilChanged()

    private val _stripeIntent = savedStateHandle.getLiveData<StripeIntent>(SAVE_STRIPE_INTENT)
    internal val stripeIntent: LiveData<StripeIntent?> = _stripeIntent

    internal var supportedPaymentMethods
        get() = savedStateHandle.get<List<PaymentMethodCode>>(
            SAVE_SUPPORTED_PAYMENT_METHOD
        )?.mapNotNull {
            lpmResourceRepository.getRepository().fromCode(it)
        } ?: emptyList()
        set(value) = savedStateHandle.set(SAVE_SUPPORTED_PAYMENT_METHOD, value.map { it.code })

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal val _paymentMethods =
        savedStateHandle.getLiveData<List<PaymentMethod>>(SAVE_PAYMENT_METHODS)

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    internal val amount: LiveData<Amount> = savedStateHandle.getLiveData<Amount>(SAVE_AMOUNT)

    internal val headerText = MutableLiveData<String>()

    /**
     * Request to retrieve the value from the repository happens when initialize any fragment
     * and any fragment will re-update when the result comes back.
     * Represents what the user last selects (add or buy) on the
     * [PaymentOptionsActivity]/[PaymentSheetActivity], and saved/restored from the preferences.
     */
    private val _savedSelection =
        savedStateHandle.getLiveData<SavedSelection>(SAVE_SAVED_SELECTION)
    private val savedSelection: LiveData<SavedSelection> = _savedSelection

    private val _transition = MutableLiveData<Event<TransitionTarget>?>(null)
    internal val transition: LiveData<Event<TransitionTarget>?> = _transition

    private val _liveMode = savedStateHandle.getLiveData<Boolean>(SAVE_STATE_LIVE_MODE)
    internal val liveMode: LiveData<Boolean> = _liveMode

    private val _selection = savedStateHandle.getLiveData<PaymentSelection>(SAVE_SELECTION)
    internal val selection: LiveData<PaymentSelection?> = _selection

    override val paymentSelection: PaymentSelection?
        get() {
            return selection.value
        }

    private val editing = MutableLiveData(false)

    val processing: LiveData<Boolean> = savedStateHandle.getLiveData<Boolean>(SAVE_PROCESSING)

    private val _contentVisible = MutableLiveData(true)
    internal val contentVisible: LiveData<Boolean> = _contentVisible.distinctUntilChanged()

    /**
     * Use this to override the current UI state of the primary button. The UI state is reset every
     * time the payment selection is changed.
     */
    private val _primaryButtonUIState = MutableLiveData<PrimaryButton.UIState?>()
    val primaryButtonUIState: LiveData<PrimaryButton.UIState?> = _primaryButtonUIState

    private val _primaryButtonState = MutableLiveData<PrimaryButton.State>()
    val primaryButtonState: LiveData<PrimaryButton.State> = _primaryButtonState

    private val _notesText = MutableLiveData<String?>()
    internal val notesText: LiveData<String?> = _notesText

    /**
     * This should be initialized from the starter args, and then from that point forward it will be
     * the last valid new payment method entered by the user.
     * In contrast to selection, this field will not be updated by the list fragment. It is used to
     * save a new payment method that is added so that the payment data entered is recovered when
     * the user returns to that payment method type.
     */
    abstract var newPaymentSelection: PaymentSelection.New?

    abstract fun onFatal(throwable: Throwable)

    val buttonsEnabled = MediatorLiveData<Boolean>().apply {
        listOf(
            processing,
            editing
        ).forEach { source ->
            addSource(source) {
                value = processing.value != true &&
                    editing.value != true
            }
        }
    }.distinctUntilChanged()

    val ctaEnabled = MediatorLiveData<Boolean>().apply {
        listOf(
            primaryButtonUIState,
            buttonsEnabled,
            selection
        ).forEach { source ->
            addSource(source) {
                value = if (primaryButtonUIState.value != null) {
                    primaryButtonUIState.value?.enabled == true && buttonsEnabled.value == true
                } else {
                    buttonsEnabled.value == true && selection.value != null
                }
            }
        }
    }.distinctUntilChanged()

    internal var lpmServerSpec
        get() = savedStateHandle.get<String>(LPM_SERVER_SPEC_STRING)
        set(value) = savedStateHandle.set(LPM_SERVER_SPEC_STRING, value)

    private val paymentOptionsStateMapper: PaymentOptionsStateMapper by lazy {
        PaymentOptionsStateMapper(
            paymentMethods = paymentMethods,
            initialSelection = savedSelection,
            currentSelection = selection,
            isGooglePayReady = isGooglePayReady,
            isLinkEnabled = linkHandler.isLinkEnabled,
            isNotPaymentFlow = this is PaymentOptionsViewModel,
        )
    }

    val paymentOptionsState: StateFlow<PaymentOptionsState> = paymentOptionsStateMapper()
        .asFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PaymentOptionsState(),
        )

    init {
        if (_savedSelection.value == null) {
            viewModelScope.launch {
                val savedSelection = withContext(workContext) {
                    prefsRepository.getSavedSelection(
                        isGooglePayReady.asFlow().first(),
                        linkHandler.isLinkEnabled.asFlow().first()
                    )
                }
                savedStateHandle[SAVE_SAVED_SELECTION] = savedSelection
            }
        }

        if (_isResourceRepositoryReady.value == null) {
            viewModelScope.launch {
                // This work should be done on the background
                CoroutineScope(workContext).launch {
                    // If we have been killed and are being restored then we need to re-populate
                    // the lpm repository
                    stripeIntent.value?.paymentMethodTypes?.let { intentPaymentMethodTypes ->
                        lpmResourceRepository.getRepository().apply {
                            if (!isLoaded()) {
                                update(intentPaymentMethodTypes, lpmServerSpec)
                            }
                        }
                    }

                    lpmResourceRepository.waitUntilLoaded()
                    addressResourceRepository.waitUntilLoaded()
                    _isResourceRepositoryReady.postValue(true)
                }
            }
        }

        viewModelScope.launch {
            // If the currently selected payment option has been removed, we set it to the one
            // determined in the payment options state.
            paymentOptionsState
                .mapNotNull { it.selectedItem?.toPaymentSelection() }
                .filter { it != selection.value }
                .collect { updateSelection(it) }
        }
    }

    protected val isReadyEvents = MediatorLiveData<Boolean>().apply {
        listOf(
            savedSelection,
            stripeIntent,
            paymentMethods,
            isGooglePayReady,
            isResourceRepositoryReady,
            linkHandler.isLinkEnabled
        ).forEach { source ->
            addSource(source) {
                value = determineIfReady()
            }
        }
    }.distinctUntilChanged().map {
        Event(it)
    }

    private fun determineIfReady(): Boolean {
        val stripeIntentValue = stripeIntent.value
        val isGooglePayReadyValue = isGooglePayReady.value
        val isResourceRepositoryReadyValue = isResourceRepositoryReady.value
        val isLinkReadyValue = linkHandler.isLinkEnabled.value
        val savedSelectionValue = savedSelection.value
        // List of Payment Methods is not passed in the config but we still wait for it to be loaded
        // before adding the Fragment.
        val paymentMethodsValue = paymentMethods.value

        return stripeIntentValue != null &&
            paymentMethodsValue != null &&
            isGooglePayReadyValue != null &&
            isResourceRepositoryReadyValue != null &&
            isLinkReadyValue != null &&
            savedSelectionValue != null
    }

    abstract fun transitionToFirstScreen()

    protected fun transitionTo(target: TransitionTarget) {
        _transition.postValue(Event(target))
    }

    fun transitionToAddPaymentScreen() {
        transitionTo(TransitionTarget.AddAnotherPaymentMethod)
    }

    internal sealed class TransitionTarget {
        object SelectSavedPaymentMethods : TransitionTarget()
        object AddAnotherPaymentMethod : TransitionTarget()
        object AddFirstPaymentMethod : TransitionTarget()
    }

    protected fun setStripeIntent(stripeIntent: StripeIntent?) {
        savedStateHandle[SAVE_STRIPE_INTENT] = stripeIntent

        /**
         * The settings of values in this function is so that
         * they will be ready in the onViewCreated method of
         * the [BaseAddPaymentMethodFragment]
         */
        val pmsToAdd = getPMsToAdd(stripeIntent, config, lpmResourceRepository.getRepository())
        supportedPaymentMethods = pmsToAdd

        if (stripeIntent != null && supportedPaymentMethods.isEmpty()) {
            onFatal(
                IllegalArgumentException(
                    "None of the requested payment methods" +
                        " (${stripeIntent.paymentMethodTypes})" +
                        " match the supported payment types" +
                        " (${
                            lpmResourceRepository.getRepository().values()
                                .map { it.code }.toList()
                        })"
                )
            )
        }

        if (stripeIntent is PaymentIntent) {
            runCatching {
                savedStateHandle[SAVE_AMOUNT] = Amount(
                    requireNotNull(stripeIntent.amount),
                    requireNotNull(stripeIntent.currency)
                )
                // Reset the primary button state to display the amount
                _primaryButtonUIState.value = null
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

    fun updatePrimaryButtonUIState(state: PrimaryButton.UIState?) {
        _primaryButtonUIState.value = state
    }

    fun updatePrimaryButtonState(state: PrimaryButton.State) {
        _primaryButtonState.value = state
    }

    fun updateBelowButtonText(text: String?) {
        _notesText.value = text
    }

    open fun updateSelection(selection: PaymentSelection?) {
        if (selection is PaymentSelection.New) {
            newPaymentSelection = selection
        }

        savedStateHandle[SAVE_SELECTION] = selection

        updateBelowButtonText(null)
    }

    fun setEditing(isEditing: Boolean) {
        editing.value = isEditing
    }

    fun setContentVisible(visible: Boolean) {
        _contentVisible.value = visible
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) {
        val paymentMethodId = paymentMethod.id ?: return

        viewModelScope.launch {
            val currentSelection = (selection.value as? PaymentSelection.Saved)?.paymentMethod?.id
            val didRemoveSelectedItem = currentSelection == paymentMethodId

            if (didRemoveSelectedItem) {
                // Remove the current selection. The new selection will be set when we're computing
                // the next PaymentOptionsState.
                _selection.value = null
            }

            savedStateHandle[SAVE_PAYMENT_METHODS] = _paymentMethods.value?.filter {
                it.id != paymentMethodId
            }

            customerConfig?.let {
                customerRepository.detachPaymentMethod(
                    it,
                    paymentMethodId
                )
            }

            val hasNoBankAccounts = paymentMethods.value.orEmpty().all { it.type != USBankAccount }
            if (hasNoBankAccounts) {
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        visible = false
                    )
                )
                updateBelowButtonText(null)
            }
        }
    }

    fun payWithLinkInline(linkConfig: LinkPaymentLauncher.Configuration, userInput: UserInput?) {
        viewModelScope.launch {
            linkHandler.payWithLinkInline(linkConfig, userInput)
        }
    }

    abstract fun onUserCancel()

    fun onUserBack() {
        // Reset the selection to the one from before opening the add payment method screen
        val paymentOptionsState = paymentOptionsState.value
        updateSelection(paymentOptionsState.selectedItem?.toPaymentSelection())
    }

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onFinish()

    abstract fun onError(@StringRes error: Int? = null)

    abstract fun onError(error: String? = null)

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
        internal const val LPM_SERVER_SPEC_STRING = "lpm_server_spec_string"
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVE_SAVED_SELECTION = "saved_selection"
        internal const val SAVE_SUPPORTED_PAYMENT_METHOD = "supported_payment_methods"
        internal const val SAVE_PROCESSING = "processing"
        internal const val SAVE_GOOGLE_PAY_READY = "google_pay_ready"
        internal const val SAVE_RESOURCE_REPOSITORY_READY = "resource_repository_ready"
        internal const val SAVE_STATE_LIVE_MODE = "save_state_live_mode"
        internal const val LINK_CONFIGURATION = "link_configuration"
    }
}

internal fun <T> LiveData<BaseSheetViewModel.Event<T>?>.observeEvents(
    lifecycleOwner: LifecycleOwner,
    observer: (T) -> Unit
) {
    observe(lifecycleOwner) { event ->
        val content = event?.getContentIfNotHandled() ?: return@observe
        observer(content)
    }
}
