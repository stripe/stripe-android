package com.stripe.android.financialconnections.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.stripe.android.financialconnections.presentation.parentViewModel

@Composable
internal fun ScrollEffects(state: LazyListState) {
    val parent = parentViewModel()
    LaunchedEffect(state.canScrollBackward) {
        val isScrolled = state.canScrollBackward
        parent.updateTopAppBarElevation(isElevated = isScrolled)
    }
}

@Composable
internal fun ScrollEffects(state: ScrollState) {
    val parent = parentViewModel()
    LaunchedEffect(state.canScrollBackward) {
        val isScrolled = state.canScrollBackward
        parent.updateTopAppBarElevation(isElevated = isScrolled)
    }
}

@Composable
internal fun ScrollEffects(canScrollBackward: Boolean) {
    val parent = parentViewModel()
    LaunchedEffect(canScrollBackward) {
        parent.updateTopAppBarElevation(isElevated = canScrollBackward)
    }
}
