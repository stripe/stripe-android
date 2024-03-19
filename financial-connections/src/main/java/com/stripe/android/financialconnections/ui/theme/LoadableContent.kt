package com.stripe.android.financialconnections.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading

@Composable
internal fun <T> LoadableContent(
    async: Async<T>,
    showLoadingInSuccessState: (T) -> Boolean = { false },
    loading: @Composable () -> Unit = { FullScreenGenericLoading() },
    content: @Composable (T) -> Unit,
) {
    Crossfade(
        targetState = async,
        label = "LoadableContentCrossfade",
    ) { state ->
        when (state) {
            Uninitialized, is Loading, is Fail -> {
                loading()
            }
            is Success -> {
                if (showLoadingInSuccessState(state())) {
                    loading()
                } else {
                    content(state())
                }
            }
        }
    }
}
