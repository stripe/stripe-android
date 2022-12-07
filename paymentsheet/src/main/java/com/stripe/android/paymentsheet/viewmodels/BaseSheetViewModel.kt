package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.StringRes
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
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.state.loggedInToLink
import com.stripe.android.paymentsheet.state.loggedOutOfLink
import com.stripe.android.paymentsheet.state.mapAsLiveData
import com.stripe.android.paymentsheet.state.removePaymentMethod
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.TestOnly
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
internal abstract class BaseSheetViewModel<TransitionTargetType>(
    application: Application,
    initialState: PaymentSheetState,
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
    val linkLauncher: LinkPaymentLauncher
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(initialState)
    protected val state: StateFlow<PaymentSheetState> = _state

    protected val fullState: PaymentSheetState.Full?
        get() = state.value as? PaymentSheetState.Full

    protected fun setFullState(state: PaymentSheetState.Full) {
        _state.value = state
    }

    protected fun updateFullState(
        transform: (PaymentSheetState.Full) -> PaymentSheetState.Full,
    ) {
        _state.update {
            if (it is PaymentSheetState.Full) {
                transform(it)
            } else {
                it
            }
        }
    }

    /**
     * This ViewModel exists during the whole user flow, and needs to share the Dagger dependencies
     * with the other, screen-specific ViewModels. So it holds a reference to the injector which is
     * passed as a parameter to the other ViewModel factories.
     */
    lateinit var injector: NonFallbackInjector

    internal val customerConfig = config?.customer
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    protected var mostRecentError: Throwable? = null

    internal val isGooglePayReady: LiveData<Boolean> = state.mapAsLiveData { it.isGooglePayReady }

    // Don't save the resource repository state because it must be re-initialized
    // with the save server specs when reconstructed.
    private var _isResourceRepositoryReady = MutableLiveData<Boolean>(null)

    internal val isResourceRepositoryReady: LiveData<Boolean?> =
        _isResourceRepositoryReady.distinctUntilChanged()

    internal val isLinkEnabled: LiveData<Boolean> = state.mapAsLiveData { it.isLinkAvailable }

    fun reportShowNewPaymentOptionForm() {
        eventReporter.onShowNewPaymentOptionForm(
            linkEnabled = fullState?.isLinkAvailable ?: false,
            activeLinkSession = fullState?.linkState?.loginState == LinkState.LoginState.LoggedIn,
        )
    }

    internal val linkConfiguration: LiveData<LinkPaymentLauncher.Configuration?> =
        state.mapAsLiveData { it.linkState?.configuration }

    internal val stripeIntent: LiveData<StripeIntent?> = state.mapAsLiveData { it.stripeIntent }

    internal val supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>
        get() = fullState?.supportedPaymentMethodTypes.orEmpty().mapNotNull {
            lpmResourceRepository.getRepository().fromCode(it)
        }

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: LiveData<List<PaymentMethod>> = state.mapAsLiveData {
        it.customerPaymentMethods
    }

    internal val amount: Amount?
        get() = fullState?.amount

    internal val headerText = MutableLiveData<String>()

    private val savedSelection: LiveData<SavedSelection> = state.mapAsLiveData { it.savedSelection }

    private val _transition = MutableLiveData<Event<TransitionTargetType?>>(Event(null))
    internal val transition: LiveData<Event<TransitionTargetType?>> = _transition

    internal val liveMode: LiveData<Boolean> = state.mapAsLiveData { it.stripeIntent.isLiveMode }
    internal val selection: LiveData<PaymentSelection?> = state.mapAsLiveData { it.selection }

    private val editing: LiveData<Boolean> = state.mapAsLiveData { it.isEditing }

    val processing: LiveData<Boolean> = state.mapAsLiveData { it.isProcessing }

    private val _contentVisible = MutableLiveData(true)
    internal val contentVisible: LiveData<Boolean> = _contentVisible.distinctUntilChanged()

    /**
     * Use this to override the current UI state of the primary button. The UI state is reset every
     * time the payment selection is changed.
     */
    val primaryButtonUIState: LiveData<PrimaryButton.UIState?> =
        state.mapAsLiveData { it.primaryButtonUiState }

    private val _primaryButtonState = MutableLiveData<PrimaryButton.State>()
    val primaryButtonState: LiveData<PrimaryButton.State> = _primaryButtonState

    internal val notesText: LiveData<String?> = state.mapAsLiveData { it.notesText }

    private val _showLinkVerificationDialog = MutableLiveData(false)
    val showLinkVerificationDialog: LiveData<Boolean> = _showLinkVerificationDialog

    private val linkVerificationChannel = Channel<Boolean>(capacity = 1)

    /**
     * This should be initialized from the starter args, and then from that point forward it will be
     * the last valid new payment method entered by the user.
     * In contrast to selection, this field will not be updated by the list fragment. It is used to
     * save a new payment method that is added so that the payment data entered is recovered when
     * the user returns to that payment method type.
     */
    abstract var newPaymentSelection: PaymentSelection.New?

    open var linkInlineSelection = MutableLiveData<PaymentSelection.New.LinkInline?>(null)

    abstract fun onFatal(throwable: Throwable)

    val buttonsEnabled: LiveData<Boolean> = state.mapAsLiveData { it.areWalletButtonsEnabled }

    val ctaEnabled: LiveData<Boolean> = state.mapAsLiveData { it.isPrimaryButtonEnabled }

    internal var lpmServerSpec
        get() = savedStateHandle.get<String>(LPM_SERVER_SPEC_STRING)
        set(value) = savedStateHandle.set(LPM_SERVER_SPEC_STRING, value)

    private val paymentOptionsStateMapper: PaymentOptionsStateMapper by lazy {
        PaymentOptionsStateMapper(
            paymentMethods = paymentMethods,
            initialSelection = savedSelection,
            currentSelection = selection,
            isGooglePayReady = isGooglePayReady,
            isLinkEnabled = isLinkEnabled,
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

//        viewModelScope.launch {
//            // If the currently selected payment option has been removed, we set it to the one
//            // determined in the payment options state.
//            paymentOptionsState
//                .mapNotNull { it.selectedItem?.toPaymentSelection() }
//                .filter { it != selection.value }
//                .collect { updateSelection(it) }
//        }
    }

    val fragmentConfigEvent = MediatorLiveData<FragmentConfig?>().apply {
        listOf(
            savedSelection,
            stripeIntent,
            paymentMethods,
            isGooglePayReady,
            isResourceRepositoryReady,
            isLinkEnabled
        ).forEach { source ->
            addSource(source) {
                value = createFragmentConfig()
            }
        }
    }.distinctUntilChanged().map {
        Event(it)
    }

    fun reportShowExistingPaymentOptions() {
        eventReporter.onShowExistingPaymentOptions(
            linkEnabled = fullState?.isLinkAvailable ?: false,
            activeLinkSession = fullState?.linkState?.loginState == LinkState.LoginState.LoggedIn,
        )
    }

    private fun createFragmentConfig(): FragmentConfig? {
        val stripeIntentValue = stripeIntent.value
        val isGooglePayReadyValue = isGooglePayReady.value
        val isResourceRepositoryReadyValue = isResourceRepositoryReady.value
        val isLinkReadyValue = isLinkEnabled.value
        val savedSelectionValue = savedSelection.value
        // List of Payment Methods is not passed in the config but we still wait for it to be loaded
        // before adding the Fragment.
        val paymentMethodsValue = paymentMethods.value

        return if (
            stripeIntentValue != null &&
            paymentMethodsValue != null &&
            isGooglePayReadyValue != null &&
            isResourceRepositoryReadyValue != null &&
            isLinkReadyValue != null &&
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

    fun transitionTo(target: TransitionTargetType) {
        _transition.postValue(Event(target))
    }

    fun updatePrimaryButtonUIState(state: PrimaryButton.UIState?) {
        updateFullState { it.copy(primaryButtonUiState = state) }
    }

    fun updatePrimaryButtonState(state: PrimaryButton.State) {
        _primaryButtonState.value = state
    }

    fun updateBelowButtonText(text: String?) {
        updateFullState { it.copy(notesText = text) }
    }

    open fun updateSelection(selection: PaymentSelection?) {
        updateFullState {
            it.copy(
                selection = selection,
                newPaymentSelection = (selection as? PaymentSelection.New) ?: it.newPaymentSelection,
                notesText = null,
            )
        }
    }

    fun setEditing(isEditing: Boolean) {
        updateFullState {
            it.copy(isEditing = isEditing)
        }
    }

    fun setContentVisible(visible: Boolean) {
        _contentVisible.value = visible
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) {
        val paymentMethodId = paymentMethod.id ?: return

        viewModelScope.launch {
            updateFullState {
                it.removePaymentMethod(paymentMethodId)
            }

            customerConfig?.let {
                customerRepository.detachPaymentMethod(
                    it,
                    paymentMethodId
                )
            }
        }
    }

    protected suspend fun requestLinkVerification(): Boolean {
        _showLinkVerificationDialog.value = true
        return linkVerificationChannel.receive()
    }

    fun handleLinkVerificationResult(success: Boolean) {
        updateFullState {
            if (success) {
                it.loggedInToLink()
            } else {
                it.loggedOutOfLink()
            }
        }
        _showLinkVerificationDialog.value = false
        linkVerificationChannel.trySend(success)
    }

    fun payWithLinkInline(configuration: LinkPaymentLauncher.Configuration, userInput: UserInput?) {
        (selection.value as? PaymentSelection.New.Card)?.paymentMethodCreateParams?.let { params ->
            updateFullState { it.copy(isProcessing = true) }

            updatePrimaryButtonState(PrimaryButton.State.StartProcessing)

            viewModelScope.launch {
                when (linkLauncher.getAccountStatusFlow(configuration).first()) {
                    AccountStatus.Verified -> {
                        updateFullState { it.loggedInToLink() }
                        completeLinkInlinePayment(
                            configuration,
                            params,
                            userInput is UserInput.SignIn
                        )
                    }
                    AccountStatus.VerificationStarted,
                    AccountStatus.NeedsVerification -> {
                        val success = requestLinkVerification()

                        if (success) {
                            completeLinkInlinePayment(
                                configuration,
                                params,
                                userInput is UserInput.SignIn
                            )
                        } else {
                            updateFullState { it.copy(isProcessing = false) }
                            updatePrimaryButtonState(PrimaryButton.State.Ready)
                        }
                    }
                    AccountStatus.SignedOut,
                    AccountStatus.Error -> {
                        updateFullState { it.loggedOutOfLink() }
                        userInput?.let {
                            linkLauncher.signInWithUserInput(configuration, userInput).fold(
                                onSuccess = {
                                    // If successful, the account was fetched or created, so try again
                                    payWithLinkInline(configuration, userInput)
                                },
                                onFailure = {
                                    onError(it.localizedMessage)
                                    updateFullState { it.copy(isProcessing = false) }
                                    updatePrimaryButtonState(PrimaryButton.State.Ready)
                                }
                            )
                        } ?: run {
                            updateFullState { it.copy(isProcessing = false) }
                            updatePrimaryButtonState(PrimaryButton.State.Ready)
                        }
                    }
                }
            }
        }
    }

    internal open fun completeLinkInlinePayment(
        configuration: LinkPaymentLauncher.Configuration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        isReturningUser: Boolean
    ) {
        viewModelScope.launch {
            onLinkPaymentDetailsCollected(
                linkLauncher.attachNewCardToAccount(
                    configuration,
                    paymentMethodCreateParams
                ).getOrNull()
            )
        }
    }

    /**
     * Method called after completing collection of payment data for a payment with Link.
     */
    abstract fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails.New?)

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
        internal const val SAVE_AMOUNT = "amount"
        internal const val LPM_SERVER_SPEC_STRING = "lpm_server_spec_string"
    }
}
