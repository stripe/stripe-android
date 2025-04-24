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
    val state: StateFlow<UpdateCardScreenState?> = _state.asStateFlow()

    lateinit var interactor: EditCardDetailsInteractor

    init {
        runCatching {
            val paymentDetails = linkAccountManager.consumerPaymentDetails.value
                ?.paymentDetails
                ?.firstOrNull { it.id == paymentDetailsId }
            require(
                value = paymentDetails is ConsumerPaymentDetails.Card,
                lazyMessage = { "Payment details with id $paymentDetailsId is not a card" }
            )
            interactor = initializeInteractor(paymentDetails)
            _state.update {
                it.copy(paymentDetails = paymentDetails)
            }
        }.onFailure {
            logger.error("Failed to render payment update screen", it)
            navigationManager.tryNavigateBack()
        }
    }

    fun onCardUpdateParamsChanged(cardUpdateParams: CardUpdateParams?) {
        _state.update { it.copy(cardUpdateParams = cardUpdateParams) }
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
        // Until card brand choice is supported in Link, we don't allow CBC.
        isCbcModifiable = false,
        areExpiryDateAndAddressModificationSupported = true,
        // Until card brand filtering is supported in Link, we use the default filter (does not filter)
        cardBrandFilter = DefaultCardBrandFilter,
        payload = EditCardPayload.create(
            card = cardPaymentDetails,
            billingPhoneNumber = linkAccountManager.linkAccount.value?.unredactedPhoneNumber
        ),
        addressCollectionMode = AddressCollectionMode.Automatic,
        onCardUpdateParamsChanged = ::onCardUpdateParamsChanged,
        // Until card brand filtering is supported in Link, we use the default filter (does not filter)
        onBrandChoiceChanged = {}
    )

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
