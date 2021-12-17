package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.model.FragmentConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
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
    resourceRepository: ResourceRepository
) : BaseSheetViewModel<PaymentOptionsViewModel.TransitionTarget>(
    config = args.config,
    prefsRepository = prefsRepositoryFactory(args.config?.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    application = application,
    logger = logger,
    injectorKey = injectorKey,
    resourceRepository = resourceRepository
) {
    @VisibleForTesting
    internal val _paymentOptionResult = MutableLiveData<PaymentOptionResult>()
    internal val paymentOptionResult: LiveData<PaymentOptionResult> = _paymentOptionResult

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newCard = args.newCard

    // This is used in the case where the last card was new and not saved. In this scenario
    // when the payment options is opened it should jump to the add card, but if the user
    // presses the back button, they shouldn't transition to it again
    private var hasTransitionToUnsavedCard = false
    private val shouldTransitionToUnsavedCard: Boolean
        get() =
            !hasTransitionToUnsavedCard &&
                (newCard as? PaymentSelection.New)?.let {
                    it.customerRequestedSave != PaymentSelection.CustomerRequestedSave.RequestReuse
                } ?: false

    init {
        _isGooglePayReady.value = args.isGooglePayReady
        setStripeIntent(args.stripeIntent)
        //TODO: Needs to be handle
        _paymentMethods.value = args.paymentMethods
        _processing.postValue(false)
    }

    override fun onFatal(throwable: Throwable) {
        _fatal.value = throwable
        _paymentOptionResult.value = PaymentOptionResult.Failed(throwable)
    }

    override fun onUserCancel() {
        _paymentOptionResult.value =
            PaymentOptionResult.Canceled(mostRecentError = _fatal.value)
    }

    fun onUserSelection() {
        selection.value?.let { paymentSelection ->
            // TODO(michelleb-stripe): Should the payment selection in the event be the saved or new item?
            eventReporter.onSelectPaymentOption(paymentSelection)

            when (paymentSelection) {
                is PaymentSelection.Saved, PaymentSelection.GooglePay -> processExistingCard(
                    paymentSelection
                )
                is PaymentSelection.New -> processNewCard(paymentSelection)
            }
        }
    }

    private fun processExistingCard(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value = PaymentOptionResult.Succeeded(paymentSelection)
    }

    private fun processNewCard(paymentSelection: PaymentSelection) {
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
        private val starterArgsSupplier: () -> PaymentOptionContract.Args
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
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
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val application = applicationSupplier()
            val starterArgs = starterArgsSupplier()
            injectWithFallback(
                starterArgsSupplier().injectorKey,
                FallbackInitializeParam(application, starterArgs.productUsage)
            )
            return subComponentBuilderProvider.get().application(application).args(starterArgs)
                .build().viewModel as T
        }
    }
}
