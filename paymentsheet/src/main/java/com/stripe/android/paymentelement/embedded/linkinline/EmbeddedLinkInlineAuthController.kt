package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.link.analytics.LinkAuthSurface
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.verification.LinkAuthFlow
import com.stripe.android.link.ui.verification.LinkAuthFlowResult
import com.stripe.android.link.ui.verification.LinkAuthFlowState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.DialogReason
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthEvent.FallbackReason
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.childScope
import com.stripe.android.uicore.elements.OTPElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Owns one returning Link customer's inline verification attempt in the Embedded Payment Element.
 *
 * It outlives the list, which `configure` rebuilds, so every rebuilt list shows the same attempt. The code is sent
 * the first time the element is on screen, and entered in the card or, when the card can't take it, in the Link
 * dialog. Both show the same [authFlow].
 */
@Suppress("TooManyFunctions", "LongParameterList")
internal class EmbeddedLinkInlineAuthController(
    val linkAccount: LinkAccount,
    private val authFlow: LinkAuthFlow,
    /**
     * False in the experiment's control groups, which keep the regular Link row but still log exposure.
     */
    val showsCard: Boolean,
    private val exposure: LoggableExperiment?,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    /**
     * A server switch that sends every code entry to the dialog.
     */
    var forcesDialog: Boolean,
    private val linkBrand: LinkBrand,
    private val host: Host,
    private val eventReporter: EventReporter,
    private val registry: EmbeddedLinkInlineAuthRegistry,
    /**
     * The element showing the card, which holds the registry's single slot.
     */
    private val registryOwner: Any,
    private val requiresAttestation: Boolean,
    private val prepareAttestation: suspend () -> Boolean,
    private val isScreenReaderOn: () -> Boolean,
    /**
     * Where the code is entered when nothing else sends it to the dialog, based on the merchant's layout.
     */
    private val layoutCodeEntry: () -> CodeEntry,
    private val cardOtpElement: OTPElement,
    private val scope: CoroutineScope,
) {
    interface Host {
        /**
         * Whether the Link dialog can be shown now, i.e. the element is composed and nothing is presented over it.
         */
        fun canPresentDialog(): Boolean

        /**
         * The customer verified. [fromCard] is false when they verified after closing the card.
         */
        fun onVerified(fromCard: Boolean)

        /**
         * The customer switched accounts, so the Link account is logged out.
         */
        fun logOut(account: LinkAccount)
    }

    sealed interface CodeEntry {
        data object Inline : CodeEntry

        data class Dialog(val reason: DialogReason) : CodeEntry
    }

    enum class Phase {
        /**
         * The card shows, or would outside the experiment's control groups.
         */
        Expanded,

        /**
         * The customer closed the card. The attempt stays alive so paying with Link can reuse the code.
         */
        Collapsed,

        /**
         * The attempt ended: verified, switched accounts, failed or invalidated.
         */
        Finished,
    }

    enum class ConfirmVerificationResult {
        Verified,
        Canceled,

        /**
         * The dialog can't be shown, so paying with Link verifies the usual way.
         */
        Unavailable,
    }

    private val _phase = MutableStateFlow(Phase.Expanded)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    private val _dialog = MutableStateFlow<EmbeddedLinkVerificationDialogSession?>(null)
    val dialog: StateFlow<EmbeddedLinkVerificationDialogSession?> = _dialog.asStateFlow()

    val card: StateFlow<EmbeddedLinkInlineCardUi?> = combineAsStateFlow(
        authFlow.state,
        phase,
    ) { authFlowState, phase ->
        if (showsCard && phase == Phase.Expanded) {
            EmbeddedLinkInlineCardUi(
                state = EmbeddedLinkInlineCardState.create(state = authFlowState, email = linkAccount.email),
                otpElement = cardOtpElement,
                linkBrand = linkBrand,
            )
        } else {
            null
        }
    }

    val isShowingCard: Boolean
        get() = showsCard && phase.value == Phase.Expanded

    val isPresentingDialog: Boolean
        get() = dialog.value != null

    /**
     * Paying with Link after closing the card verifies in the dialog first, reusing the code the card sent.
     */
    val verifiesBeforeConfirming: Boolean
        get() = phase.value == Phase.Collapsed && dialog.value == null && !authFlow.isFinished

    private val didBecomeVisible = MutableStateFlow(false)

    /**
     * Whether the element still has to report the first time it's on screen.
     */
    val needsVisibility: StateFlow<Boolean> = combineAsStateFlow(didBecomeVisible, phase) { didBecomeVisible, phase ->
        !didBecomeVisible && phase != Phase.Finished
    }

    /**
     * Set once a code was sent, to tell a failed first send apart from later errors.
     */
    private var didSendCode = false
    private var confirmVerification: CompletableDeferred<ConfirmVerificationResult>? = null

    init {
        if (showsCard) {
            registry.claim(registryOwner)
        }
        scope.launch {
            authFlow.state.collect(::onAuthFlowStateChanged)
        }
        bindCardCode()
    }

    /**
     * Call the first time the element is on screen. Logs the experiment exposure and, unless this is a control
     * group, sends the code.
     */
    fun elementBecameVisible() {
        if (didBecomeVisible.value || phase.value == Phase.Finished) return
        didBecomeVisible.value = true
        exposure?.let { eventReporter.onExperimentExposure(it, paymentMethodMetadata) }
        if (!isShowingCard) return
        eventReporter.onEmbeddedLinkInlineAuthEvent(EmbeddedLinkInlineAuthEvent.Shown)
        scope.launch { sendFirstCode() }
    }

    fun onCodeTapped() {
        if (!isShowingCard || isPresentingDialog) return
        when (val codeEntry = codeEntry()) {
            is CodeEntry.Inline -> {
                eventReporter.onEmbeddedLinkInlineAuthEvent(EmbeddedLinkInlineAuthEvent.Focused)
            }
            is CodeEntry.Dialog -> presentDialog(codeEntry.reason)
        }
    }

    fun onAction(action: EmbeddedLinkInlineCardState.Action) {
        when (action) {
            EmbeddedLinkInlineCardState.Action.Resend -> authFlow.resend()
            EmbeddedLinkInlineCardState.Action.Email -> {
                // Email codes can need a phone number match first, which only the dialog shows. The dialog opens
                // first, so the step change is shown there rather than reported as one the card can't show.
                presentDialog(DialogReason.Email)
                authFlow.sendToEmail()
            }
        }
    }

    /**
     * The customer closed the card. The attempt and its code stay usable.
     */
    fun onClose() {
        if (!isShowingCard) return
        eventReporter.onEmbeddedLinkInlineAuthEvent(EmbeddedLinkInlineAuthEvent.Dismissed)
        registry.recordDismissal(linkAccount.email)
        registry.release(registryOwner)
        _phase.value = Phase.Collapsed
    }

    fun onSwitchAccount() {
        authFlow.cancel(switchAccount = true)
    }

    /**
     * Verifies in the Link dialog before paying with Link.
     */
    suspend fun verifyBeforeConfirming(): ConfirmVerificationResult {
        if (!verifiesBeforeConfirming || confirmVerification != null) return ConfirmVerificationResult.Unavailable
        val result = CompletableDeferred<ConfirmVerificationResult>()
        confirmVerification = result
        if (!presentDialog(DialogReason.Confirm)) {
            resumeConfirmVerification(ConfirmVerificationResult.Unavailable)
        }
        return result.await()
    }

    /**
     * Ends the attempt without logging a cancellation, e.g. when the Link account changes or Link isn't shown.
     */
    fun invalidate() {
        if (phase.value == Phase.Finished) return
        _dialog.value?.close()
        _dialog.value = null
        authFlow.invalidate()
        finish()
        resumeConfirmVerification(ConfirmVerificationResult.Canceled)
    }

    /**
     * Another surface replacing or logging out the account ends this attempt.
     */
    fun onAccountChanged(account: LinkAccount?) {
        if (phase.value == Phase.Finished) return
        if (account == null || !account.email.equals(linkAccount.email, ignoreCase = true)) {
            invalidate()
        }
    }

    /**
     * The dialog closed without finishing the attempt. The card stays valid.
     */
    fun onDialogDismissed() {
        val dialog = _dialog.value ?: return
        dialog.close()
        _dialog.value = null
        authFlow.analyticsSurface = LinkAuthSurface.EmbeddedInline
        resumeConfirmVerification(ConfirmVerificationResult.Canceled)
        // The card only shows code steps, so steps like a phone match continue in the dialog next time.
        if (isShowingCard && authFlow.state.value.screen != LinkAuthFlowState.Screen.Otp) {
            onClose()
        }
    }

    fun onAuthFlowFinished(result: LinkAuthFlowResult) {
        if (phase.value == Phase.Finished) return
        val wasShowingCard = isShowingCard
        // The dialog closes before the result is handled.
        _dialog.value?.close()
        _dialog.value = null
        when (result) {
            is LinkAuthFlowResult.Completed -> {
                finish()
                host.onVerified(fromCard = wasShowingCard)
                resumeConfirmVerification(ConfirmVerificationResult.Verified)
            }
            is LinkAuthFlowResult.SwitchAccount -> {
                finish()
                resumeConfirmVerification(ConfirmVerificationResult.Canceled)
                host.logOut(linkAccount)
            }
            is LinkAuthFlowResult.RequiresWebAuth -> {
                // Native Link can't hand off to the web from here, so the regular Link row takes over.
                logFallback(FallbackReason.WebHandoff, wasShowingCard)
                finish()
                resumeConfirmVerification(ConfirmVerificationResult.Canceled)
            }
            is LinkAuthFlowResult.Canceled,
            is LinkAuthFlowResult.Failed -> {
                finish()
                resumeConfirmVerification(ConfirmVerificationResult.Canceled)
            }
        }
    }

    private fun codeEntry(): CodeEntry {
        if (forcesDialog) {
            return CodeEntry.Dialog(DialogReason.ForcedByServer)
        }
        if (isScreenReaderOn()) {
            return CodeEntry.Dialog(DialogReason.ScreenReader)
        }
        val state = authFlow.state.value
        val isFirstSmsCode = state.screen == LinkAuthFlowState.Screen.Otp &&
            state.verificationType == VerificationType.SMS &&
            state.step == 1
        if (!isFirstSmsCode) {
            return CodeEntry.Dialog(DialogReason.UnsupportedStep)
        }
        return layoutCodeEntry()
    }

    private suspend fun sendFirstCode() {
        // Native Link needs an attested app. Without one, verification would fall back to the web.
        if (requiresAttestation && !prepareAttestation()) {
            fallBack(FallbackReason.AttestationFailed)
            return
        }
        if (!isShowingCard || authFlow.hasStarted) return
        registry.recordAutoSend(linkAccount.email)
        authFlow.analyticsSurface = LinkAuthSurface.EmbeddedInline
        authFlow.start()
    }

    /**
     * @return whether the dialog is now showing.
     */
    private fun presentDialog(reason: DialogReason): Boolean {
        if (isPresentingDialog || phase.value == Phase.Finished || !host.canPresentDialog()) return false
        eventReporter.onEmbeddedLinkInlineAuthEvent(EmbeddedLinkInlineAuthEvent.DialogShown(reason))
        authFlow.analyticsSurface = LinkAuthSurface.EmbeddedDialog
        _dialog.value = EmbeddedLinkVerificationDialogSession(
            authFlow = authFlow,
            linkAccount = linkAccount,
            linkBrand = linkBrand,
            scope = scope.childScope(EmptyCoroutineContext),
            onDismiss = ::onDialogDismissed,
        )
        return true
    }

    private fun onAuthFlowStateChanged(state: LinkAuthFlowState) {
        if (state.isCodeSent) {
            didSendCode = true
        }
        if (!isShowingCard || isPresentingDialog || !authFlow.hasStarted || state.isLoading) return
        when (state.screen) {
            LinkAuthFlowState.Screen.Otp -> {
                if (!didSendCode) {
                    // The first code couldn't be sent, e.g. because of rate limits.
                    fallBack(FallbackReason.SendFailed)
                }
            }
            LinkAuthFlowState.Screen.PhoneMatch -> presentDialog(DialogReason.PhoneMatch)
            LinkAuthFlowState.Screen.Blocked -> fallBack(FallbackReason.Blocked)
            LinkAuthFlowState.Screen.Loading -> Unit
        }
    }

    private fun bindCardCode() {
        scope.launch {
            // Collects every completed code, so the same code entered again after a failure is resubmitted.
            cardOtpElement.otpCompleteFlow.collect { code ->
                if (isShowingCard && !isPresentingDialog) {
                    authFlow.confirm(code)
                }
            }
        }
        scope.launch {
            // A stale error goes away once the customer types a new code.
            cardOtpElement.controller.fieldValue.drop(1).filter { it.isNotEmpty() }.collect {
                authFlow.clearError()
            }
        }
        scope.launch {
            authFlow.state.map { it.inputRevision }.distinctUntilChanged().drop(1).collect {
                cardOtpElement.controller.reset()
            }
        }
    }

    /**
     * Ends the attempt because the card can't continue it, showing the regular Link row instead.
     */
    private fun fallBack(reason: FallbackReason) {
        logFallback(reason, isShowingCard)
        invalidate()
    }

    private fun logFallback(reason: FallbackReason, wasShowingCard: Boolean) {
        if (wasShowingCard) {
            eventReporter.onEmbeddedLinkInlineAuthEvent(EmbeddedLinkInlineAuthEvent.FellBack(reason))
        }
    }

    private fun finish() {
        _phase.value = Phase.Finished
        registry.release(registryOwner)
        scope.cancel()
    }

    private fun resumeConfirmVerification(result: ConfirmVerificationResult) {
        val confirmVerification = confirmVerification ?: return
        this.confirmVerification = null
        confirmVerification.complete(result)
    }
}
