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
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.injection.LinkPaymentLauncherFactory
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.model.PaymentMethod
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
    override var newLpm = args.newLpm

    // This is used in the case where the last card was new and not saved. In this scenario
    // when the payment options is opened it should jump to the add card, but if the user
    // presses the back button, they shouldn't transition to it again
    private var hasTransitionToUnsavedCard = false
    private val shouldTransitionToUnsavedCard: Boolean
        get() =
            !hasTransitionToUnsavedCard && newLpm != null

    init {
        savedStateHandle[SAVE_GOOGLE_PAY_READY] = args.isGooglePayReady
        setupLink(args.stripeIntent, false)
        setStripeIntent(args.stripeIntent)
        savedStateHandle[SAVE_PAYMENT_METHODS] = args.paymentMethods
        savedStateHandle[SAVE_PROCESSING] = false
    }

    override fun onFatal(throwable: Throwable) {
        _fatal.value = throwable
        _paymentOptionResult.value = PaymentOptionResult.Failed(throwable)
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

    override fun onError(@StringRes error: Int?) {
        error?.let {
            _error.value = getApplication<Application>().getString(error)
        }
    }

    fun onUserSelection() {
        selection.value?.let { paymentSelection ->
            // TODO(michelleb-stripe): Should the payment selection in the event be the saved or new item?
            eventReporter.onSelectPaymentOption(paymentSelection)

            when (paymentSelection) {
                is PaymentSelection.Saved -> {
                    // We don't want the USBankAccount selection to close the payment sheet right
                    // away, the user needs to accept a mandate
                    if (paymentSelection.paymentMethod.type != PaymentMethod.Type.USBankAccount) {
                        processExistingPaymentMethod(
                            paymentSelection
                        )
                    }
                }
                is PaymentSelection.GooglePay -> processExistingPaymentMethod(paymentSelection)
                is PaymentSelection.New -> processNewPaymentMethod(paymentSelection)
            }
        }
    }

    override fun onLinkLaunched() {
        super.onLinkLaunched()
        _processing.value = true
    }

    override fun onLinkPaymentResult(result: LinkActivityResult) {
        when (result) {
            is LinkActivityResult.Success.Selected -> {
                val linkSelection = PaymentSelection.New.Link(
                    result.paymentDetails, result.paymentMethodCreateParams
                )
                updateSelection(linkSelection)
                onUserSelection()
            }
            else -> {
                super.onLinkPaymentResult(result)
                _processing.value = false
            }
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
            selection is PaymentSelection.Saved -> {
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
        _paymentOptionResult.value = PaymentOptionResult.Succeeded(paymentSelection)
    }

    private fun processNewPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value = PaymentOptionResult.Succeeded(paymentSelection)
    }

    fun resolveTransitionTarget(config: FragmentConfig) {
        if (shouldTransitionToUnsavedCard) {
            hasTransitionToUnsavedCard = true
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
}
