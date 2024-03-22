package com.stripe.android.financialconnections.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.error.ErrorContent
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction.Close
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction.EnterDetailsManually
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction.SelectAnotherBank
import com.stripe.android.financialconnections.presentation.FinancialConnectionsErrorAction.TryAgain

@Composable
internal fun <T> LoadableContent(
    payload: Async<T>,
    onErrorAction: (FinancialConnectionsErrorAction) -> Unit,
    modifier: Modifier = Modifier,
    customMapping: LoadableContentCustomMapping = LoadableContentCustomMapping(),
    content: @Composable (T) -> Unit,
) {
    Box {
        when (payload) {
            is Uninitialized, is Loading -> {
                FullScreenGenericLoading()
            }
            is Fail -> {
                ErrorContent(
                    error = payload.error,
                    onSelectAnotherBank = { onErrorAction(SelectAnotherBank) },
                    onEnterDetailsManually = { onErrorAction(EnterDetailsManually) },
                    onTryAgain = { onErrorAction(TryAgain) },
                    onCloseFromErrorClick = { onErrorAction(Close(it)) },
                )
            }
            is Success -> {
                content(payload())
            }
        }
    }
}

internal data class LoadableContentCustomMapping(
    val showContentOnIncomplete: Boolean = false,
)
