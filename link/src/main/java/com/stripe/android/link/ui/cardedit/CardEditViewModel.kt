package com.stripe.android.link.ui.cardedit

import androidx.compose.ui.util.fastFold
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CardEditViewModel @Inject constructor(
    private val paymentDetailsId: String,
    private val linkAccount: LinkAccount,
    private val configuration: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val logger: Logger,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {

    private val _formElements = MutableStateFlow(listOf<FormElement>())
    val formElements: StateFlow<List<FormElement>> = _formElements

    val primaryButtonEnabled = formElements.mapAsStateFlow { elements ->
        if (elements.isEmpty()) return@mapAsStateFlow false
        elements.fastFold(
            initial = true
        ) { current, element ->
            current && element.isComplete()
        }
    }

    private fun FormElement.isComplete(): Boolean {
        return getFormFieldValueFlow().value.fastFold(
            initial = true
        ) { current, fieldValue ->
            current && fieldValue.second.isComplete
        }
    }

    private val _viewState = MutableStateFlow(
        value = CardEditState(
            isProcessing = false,
            isDefault = false
        )
    )
    val viewState: StateFlow<CardEditState> = _viewState

    private val cardDetailsSpec = CardDetailsSectionSpec(IdentifierSpec.Generic("card_details_section"))
    private val billingSpec = CardBillingSpec(allowedCountryCodes = CountryUtils.supportedBillingCountries)

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
                    .firstOrNull { it.id == paymentDetailsId }

                if (card == null) {
                    logger.error("CardEditViewModel error: ", CardNotFoundException())
                    return@fold dismissWithResult(LinkActivityResult.Failed(CardNotFoundException()))
                }
                val formElements = card.formElements()
                _formElements.value = formElements
            },
            onFailure = { e ->
                logger.error("CardEditViewModel error: ", e)
                dismissWithResult(LinkActivityResult.Failed(e))
            }
        )
    }

    private fun ConsumerPaymentDetails.Card.formElements(): List<FormElement> {
        val cardDetailElement = cardDetailsSpec.transform(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "•••• $last4",
                IdentifierSpec.CardBrand to brand.code,
                IdentifierSpec.CardExpMonth to expiryMonth.toString().padStart(length = 2, padChar = '0'),
                IdentifierSpec.CardExpYear to expiryYear.toString()
            )
        )

        val billingAddressElement = billingSpec.transform(
            initialValues = emptyMap<IdentifierSpec, String?>()
                .plus(
                    billingAddress?.countryCode?.value?.let {
                        mapOf(IdentifierSpec.Country to it)
                    } ?: emptyMap()
                ).plus(
                    billingAddress?.postalCode?.let { mapOf(IdentifierSpec.PostalCode to it) } ?: emptyMap()
                ),
            shippingValues = emptyMap()
        )
        return listOfNotNull(
            cardDetailElement,
            billingAddressElement
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
