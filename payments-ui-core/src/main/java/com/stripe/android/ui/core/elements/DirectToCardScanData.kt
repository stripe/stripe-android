package com.stripe.android.ui.core.elements

import kotlinx.coroutines.flow.StateFlow

class DirectToCardScanData(
    val shouldOpenCardScanAutomatically: StateFlow<Boolean>,
    val onCardScanSheetResult: () -> Unit,
)