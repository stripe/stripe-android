package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.utils.requireApplication
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@JvmSuppressWildcards
internal class PaymentOptionsViewModel @Inject constructor(
    args: PaymentOptionContract.Args,
    prefsRepositoryFactory: (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    @IOContext workContext: CoroutineContext,
    application: Application,
    logger: Logger,
    @InjectorKey injectorKey: String,
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    addressResourceRepository: ResourceRepository<AddressRepository>,
    savedStateHandle: SavedStateHandle,
    linkLauncher: LinkPaymentLauncher
) : BaseSheetViewModel<PaymentOptionsViewModel.TransitionTarget>(
    application = application,
    initialState = args.state,
    config = args.state.config,
    prefsRepository = prefsRepositoryFactory(args.state.config?.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    logger = logger,
    injectorKey = injectorKey,
    lpmResourceRepository = lpmResourceRepository,
    addressResourceRepository = addressResourceRepository,
    savedStateHandle = savedStateHandle,
    linkLauncher = linkLauncher
) {
    @VisibleForTesting
    internal val _paymentOptionResult = MutableLiveData<PaymentOptionResult>()
    internal val paymentOptionResult: LiveData<PaymentOptionResult> = _paymentOptionResult

    private val _error = MutableLiveData<String>()
    internal val error: LiveData<String>
        get() = _error

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newPaymentSelection = args.state.newPaymentSelection

    override var linkInlineSelection = MutableLiveData<PaymentSelection.New.LinkInline?>(
        args.state.newPaymentSelection as? PaymentSelection.New.LinkInline,
    )

    // This is used in the case where the last card was new and not saved. In this scenario
    // when the payment options is opened it should jump to the add card, but if the user
    // presses the back button, they shouldn't transition to it again
    internal var hasTransitionToUnsavedLpm
        get() = savedStateHandle.get<Boolean>(SAVE_STATE_HAS_OPEN_SAVED_LPM)
        set(value) = savedStateHandle.set(SAVE_STATE_HAS_OPEN_SAVED_LPM, value)

    private val shouldTransitionToUnsavedCard: Boolean
        get() = hasTransitionToUnsavedLpm != true && newPaymentSelection != null

    init {
        val linkState = args.state.linkState

        if (linkState != null) {
            setupLink(linkState)
        }

        // If we are not recovering from don't keep activities than the resources
        // repository is loaded, and we should save off the LPM repository server specs so
        // it can be restored after don't keep activities or process killed.
        if (lpmResourceRepository.getRepository().isLoaded()) {
            lpmServerSpec =
                lpmResourceRepository.getRepository().serverSpecLoadingState.serverLpmSpecs
        }
    }

    override fun onFatal(throwable: Throwable) {
        mostRecentError = throwable
        _paymentOptionResult.value =
            PaymentOptionResult.Failed(
                error = throwable,
                paymentMethods = fullState?.customerPaymentMethods,
            )
    }

    override fun onUserCancel() {
        _paymentOptionResult.value =
            PaymentOptionResult.Canceled(
                mostRecentError = mostRecentError,
                paymentMethods = fullState?.customerPaymentMethods,
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

    private fun setupLink(linkState: LinkState) {
        // TODO In PSL!
        if (linkState.isReadyForUse) {
            // If account exists, select Link by default
            updateFullState { it.copy(selection = PaymentSelection.Link) }
        }
    }

    override fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails.New?) {
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
        updateFullState { it.copy(isProcessing = false) }
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
                paymentMethods = fullState?.customerPaymentMethods,
            )
    }

    private fun processNewPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value =
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = fullState?.customerPaymentMethods,
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
        private val starterArgsSupplier: () -> PaymentOptionContract.Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<PaymentOptionsViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val starterArgs = starterArgsSupplier()

            val injector = injectWithFallback(
                starterArgs.injectorKey,
                FallbackInitializeParam(application, starterArgs.productUsage)
            )

            val subcomponent = subComponentBuilderProvider.get()
                .application(application)
                .args(starterArgs)
                .savedStateHandle(savedStateHandle)
                .build()

            val viewModel = subcomponent.viewModel
            viewModel.injector = requireNotNull(injector as NonFallbackInjector)
            return viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector {
            val component = DaggerPaymentOptionsViewModelFactoryComponent.builder()
                .context(arg.application)
                .productUsage(arg.productUsage)
                .build()
            component.inject(this)
            return component
        }
    }

    companion object {
        const val SAVE_STATE_HAS_OPEN_SAVED_LPM = "hasTransitionToUnsavedLpm"
    }
}
