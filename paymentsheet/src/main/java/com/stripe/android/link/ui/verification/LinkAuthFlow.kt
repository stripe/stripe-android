package com.stripe.android.link.ui.verification

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSession.VerificationFactor.FactorType
import com.stripe.android.model.VerificationType
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

/**
 * Coordinates one Link authentication attempt (SMS OTP, email OTP, phone-match and MFA)
 * independently of its presentation.
 *
 * The initial screen is computed on construction without sending any requests; codes are only
 * sent once [start] is called.
 */
internal class LinkAuthFlow(
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val capabilities: List<VerificationType>,
    private val consentGranted: () -> Boolean?,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val onFinish: (LinkAuthFlowResult) -> Unit,
) {
    private data class Challenge(
        val type: VerificationType,
        val factorId: String?,
        val phoneNumber: String?,
        val resendDeadline: Long?,
        val isStarted: Boolean,
    )

    private data class Route(
        val screen: LinkAuthFlowState.Screen,
        val challenge: Challenge?,
    )

    private val history = ArrayDeque<Route>()
    private val previousChallenges = mutableMapOf<VerificationType, Challenge>()
    private var didStart = false
    private var finished = false
    private var generation = 0
    private var cooldownJob: Job? = null

    private var screen = LinkAuthFlowState.Screen.Loading
    private var challenge: Challenge? = null
    private var step = 1
    private var isLoading = false
    private var isResending = false
    private var errorMessage: ResolvableString? = null
    private var inputRevision = 0

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<LinkAuthFlowState> = _state.asStateFlow()

    private val account: LinkAccount?
        get() = linkAccountManager.linkAccountInfo.value.account

    init {
        advance(previous = null)
    }

    fun start() {
        if (didStart || finished) return
        didStart = true
        advance(previous = null)
    }

    fun cancel(switchAccount: Boolean) {
        if (finished) return
        linkEventsReporter.on2FACancel()
        finish(if (switchAccount) LinkAuthFlowResult.SwitchAccount else LinkAuthFlowResult.Canceled)
    }

    fun goBack() {
        if (!canGoBack()) return
        val route = history.removeLastOrNull() ?: return
        generation += 1
        screen = route.screen
        challenge = route.challenge
        resetInput()
        publish()
    }

    fun sendToEmail() {
        if (!didStart || isLoading || finished || LinkAuthFlowState.Action.Email !in actions()) return
        val factor = factorFor(VerificationType.EMAIL) ?: return
        history.addLast(Route(screen, challenge))
        select(factor)
    }

    fun submitPhoneNumber(phoneNumber: String) {
        if (!didStart || screen != LinkAuthFlowState.Screen.PhoneMatch || isLoading || finished) return
        challenge = challenge?.copy(phoneNumber = phoneNumber)
        sendCode(isResend = false)
    }

    fun resend() {
        if (!canResend()) return
        challenge?.type?.let { linkEventsReporter.on2FAResendCode(verificationType = it.value) }
        sendCode(isResend = true)
    }

    fun confirm(code: String) {
        if (!canSubmitCode()) return
        val challenge = challenge ?: return
        request(
            operation = {
                linkAccountManager.confirmVerification(
                    code = code,
                    type = challenge.type,
                    consentGranted = consentGranted(),
                )
            },
        ) { result ->
            result.fold(
                onSuccess = {
                    step += 1
                    history.clear()
                    previousChallenges.clear()
                    resetInput()
                    advance(previous = challenge.type)
                },
                onFailure = { error ->
                    resetInput()
                    errorMessage = error.errorMessage
                    when (error.stripeErrorCode) {
                        ERROR_VERIFICATION_EXPIRED, ERROR_VERIFICATION_NOT_FOUND -> {
                            markNotStarted(challenge.type)
                            sendCode(isResend = false)
                            return@fold
                        }
                        ERROR_VERIFICATION_MAX_ATTEMPTS_EXCEEDED -> {
                            markNotStarted(challenge.type)
                        }
                    }
                    publish()
                }
            )
        }
    }

    private fun markNotStarted(type: VerificationType) {
        challenge = challenge?.copy(isStarted = false)
        previousChallenges[type]?.let { previousChallenges[type] = it.copy(isStarted = false) }
    }

    private fun factorFor(type: VerificationType): ConsumerSession.VerificationFactor? {
        if (type !in capabilities) return null
        val factors = account?.availableVerificationFactors
        if (factors != null) {
            return factors.firstOrNull { it.isStartable && it.type.verificationType == type }
        }
        // Legacy endpoints predate the factor list and only support SMS.
        return if (type == VerificationType.SMS) {
            ConsumerSession.VerificationFactor(
                type = FactorType.Sms,
                id = null,
                providesFurtherVerification = true,
                temporarilyDisabled = false,
            )
        } else {
            null
        }
    }

    private fun advance(previous: VerificationType?) {
        if (finished) return
        val account = account
        if (account == null) {
            block()
            return
        }
        if (account.isVerified) {
            if (!didStart) {
                screen = LinkAuthFlowState.Screen.Loading
                publish()
                return
            }
            finish(LinkAuthFlowResult.Completed)
            return
        }
        if (account.webviewRequired) {
            if (!didStart) {
                screen = LinkAuthFlowState.Screen.Loading
                publish()
                return
            }
            finish(LinkAuthFlowResult.RequiresWebAuth)
            return
        }
        val order = if (previous == VerificationType.SMS) {
            listOf(VerificationType.EMAIL, VerificationType.SMS)
        } else {
            listOf(VerificationType.SMS, VerificationType.EMAIL)
        }
        val factor = order.firstNotNullOfOrNull { factorFor(it) }
        // A legacy response has no advancement information; don't loop on the same identifier.
        if (factor == null || (previous != null && account.availableVerificationFactors == null)) {
            block()
            return
        }
        select(factor)
    }

    private fun select(factor: ConsumerSession.VerificationFactor) {
        val type = factor.type.verificationType ?: return
        val previous = previousChallenges[type]
        if (previous != null && previous.factorId == factor.id && previous.isStarted) {
            challenge = previous
            screen = LinkAuthFlowState.Screen.Otp
            resetInput()
            publish()
            return
        }
        val isInitialSelection = step == 1 && history.isEmpty() && previousChallenges.isEmpty()
        val alreadyStarted = didStart && isInitialSelection && account?.hasStartedSession(type) == true
        challenge = Challenge(
            type = type,
            factorId = factor.id,
            phoneNumber = null,
            resendDeadline = null,
            isStarted = alreadyStarted,
        )
        resetInput()
        if (alreadyStarted) {
            // A code was already sent before this flow started (e.g. by the lookup), so don't send another.
            screen = LinkAuthFlowState.Screen.Otp
            challenge?.let { previousChallenges[type] = it }
            publish()
        } else if (type == VerificationType.EMAIL && account?.emailOtpRequiresAdditionalInfo != false) {
            screen = LinkAuthFlowState.Screen.PhoneMatch
            publish()
        } else {
            screen = LinkAuthFlowState.Screen.Otp
            if (didStart) {
                sendCode(isResend = false)
            } else {
                publish()
            }
        }
    }

    private fun sendCode(isResend: Boolean) {
        val challenge = challenge ?: return
        if (isLoading || finished) return
        val previousScreen = screen
        errorMessage = null
        isResending = isResend
        request(
            operation = {
                linkAccountManager.startVerification(
                    type = challenge.type,
                    accountPhoneNumber = challenge.phoneNumber,
                    isResend = isResend,
                )
            },
        ) { result ->
            isResending = false
            result.fold(
                onSuccess = { account ->
                    if (account.isVerified || account.webviewRequired) {
                        advance(previous = null)
                        return@fold
                    }
                    if (!account.hasStartedSession(challenge.type)) {
                        block()
                        return@fold
                    }
                    var started = this.challenge?.copy(isStarted = true) ?: return@fold
                    if (previousScreen == LinkAuthFlowState.Screen.PhoneMatch && step == 1) {
                        history.addLast(Route(LinkAuthFlowState.Screen.PhoneMatch, started))
                    }
                    if (isResend) {
                        started = started.copy(resendDeadline = now() + RESEND_COOLDOWN_MILLIS)
                    }
                    this.challenge = started
                    previousChallenges[challenge.type] = started
                    screen = LinkAuthFlowState.Screen.Otp
                    resetInput()
                    publish()
                    if (isResend) startCooldownTicker()
                },
                onFailure = { error ->
                    val code = error.stripeErrorCode
                    if (challenge.type == VerificationType.EMAIL && code in PHONE_MATCH_ERROR_CODES) {
                        screen = LinkAuthFlowState.Screen.PhoneMatch
                        this.challenge = this.challenge?.copy(isStarted = false)
                    }
                    errorMessage = error.errorMessage
                    publish()
                }
            )
        }
    }

    private fun startCooldownTicker() {
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            while (resendSecondsRemaining() > 0) {
                delay(COOLDOWN_TICK_MILLIS)
                publish()
            }
        }
    }

    private fun resetInput() {
        inputRevision += 1
        errorMessage = null
    }

    private fun block() {
        screen = LinkAuthFlowState.Screen.Blocked
        errorMessage = resolvableString(R.string.stripe_link_auth_blocked)
        isLoading = false
        history.clear()
        publish()
    }

    private fun finish(result: LinkAuthFlowResult) {
        if (finished) return
        finished = true
        generation += 1
        isLoading = false
        history.clear()
        challenge = null
        previousChallenges.clear()
        cooldownJob?.cancel()
        publish()
        onFinish(result)
    }

    private fun request(
        operation: suspend () -> Result<LinkAccount>,
        completion: (Result<LinkAccount>) -> Unit,
    ) {
        if (!didStart || finished || isLoading) return
        isLoading = true
        publish()
        val requestGeneration = generation
        scope.launch {
            val result = execute(operation, requestGeneration) ?: return@launch
            isLoading = false
            completion(result)
        }
    }

    /**
     * Runs [operation], recovering the consumer session once if its credentials expired.
     *
     * @return the result to handle, or null if the response is stale or the flow finished.
     */
    private suspend fun execute(
        operation: suspend () -> Result<LinkAccount>,
        requestGeneration: Int,
    ): Result<LinkAccount>? {
        val result = operation()
        if (isStale(requestGeneration)) return null
        val error = result.exceptionOrNull()
        if (error == null || error.stripeErrorCode !in SESSION_ERROR_CODES) return result

        val recovered = linkAccountManager.recoverSession()
        if (isStale(requestGeneration)) return null
        if (recovered.isFailure) {
            finish(LinkAuthFlowResult.Failed(error))
            return null
        }
        val replayed = operation()
        if (isStale(requestGeneration)) return null
        if (replayed.isFailure) {
            finish(LinkAuthFlowResult.Failed(error))
            return null
        }
        return replayed
    }

    private fun isStale(requestGeneration: Int) = finished || generation != requestGeneration

    private fun canGoBack() = step == 1 && history.isNotEmpty() && !isLoading

    private fun canSubmitCode() =
        screen == LinkAuthFlowState.Screen.Otp && challenge?.isStarted == true && !isLoading && !finished

    private fun canResend() = didStart && screen == LinkAuthFlowState.Screen.Otp && !isLoading && !finished &&
        resendSecondsRemaining() == 0

    private fun resendSecondsRemaining(): Int {
        val deadline = challenge?.resendDeadline ?: return 0
        return max(0, ceil((deadline - now()) / MILLIS_PER_SECOND).toInt())
    }

    private fun actions(): List<LinkAuthFlowState.Action> {
        if (screen != LinkAuthFlowState.Screen.Otp) return emptyList()
        return buildList {
            add(LinkAuthFlowState.Action.Resend)
            if (step == 1 && challenge?.type == VerificationType.SMS && factorFor(VerificationType.EMAIL) != null) {
                add(LinkAuthFlowState.Action.Email)
            }
        }
    }

    private fun recipient(): String {
        val account = account ?: return ""
        return if (challenge?.type == VerificationType.EMAIL) account.email else account.redactedPhoneNumber
    }

    private fun publish() {
        _state.value = buildState()
    }

    private fun buildState() = LinkAuthFlowState(
        screen = screen,
        verificationType = challenge?.type,
        step = step,
        isLoading = isLoading || (screen == LinkAuthFlowState.Screen.Otp && !didStart && !finished),
        isResending = isResending && isLoading,
        errorMessage = errorMessage,
        inputRevision = inputRevision,
        canGoBack = canGoBack(),
        canSubmitCode = canSubmitCode(),
        canResend = canResend(),
        resendSecondsRemaining = resendSecondsRemaining(),
        actions = actions(),
        recipient = recipient(),
        phoneNumberLastTwoDigits = account?.phoneNumberLastTwoDigits,
        phoneNumberCountry = account?.phoneNumberCountry,
    )

    private fun LinkAccount.hasStartedSession(type: VerificationType) = verificationSessions.any {
        it.type.value.equals(type.value, ignoreCase = true) &&
            it.state == ConsumerSession.VerificationSession.SessionState.Started
    }

    private val FactorType.verificationType: VerificationType?
        get() = when (this) {
            FactorType.Sms -> VerificationType.SMS
            FactorType.Email -> VerificationType.EMAIL
            FactorType.Unknown -> null
        }

    private val Throwable.stripeErrorCode: String?
        get() = (this as? StripeException)?.stripeError?.code

    private companion object {
        const val RESEND_COOLDOWN_MILLIS = 10_000L
        const val COOLDOWN_TICK_MILLIS = 1_000L
        const val MILLIS_PER_SECOND = 1_000.0

        const val ERROR_VERIFICATION_EXPIRED = "consumer_verification_expired"
        const val ERROR_VERIFICATION_NOT_FOUND = "consumer_verification_not_found"
        const val ERROR_VERIFICATION_MAX_ATTEMPTS_EXCEEDED = "consumer_verification_max_attempts_exceeded"

        val PHONE_MATCH_ERROR_CODES = setOf("phone_number_missing", "phone_number_mismatch")
        val SESSION_ERROR_CODES = setOf("consumer_session_expired", "consumer_session_credentials_invalid")
    }
}

internal data class LinkAuthFlowState(
    val screen: Screen,
    val verificationType: VerificationType?,
    val step: Int,
    val isLoading: Boolean,
    val isResending: Boolean,
    val errorMessage: ResolvableString?,
    val inputRevision: Int,
    val canGoBack: Boolean,
    val canSubmitCode: Boolean,
    val canResend: Boolean,
    val resendSecondsRemaining: Int,
    val actions: List<Action>,
    val recipient: String,
    val phoneNumberLastTwoDigits: String?,
    val phoneNumberCountry: String?,
) {
    enum class Screen { Loading, Otp, PhoneMatch, Blocked }

    enum class Action { Resend, Email }
}

internal sealed interface LinkAuthFlowResult {
    data object Completed : LinkAuthFlowResult
    data object Canceled : LinkAuthFlowResult
    data object SwitchAccount : LinkAuthFlowResult
    data object RequiresWebAuth : LinkAuthFlowResult
    data class Failed(val error: Throwable) : LinkAuthFlowResult
}
