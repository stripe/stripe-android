package com.stripe.android.link.ui.cardedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.LinkMode
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CardEditViewModel @Inject constructor(
    private val paymentDetailsId: String,
    val linkAccount: LinkAccount,
    private val configuration: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val logger: Logger,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {

    lateinit var paymentDetails: ConsumerPaymentDetails.PaymentDetails
    private val _state = MutableStateFlow(
        value = CardEditState(
            paymentDetailsId = paymentDetailsId,
            isProcessing = false,
            errorMessage = null,
            setAsDefault = false,
            isDefault = false,
            linkAccount = linkAccount,
        )
    )
    val state: StateFlow<CardEditState> = _state

    private val _formElements = MutableStateFlow<List<FormElement>>(emptyList())
    val formElements: StateFlow<List<FormElement>> = _formElements

    init {
        viewModelScope.launch {
            loadCard()
        }
    }

    private suspend fun loadCard() {
        linkAccountManager.listPaymentDetails(
            paymentMethodTypes = configuration.stripeIntent.supportedPaymentMethodTypes(linkAccount)
        ).fold(
            onSuccess = { paymentDetails ->
                val card = paymentDetails.paymentDetails
                    .filterIsInstance<ConsumerPaymentDetails.Card>()
                    .firstOrNull()
//                    .firstOrNull { it.id == paymentDetailsId }

                if (card == null) {
//                    logger.error("CardEditViewModel error: ", CardNotFoundException())
                    return@fold dismissWithResult(LinkActivityResult.Failed(Exception()))
                }
//                CardDefinition.uiDefinitionFactory().formElements(
//                    definition = CardDefinition,
//                    metadata = card.metadata(),
//                    sharedDataSpecs = emptyList(),
//                    arguments = UiDefinitionFactory.Arguments()
//                )
                val formElements = card.formHelper().formElementsForCode(CardDefinition.type.code)
                val formData = FormData(
                    formElements = formElements,
                    formArguments = FormArguments(
                        paymentMethodCode = CardDefinition.type.code,
                        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                        merchantName = configuration.merchantName,
                        amount = null,
                        shippingDetails = AddressDetails(
                            name = configuration.customerInfo.name,
                            address = configuration.shippingValues?.let { null },
                            phoneNumber = configuration.customerInfo.phone,
                            isCheckboxSelected = false
                        ),
                        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(
                            overrideAllowRedisplay = null
                        ),
                        hasIntentToSetup = false
                    ),
                    usBankAccountFormArguments = USBankAccountFormArguments.create(
                        stripeIntent = configuration.stripeIntent,
                        linkMode = LinkMode.LinkCardBrand,
                        shippingDetails = null
                    ),
                    onFormFieldValuesChanged = {}
                )
                _state.update {
                    it.copy(
                        formData = formData
                    )
                }
            },
            onFailure = { e ->
                logger.error("CardEditViewModel error: ", e)
                dismissWithResult(LinkActivityResult.Failed(e))
            }
        )
    }

    private fun ConsumerPaymentDetails.PaymentDetails.formHelper(): FormHelper {
        return FormHelper.create(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = metadata()
        )
    }

    private fun ConsumerPaymentDetails.PaymentDetails.metadata(): PaymentMethodMetadata {
        return PaymentMethodMetadata(
            stripeIntent = configuration.stripeIntent,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            allowsDelayedPaymentMethods = false,
            allowsPaymentMethodsRequiringShippingAddress = true,
            paymentMethodOrder = emptyList(),
            cbcEligibility = CardBrandChoiceEligibility.create(
                isEligible = false,
                preferredNetworks = emptyList(),
            ),
            merchantName = configuration.merchantName,
            defaultBillingDetails = null,
            shippingDetails = null,
            hasCustomerConfiguration = true,
            sharedDataSpecs = emptyList(),
            externalPaymentMethodSpecs = emptyList(),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(null),
            linkInlineConfiguration = null,
            linkMode = LinkMode.LinkCardBrand,
            paymentMethodIncentive = null,
            isGooglePayReady = false,
            cardBrandFilter = DefaultCardBrandFilter
        )
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            paymentDetailsId: String,
            linkAccount: LinkAccount,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    CardEditViewModel(
                        paymentDetailsId = paymentDetailsId,
                        configuration = parentComponent.configuration,
                        linkAccountManager = parentComponent.linkAccountManager,
                        logger = parentComponent.logger,
                        linkAccount = linkAccount,
                        dismissWithResult = dismissWithResult,
                        cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory
                    )
                }
            }
        }
    }
}
