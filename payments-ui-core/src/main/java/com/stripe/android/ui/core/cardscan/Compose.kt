package com.stripe.android.ui.core.cardscan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import com.stripe.android.stripecardscan.cardscan.CardScanConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
import com.stripe.android.stripecardscan.externalscan.ComposeCardScanner
import com.stripe.android.stripecardscan.externalscan.rememberGoogleCardScanner

@Composable
fun rememberCardScanner(
    elementsSessionId: String?,
    onResult: (CardScanSheetResult) -> Unit
): ComposeCardScanner {
    val cardScanLauncher = rememberLauncherForActivityResult(CardScanContract()) {
        onResult(it)
    }
    return rememberGoogleCardScanner(
        onCardScanResult = onResult,
        onCardScanUnavailable = {
            cardScanLauncher.launch(CardScanContract.Args(CardScanConfiguration(elementsSessionId)))
        }
    )
}