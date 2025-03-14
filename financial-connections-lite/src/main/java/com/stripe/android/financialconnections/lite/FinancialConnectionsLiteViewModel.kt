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
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Failed
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.FinishWithResult
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.lite.di.Di
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal class FinancialConnectionsLiteViewModel(
    private val logger: Logger,
    private val savedStateHandle: SavedStateHandle,
    private val repository: FinancialConnectionsLiteRepository,
    private val workContext: CoroutineDispatcher,
    applicationId: String
) : ViewModel() {

    private val args: FinancialConnectionsSheetActivityArgs
        get() = savedStateHandle.get<FinancialConnectionsSheetActivityArgs>(EXTRA_ARGS)!!

    private val _viewEffects = MutableSharedFlow<ViewEffect>()
    val viewEffects: SharedFlow<ViewEffect>
        get() = _viewEffects

    init {
        viewModelScope.launch(workContext) {
            repository.synchronize(
                configuration = args.configuration,
                applicationId = applicationId
            ).onSuccess { sync ->
                _viewEffects.emit(
                    OpenAuthFlowWithUrl(requireNotNull(sync.manifest.hostedAuthUrl))
                )
            }.onFailure { throwable ->
                logger.error("Failed to synchronize session", throwable)
                _viewEffects.emit(
                    FinishWithResult(
                        result = Failed(
                            error = throwable,
                        )
                    )
                )
            }
        }
    }

    fun handleUrl(uri: Uri) {
        if (uri.toString().contains("success") || uri.toString().contains("cancel")) {
            viewModelScope.launch(workContext) {
                repository.getFinancialConnectionsSession(
                    configuration = args.configuration
                ).onSuccess {
                    _viewEffects.emit(
                        FinishWithResult(
                            Completed(
                                financialConnectionsSession = it,
                            )
                        )
                    )
                }.onFailure { throwable ->
                    logger.error("Failed to retrieve financial connections session", throwable)
                    _viewEffects.emit(
                        FinishWithResult(
                            result = Failed(
                                error = throwable,
                            )
                        )
                    )
                }
            }
        }
    }

    internal sealed class ViewEffect {
        data class OpenAuthFlowWithUrl(val url: String) : ViewEffect()
        data class FinishWithResult(val result: FinancialConnectionsSheetActivityResult) : ViewEffect()
    }

    class Factory : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            val appContext = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Context

            if (modelClass.isAssignableFrom(FinancialConnectionsLiteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FinancialConnectionsLiteViewModel(
                    savedStateHandle = savedStateHandle,
                    applicationId = appContext.packageName,
                    logger = Di.logger,
                    workContext = Di.workContext,
                    repository = Di.repository()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
