package com.stripe.android.attestation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.attestation.AttestationActivity.Companion.getArgs
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

internal class AttestationViewModel @Inject constructor(
    private val integrityRequestManager: IntegrityRequestManager
) : ViewModel() {
    private val _result = MutableSharedFlow<AttestationActivityResult>(replay = 1)
    val result: Flow<AttestationActivityResult> = _result

    suspend fun attest() {
        integrityRequestManager.requestToken()
            .onSuccess { token ->
                _result.emit(AttestationActivityResult.Success(token))
            }.onFailure { error ->
                _result.emit(AttestationActivityResult.Failed(error))
            }
    }

    internal class NoArgsException : IllegalArgumentException("No args received for AttestationActivity")

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val args: AttestationArgs = getArgs(createSavedStateHandle())
                    ?: throw NoArgsException()
                val app = this[APPLICATION_KEY] as Application
                DaggerAttestationComponent
                    .builder()
                    .context(app)
                    .application(app)
                    .publishableKeyProvider { args.publishableKey }
                    .productUsage(args.productUsage.toSet())
                    .build()
                    .attestationViewModel
            }
        }
    }
}
