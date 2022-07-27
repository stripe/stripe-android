package com.stripe.android.paymentsheet

import android.app.Application
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher.Companion.LINK_ENABLED
import com.stripe.android.link.injection.LinkPaymentLauncherFactory
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@JvmSuppressWildcards
internal class PaymentOptionsViewModel @Inject constructor(
    args: PaymentOptionContract.Args,
    prefsRepositoryFactory:
        (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    @IOContext workContext: CoroutineContext,
    application: Application,
    logger: Logger,
    @InjectorKey injectorKey: String,
    resourceRepository: ResourceRepository,
    savedStateHandle: SavedStateHandle,
    linkPaymentLauncherFactory: LinkPaymentLauncherFactory
) : BaseSheetViewModel<PaymentOptionsViewModel.TransitionTarget>(
    application = application,
    config = args.config,
    prefsRepository = prefsRepositoryFactory(args.config?.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    logger = logger,
    injectorKey = injectorKey,
    resourceRepository = resourceRepository,
    savedStateHandle = savedStateHandle,
    linkPaymentLauncherFactory = linkPaymentLauncherFactory
) {
    @VisibleForTesting
    internal val _paymentOptionResult = MutableLiveData<PaymentOptionResult>()
    internal val paymentOptionResult: LiveData<PaymentOptionResult> = _paymentOptionResult

    private val _error = MutableLiveData<String>()
    internal val error: LiveData<String>
        get() = _error

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newPaymentSelection = args.newLpm

    // This is used in the case where the last card was new and not saved. In this scenario
    // when the payment options is opened it should jump to the add card, but if the user
    // presses the back button, they shouldn't transition to it again
    internal var hasTransitionToUnsavedLpm
        get() = savedStateHandle.get<Boolean>(SAVE_STATE_HAS_OPEN_SAVED_LPM)
        set(value) = savedStateHandle.set(SAVE_STATE_HAS_OPEN_SAVED_LPM, value)

    private val shouldTransitionToUnsavedCard: Boolean
        get() = hasTransitionToUnsavedLpm != true && newPaymentSelection != null

    init {
        savedStateHandle[SAVE_GOOGLE_PAY_READY] = args.isGooglePayReady
        setupLink(args.stripeIntent)

        // After recovering from don't keep activities the stripe intent will be saved,
        // calling setStripeIntent would require the repository be initialized, which
        // would not be the case.
        if (stripeIntent.value == null) {
            setStripeIntent(args.stripeIntent)
        }
        savedStateHandle[SAVE_PAYMENT_METHODS] = args.paymentMethods
        savedStateHandle[SAVE_PROCESSING] = false

        // If we are not recovering from don't keep activities than the resources
        // repository is loaded, and we should save off the LPM repository server specs so
        // it can be restored after don't keep activities or process killed.
        if (resourceRepository.getLpmRepository().isLoaded()) {
            lpmServerSpec =
                resourceRepository.getLpmRepository().serverSpecLoadingState.serverLpmSpecs
        }
    }

    override fun onFatal(throwable: Throwable) {
        _fatal.value = throwable
        _paymentOptionResult.value =
            PaymentOptionResult.Failed(
                error = throwable,
                paymentMethods = _paymentMethods.value
            )
    }

    override fun onUserCancel() {
        _paymentOptionResult.value =
            PaymentOptionResult.Canceled(
                mostRecentError = _fatal.value,
                paymentMethods = _paymentMethods.value
            )
    }

    override fun onFinish() {
        onUserSelection()
    }

    override fun onError(@StringRes error: Int?) =
        onError(error?.let { getApplication<Application>().getString(it) })

    override fun onError(error: String?) {
        error?.let {
            _error.value = it
        }
    }

    fun onUserSelection() {
        selection.value?.let { paymentSelection ->
            // TODO(michelleb-stripe): Should the payment selection in the event be the saved or new item?
            eventReporter.onSelectPaymentOption(paymentSelection)

            when (paymentSelection) {
                is PaymentSelection.Saved ->
                    // We don't want the USBankAccount selection to close the payment sheet right
                    // away, the user needs to accept a mandate
                    if (paymentSelection.paymentMethod.type != PaymentMethod.Type.USBankAccount) {
                        processExistingPaymentMethod(
                            paymentSelection
                        )
                    }
                is PaymentSelection.GooglePay,
                is PaymentSelection.Link -> processExistingPaymentMethod(paymentSelection)
                is PaymentSelection.New -> processNewPaymentMethod(paymentSelection)
            }
        }
    }

    override fun setupLink(stripeIntent: StripeIntent) {
        if (LINK_ENABLED &&
            stripeIntent.paymentMethodTypes.contains(PaymentMethod.Type.Link.code)
        ) {
            viewModelScope.launch {
                when (
                    linkLauncher.setup(
                        stripeIntent,
                        (newPaymentSelection as? PaymentSelection.New.LinkInline)
                            ?.linkPaymentDetails,
                        this
                    )
                ) {
                    AccountStatus.Verified,
                    AccountStatus.VerificationStarted,
                    AccountStatus.NeedsVerification -> {
                        // If account exists, select link by default
                        savedStateHandle[SAVE_SELECTION] = PaymentSelection.Link
                    }
                    AccountStatus.SignedOut -> {}
                }
                _isLinkEnabled.value = true
            }
        } else {
            _isLinkEnabled.value = false
        }
    }

    override fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails?) {
        linkPaymentDetails?.let {
            // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
            updateSelection(PaymentSelection.New.LinkInline(it))
            onUserSelection()
        } ?: run {
            // Creating Link PaymentDetails failed, fallback to regular checkout.
            // paymentSelection is already set to the card parameters from the form.
            onUserSelection()
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        _processing.value = false
    }

    override fun updateSelection(selection: PaymentSelection?) {
        super.updateSelection(selection)
        when {
            selection is PaymentSelection.Saved &&
                selection.paymentMethod.type == PaymentMethod.Type.USBankAccount -> {
                updateBelowButtonText(
                    ACHText.getContinueMandateText(getApplication())
                )
                updatePrimaryButtonUIState(
                    PrimaryButton.UIState(
                        label = getApplication<Application>().getString(
                            R.string.stripe_continue_button_label
                        ),
                        visible = true,
                        enabled = true,
                        onClick = {
                            processExistingPaymentMethod(selection)
                        }
                    )
                )
            }
            selection is PaymentSelection.Saved ||
                selection is PaymentSelection.GooglePay -> {
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        visible = false
                    )
                )
            }
            else -> {
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        label = getApplication<Application>().getString(
                            R.string.stripe_continue_button_label
                        ),
                        visible = true,
                        enabled = true,
                        onClick = {
                            onUserSelection()
                        }
                    )
                )
            }
        }
    }

    private fun processExistingPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value =
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = _paymentMethods.value
            )
    }

    private fun processNewPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value =
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = _paymentMethods.value
            )
    }

    fun resolveTransitionTarget(config: FragmentConfig) {
        if (shouldTransitionToUnsavedCard) {
            hasTransitionToUnsavedLpm = true
            transitionTo(
                // Until we add a flag to the transitionTarget to specify if we want to add the item
                // to the backstack, we need to use the full sheet.
                TransitionTarget.AddPaymentMethodFull(config)
            )
        }
    }

    internal sealed class TransitionTarget {
        abstract val fragmentConfig: FragmentConfig

        // User has saved PM's and is selected
        data class SelectSavedPaymentMethod(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget()

        // User has saved PM's and is adding a new one
        data class AddPaymentMethodFull(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget()

        // User has no saved PM's
        data class AddPaymentMethodSheet(
            override val fragmentConfig: FragmentConfig
        ) : TransitionTarget()
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val starterArgsSupplier: () -> PaymentOptionContract.Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs),
        Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val productUsage: Set<String>
        )

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerPaymentOptionsViewModelFactoryComponent.builder()
                .context(arg.application)
                .productUsage(arg.productUsage)
                .build().inject(this)
        }

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<PaymentOptionsViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            val application = applicationSupplier()
            val starterArgs = starterArgsSupplier()
            injectWithFallback(
                starterArgsSupplier().injectorKey,
                FallbackInitializeParam(application, starterArgs.productUsage)
            )
            return subComponentBuilderProvider.get()
                .application(application)
                .args(starterArgs)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }
    }

    companion object {
        const val SAVE_STATE_HAS_OPEN_SAVED_LPM = "hasTransitionToUnsavedLpm"
    }
}
