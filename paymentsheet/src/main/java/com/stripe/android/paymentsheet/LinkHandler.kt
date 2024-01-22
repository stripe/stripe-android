package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.analytics.LinkAnalyticsHelper
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

internal class LinkHandler @Inject constructor(
    private val linkLauncher: LinkPaymentLauncher,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val savedStateHandle: SavedStateHandle,
    private val linkStore: LinkStore,
    linkAnalyticsComponentBuilder: LinkAnalyticsComponent.Builder,
) {
    sealed class ProcessingState {
        object Ready : ProcessingState()

        object Launched : ProcessingState()

        object Started : ProcessingState()

        class PaymentDetailsCollected(
            val paymentSelection: PaymentSelection?
        ) : ProcessingState()

        data class Error(val message: String?) : ProcessingState()

        object Cancelled : ProcessingState()

        data class PaymentMethodCollected(val paymentMethod: PaymentMethod) : ProcessingState()

        class CompletedWithPaymentResult(val result: PaymentResult) : ProcessingState()

        object CompleteWithoutLink : ProcessingState()
    }

    private val _processingState =
        MutableSharedFlow<ProcessingState>(replay = 1, extraBufferCapacity = 5)
    val processingState: Flow<ProcessingState> = _processingState

    val linkInlineSelection = MutableStateFlow<PaymentSelection.New.LinkInline?>(null)

    private val _isLinkEnabled = MutableStateFlow<Boolean?>(null)
    val isLinkEnabled: StateFlow<Boolean?> = _isLinkEnabled

    private val _linkConfiguration = MutableStateFlow<LinkConfiguration?>(null)
    val linkConfiguration: StateFlow<LinkConfiguration?> = _linkConfiguration.asStateFlow()

    val accountStatus: Flow<AccountStatus> = _linkConfiguration
        .filterNotNull()
        .flatMapLatest(linkConfigurationCoordinator::getAccountStatusFlow)

    val linkSignupMode: Flow<LinkSignupMode?> = combine(
        linkConfiguration,
        linkInlineSelection,
        accountStatus,
    ) { linkConfig, linkInlineSelection, linkAccountStatus ->
        val linkInlineSelectionValid = linkInlineSelection != null
        val validFundingSource = linkConfig?.stripeIntent?.linkFundingSources?.contains(Card.code) == true
        val notLoggedIn = linkAccountStatus == AccountStatus.SignedOut
        val ableToShowLink = validFundingSource && (notLoggedIn || linkInlineSelectionValid)
        linkConfig?.signupMode.takeIf { ableToShowLink }
    }

    private val linkAnalyticsHelper: LinkAnalyticsHelper by lazy {
        linkAnalyticsComponentBuilder.build().linkAnalyticsHelper
    }

    fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        linkLauncher.register(
            activityResultCaller,
            ::onLinkActivityResult,
        )
    }

    fun unregisterFromActivity() {
        linkLauncher.unregister()
    }

    fun setupLink(state: LinkState?) {
        _isLinkEnabled.value = state != null

        if (state == null) return

        _linkConfiguration.value = state.configuration
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
                                _processingState.emit(ProcessingState.Error(it.localizedMessage))
                                savedStateHandle[SAVE_PROCESSING] = false
                                _processingState.emit(ProcessingState.Ready)
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
                    PaymentSelection.New.LinkInline(linkPaymentDetails)
                }
                is LinkPaymentDetails.Saved -> {
                    PaymentSelection.Saved(
                        paymentMethod = PaymentMethod.Builder()
                            .setId(linkPaymentDetails.paymentDetails.id)
                            .setCode(paymentMethodCreateParams.typeCode)
                            .setType(PaymentMethod.Type.Card)
                            .build(),
                        walletType = PaymentSelection.Saved.WalletType.Link,
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

    fun launchLink() {
        val config = _linkConfiguration.value ?: return

        linkLauncher.present(
            config,
        )

        _processingState.tryEmit(ProcessingState.Launched)
    }

    /**
     * Method called with the result of launching the Link UI to collect a payment.
     */
    fun onLinkActivityResult(result: LinkActivityResult) {
        val paymentMethod = (result as? LinkActivityResult.Completed)?.paymentMethod
        val cancelPaymentFlow = result is LinkActivityResult.Canceled &&
            result.reason == LinkActivityResult.Canceled.Reason.BackPressed

        if (paymentMethod != null) {
            // If payment was completed inside the Link UI, dismiss immediately.
            _processingState.tryEmit(ProcessingState.PaymentMethodCollected(paymentMethod))
            linkStore.markLinkAsUsed()
        } else if (cancelPaymentFlow) {
            // We launched the user straight into Link, but they decided to exit out of it.
            _processingState.tryEmit(ProcessingState.Cancelled)
        } else {
            val paymentResult = result.convertToPaymentResult()
            _processingState.tryEmit(ProcessingState.CompletedWithPaymentResult(paymentResult))
            linkStore.markLinkAsUsed()
        }
    }

    private fun LinkActivityResult.convertToPaymentResult() =
        when (this) {
            is LinkActivityResult.Completed -> PaymentResult.Completed
            is LinkActivityResult.Canceled -> PaymentResult.Canceled
            is LinkActivityResult.Failed -> PaymentResult.Failed(error)
        }
}
