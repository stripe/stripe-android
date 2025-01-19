package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
            val paymentSelection: PaymentSelection?
        ) : ProcessingState()

        data object CompleteWithoutLink : ProcessingState()
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

    @OptIn(DelicateCoroutinesApi::class)
    fun setupLink(state: LinkState?, launchEagerly: (LinkAccount) -> Unit = {}) {
        _isLinkEnabled.value = state != null

        if (state == null) return

        _linkConfiguration.value = state.configuration

        if (state.configuration.useAttestationEndpointsForLink) {
            GlobalScope.launch {
                linkConfigurationCoordinator.getAccountFlow(state.configuration)
                    .collectLatest { account ->
                        if (account != null) {
                            launchEagerly(account)
                        }
                    }
            }
        }
    }

    suspend fun payWithLinkInline(
        userInput: UserInput?,
        paymentSelection: PaymentSelection?,
        shouldCompleteLinkInlineFlow: Boolean,
    ) {
        (paymentSelection as? PaymentSelection.New.Card?)?.paymentMethodCreateParams?.let { params ->
            savedStateHandle[SAVE_PROCESSING] = true
            _processingState.emit(ProcessingState.Started)

            val configuration = requireNotNull(_linkConfiguration.value)

            when (linkConfigurationCoordinator.getAccountStatusFlow(configuration).first()) {
                AccountStatus.Verified -> {
                    completeLinkInlinePayment(
                        configuration,
                        params,
                        paymentSelection.customerRequestedSave,
                        userInput is UserInput.SignIn && shouldCompleteLinkInlineFlow
                    )
                }
                AccountStatus.VerificationStarted,
                AccountStatus.NeedsVerification -> {
                    linkAnalyticsHelper.onLinkPopupSkipped()
                    _processingState.emit(ProcessingState.CompleteWithoutLink)
                }
                AccountStatus.SignedOut,
                AccountStatus.Error -> {
                    userInput?.let {
                        linkConfigurationCoordinator.signInWithUserInput(configuration, userInput).fold(
                            onSuccess = {
                                // If successful, the account was fetched or created, so try again
                                payWithLinkInline(
                                    userInput = userInput,
                                    paymentSelection = paymentSelection,
                                    shouldCompleteLinkInlineFlow = shouldCompleteLinkInlineFlow,
                                )
                            },
                            onFailure = {
                                _processingState.emit(ProcessingState.CompleteWithoutLink)
                            }
                        )
                    } ?: run {
                        savedStateHandle[SAVE_PROCESSING] = false
                        _processingState.emit(ProcessingState.Ready)
                    }
                }
            }
        }
    }

    private suspend fun completeLinkInlinePayment(
        configuration: LinkConfiguration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
        shouldCompleteLinkInlineFlow: Boolean
    ) {
        if (shouldCompleteLinkInlineFlow) {
            linkAnalyticsHelper.onLinkPopupSkipped()
            _processingState.emit(ProcessingState.CompleteWithoutLink)
        } else {
            val linkPaymentDetails = linkConfigurationCoordinator.attachNewCardToAccount(
                configuration,
                paymentMethodCreateParams
            ).getOrNull()

            val paymentSelection = when (linkPaymentDetails) {
                is LinkPaymentDetails.New -> {
                    PaymentSelection.New.LinkInline(linkPaymentDetails, customerRequestedSave)
                }
                is LinkPaymentDetails.Saved -> {
                    val last4 = linkPaymentDetails.paymentDetails.last4

                    PaymentSelection.Saved(
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
                null -> null
            }

            if (paymentSelection != null) {
                linkStore.markLinkAsUsed()
            }

            _processingState.emit(ProcessingState.PaymentDetailsCollected(paymentSelection))
        }
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
