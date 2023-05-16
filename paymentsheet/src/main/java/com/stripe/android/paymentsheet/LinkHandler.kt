package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel.Companion.SAVE_PROCESSING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkHandler @Inject constructor(
    val linkLauncher: LinkPaymentLauncher,
    private val savedStateHandle: SavedStateHandle,
) {
    sealed class ProcessingState {
        object Ready : ProcessingState()

        object Launched : ProcessingState()

        object Started : ProcessingState()

        class PaymentDetailsCollected(val details: LinkPaymentDetails.New?) : ProcessingState()

        data class Error(val message: String?) : ProcessingState()

        object Cancelled : ProcessingState()

        object Completed : ProcessingState()

        class CompletedWithPaymentResult(val result: PaymentResult) : ProcessingState()
    }

    private val _processingState =
        MutableSharedFlow<ProcessingState>(replay = 1, extraBufferCapacity = 5)
    val processingState: Flow<ProcessingState> = _processingState

    var linkInlineSelection = MutableStateFlow<PaymentSelection.New.LinkInline?>(null)

    private var launchedLinkDirectly: Boolean = false

    private val _showLinkVerificationDialog = MutableStateFlow(false)
    val showLinkVerificationDialog: StateFlow<Boolean> = _showLinkVerificationDialog

    private val _isLinkEnabled = MutableStateFlow<Boolean?>(null)
    val isLinkEnabled: StateFlow<Boolean?> = _isLinkEnabled

    private val _activeLinkSession = MutableStateFlow(false)
    val activeLinkSession: StateFlow<Boolean> = _activeLinkSession

    private val linkConfiguration = MutableStateFlow<LinkPaymentLauncher.Configuration?>(null)

    val accountStatus: Flow<AccountStatus> = linkConfiguration
        .filterNotNull()
        .flatMapLatest(linkLauncher::getAccountStatusFlow)

    private val linkVerificationChannel = Channel<Boolean>(capacity = 1)

    fun registerFromActivity(activityResultCaller: ActivityResultCaller) {
        linkLauncher.register(
            activityResultCaller,
            ::onLinkActivityResult,
        )
    }

    fun unregisterFromActivity() {
        linkLauncher.unregister()
    }

    private fun setupLink(state: LinkState?) {
        _isLinkEnabled.value = state != null
        _activeLinkSession.value = state?.loginState == LinkState.LoginState.LoggedIn

        if (state == null) return

        linkConfiguration.value = state.configuration
    }

    fun setupLinkLaunchingEagerly(scope: CoroutineScope, state: LinkState?) {
        setupLink(state)

        when (state?.loginState) {
            LinkState.LoginState.LoggedIn -> {
                launchLink(state.configuration, launchedDirectly = true)
            }
            LinkState.LoginState.NeedsVerification -> {
                scope.launch {
                    setupLinkWithVerification(state.configuration)
                }
            }
            LinkState.LoginState.LoggedOut -> {
                // Nothing to do here
            }
            null -> {
                // Nothing to do here
            }
        }
    }

    fun prepareLink(state: LinkState?) {
        setupLink(state)
    }

    private suspend fun requestLinkVerification(): Boolean {
        _showLinkVerificationDialog.value = true
        return linkVerificationChannel.receive()
    }

    fun handleLinkVerificationResult(success: Boolean) {
        _showLinkVerificationDialog.value = false
        _activeLinkSession.value = success
        linkVerificationChannel.trySend(success)
    }

    suspend fun payWithLinkInline(
        userInput: UserInput?,
        paymentSelection: PaymentSelection?,
        shouldCompleteLinkInlineFlow: Boolean,
    ) {
        (paymentSelection as? PaymentSelection.New.Card?)?.paymentMethodCreateParams?.let { params ->
            savedStateHandle[SAVE_PROCESSING] = true
            _processingState.emit(ProcessingState.Started)

            val configuration = requireNotNull(linkConfiguration.value)

            when (linkLauncher.getAccountStatusFlow(configuration).first()) {
                AccountStatus.Verified -> {
                    _activeLinkSession.value = true
                    completeLinkInlinePayment(
                        configuration,
                        params,
                        userInput is UserInput.SignIn && shouldCompleteLinkInlineFlow
                    )
                }
                AccountStatus.VerificationStarted,
                AccountStatus.NeedsVerification -> {
                    val success = requestLinkVerification()

                    if (success) {
                        completeLinkInlinePayment(
                            configuration,
                            params,
                            userInput is UserInput.SignIn && shouldCompleteLinkInlineFlow
                        )
                    } else {
                        savedStateHandle[SAVE_PROCESSING] = false
                        _processingState.emit(ProcessingState.Ready)
                    }
                }
                AccountStatus.SignedOut,
                AccountStatus.Error -> {
                    _activeLinkSession.value = false
                    userInput?.let {
                        linkLauncher.signInWithUserInput(configuration, userInput).fold(
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

    private suspend fun setupLinkWithVerification(
        configuration: LinkPaymentLauncher.Configuration,
    ) {
        val success = requestLinkVerification()
        if (success) {
            launchLink(configuration, launchedDirectly = true)
        }
    }

    private suspend fun completeLinkInlinePayment(
        configuration: LinkPaymentLauncher.Configuration,
        paymentMethodCreateParams: PaymentMethodCreateParams,
        shouldCompleteLinkInlineFlow: Boolean
    ) {
        if (shouldCompleteLinkInlineFlow) {
            launchLink(configuration, launchedDirectly = false, paymentMethodCreateParams)
        } else {
            _processingState.emit(
                ProcessingState.PaymentDetailsCollected(
                    linkLauncher.attachNewCardToAccount(
                        configuration,
                        paymentMethodCreateParams
                    ).getOrNull()
                )
            )
        }
    }

    fun launchLink() {
        val config = linkConfiguration.value ?: return
        launchLink(config, launchedDirectly = false)
    }

    fun launchLink(
        configuration: LinkPaymentLauncher.Configuration,
        launchedDirectly: Boolean,
        paymentMethodCreateParams: PaymentMethodCreateParams? = null
    ) {
        launchedLinkDirectly = launchedDirectly

        linkLauncher.present(
            configuration,
            paymentMethodCreateParams,
        )

        _processingState.tryEmit(ProcessingState.Launched)
    }

    /**
     * Method called with the result of launching the Link UI to collect a payment.
     */
    fun onLinkActivityResult(result: LinkActivityResult) {
        val completePaymentFlow = result is LinkActivityResult.Completed
        val cancelPaymentFlow = launchedLinkDirectly &&
            result is LinkActivityResult.Canceled && result.reason == LinkActivityResult.Canceled.Reason.BackPressed

        if (completePaymentFlow) {
            // If payment was completed inside the Link UI, dismiss immediately.
            _processingState.tryEmit(ProcessingState.Completed)
        } else if (cancelPaymentFlow) {
            // We launched the user straight into Link, but they decided to exit out of it.
            _processingState.tryEmit(ProcessingState.Cancelled)
        } else {
            _processingState.tryEmit(
                ProcessingState.CompletedWithPaymentResult(result.convertToPaymentResult())
            )
        }
    }

    private fun LinkActivityResult.convertToPaymentResult() =
        when (this) {
            is LinkActivityResult.Completed -> PaymentResult.Completed
            is LinkActivityResult.Canceled -> PaymentResult.Canceled
            is LinkActivityResult.Failed -> PaymentResult.Failed(error)
        }
}
