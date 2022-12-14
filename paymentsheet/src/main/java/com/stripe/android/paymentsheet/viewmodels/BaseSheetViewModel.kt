package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val linkLauncher: LinkPaymentLauncher
) : AndroidViewModel(application) {
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
    protected val _fatal = MutableStateFlow<Throwable?>(null)

    @VisibleForTesting
    internal val _isGooglePayReady = MutableStateFlow(savedStateHandle.get<Boolean>(SAVE_GOOGLE_PAY_READY) ?: false)
    internal val isGooglePayReady: StateFlow<Boolean> = _isGooglePayReady

    // Don't save the resource repository state because it must be re-initialized
    // with the save server specs when reconstructed.
    private var _isResourceRepositoryReady = MutableStateFlow<Boolean?>(null)
    internal val isResourceRepositoryReady: StateFlow<Boolean?> = _isResourceRepositoryReady

    @VisibleForTesting
    internal val _isLinkEnabled = MutableStateFlow(false)
    internal val isLinkEnabled: StateFlow<Boolean> = _isLinkEnabled

    internal val activeLinkSession = MutableStateFlow(false)

    protected val _linkConfiguration = MutableStateFlow(
        savedStateHandle.get<LinkPaymentLauncher.Configuration?>(LINK_CONFIGURATION)
    )
    internal val linkConfiguration: StateFlow<LinkPaymentLauncher.Configuration?> =
        _linkConfiguration

    internal val stripeIntent: StateFlow<StripeIntent?> =
        savedStateHandle.getStateFlow<StripeIntent?>(SAVE_STRIPE_INTENT, null)

    internal var supportedPaymentMethods
        get() = savedStateHandle.get<List<PaymentMethodCode>>(
            SAVE_SUPPORTED_PAYMENT_METHOD
        )?.mapNotNull {
            lpmResourceRepository.getRepository().fromCode(it)
        } ?: emptyList()
        set(value) = savedStateHandle.set(SAVE_SUPPORTED_PAYMENT_METHOD, value.map { it.code })

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: StateFlow<List<PaymentMethod>> =
        savedStateHandle.getStateFlow(SAVE_PAYMENT_METHODS, listOf())

    internal val amount: StateFlow<Amount?> = savedStateHandle.getStateFlow(SAVE_AMOUNT, null)

    internal val headerText = MutableStateFlow<String?>(null)

    /**
     * Request to retrieve the value from the repository happens when initialize any fragment
     * and any fragment will re-update when the result comes back.
     * Represents what the user last selects (add or buy) on the
     * [PaymentOptionsActivity]/[PaymentSheetActivity], and saved/restored from the preferences.
     */
    private val savedSelection: StateFlow<SavedSelection> =
        savedStateHandle.getStateFlow<SavedSelection>(SAVE_SAVED_SELECTION, SavedSelection.None)

    private val _transition = MutableStateFlow<Event<TransitionTarget>?>(null)
    internal val transition: StateFlow<Event<TransitionTarget>?> = _transition

    private val _liveMode = MutableStateFlow(savedStateHandle[SAVE_STATE_LIVE_MODE] ?: false)
    internal val liveMode: StateFlow<Boolean> = _liveMode

    internal val selection: StateFlow<PaymentSelection?> =
        savedStateHandle.getStateFlow(SAVE_SELECTION, null)

    private val editing = MutableStateFlow(false)

    val processing: StateFlow<Boolean> = savedStateHandle
        .getStateFlow(SAVE_PROCESSING, false)

    private val _contentVisible = MutableStateFlow(true)
    internal val contentVisible: StateFlow<Boolean> = _contentVisible

    /**
     * Use this to override the current UI state of the primary button. The UI state is reset every
     * time the payment selection is changed.
     */
    private val _primaryButtonUIState = MutableStateFlow<PrimaryButton.UIState?>(null)
    val primaryButtonUIState: StateFlow<PrimaryButton.UIState?> = _primaryButtonUIState

    // TODO: (jameswoo) Figure out if there is a better way to force state flow to update
    private val _resetPrimaryButtonUI = MutableStateFlow(0L)
    internal val resetPrimaryButtonUI: StateFlow<Long> = _resetPrimaryButtonUI

    private val _primaryButtonState = MutableStateFlow<PrimaryButton.State?>(null)
    val primaryButtonState: StateFlow<PrimaryButton.State?> = _primaryButtonState

    private val _notesText = MutableStateFlow<String?>(null)
    internal val notesText: StateFlow<String?> = _notesText

    private val _showLinkVerificationDialog = MutableStateFlow(false)
    val showLinkVerificationDialog: StateFlow<Boolean> = _showLinkVerificationDialog

    private val linkVerificationChannel = Channel<Boolean>(capacity = 1)

    /**
     * This should be initialized from the starter args, and then from that point forward it will be
     * the last valid new payment method entered by the user.
     * In contrast to selection, this field will not be updated by the list fragment. It is used to
     * save a new payment method that is added so that the payment data entered is recovered when
     * the user returns to that payment method type.
     */
    abstract var newPaymentSelection: PaymentSelection.New?

    open var linkInlineSelection = MutableStateFlow<PaymentSelection.New.LinkInline?>(null)

    abstract fun onFatal(throwable: Throwable)

    val buttonsEnabled = combine(
        processing,
        editing
    ) { processing, editing ->
        !processing && !editing
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val ctaEnabled = combine(
        primaryButtonUIState,
        buttonsEnabled,
        selection
    ) { primaryButtonUIState, buttonsEnabled, selection ->
        if (primaryButtonUIState != null) {
            primaryButtonUIState.enabled && buttonsEnabled
        } else {
            buttonsEnabled && selection != null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

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

    val paymentOptionsState: StateFlow<PaymentOptionsState?> = paymentOptionsStateMapper()
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
                    _isResourceRepositoryReady.update { true }
                }
            }
        }

        viewModelScope.launch {
            // If the currently selected payment option has been removed, we set it to the one
            // determined in the payment options state.
            paymentOptionsState
                .mapNotNull { it?.selectedItem?.toPaymentSelection() }
                .filter { it != selection.value }
                .collect { updateSelection(it) }
        }
    }

    protected val isReadyEvents = combine(
        combine(
            savedSelection,
            stripeIntent,
            paymentMethods,
            ::Triple
        ),
        combine(
            isGooglePayReady,
            isResourceRepositoryReady,
            isLinkEnabled,
            ::Triple
        )
    ) { _, _ ->
        determineIfReady()
    }.map {
        Event(it)
    }

    private fun determineIfReady(): Boolean {
        val stripeIntentValue = stripeIntent.value
        val isResourceRepositoryReadyValue = isResourceRepositoryReady.value
        val savedSelectionValue = savedSelection.value
        // List of Payment Methods is not passed in the config but we still wait for it to be loaded
        // before adding the Fragment.

        return stripeIntentValue != null &&
            isResourceRepositoryReadyValue != null &&
            savedSelectionValue != null
    }

    abstract fun transitionToFirstScreen()

    protected fun transitionTo(target: TransitionTarget) {
        _transition.update { Event(target) }
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
                forceUpdatePrimaryButtonUI()
                _primaryButtonUIState.update { null }
            }.onFailure {
                onFatal(
                    IllegalStateException("PaymentIntent must contain amount and currency.")
                )
            }
        }

        if (stripeIntent != null) {
            _liveMode.update { stripeIntent.isLiveMode }
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

    private fun forceUpdatePrimaryButtonUI() {
        _resetPrimaryButtonUI.update { it + 1 }
    }

    fun updatePrimaryButtonUIState(state: PrimaryButton.UIState?) {
        _primaryButtonUIState.update { state }
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
                savedStateHandle[SAVE_SELECTION] = null
            }

            savedStateHandle[SAVE_PAYMENT_METHODS] = paymentMethods.value.filter {
                it.id != paymentMethodId
            }

            customerConfig?.let {
                customerRepository.detachPaymentMethod(
                    it,
                    paymentMethodId
                )
            }

            val hasNoBankAccounts = paymentMethods.value.all { it.type != USBankAccount }
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

    protected suspend fun requestLinkVerification(): Boolean {
        _showLinkVerificationDialog.value = true
        return linkVerificationChannel.receive()
    }

    fun handleLinkVerificationResult(success: Boolean) {
        _showLinkVerificationDialog.value = false
        activeLinkSession.value = success
        linkVerificationChannel.trySend(success)
    }

    fun payWithLinkInline(configuration: LinkPaymentLauncher.Configuration, userInput: UserInput?) {
        (selection.value as? PaymentSelection.New.Card)?.paymentMethodCreateParams?.let { params ->
            savedStateHandle[SAVE_PROCESSING] = true
            updatePrimaryButtonState(PrimaryButton.State.StartProcessing)

            viewModelScope.launch {
                when (linkLauncher.getAccountStatusFlow(configuration).first()) {
                    AccountStatus.Verified -> {
                        activeLinkSession.value = true
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
                            savedStateHandle[SAVE_PROCESSING] = false
                            updatePrimaryButtonState(PrimaryButton.State.Ready)
                        }
                    }
                    AccountStatus.SignedOut,
                    AccountStatus.Error -> {
                        activeLinkSession.value = false
                        userInput?.let {
                            linkLauncher.signInWithUserInput(configuration, userInput).fold(
                                onSuccess = {
                                    // If successful, the account was fetched or created, so try again
                                    payWithLinkInline(configuration, userInput)
                                },
                                onFailure = {
                                    onError(it.localizedMessage)
                                    savedStateHandle[SAVE_PROCESSING] = false
                                    updatePrimaryButtonState(PrimaryButton.State.Ready)
                                }
                            )
                        } ?: run {
                            savedStateHandle[SAVE_PROCESSING] = false
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
        updateSelection(paymentOptionsState?.selectedItem?.toPaymentSelection())
    }

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onFinish()

    abstract fun onError(@StringRes error: Int? = null)

    abstract fun onError(error: String? = null)

    data class UserErrorMessage(val message: String)

    /**
     * Used as a wrapper for data that is exposed via a Flow that represents an event.
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
