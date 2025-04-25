package com.stripe.android.link.ui.updatecard

import androidx.annotation.VisibleForTesting
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
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParams.Card.Networks
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
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            runCatching {
                _state.update { it.copy(loading = true, error = null) }
                val cardParams = requireNotNull(state.value.cardUpdateParams)
                val paymentDetailsId = requireNotNull(state.value.paymentDetailsId)
                val updateParams = ConsumerPaymentDetailsUpdateParams(
                    id = paymentDetailsId,
                    // When updating a card that is not the default and you send isDefault=false to the server you get
                    // "Can't unset payment details when it's not the default", so send nil instead of false
                    isDefault = state.value.isDefault.takeIf { it == true },
                    cardPaymentMethodCreateParamsMap = cardParams.toApiParams().toParamMap()
                )
                linkAccountManager.updatePaymentDetails(updateParams = updateParams).getOrThrow()
                _state.update { it.copy(loading = false, error = null) }
            }.onFailure { throwable ->
                logger.error("Failed to update payment details", throwable)
                _state.update { it.copy(loading = false, error = throwable) }
            }.onSuccess {
                navigationManager.tryNavigateBack()
            }
        }
    }

    private fun CardUpdateParams.toApiParams(): PaymentMethodCreateParams = PaymentMethodCreateParams.create(
        card = PaymentMethodCreateParams.Card.Builder().apply {
            setExpiryMonth(this@toApiParams.expiryMonth)
            setExpiryYear(this@toApiParams.expiryYear)
            state.value.preferredCardBrand?.let { preferredCardBrand ->
                setNetworks(Networks(preferred = preferredCardBrand.code))
            }
        }.build(),
        billingDetails = billingDetails
    )

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

    @VisibleForTesting
    internal fun onCardUpdateParamsChanged(cardUpdateParams: CardUpdateParams?) {
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
