package com.stripe.android.ui.core.cardscan

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

internal interface CardScanLauncher {
    val loadingState: StateFlow<CardScanLoadingState>
    fun launch(context: Context)
}
