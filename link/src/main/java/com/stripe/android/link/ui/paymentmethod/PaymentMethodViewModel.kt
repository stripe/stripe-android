package com.stripe.android.link.ui.paymentmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.payments.paymentlauncher.PaymentResult
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
    val args: LinkActivityContract.Args,
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

    fun startPayment(formValues: Map<IdentifierSpec, FormFieldEntry>) {
        val paymentMethodCreateParams =
            FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
                formValues,
                paymentMethod.type
            )

        viewModelScope.launch {
            _isProcessing.emit(true)

            linkRepository.createPaymentDetails(
                paymentMethod.createParams(
                    paymentMethodCreateParams,
                    linkAccount.email
                ),
                linkAccount.clientSecret,
                args.stripeIntent
            ).fold(
                onSuccess = { paymentDetails ->
                    if (args.completePayment) {
                        completePayment(paymentDetails, paymentMethod)
                    } else {
                        navigator.dismiss(LinkActivityResult.Success.Selected(paymentDetails))
                    }
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
        linkPaymentDetails: LinkPaymentDetails,
        paymentMethod: SupportedPaymentMethod
    ) {
        val params = ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
            .createConfirmStripeIntentParams(
                linkAccount.clientSecret,
                linkPaymentDetails.paymentDetails,
                paymentMethod.extraConfirmationParams(linkPaymentDetails.paymentMethodCreateParams)
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
                            navigator.dismiss(LinkActivityResult.Success.Completed)
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
