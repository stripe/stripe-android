package com.stripe.android.utils

import com.stripe.android.ui.core.elements.AutomaticallyLaunchedCardScanFormDataHelper
import com.stripe.android.ui.core.elements.CardDetailsAction
import com.stripe.android.ui.core.elements.CardScanAction

val CardDetailsAction.shouldAutomaticallyLaunchCardScan: Boolean?
    get() = getAutomaticallyLaunchedCardScanFormDataHelper()?.shouldLaunchCardScanAutomatically

val CardDetailsAction.hasAutomaticallyLaunchedCardScan: Boolean?
    get() = getAutomaticallyLaunchedCardScanFormDataHelper()?.hasAutomaticallyLaunchedCardScan

fun CardDetailsAction.setHasAutomaticallyLaunchedCardScan() {
    getAutomaticallyLaunchedCardScanFormDataHelper()?.let { helper ->
        helper.hasAutomaticallyLaunchedCardScan = true
    }
}

private fun CardDetailsAction.getAutomaticallyLaunchedCardScanFormDataHelper():
    AutomaticallyLaunchedCardScanFormDataHelper? {
    val cardScanAction = this as? CardScanAction

    return cardScanAction?.automaticallyLaunchedCardScanFormDataHelper
}
