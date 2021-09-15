package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.Logger
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class PaymentOptionsViewModel(
    args: PaymentOptionContract.Args,
    prefsRepository: PrefsRepository,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    workContext: CoroutineContext,
    application: Application,
    logger: Logger
) : BaseSheetViewModel<PaymentOptionsViewModel.TransitionTarget>(
    config = args.config,
    prefsRepository = prefsRepository,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    application = application,
    logger = logger
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
                (newCard as? PaymentSelection.New)?.let { !it.shouldSavePaymentMethod } ?: false

    init {
        _isGooglePayReady.value = args.isGooglePayReady
        setStripeIntent(args.stripeIntent)
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

    fun resolveTransitionTarget(config: com.stripe.android.paymentsheet.model.FragmentConfig) {
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
        abstract val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig

        // User has saved PM's and is selected
        data class SelectSavedPaymentMethod(
            override val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig
        ) : TransitionTarget()

        // User has saved PM's and is adding a new one
        data class AddPaymentMethodFull(
            override val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig
        ) : TransitionTarget()

        // User has no saved PM's
        data class AddPaymentMethodSheet(
            override val fragmentConfig: com.stripe.android.paymentsheet.model.FragmentConfig
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
        lateinit var eventReporter: EventReporter

        @Inject
        lateinit var customerRepository: CustomerRepository

        @Inject
        @IOContext
        lateinit var workContext: CoroutineContext

        @Inject
        @JvmSuppressWildcards
        lateinit var prefsRepositoryFactory:
            (PaymentSheet.CustomerConfiguration?) -> PrefsRepository

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val application = applicationSupplier()
            val starterArgs = starterArgsSupplier()

            val logger = Logger.getInstance(starterArgs.enableLogging)
            WeakMapInjectorRegistry.retrieve(starterArgsSupplier().injectorKey)?.let {
                logger.info(
                    "Injector available, " +
                        "injecting dependencies into PaymentOptionsViewModel.Factory"
                )
                it.inject(this)
            } ?: run {
                logger.info(
                    "Injector unavailable, " +
                        "initializing dependencies of PaymentOptionsViewModel.Factory"
                )
                fallbackInitialize(
                    FallbackInitializeParam(application, starterArgs.productUsage)
                )
            }

            return PaymentOptionsViewModel(
                starterArgs,
                prefsRepositoryFactory(starterArgs.config?.customer),
                eventReporter,
                customerRepository,
                workContext,
                applicationSupplier(),
                logger
            ) as T
        }
    }
}
