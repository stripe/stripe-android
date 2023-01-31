package com.stripe.android.link.ui.cardedit

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.NonFallbackInjectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.link.ui.wallet.PaymentDetailsResult
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.forms.LinkCardForm
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class CardEditViewModel @Inject constructor(
    val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val logger: Logger,
    private val args: LinkActivityContract.Args,
    private val formControllerProvider: Provider<FormControllerSubcomponent.Builder>
) : ViewModel() {

    lateinit var paymentDetails: ConsumerPaymentDetails.PaymentDetails
    val isDefault by lazy { paymentDetails.isDefault }

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing
    val isEnabled: Flow<Boolean> = _isProcessing.map { !it }

    val formController = MutableStateFlow<FormController?>(null)

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    private val _setAsDefault = MutableStateFlow(false)
    val setAsDefault: StateFlow<Boolean> = _setAsDefault

    @VisibleForTesting
    fun initWithPaymentDetailsId(paymentDetailsId: String) {
        viewModelScope.launch {
            linkAccountManager.listPaymentDetails().fold(
                onSuccess = { response ->
                    response.paymentDetails.filterIsInstance<ConsumerPaymentDetails.Card>()
                        .firstOrNull { it.id == paymentDetailsId }?.let {
                            paymentDetails = it

                            formController.value = formControllerProvider.get()
                                .formSpec(LinkCardForm)
                                .initialValues(it.buildInitialFormValues())
                                .viewOnlyFields(setOf(IdentifierSpec.CardNumber))
                                .viewModelScope(viewModelScope)
                                .stripeIntent(args.stripeIntent)
                                .shippingValues(null)
                                .merchantName(args.merchantName)
                                .build().formController
                        } ?: dismiss(
                        PaymentDetailsResult.Failure(
                            ErrorMessage.Raw("Payment details $paymentDetailsId not found.")
                        ),
                        userInitiated = false
                    )
                },
                onFailure = {
                    dismiss(
                        PaymentDetailsResult.Failure(it.getErrorMessage()),
                        userInitiated = false
                    )
                }
            )
        }
    }

    fun setAsDefault(checked: Boolean) {
        _setAsDefault.value = checked
    }

    fun updateCard(formValues: Map<IdentifierSpec, FormFieldEntry>) {
        clearError()
        _isProcessing.value = true

        val paymentMethodCreateParams =
            FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
                formValues,
                PaymentMethod.Type.Card.code,
                requiresMandate = false
            )

        viewModelScope.launch {
            val updateParams = ConsumerPaymentDetailsUpdateParams(
                id = paymentDetails.id,
                isDefault = setAsDefault.value.takeUnless { isDefault || it == isDefault },
                cardPaymentMethodCreateParams = paymentMethodCreateParams
            )

            linkAccountManager.updatePaymentDetails(updateParams).fold(
                onSuccess = {
                    _isProcessing.value = false
                    dismiss(PaymentDetailsResult.Success(paymentDetails.id), userInitiated = false)
                },
                onFailure = ::onError
            )
        }
    }

    fun dismiss(result: PaymentDetailsResult, userInitiated: Boolean) {
        navigator.setResult(PaymentDetailsResult.KEY, result)
        navigator.onBack(userInitiated)
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        _isProcessing.value = false
        _errorMessage.value = it
    }

    private fun ConsumerPaymentDetails.Card.buildInitialFormValues() = mapOf(
        IdentifierSpec.CardNumber to "•••• $last4",
        IdentifierSpec.CardBrand to brand.code,
        IdentifierSpec.CardExpMonth to expiryMonth.toString().padStart(length = 2, padChar = '0'),
        IdentifierSpec.CardExpYear to expiryYear.toString()
    ).plus(
        billingAddress?.countryCode?.value?.let {
            mapOf(IdentifierSpec.Country to it)
        } ?: emptyMap()
    ).plus(
        billingAddress?.postalCode?.let { mapOf(IdentifierSpec.PostalCode to it) } ?: emptyMap()
    )

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: NonFallbackInjector,
        private val paymentDetailsId: String
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().cardEditViewModel.apply {
                    initWithPaymentDetailsId(paymentDetailsId)
                } as T
        }
    }
}
