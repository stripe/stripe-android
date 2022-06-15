package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
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
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.injection.LinkPaymentLauncherFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.link.ui.verification.LinkVerificationCallback
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
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
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.ui.PrimaryButton
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
    val resourceRepository: ResourceRepository,
    val savedStateHandle: SavedStateHandle,
    internal val linkPaymentLauncherFactory: LinkPaymentLauncherFactory
) : AndroidViewModel(application) {
    internal val customerConfig = config?.customer
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    // a fatal error
    protected val _fatal = MutableLiveData<Throwable>()

    @VisibleForTesting
    internal val _isGooglePayReady = savedStateHandle.getLiveData<Boolean>(SAVE_GOOGLE_PAY_READY)
    internal val isGooglePayReady: LiveData<Boolean> = _isGooglePayReady.distinctUntilChanged()

    private val _isResourceRepositoryReady = savedStateHandle.getLiveData<Boolean>(
        SAVE_RESOURCE_REPOSITORY_READY
    )
    internal val isResourceRepositoryReady: LiveData<Boolean> =
        _isResourceRepositoryReady.distinctUntilChanged()

    private val _isLinkEnabled = MutableLiveData<Boolean>()
    internal val isLinkEnabled: LiveData<Boolean> = _isLinkEnabled.distinctUntilChanged()

    private val _stripeIntent = savedStateHandle.getLiveData<StripeIntent>(SAVE_STRIPE_INTENT)
    internal val stripeIntent: LiveData<StripeIntent?> = _stripeIntent

    internal var supportedPaymentMethods
        get() = savedStateHandle.get<List<PaymentMethodCode>>(
            SAVE_SUPPORTED_PAYMENT_METHOD
        )?.mapNotNull {
            resourceRepository.getLpmRepository().fromCode(it)
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

    @VisibleForTesting
    internal val _amount = savedStateHandle.getLiveData<Amount>(SAVE_AMOUNT)
    internal val amount: LiveData<Amount> = _amount

    internal val headerText = MutableLiveData<String>()
    internal val googlePayDividerVisibilility: MutableLiveData<Boolean> = MutableLiveData(false)

    internal var addFragmentSelectedLPM
        get() = requireNotNull(
            resourceRepository.getLpmRepository().fromCode(
                savedStateHandle.get<PaymentMethodCode>(
                    SAVE_SELECTED_ADD_LPM
                ) ?: newPaymentSelection?.paymentMethodCreateParams?.typeCode
            ) ?: supportedPaymentMethods.first()
        )
        set(value) = savedStateHandle.set(SAVE_SELECTED_ADD_LPM, value.code)

    /**
     * Request to retrieve the value from the repository happens when initialize any fragment
     * and any fragment will re-update when the result comes back.
     * Represents what the user last selects (add or buy) on the
     * [PaymentOptionsActivity]/[PaymentSheetActivity], and saved/restored from the preferences.
     */
    private val _savedSelection =
        savedStateHandle.getLiveData<SavedSelection>(SAVE_SAVED_SELECTION)
    private val savedSelection: LiveData<SavedSelection> = _savedSelection

    private val _transition = MutableLiveData<Event<TransitionTargetType?>>(Event(null))
    internal val transition: LiveData<Event<TransitionTargetType?>> = _transition

    @VisibleForTesting
    internal val _liveMode = MutableLiveData<Boolean>()
    internal val liveMode: LiveData<Boolean> = _liveMode

    /**
     * On [ComposeFormDataCollectionFragment] this is set every time the details in the add
     * card fragment is determined to be valid (not necessarily selected)
     * On [BasePaymentMethodsListFragment] this is set when a user selects one of the options
     */
    private val _selection = savedStateHandle.getLiveData<PaymentSelection>(SAVE_SELECTION)

    internal val selection: LiveData<PaymentSelection?> = _selection

    private val editing = MutableLiveData(false)

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    internal val _processing = savedStateHandle.getLiveData<Boolean>(SAVE_PROCESSING)
    val processing: LiveData<Boolean> = _processing

    @VisibleForTesting
    internal val _contentVisible = MutableLiveData(true)
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

    var usBankAccountSavedScreenState: USBankAccountFormScreenState? = null

    private var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null
    val linkLauncher = linkPaymentLauncherFactory.create(
        merchantName = merchantName,
        customerEmail = config?.defaultBillingDetails?.email,
        customerPhone = config?.defaultBillingDetails?.phone
    )

    private val _showLinkVerificationDialog = MutableLiveData(false)
    val showLinkVerificationDialog: LiveData<Boolean> = _showLinkVerificationDialog

    /**
     * Function called when the Link verification dialog is dismissed.
     */
    var linkVerificationCallback: LinkVerificationCallback? = null

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
            selection,
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

    init {
        if (_savedSelection.value == null) {
            viewModelScope.launch {
                val savedSelection = withContext(workContext) {
                    prefsRepository.getSavedSelection(isGooglePayReady.asFlow().first())
                }
                savedStateHandle[SAVE_SAVED_SELECTION] = savedSelection
            }
        }

        if (_isResourceRepositoryReady.value == null) {
            viewModelScope.launch {
                resourceRepository.waitUntilLoaded()
                savedStateHandle[SAVE_RESOURCE_REPOSITORY_READY] = true
            }
        }
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

    open fun transitionTo(target: TransitionTargetType) {
        _transition.postValue(Event(target))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun setStripeIntent(stripeIntent: StripeIntent?) {
        savedStateHandle[SAVE_STRIPE_INTENT] = stripeIntent

        /**
         * The settings of values in this function is so that
         * they will be ready in the onViewCreated method of
         * the [BaseAddPaymentMethodFragment]
         */
        val pmsToAdd = getPMsToAdd(stripeIntent, config, resourceRepository.getLpmRepository())
        supportedPaymentMethods = pmsToAdd

        if (stripeIntent != null && supportedPaymentMethods.isEmpty()) {
            onFatal(
                IllegalArgumentException(
                    "None of the requested payment methods" +
                        " (${stripeIntent.paymentMethodTypes})" +
                        " match the supported payment types" +
                        " (${
                        resourceRepository.getLpmRepository().values()
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

    fun getAddFragmentSelectedLpm() =
        savedStateHandle.getLiveData(
            SAVE_SELECTED_ADD_LPM,
            newPaymentSelection?.paymentMethodCreateParams?.typeCode
        ).map {
            resourceRepository.getLpmRepository().fromCode(it)
                ?: supportedPaymentMethods.first()
        }

    fun setEditing(isEditing: Boolean) {
        editing.value = isEditing
    }

    fun setContentVisible(visible: Boolean) {
        _contentVisible.value = visible
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) = runBlocking {
        launch {
            paymentMethod.id?.let { paymentMethodId ->
                savedStateHandle[SAVE_PAYMENT_METHODS] = _paymentMethods.value?.filter {
                    it.id != paymentMethodId
                }

                customerConfig?.let {
                    customerRepository.detachPaymentMethod(
                        it,
                        paymentMethodId
                    )
                }

                if (_paymentMethods.value?.all {
                    it.type != PaymentMethod.Type.USBankAccount
                } == true
                ) {
                    updatePrimaryButtonUIState(
                        primaryButtonUIState.value?.copy(
                            visible = false
                        )
                    )
                    updateBelowButtonText(null)
                }
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    protected fun setupLink(stripeIntent: StripeIntent, completePayment: Boolean) {
        // TODO(brnunes-stripe): Enable Link by deleting the 2 lines below
        _isLinkEnabled.value = false
        return

        if (stripeIntent.paymentMethodTypes.contains(PaymentMethod.Type.Link.code)) {
            viewModelScope.launch {
                when (
                    linkLauncher.setup(
                        stripeIntent,
                        completePayment,
                        (newPaymentSelection as? PaymentSelection.New.Link)?.linkPaymentDetails,
                        this
                    )
                ) {
                    AccountStatus.Verified -> launchLink()
                    AccountStatus.VerificationStarted,
                    AccountStatus.NeedsVerification -> {
                        linkVerificationCallback = { success ->
                            linkVerificationCallback = null
                            _showLinkVerificationDialog.value = false

                            if (success) {
                                launchLink()
                            }
                        }
                        _showLinkVerificationDialog.value = true
                    }
                    AccountStatus.SignedOut -> {}
                }
                _isLinkEnabled.value = true
            }
        } else {
            _isLinkEnabled.value = false
        }
    }

    fun payWithLink(userInput: UserInput) {
        (selection.value as? PaymentSelection.New.Card)?.paymentMethodCreateParams?.let { params ->
            savedStateHandle[SAVE_PROCESSING] = true
            updatePrimaryButtonState(PrimaryButton.State.StartProcessing)

            when (linkLauncher.accountStatus.value) {
                AccountStatus.Verified -> createLinkPaymentDetails(params)
                AccountStatus.VerificationStarted,
                AccountStatus.NeedsVerification -> {
                    linkVerificationCallback = { success ->
                        linkVerificationCallback = null
                        _showLinkVerificationDialog.value = false

                        if (success) {
                            createLinkPaymentDetails(params)
                        } else {
                            savedStateHandle[SAVE_PROCESSING] = false
                            updatePrimaryButtonState(PrimaryButton.State.Ready)
                        }
                    }
                    _showLinkVerificationDialog.value = true
                }
                AccountStatus.SignedOut -> {
                    viewModelScope.launch {
                        linkLauncher.signInWithUserInput(userInput).fold(
                            onSuccess = {
                                // If successful, the account was fetched or created, so try again
                                payWithLink(userInput)
                            },
                            onFailure = {
                                onError(it.localizedMessage)
                                savedStateHandle[SAVE_PROCESSING] = false
                                updatePrimaryButtonState(PrimaryButton.State.Ready)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun createLinkPaymentDetails(paymentMethodCreateParams: PaymentMethodCreateParams) {
        viewModelScope.launch {
            onLinkPaymentDetailsCollected(
                linkLauncher.attachNewCardToAccount(paymentMethodCreateParams).getOrNull()
            )
        }
    }

    fun launchLink() {
        linkActivityResultLauncher?.let { activityResultLauncher ->
            linkLauncher.present(
                activityResultLauncher
            )
            onLinkLaunched()
        }
    }

    /**
     * Method called when the Link UI is launched. Should be used to update the PaymentSheet UI
     * accordingly.
     */
    open fun onLinkLaunched() {
        setContentVisible(false)
    }

    /**
     * Method called with the result of launching the Link UI to collect a payment.
     */
    open fun onLinkActivityResult(result: LinkActivityResult) {
        setContentVisible(true)
    }

    /**
     * Method called after completing collection of payment data for a payment with Link.
     */
    abstract fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails?)

    abstract fun onUserCancel()

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onFinish()

    abstract fun onError(@StringRes error: Int? = null)

    abstract fun onError(error: String? = null)

    /**
     * Used to set up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onCreate`.
     */
    open fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            LinkActivityContract(),
            ::onLinkActivityResult
        )
    }

    /**
     * Used to clean up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onDestroy`.
     */
    open fun unregisterFromActivity() {
        linkActivityResultLauncher = null
    }

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
        internal const val SAVE_SELECTED_ADD_LPM = "selected_add_lpm"
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVE_SAVED_SELECTION = "saved_selection"
        internal const val SAVE_SUPPORTED_PAYMENT_METHOD = "supported_payment_methods"
        internal const val SAVE_PROCESSING = "processing"
        internal const val SAVE_GOOGLE_PAY_READY = "google_pay_ready"
        internal const val SAVE_RESOURCE_REPOSITORY_READY = "resource_repository_ready"
    }
}
