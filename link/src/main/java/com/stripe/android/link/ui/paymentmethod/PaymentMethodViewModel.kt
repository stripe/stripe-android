package com.stripe.android.link.ui.paymentmethod

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.R
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that controls the PaymentMethod screen, managing what payment method form to show and
 * how the user interacts with it to add a new payment method.
 */
internal class PaymentMethodViewModel @Inject constructor(
    args: LinkActivityContract.Args,
    val linkAccount: LinkAccount,
    private val linkRepository: LinkRepository,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val confirmationManager: ConfirmationManager,
    private val logger: Logger
) : ViewModel() {
    private val stripeIntent = args.stripeIntent

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: Flow<Boolean> = _isProcessing
    val isEnabled: Flow<Boolean> = _isProcessing.map { !it }

    val paymentMethod = SupportedPaymentMethod.Card()

    fun payButtonLabel(resources: Resources) = when (stripeIntent) {
        is PaymentIntent -> Amount(
            requireNotNull(stripeIntent.amount),
            requireNotNull(stripeIntent.currency)
        ).buildPayButtonLabel(resources)
        is SetupIntent -> resources.getString(R.string.stripe_setup_button_label)
    }

    fun startPayment(formValues: Map<IdentifierSpec, FormFieldEntry>) {
        val createParams = paymentMethod.createParams(
            FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
                formValues,
                paymentMethod.type
            )
        )

        viewModelScope.launch {
            _isProcessing.emit(true)
            linkRepository.createPaymentDetails(
                createParams,
                linkAccount.clientSecret
            ).fold(
                onSuccess = {
                    completePayment(it, paymentMethod, formValues)
                },
                onFailure = ::onError
            )
        }
    }

    fun payAnotherWay() {
        navigator.dismiss()
        linkAccountManager.logout()
    }

    private fun completePayment(
        paymentDetails: ConsumerPaymentDetails,
        paymentMethod: SupportedPaymentMethod,
        formValues: Map<IdentifierSpec, FormFieldEntry>
    ) {
        val params = ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
            .create(
                linkAccount.clientSecret,
                paymentDetails.paymentDetails.first(),
                paymentMethod.extraConfirmationParams(formValues)
            )

        confirmationManager.confirmStripeIntent(params) { result ->
            result.fold(
                onSuccess = { paymentResult ->
                    when (paymentResult) {
                        is PaymentResult.Canceled -> {
                            // no-op, let the user continue their flow
                        }
                        is PaymentResult.Failed -> {
                            onError(paymentResult.throwable)
                        }
                        is PaymentResult.Completed ->
                            navigator.dismiss(LinkActivityResult.Success)
                    }
                },
                onFailure = ::onError
            )

            _isProcessing.tryEmit(false)
        }
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        _isProcessing.tryEmit(false)
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().paymentMethodViewModel as T
        }
    }
}
