package com.stripe.android.link.ui.updatecard

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.CompleteLinkFlow
import com.stripe.android.link.confirmation.CompleteLinkFlow.Result
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.utils.withEffectiveBillingDetails
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParams.Card.Networks
import com.stripe.android.paymentsheet.CardUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.CardEditConfiguration
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
    private val completeLinkFlow: CompleteLinkFlow,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
    paymentDetailsId: String,
    billingDetailsUpdateFlow: BillingDetailsUpdateFlow?,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UpdateCardScreenState(
            paymentDetailsId = paymentDetailsId,
            billingDetailsUpdateFlow = billingDetailsUpdateFlow,
            primaryButtonLabel = primaryButtonLabel(billingDetailsUpdateFlow)
        )
    )

    val state: StateFlow<UpdateCardScreenState> = _state.asStateFlow()

    private val _interactor = MutableStateFlow<EditCardDetailsInteractor?>(null)

    val interactor: StateFlow<EditCardDetailsInteractor?> = _interactor.asStateFlow()

    init {
        runCatching {
            val paymentDetails = linkAccountManager.consumerState.value
                ?.paymentDetails?.find { it.details.id == paymentDetailsId }
                ?.details
            requireNotNull(paymentDetails) { "Payment details with id $paymentDetailsId not found" }
            _state.update {
                it.copy(
                    paymentDetailsId = paymentDetailsId,
                    isDefault = paymentDetails.isDefault
                )
            }
            _interactor.value = initializeInteractor(paymentDetails)
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
                    val paymentUpdateParams = requireNotNull(state.value.cardUpdateParams)
                    val paymentDetailsId = requireNotNull(state.value.paymentDetailsId)
                    val updateParams = ConsumerPaymentDetailsUpdateParams(
                        id = paymentDetailsId,
                        // When updating a payment that is not the default and you send isDefault=false to the server,
                        // you get "Can't unset payment details when it's not the default", so send nil instead of false
                        isDefault = state.value.isDefault.takeIf { it == true },
                        cardPaymentMethodCreateParamsMap = paymentUpdateParams.toApiParams().toParamMap()
                    )
                    val result = linkAccountManager.updatePaymentDetails(
                        updateParams = updateParams,
                        phone = paymentUpdateParams.billingDetails?.phone
                    ).getOrThrow()

                    if (state.value.isBillingDetailsUpdateFlow) {
                        // In billing details update flow, automatically confirm payment after updating
                        val updatedPaymentDetails = result.paymentDetails.single { it.id == paymentDetailsId }
                        val account = requireNotNull(linkAccountManager.linkAccountInfo.value.account) {
                            "LinkAccount should not be null in billing details update flow"
                        }

                        val confirmationResult = completeLinkFlow(
                            selectedPaymentDetails = LinkPaymentMethod.ConsumerPaymentDetails(
                                details = updatedPaymentDetails,
                                collectedCvc = state.value.billingDetailsUpdateFlow?.cvc,
                                billingPhone = paymentUpdateParams.billingDetails?.phone,
                            ),
                            linkAccount = account
                        )

                        _state.update { it.copy(processing = false) }
                        when (confirmationResult) {
                            is Result.Canceled -> Unit
                            is Result.Failed -> _state.update { it.copy(error = confirmationResult.error) }
                            is Result.Completed -> dismissWithResult(confirmationResult.linkActivityResult)
                        }
                    } else {
                        // Regular update flow, just navigate back
                        _state.update { it.copy(processing = false, error = null) }
                        navigationManager.tryNavigateBack()
                    }
                }.onFailure { throwable ->
                    logger.error("Failed to update payment details", throwable)
                    _state.update { it.copy(processing = false, error = throwable.stripeErrorMessage()) }
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
        paymentDetails: ConsumerPaymentDetails.PaymentDetails
    ): EditCardDetailsInteractor {
        // If this is a billing details update flow, we need to use the effective billing details
        val paymentDetails = if (state.value.isBillingDetailsUpdateFlow) {
            paymentDetails.withEffectiveBillingDetails(
                configuration = configuration,
                linkAccount = linkAccountManager.linkAccountInfo.value.account
            )
        } else {
            paymentDetails
        }

        val cardEditConfiguration = (paymentDetails as? ConsumerPaymentDetails.Card)?.let {
            CardEditConfiguration(
                cardBrandFilter = DefaultCardBrandFilter,
                isCbcModifiable = it.availableNetworks.size > 1,
                areExpiryDateAndAddressModificationSupported = true,
            )
        }

        val defaultConfiguration = configuration.billingDetailsCollectionConfiguration

        return DefaultEditCardDetailsInteractor.Factory().create(
            coroutineScope = viewModelScope,
            cardEditConfiguration = cardEditConfiguration,
            payload = EditCardPayload.create(
                details = paymentDetails,
                billingPhoneNumber = linkAccountManager.linkAccountInfo.value.account?.unredactedPhoneNumber
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = defaultConfiguration.name,
                email = defaultConfiguration.email,
                // Cannot update phone number when not in the billing details update flow
                phone = defaultConfiguration.phone.takeIf {
                    state.value.isBillingDetailsUpdateFlow
                } ?: PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                // Should always allow updating ZIP/postal code at minimum
                address = if (
                    paymentDetails.type == PaymentMethod.Type.Card.code &&
                    defaultConfiguration.address ==
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                ) {
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
                } else {
                    defaultConfiguration.address
                },
                attachDefaultsToPaymentMethod = defaultConfiguration.attachDefaultsToPaymentMethod,
            ),
            onCardUpdateParamsChanged = ::onCardUpdateParamsChanged,
            onBrandChoiceChanged = ::onBrandChoiceChanged,
            // We prefill in the billing details update flow, so the form might
            // already be complete on first render. The user can submit without modifying.
            requiresModification = state.value.isBillingDetailsUpdateFlow.not()
        )
    }

    @VisibleForTesting
    internal fun onCardUpdateParamsChanged(cardUpdateParams: CardUpdateParams?) {
        _state.update { it.copy(cardUpdateParams = cardUpdateParams) }
    }

    private fun onBrandChoiceChanged(cardBrand: CardBrand) {
        _state.update { it.copy(preferredCardBrand = cardBrand) }
    }

    private fun primaryButtonLabel(
        billingDetailsUpdateFlow: BillingDetailsUpdateFlow?
    ): ResolvableString = if (billingDetailsUpdateFlow != null) {
        // In billing details update flow, payment details are updated and then confirmed,
        completePaymentButtonLabel(configuration.stripeIntent, linkLaunchMode)
    } else {
        // In regular update flow, we just update the card details
        R.string.stripe_link_update_card_confirm_cta.resolvableString
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            paymentDetailsId: String,
            billingDetailsUpdateFlow: BillingDetailsUpdateFlow?,
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
                        completeLinkFlow = DefaultCompleteLinkFlow(
                            linkConfirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                                confirmationHandler = parentComponent.viewModel.confirmationHandler
                            ),
                            linkAccountManager = parentComponent.linkAccountManager,
                            dismissalCoordinator = parentComponent.dismissalCoordinator,
                            linkLaunchMode = parentComponent.linkLaunchMode,
                        ),
                        dismissWithResult = dismissWithResult,
                        paymentDetailsId = paymentDetailsId,
                        billingDetailsUpdateFlow = billingDetailsUpdateFlow,
                    )
                }
            }
        }
    }
}
