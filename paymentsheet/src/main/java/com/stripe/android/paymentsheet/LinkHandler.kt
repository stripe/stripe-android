package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkHandler @Inject constructor(
    val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    private val linkStore: LinkStore,
    linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
) {
    sealed class ProcessingState {
        data object Ready : ProcessingState()

        data object Started : ProcessingState()

        data class PaymentDetailsCollected(
            val paymentSelection: PaymentSelection
        ) : ProcessingState()
    }

    private val _processingState =
        MutableSharedFlow<ProcessingState>(replay = 1, extraBufferCapacity = 5)
    val processingState: Flow<ProcessingState> = _processingState

    private val _isLinkEnabled = MutableStateFlow<Boolean?>(null)
    val isLinkEnabled: StateFlow<Boolean?> = _isLinkEnabled

    private val _linkConfiguration = MutableStateFlow<LinkConfiguration?>(null)
    val linkConfiguration: StateFlow<LinkConfiguration?> = _linkConfiguration.asStateFlow()

    private val linkAnalyticsHelper: LinkAnalyticsHelper by lazy {
        linkAnalyticsComponentBuilder.build().linkAnalyticsHelper
    }

    fun setupLink(
        state: LinkState?,
    ) {
        _isLinkEnabled.value = state != null

        if (state == null) return

        _linkConfiguration.value = state.configuration
    }

    fun setupLinkLaunchEagerly(
        coroutineScope: CoroutineScope,
        state: LinkState?,
        launchEagerly: Boolean = false,
        launchLink: (LinkAccount) -> Unit = {},
    ) {
        setupLink(state)

        if (state == null) return

        _linkConfiguration.value = state.configuration

        coroutineScope.launch {
            if (launchEagerly.not()) return@launch
            val linkAccount =
                linkConfigurationCoordinator.getLinkAccountFlow(state.configuration).first() ?: return@launch
            if (linkAccount.isVerified) {
                launchLink(linkAccount)
            }
            when (linkAccount.accountStatus) {
                AccountStatus.Verified,
                AccountStatus.NeedsVerification,
                AccountStatus.VerificationStarted -> {
                    launchLink(linkAccount)
                }
                else -> Unit
            }
        }

    }

    suspend fun payWithLinkInline(
        paymentSelection: PaymentSelection.New.LinkInline,
        shouldCompleteLinkInlineFlow: Boolean,
    ) {
        savedStateHandle[SAVE_PROCESSING] = true
        _processingState.emit(ProcessingState.Started)

        val configuration = requireNotNull(_linkConfiguration.value)

        when (linkConfigurationCoordinator.getAccountStatusFlow(configuration).first()) {
            AccountStatus.Verified -> {
                completeLinkInlinePayment(
                    paymentSelection,
                    configuration,
                    paymentSelection.input is UserInput.SignIn && shouldCompleteLinkInlineFlow
                )
            }
            AccountStatus.VerificationStarted,
            AccountStatus.NeedsVerification -> {
                linkAnalyticsHelper.onLinkPopupSkipped()
                _processingState.emit(ProcessingState.PaymentDetailsCollected(paymentSelection.toNewSelection()))
            }
            AccountStatus.SignedOut,
            AccountStatus.Error -> {
                linkConfigurationCoordinator.signInWithUserInput(configuration, paymentSelection.input).fold(
                    onSuccess = {
                        // If successful, the account was fetched or created, so try again
                        payWithLinkInline(
                            paymentSelection = paymentSelection,
                            shouldCompleteLinkInlineFlow = shouldCompleteLinkInlineFlow,
                        )
                    },
                    onFailure = {
                        _processingState.emit(
                            ProcessingState.PaymentDetailsCollected(paymentSelection.toNewSelection())
                        )
                    }
                )
            }
        }
    }

    private suspend fun completeLinkInlinePayment(
        paymentSelection: PaymentSelection.New.LinkInline,
        configuration: LinkConfiguration,
        shouldCompleteLinkInlineFlow: Boolean
    ) {
        val paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams
        val customerRequestedSave = paymentSelection.customerRequestedSave

        if (shouldCompleteLinkInlineFlow) {
            linkAnalyticsHelper.onLinkPopupSkipped()
            _processingState.emit(ProcessingState.PaymentDetailsCollected(paymentSelection.toNewSelection()))
        } else {
            val linkPaymentDetails = linkConfigurationCoordinator.attachNewCardToAccount(
                configuration,
                paymentMethodCreateParams
            ).getOrNull()

            val nextSelection = when (linkPaymentDetails) {
                is LinkPaymentDetails.New -> createGenericSelection(
                    linkPaymentDetails = linkPaymentDetails,
                    customerRequestedSave = customerRequestedSave,
                )
                is LinkPaymentDetails.Saved -> createSavedSelection(
                    linkPaymentDetails = linkPaymentDetails,
                    paymentMethodCreateParams = paymentMethodCreateParams,
                    customerRequestedSave = customerRequestedSave,
                )
                null -> null
            }

            if (nextSelection != null) {
                linkStore.markLinkAsUsed()
            }

            _processingState.emit(
                ProcessingState.PaymentDetailsCollected(
                    paymentSelection = nextSelection ?: paymentSelection.toNewSelection()
                )
            )
        }
    }

    private fun createGenericSelection(
        linkPaymentDetails: LinkPaymentDetails.New,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
    ): PaymentSelection.New.GenericPaymentMethod {
        return PaymentSelection.New.GenericPaymentMethod(
            paymentMethodCreateParams = linkPaymentDetails.paymentMethodCreateParams,
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = customerRequestedSave.setupFutureUsage
            ),
            paymentMethodExtraParams = null,
            customerRequestedSave = customerRequestedSave,
            label = resolvableString("···· ${linkPaymentDetails.paymentDetails.last4}"),
            iconResource = R.drawable.stripe_ic_paymentsheet_link,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            createdFromLink = true,
        )
    }

    private fun createSavedSelection(
        linkPaymentDetails: LinkPaymentDetails.Saved,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
    ): PaymentSelection.Saved {
        val last4 = linkPaymentDetails.paymentDetails.last4

        return PaymentSelection.Saved(
            paymentMethod = PaymentMethod.Builder()
                .setId(linkPaymentDetails.paymentDetails.id)
                .setCode(paymentMethodCreateParams.typeCode)
                .setCard(
                    PaymentMethod.Card(
                        last4 = last4,
                        wallet = Wallet.LinkWallet(last4)
                    )
                )
                .setType(PaymentMethod.Type.Card)
                .build(),
            walletType = PaymentSelection.Saved.WalletType.Link,
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession?.takeIf {
                    customerRequestedSave ==
                        PaymentSelection.CustomerRequestedSave.RequestReuse
                } ?: ConfirmPaymentIntentParams.SetupFutureUsage.Blank
            )
        )
    }

    private fun PaymentSelection.New.LinkInline.toNewSelection(): PaymentSelection.New.Card {
        return PaymentSelection.New.Card(
            paymentMethodCreateParams = paymentMethodCreateParams,
            brand = brand,
            customerRequestedSave = customerRequestedSave,
            paymentMethodOptionsParams = paymentMethodOptionsParams,
            paymentMethodExtraParams = paymentMethodExtraParams
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun logOut() {
        val configuration = linkConfiguration.value ?: return

        GlobalScope.launch {
            // This usage is intentional. We want the request to be sent without regard for the UI lifecycle.
            linkConfigurationCoordinator.logOut(configuration = configuration)
        }
    }
}
