package com.stripe.android.attestation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.attestation.AttestationActivity.Companion.getArgs
import com.stripe.android.attestation.analytics.AttestationAnalyticsEventsReporter
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
import com.stripe.attestation.AttestationError
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class AttestationViewModel @Inject constructor(
    private val integrityRequestManager: IntegrityRequestManager,
    @IOContext private val workContext: CoroutineContext,
    private val attestationAnalyticsEventsReporter: AttestationAnalyticsEventsReporter,
    private val errorReporter: ErrorReporter
) : ViewModel() {
    private val _result = MutableSharedFlow<AttestationActivityResult>(replay = 1)
    val result: Flow<AttestationActivityResult> = _result

    init {
        viewModelScope.launch(workContext) {
            attest()
        }
    }

    private suspend fun attest() {
        attestationAnalyticsEventsReporter.requestToken()
        integrityRequestManager.requestToken()
            .onSuccess { token ->
                attestationAnalyticsEventsReporter.requestTokenSucceeded()
                _result.emit(AttestationActivityResult.Success(token))
            }.onFailure { error ->
                errorReporter.report(
                    errorEvent = errorEventFor(error),
                    stripeException = StripeException.create(error)
                )
                attestationAnalyticsEventsReporter.requestTokenFailed(error)
                _result.emit(AttestationActivityResult.Failed)
            }
    }

    private fun errorEventFor(error: Throwable): ErrorReporter.ErrorEvent {
        return when ((error as? AttestationError)?.errorType) {
            AttestationError.ErrorType.CLOUD_PROJECT_NUMBER_IS_INVALID ->
                UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_CLOUD_PROJECT_NUMBER_IS_INVALID
            AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID ->
                UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_INTEGRITY_TOKEN_PROVIDER_INVALID
            AttestationError.ErrorType.REQUEST_HASH_TOO_LONG ->
                UnexpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_REQUEST_HASH_TOO_LONG
            else -> ExpectedErrorEvent.INTENT_CONFIRMATION_HANDLER_ATTESTATION_REQUEST_TOKEN_FAILED
        }
    }

    internal class NoArgsException : IllegalArgumentException("No args received for AttestationActivity")

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val args: AttestationArgs = getArgs(createSavedStateHandle())
                    ?: throw NoArgsException()
                val app = this[APPLICATION_KEY] as Application
                DaggerAttestationComponent.factory()
                    .build(
                        application = app,
                        apiConfiguration = args.apiConfiguration,
                        productUsage = args.productUsage.toSet()
                    )
                    .attestationViewModel
            }
        }
    }
}
