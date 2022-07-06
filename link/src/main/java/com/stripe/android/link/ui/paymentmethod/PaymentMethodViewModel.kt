package com.stripe.android.link.ui.paymentmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.R
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val confirmationManager: ConfirmationManager,
    private val logger: Logger,
    private val formControllerProvider: Provider<FormControllerSubcomponent.Builder>
) : ViewModel() {
    private val stripeIntent = args.stripeIntent

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    val isEnabled: Flow<Boolean> = _isProcessing.map { !it }

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    private val isRootScreen = navigator.isOnRootScreen() == true

    val secondaryButtonLabel = if (isRootScreen) {
        R.string.wallet_pay_another_way
    } else {
        R.string.cancel
    }

    val paymentMethod = SupportedPaymentMethod.Card

    val formController = MutableStateFlow<FormController?>(null)

    fun init(loadFromArgs: Boolean) {
        formController.value =
            formControllerProvider.get()
                .formSpec(LayoutSpec(paymentMethod.formSpec))
                .viewOnlyFields(emptySet())
                .viewModelScope(viewModelScope)
                .initialValues(
                    (args.selectedPaymentDetails as? LinkPaymentDetails.New)
                        ?.takeIf { loadFromArgs }?.buildFormValues() ?: emptyMap()
                )
                .stripeIntent(args.stripeIntent)
                .merchantName(args.merchantName)
                .build().formController
    }

    fun startPayment(formValues: Map<IdentifierSpec, FormFieldEntry>) {
        clearError()
        val paymentMethodCreateParams =
            FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
                formValues,
                paymentMethod.type.code,
                paymentMethod.requiresMandate
            )

        viewModelScope.launch {
            _isProcessing.value = true

            linkAccountManager.createPaymentDetails(
                paymentMethod,
                paymentMethodCreateParams,
                linkAccount.email,
                args.stripeIntent
            ).fold(
                onSuccess = { paymentDetails ->
                    if (args.completePayment) {
                        completePayment(paymentDetails)
                    } else {
                        navigator.dismiss(LinkActivityResult.Success.Selected(paymentDetails))
                    }
                },
                onFailure = ::onError
            )
        }
    }

    fun onSecondaryButtonClick() {
        if (isRootScreen) {
            payAnotherWay()
        } else {
            navigator.onBack()
        }
    }

    private fun payAnotherWay() {
        clearError()
        navigator.dismiss()
        linkAccountManager.logout()
    }

    private fun completePayment(linkPaymentDetails: LinkPaymentDetails) {
        val params = ConfirmStripeIntentParamsFactory.createFactory(stripeIntent)
            .createConfirmStripeIntentParams(linkPaymentDetails.paymentMethodCreateParams)

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

            _isProcessing.value = false
        }
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        _isProcessing.value = false
        _errorMessage.value = it
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: NonFallbackInjector,
        private val loadFromArgs: Boolean
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().paymentMethodViewModel.apply {
                    init(loadFromArgs)
                } as T
        }
    }
}
