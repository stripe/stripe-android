package com.stripe.android.financialconnections.lite

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForData
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForInstantDebits
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.ForToken
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.FinishWithResult
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenCustomTab
import com.stripe.android.financialconnections.lite.di.Di
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import com.stripe.android.financialconnections.utils.HostedAuthUrlBuilder
import com.stripe.android.financialconnections.utils.InstantDebitsResultBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.net.toUri

internal class FinancialConnectionsLiteViewModel(
    private val logger: Logger,
    savedStateHandle: SavedStateHandle,
    private val repository: FinancialConnectionsLiteRepository,
    private val workContext: CoroutineDispatcher,
    applicationId: String
) : ViewModel() {

    private val args: FinancialConnectionsSheetActivityArgs =
        savedStateHandle[EXTRA_ARGS] ?: throw IllegalStateException("Missing arguments")

    private val _viewEffects = MutableSharedFlow<ViewEffect>()
    val viewEffects: SharedFlow<ViewEffect> = _viewEffects.asSharedFlow()

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    init {
        viewModelScope.launch(workContext) {
            runCatching {
                val sync = repository.synchronize(args.configuration, applicationId).getOrThrow()
                val hostedAuthUrl = HostedAuthUrlBuilder.create(
                    args,
                    sync.manifest.hostedAuthUrl
                )
                val state = State(
                    successUrl = requireNotNull(sync.manifest.successUrl),
                    cancelUrl = requireNotNull(sync.manifest.cancelUrl),
                    hostedAuthUrl = requireNotNull(hostedAuthUrl)
                )
                _state.update { state }
                _viewEffects.emit(OpenAuthFlowWithUrl(state.hostedAuthUrl))
            }.onFailure {
                handleError(it, "Failed to synchronize session")
            }
        }
    }

    fun handleUrl(uri: String) = withState { state ->
        when {
            uri.contains(state.successUrl) -> {
                when (args) {
                    is ForData -> onSuccessFromDataFlow(userCancelled = false)
                    is ForToken -> onSuccessFromTokenFlow(userCancelled = false)
                    is ForInstantDebits -> onSuccessFromInstantDebits(uri)
                }
            }
            uri.contains(state.cancelUrl) -> {
                when (args) {
                    is ForData -> onSuccessFromDataFlow(userCancelled = true)
                    is ForToken -> onSuccessFromTokenFlow(userCancelled = true)
                    is ForInstantDebits -> onAuthFlowCanceled()
                }
            }
            else -> {
                launchInBrowser(uri)
            }
        }
    }

    private fun onSuccessFromTokenFlow(userCancelled: Boolean) {
        viewModelScope.launch(workContext) {
            runCatching {
                val session = repository.getFinancialConnectionsSession(args.configuration).getOrThrow()
                if (session.paymentAccount == null && userCancelled) {
                    _viewEffects.emit(FinishWithResult(result = Canceled))
                } else {
                    _viewEffects.emit(
                        FinishWithResult(
                            Completed(
                                financialConnectionsSession = session,
                                token = requireNotNull(session.parsedToken)
                            )
                        )
                    )
                }
            }.onFailure {
                handleError(it, "Failed to complete session for token flow")
            }
        }
    }

    private fun onSuccessFromDataFlow(userCancelled: Boolean) {
        viewModelScope.launch(workContext) {
            runCatching {
                val session = repository.getFinancialConnectionsSession(args.configuration).getOrThrow()
                if (session.paymentAccount == null && userCancelled) {
                    _viewEffects.emit(FinishWithResult(result = Canceled))
                } else {
                    _viewEffects.emit(FinishWithResult(result = Completed(financialConnectionsSession = session)))
                }
            }.onFailure {
                handleError(it, "Failed to complete session for data flow")
            }
        }
    }

    private fun onSuccessFromInstantDebits(url: String) = viewModelScope.launch {
        InstantDebitsResultBuilder.fromUri(url.toUri())
            .onSuccess {
                _viewEffects.emit(
                    FinishWithResult(
                        Completed(
                            instantDebits = it,
                            financialConnectionsSession = null,
                            token = null
                        )
                    )
                )
            }.onFailure { error ->
                handleError(error, "Failed to parse instant debits result from url: $url")
            }
    }

    private fun onAuthFlowCanceled() {
        viewModelScope.launch {
            _viewEffects.emit(FinishWithResult(result = Canceled))
        }
    }

    private fun launchInBrowser(uri: String) {
        viewModelScope.launch {
            _viewEffects.emit(OpenCustomTab(uri))
        }
    }

    private fun withState(block: (State) -> Unit) = runCatching {
        block(requireNotNull(_state.value))
    }.onFailure {
        handleError(it, "State is null")
    }

    private fun handleError(error: Throwable, message: String) {
        logger.error(message, error)
        viewModelScope.launch {
            _viewEffects.emit(FinishWithResult(result = Failed(error)))
        }
    }

    internal data class State(
        val successUrl: String,
        val cancelUrl: String,
        val hostedAuthUrl: String
    )

    internal sealed class ViewEffect {
        data class OpenAuthFlowWithUrl(val url: String) : ViewEffect()
        data class OpenCustomTab(val url: String) : ViewEffect()
        data class FinishWithResult(val result: FinancialConnectionsSheetActivityResult) : ViewEffect()
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            val appContext = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Context

            return FinancialConnectionsLiteViewModel(
                savedStateHandle = savedStateHandle,
                applicationId = appContext.packageName,
                logger = Di.logger,
                workContext = Di.workContext,
                repository = Di.repository()
            ) as T
        }
    }
}
