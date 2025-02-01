package com.stripe.android.link.ui.cardedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.ui.core.elements.CvcElement
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowController
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CardEditViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val linkAccountManager: LinkAccountManager,
    private val errorReporter: ErrorReporter,
    private val navigateAndClearStack: (route: LinkScreen) -> Unit,
    private val goBack: () -> Unit
) : ViewModel() {
    val id = savedStateHandle.get<String>(PAYMENT_DETAILS_ID) ?: run {
        errorReporter.report(
            errorEvent = ErrorReporter.UnexpectedErrorEvent.LINK_NATIVE_CARD_EDIT_PAYMENT_DETAILS_ID_NOT_FOUND,
        )
        goBack()
        ""
    }
    private val _cardEditState = MutableStateFlow(CardEditState())
    val cardEditState: StateFlow<CardEditState> = _cardEditState

    private val expiryDateController = SimpleTextFieldController(
        textFieldConfig = DateConfig()
    )
    private val cvcController = CvcController(
        cardBrandFlow = cardEditState.mapAsStateFlow {
            it.paymentDetail?.brand ?: CardBrand.Unknown
        }
    )
    val cardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        collectionMode = BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
    )
    val cvcExpiryElement = cvcExpiryElement()

    init {
        viewModelScope.launch {
            load()
        }

        viewModelScope.launch {
            listenToCard()
        }
    }

    fun defaultValueChanged(checked: Boolean) {
        val defaultState = _cardEditState.value.cardDefaultState

        _cardEditState.update {
            it.copy(
                cardDefaultState = when (defaultState) {
                    CardDefaultState.CardIsDefault -> defaultState
                    is CardDefaultState.Value -> defaultState.copy(enabled = checked)
                    null -> null
                }
            )
        }
    }

    fun updateCardClicked() {
        viewModelScope.launch {
            val entry = _cardEditState.value.cardDetailsEntry ?: return@launch
            _cardEditState.update {
                it.copy(isUpdating = true)
            }

            val params = FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
                fieldValuePairs = mapOf(entry.date, entry.cvc, entry.country, entry.postalCode),
                code = PaymentMethod.Type.Card.code,
                requiresMandate = false
            )
            linkAccountManager.updatePaymentDetails(
                updateParams = ConsumerPaymentDetailsUpdateParams(
                    id = id,
                    cardPaymentMethodCreateParamsMap = params.toParamMap(),
                    isDefault = when (val defaultState = cardEditState.value.cardDefaultState) {
                        CardDefaultState.CardIsDefault -> null
                        is CardDefaultState.Value -> defaultState.enabled
                        null -> null
                    }
                )
            ).fold(
                onSuccess = {
                    navigateAndClearStack(LinkScreen.Wallet)
                },
                onFailure = ::onError
            )
        }
    }

    private fun onError(error: Throwable) {
        _cardEditState.update {
            it.copy(
                errorMessage = error.stripeErrorMessage(),
                isUpdating = false
            )
        }
    }

    private suspend fun load() {
        linkAccountManager.listPaymentDetails(
            paymentMethodTypes = setOf(ConsumerPaymentDetails.Card.TYPE)
        ).fold(
            onSuccess = { response ->
                val card = response.paymentDetails.firstOrNull { it.id == id } as? ConsumerPaymentDetails.Card
                _cardEditState.update {
                    it.copy(
                        paymentDetail = card,
                        cardDefaultState = card?.let {
                            if (card.isDefault) {
                                CardDefaultState.CardIsDefault
                            } else {
                                CardDefaultState.Value(enabled = false)
                            }
                        }
                    )
                }
            },
            onFailure = {
                goBack()
            }
        )
    }

    private suspend fun listenToCard() {
        combine(
            flow = cardBillingAddressElement.getFormFieldValueFlow(),
            flow2 = cvcExpiryElement.getFormFieldValueFlow()
        ) { addressElementFlow, cvcExpFlow ->
            val postalCode = addressElementFlow.valueOrNull(IdentifierSpec.PostalCode) ?: return@combine null
            val country = addressElementFlow.valueOrNull(IdentifierSpec.Country) ?: return@combine null
            val cvc = cvcExpFlow.valueOrNull(IdentifierSpec.CardCvc) ?: return@combine null
            val date = cvcExpFlow.valueOrNull(dateExpSpec) ?: return@combine null
            CardDetailsEntry(
                date = date,
                country = country,
                postalCode = postalCode,
                cvc = cvc
            )
        }.collectLatest { entry ->
            _cardEditState.update {
                it.copy(
                    cardDetailsEntry = entry
                )
            }
        }
    }

    private fun cvcExpiryElement(): RowElement {
        val rowFields = buildList {
            add(
                element = SimpleTextElement(
                    identifier = dateExpSpec,
                    controller = expiryDateController
                )
            )

            add(
                element = CvcElement(
                    _identifier = IdentifierSpec.CardCvc,
                    controller = cvcController
                )
            )
        }

        return RowElement(
            _identifier = IdentifierSpec.Generic("cvc_expiry_row"),
            fields = rowFields,
            controller = RowController(rowFields)
        )
    }

//    private fun addressElement(): RowElement {}

    private fun List<Pair<IdentifierSpec, FormFieldEntry>>.valueOrNull(
        identifierSpec: IdentifierSpec
    ): Pair<IdentifierSpec, FormFieldEntry>? {
        return firstOrNull {
            it.first == identifierSpec
        }?.takeIf { it.second.isComplete }
    }

    companion object {
        internal const val PAYMENT_DETAILS_ID = "paymentDetailsId"
        internal val dateExpSpec = IdentifierSpec.Generic("date_exp")

        fun factory(
            parentComponent: NativeLinkComponent,
            navigateAndClearStack: (route: LinkScreen) -> Unit,
            goBack: () -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    CardEditViewModel(
                        savedStateHandle = parentComponent.savedStateHandle,
                        linkAccountManager = parentComponent.linkAccountManager,
                        errorReporter = parentComponent.errorReporter,
                        navigateAndClearStack = navigateAndClearStack,
                        goBack = goBack
                    )
                }
            }
        }
    }
}
