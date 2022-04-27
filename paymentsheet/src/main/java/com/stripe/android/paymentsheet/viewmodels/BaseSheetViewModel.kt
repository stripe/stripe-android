package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
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
import com.stripe.android.link.injection.LinkPaymentLauncherFactory
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
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment
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
        get() = savedStateHandle.get<List<SupportedPaymentMethod>>(
            SAVE_SUPPORTED_PAYMENT_METHOD
        ) ?: emptyList()
        set(value) = savedStateHandle.set(SAVE_SUPPORTED_PAYMENT_METHOD, value)

    @VisibleForTesting
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

    private var addFragmentSelectedLPM =
        savedStateHandle.get<SupportedPaymentMethod>(SAVE_SELECTED_ADD_LPM)

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

    @VisibleForTesting
    internal val _processing = savedStateHandle.getLiveData<Boolean>(SAVE_PROCESSING)
    val processing: LiveData<Boolean> = _processing

    @VisibleForTesting
    internal val _contentVisible = MutableLiveData(true)
    internal val contentVisible: LiveData<Boolean> = _contentVisible.distinctUntilChanged()

    private val _primaryButtonText = MutableLiveData<String>()
    val primaryButtonText: LiveData<String>
        get() = _primaryButtonText

    private val primaryButtonEnabled = MutableLiveData<Boolean?>()

    private val _primaryButtonOnClick = MutableLiveData<() -> Unit>()
    val primaryButtonOnClick: LiveData<() -> Unit>
        get() = _primaryButtonOnClick

    private val _notesText = MutableLiveData<String?>()
    internal val notesText: LiveData<String?>
        get() = _notesText

    protected var linkActivityResultLauncher:
        ActivityResultLauncher<LinkActivityContract.Args>? = null
    val linkLauncher = linkPaymentLauncherFactory.create(merchantName, null)

    private val _showLinkVerificationDialog = MutableLiveData(false)
    val showLinkVerificationDialog: LiveData<Boolean> = _showLinkVerificationDialog

    /**
     * This should be initialized from the starter args, and then from that
     * point forward it will be the last valid card seen or entered in the add card view.
     * In contrast to selection, this field will not be updated by the list fragment. On the
     * Payment Sheet it is used to save a new card that is added for when you go back to the list
     * and reopen the card view. It is used on the Payment Options sheet similar to what is
     * described above, and when you have an unsaved card.
     */
    abstract var newLpm: PaymentSelection.New?

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
            primaryButtonEnabled,
            buttonsEnabled,
            selection,
        ).forEach { source ->
            addSource(source) {
                value = primaryButtonEnabled.value
                    ?: (
                        buttonsEnabled.value == true &&
                            selection.value != null
                        )
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
        val pmsToAdd = SupportedPaymentMethod.getPMsToAdd(stripeIntent, config)
        savedStateHandle[SAVE_SUPPORTED_PAYMENT_METHOD] = pmsToAdd

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
                savedStateHandle[SAVE_AMOUNT] = Amount(
                    requireNotNull(stripeIntent.amount),
                    requireNotNull(stripeIntent.currency)
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

    fun updatePrimaryButtonText(text: String) {
        _primaryButtonText.value = text
    }

    fun updatePrimaryButtonEnabled(enabled: Boolean) {
        primaryButtonEnabled.value = enabled
    }

    fun updatePrimaryButtonOnClick(onPress: () -> Unit) {
        _primaryButtonOnClick.value = onPress
    }

    fun updateNotes(text: String?) {
        _notesText.value = text
    }

    fun updateSelection(selection: PaymentSelection?) {
        if (selection is PaymentSelection.New) {
            newLpm = selection
        }
        primaryButtonEnabled.value = null
        savedStateHandle[SAVE_SELECTION] = selection
    }

    fun setAddFragmentSelectedLPM(lpm: SupportedPaymentMethod) {
        savedStateHandle[SAVE_SELECTED_ADD_LPM] = lpm
    }

    fun getAddFragmentSelectedLpm() =
        savedStateHandle.getLiveData(
            SAVE_SELECTED_ADD_LPM,
            SupportedPaymentMethod.fromCode(
                newLpm?.paymentMethodCreateParams?.typeCode
            ) ?: SupportedPaymentMethod.Card
        )

    fun getAddFragmentSelectedLpmValue() =
        savedStateHandle.get<SupportedPaymentMethod>(
            SAVE_SELECTED_ADD_LPM
        ) ?: SupportedPaymentMethod.Card

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
            }
        }
    }

    protected fun setupLink(unused: StripeIntent) {
        // TODO(brnunes-stripe): Enable Link
//        if (stripeIntent.paymentMethodTypes.contains(PaymentMethod.Type.Link.code)) {
//            viewModelScope.launch {
//                when (linkLauncher.setup(stripeIntent)) {
//                    AccountStatus.Verified -> launchLink()
//                    AccountStatus.VerificationStarted,
//                    AccountStatus.NeedsVerification -> _showLinkVerificationDialog.value = true
//                    AccountStatus.SignedOut -> {}
//                }
//                _isLinkEnabled.value = true
//            }
//        } else {
        _isLinkEnabled.value = false
//        }
    }

    fun onLinkVerificationDismissed() {
        _showLinkVerificationDialog.value = false
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
     * Method called with the result of a Link payment.
     */
    open fun onLinkPaymentResult(result: LinkActivityResult) {
        setContentVisible(true)
    }

    abstract fun onUserCancel()

    /**
     * Used to set up any dependencies that require a reference to the current Activity.
     * Must be called from the Activity's `onCreate`.
     */
    open fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        linkActivityResultLauncher = activityResultCaller.registerForActivityResult(
            LinkActivityContract(),
            ::onLinkPaymentResult
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
