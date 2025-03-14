package com.stripe.android.financialconnections.lite

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Canceled
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class FinancialConnectionsLiteViewModel(
    val savedStateHandle: SavedStateHandle,
    val repository: FinancialConnectionsLiteRepository,
    applicationId: String
) : ViewModel() {

    private val args: FinancialConnectionsSheetActivityArgs
        get() = savedStateHandle.get<FinancialConnectionsSheetActivityArgs>(EXTRA_ARGS)!!

    internal val viewEffects = MutableSharedFlow<ViewEffect>()

    init {
        viewModelScope.launch {
            val sync = repository.synchronize(
                configuration = args.configuration,
                applicationId = applicationId
            )
            viewEffects.emit(
                OpenAuthFlowWithUrl(requireNotNull(sync.manifest.hostedAuthUrl))
            )
        }
    }

    fun handleUrl(uri: Uri) {
        if (uri.toString().contains("success") || uri.toString().contains("cancel")) {
            viewModelScope.launch {
                val fcSession = repository.getFinancialConnectionsSession(
                    configuration = args.configuration
                )
                if (fcSession.paymentAccount != null) {
                    viewEffects.emit(
                        ViewEffect.FinishWithResult(
                            Completed(
                                financialConnectionsSession = fcSession,
                            )
                        )
                    )
                } else {
                    viewEffects.emit(
                        ViewEffect.FinishWithResult(Canceled)
                    )
                }
            }
        }
    }

    internal sealed class ViewEffect {
        data class OpenAuthFlowWithUrl(val url: String) : ViewEffect()
        data class FinishWithResult(val result: FinancialConnectionsSheetActivityResult) : ViewEffect()
    }
}
