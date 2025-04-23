package com.stripe.android.link.ui.updatecard

import androidx.lifecycle.SavedStateHandle
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
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.ui.DefaultEditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.EditCardPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class UpdateCardScreenViewModel @Inject constructor(
    private val logger: Logger,
    private val linkAccountManager: LinkAccountManager,
    private val savedStateHandle: SavedStateHandle,
    initialState: UpdateCardScreenState,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<UpdateCardScreenState> = _state.asStateFlow()

    private val cardUpdateParams = MutableStateFlow<CardUpdateParams?>(null)

    val card = linkAccountManager.paymentDetails.value!!.paymentDetails
        .first { it.id == state.value.paymentDetailsId } as ConsumerPaymentDetails.Card

    var interactor = DefaultEditCardDetailsInteractor.Factory().create(
        coroutineScope = viewModelScope,
        // Until card brand choice is supported in Link, we don't allow CBC.
        isCbcModifiable = false,
        areExpiryDateAndAddressModificationSupported = true,
        // Until card brand filtering is supported in Link, we use the default filter (does not filter)
        cardBrandFilter = DefaultCardBrandFilter,
        payload = EditCardPayload.create(
            card = card,
            billingPhoneNumber = linkAccountManager.linkAccount.value?.unredactedPhoneNumber
        ),
        addressCollectionMode = AddressCollectionMode.Automatic,
        onCardUpdateParamsChanged = { cardUpdateParams.value = it },
        // Until card brand filtering is supported in Link, we use the default filter (does not filter)
        onBrandChoiceChanged = {}
    )

    fun onUpdateClicked() {
        logger.info("Update button clicked")
    }

    fun onCancelClicked() {
        logger.info("Cancel button clicked")
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            paymentDetailsId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    UpdateCardScreenViewModel(
                        initialState = UpdateCardScreenState.create(paymentDetailsId),
                        logger = parentComponent.logger,
                        savedStateHandle = parentComponent.savedStateHandle,
                        linkAccountManager = parentComponent.linkAccountManager
                    )
                }
            }
        }
    }
}
