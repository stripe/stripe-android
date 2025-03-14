package com.stripe.android.financialconnections.lite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import com.stripe.android.financialconnections.lite.FinancialConnectionsLiteViewModel.ViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.lite.repository.FinancialConnectionsLiteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class FinancialConnectionsLiteViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: FinancialConnectionsLiteRepository,
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

    internal sealed class ViewEffect {
        data class OpenAuthFlowWithUrl(val url: String) : ViewEffect()
    }
}
