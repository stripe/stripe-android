package com.stripe.android.link.ui.updatecard

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.CompleteLinkWithPayment
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.utils.effectiveBillingDetails
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParams.Card.Networks
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.R
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
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val configuration: LinkConfiguration,
    private val linkLaunchMode: LinkLaunchMode,
    private val completeLinkWithPayment: CompleteLinkWithPayment,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
    paymentDetailsId: String,
    isBillingDetailsUpdateFlow: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UpdateCardScreenState(
            paymentDetailsId = paymentDetailsId,
            isBillingDetailsUpdateFlow = isBillingDetailsUpdateFlow,
            primaryButtonLabel = if (isBillingDetailsUpdateFlow) {
                completePaymentButtonLabel(configuration.stripeIntent, linkLaunchMode)
            } else {
                R.string.stripe_link_update_card_confirm_cta.resolvableString
            }
        )
    )
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
            interactor = initializeInteractor(isBillingDetailsUpdateFlow, paymentDetails)
        }.onFailure {
            logger.error("Failed to render payment update screen", it)
            navigationManager.tryNavigateBack()
        }
    }

    fun onUpdateClicked() {
        viewModelScope.launch {
            dismissalCoordinator.withDismissalDisabled {
                runCatching {
                    _state.update { it.copy(processing = true, error = null) }
                    val cardParams = requireNotNull(state.value.cardUpdateParams)
                    val paymentDetailsId = requireNotNull(state.value.paymentDetailsId)
                    val updateParams = ConsumerPaymentDetailsUpdateParams(
                        id = paymentDetailsId,
                        // When updating a card that is not the default and you send isDefault=false to the server,
                        // you get "Can't unset payment details when it's not the default", so send nil instead of false
                        isDefault = state.value.isDefault.takeIf { it == true },
                        cardPaymentMethodCreateParamsMap = cardParams.toApiParams().toParamMap()
                    )
                    val result = linkAccountManager.updatePaymentDetails(updateParams = updateParams).getOrThrow()

                    if (state.value.isBillingDetailsUpdateFlow) {
                        // In billing details update flow, automatically confirm payment after updating
                        val updatedPaymentDetails = result.paymentDetails.single { it.id == paymentDetailsId }
                        val account = requireNotNull(linkAccountManager.linkAccountInfo.value.account) {
                            "LinkAccount should not be null in billing details update flow"
                        }

                        val confirmationResult = completeLinkWithPayment(
                            selectedPaymentDetails = updatedPaymentDetails,
                            linkAccount = account,
                            cvc = null, // CVC is already included in the updated payment details
                            linkLaunchMode = linkLaunchMode,
                        )

                        dismissWithResult(confirmationResult)
                    } else {
                        // Regular update flow, just navigate back
                        _state.update { it.copy(processing = false, error = null) }
                        navigationManager.tryNavigateBack()
                    }
                }.onFailure { throwable ->
                    logger.error("Failed to update payment details", throwable)
                    _state.update { it.copy(processing = false, error = throwable) }
                }
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

    private fun initializeInteractor(
        isBillingDetailsUpdateFlow: Boolean,
        cardPaymentDetails: ConsumerPaymentDetails.Card
    ): EditCardDetailsInteractor {
        val effectiveBillingDetails = if (isBillingDetailsUpdateFlow) {
            // Use effective billing details when in billing details update flow
            val linkAccount = linkAccountManager.linkAccountInfo.value.account
            if (linkAccount != null) {
                effectiveBillingDetails(
                    configuration = configuration,
                    linkAccount = linkAccount,
                    originalBillingDetails = null // Start with empty billing details for new card entry
                )
            } else null
        } else null

        return DefaultEditCardDetailsInteractor.Factory().create(
            coroutineScope = viewModelScope,
            areExpiryDateAndAddressModificationSupported = true,
            // Until card brand filtering is supported in Link, we use the default filter (does not filter)
            cardBrandFilter = DefaultCardBrandFilter,
            payload = EditCardPayload.create(
                card = cardPaymentDetails,
                billingPhoneNumber = linkAccountManager.linkAccountInfo.value.account?.unredactedPhoneNumber
            ).let { payload ->
                // Apply effective billing details to the payload if available
                if (effectiveBillingDetails != null) {
                    payload.copy(billingDetails = effectiveBillingDetails)
                } else {
                    payload
                }
            },
            addressCollectionMode = configuration.billingDetailsCollectionConfiguration.address,
            onCardUpdateParamsChanged = ::onCardUpdateParamsChanged,
            isCbcModifiable = cardPaymentDetails.availableNetworks.size > 1,
            onBrandChoiceChanged = ::onBrandChoiceChanged
        )
    }

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
            paymentDetailsId: String,
            isBillingDetailsUpdateFlow: Boolean,
            dismissWithResult: (LinkActivityResult) -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    UpdateCardScreenViewModel(
                        logger = parentComponent.logger,
                        linkAccountManager = parentComponent.linkAccountManager,
                        navigationManager = parentComponent.navigationManager,
                        dismissalCoordinator = parentComponent.dismissalCoordinator,
                        configuration = parentComponent.configuration,
                        linkLaunchMode = parentComponent.linkLaunchMode,
                        completeLinkWithPayment = CompleteLinkWithPayment(
                            linkConfirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                                confirmationHandler = parentComponent.viewModel.confirmationHandler
                            ),
                            linkAccountManager = parentComponent.linkAccountManager,
                            dismissalCoordinator = parentComponent.dismissalCoordinator,
                        ),
                        dismissWithResult = dismissWithResult,
                        paymentDetailsId = paymentDetailsId,
                        isBillingDetailsUpdateFlow = isBillingDetailsUpdateFlow,
                    )
                }
            }
        }
    }
}
