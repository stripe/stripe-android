package com.stripe.android.link.ui.updatecard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.Logger
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.ui.DefaultEditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardPayload
import com.stripe.android.uicore.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal class UpdateCardScreenViewModel @Inject constructor(
    private val logger: Logger,
    private val linkAccountManager: LinkAccountManager,
    private val navigationManager: NavigationManager,
    paymentDetailsId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateCardScreenState())
    val state: StateFlow<UpdateCardScreenState> = _state.asStateFlow()

    var interactor: EditCardDetailsInteractor? = null

    init {
        runCatching {
            val paymentDetails = linkAccountManager.consumerPaymentDetails.value
                ?.paymentDetails
                ?.firstOrNull { it.id == paymentDetailsId }
            require(
                value = paymentDetails is ConsumerPaymentDetails.Card,
                lazyMessage = { "Payment details with id $paymentDetailsId is not a card" }
            )
            _state.update {
                it.copy(
                    paymentDetailsId = paymentDetailsId,
                    isDefault = paymentDetails.isDefault
                )
            }
            interactor = initializeInteractor(paymentDetails)
        }.onFailure {
            logger.error("Failed to render payment update screen", it)
            navigationManager.tryNavigateBack()
        }
    }

    fun onUpdateClicked() {
        logger.info("Update button clicked")
    }

    fun onCancelClicked() {
        logger.info("Cancel button clicked")
    }

    private fun initializeInteractor(
        cardPaymentDetails: ConsumerPaymentDetails.Card
    ): EditCardDetailsInteractor = DefaultEditCardDetailsInteractor.Factory().create(
        coroutineScope = viewModelScope,
        areExpiryDateAndAddressModificationSupported = true,
        // Until card brand filtering is supported in Link, we use the default filter (does not filter)
        cardBrandFilter = DefaultCardBrandFilter,
        payload = EditCardPayload.create(
            card = cardPaymentDetails,
            billingPhoneNumber = linkAccountManager.linkAccount.value?.unredactedPhoneNumber
        ),
        addressCollectionMode = AddressCollectionMode.Automatic,
        onCardUpdateParamsChanged = ::onCardUpdateParamsChanged,
        isCbcModifiable = cardPaymentDetails.availableNetworks.size > 1,
        onBrandChoiceChanged = ::onBrandChoiceChanged
    )

    private fun onCardUpdateParamsChanged(cardUpdateParams: CardUpdateParams?) {
        _state.update { it.copy(cardUpdateParams = cardUpdateParams) }
    }

    private fun onBrandChoiceChanged(cardBrand: CardBrand) {
        _state.update { it.copy(preferredCardBrand = cardBrand) }
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            paymentDetailsId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    UpdateCardScreenViewModel(
                        logger = parentComponent.logger,
                        linkAccountManager = parentComponent.linkAccountManager,
                        navigationManager = parentComponent.navigationManager,
                        paymentDetailsId = paymentDetailsId
                    )
                }
            }
        }
    }
}
