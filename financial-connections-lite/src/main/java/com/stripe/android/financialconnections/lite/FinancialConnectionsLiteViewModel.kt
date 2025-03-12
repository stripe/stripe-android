package com.stripe.android.financialconnections.lite

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs.Companion.EXTRA_ARGS
import kotlinx.coroutines.flow.MutableSharedFlow

internal class FinancialConnectionsLiteViewModel(
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args: FinancialConnectionsSheetActivityArgs
        get() = savedStateHandle.get<FinancialConnectionsSheetActivityArgs>(EXTRA_ARGS)!!

    internal val viewEffects = MutableSharedFlow<ViewEffect>()

    init {
        Log.d("FCLiteViewModel", "args: $args")
    }

    internal sealed class ViewEffect {
        data class OpenAuthFlowWithUrl(val url: String) : ViewEffect()
    }
}
