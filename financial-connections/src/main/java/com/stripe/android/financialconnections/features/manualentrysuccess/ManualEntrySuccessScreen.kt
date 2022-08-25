@file:Suppress("TooManyFunctions")

package com.stripe.android.financialconnections.features.manualentrysuccess

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun ManualEntrySuccessScreen(
    microdepositVerificationMethod: MicrodepositVerificationMethod,
    last4: String?
) {
    ManualEntrySuccessContent(
        microdepositVerificationMethod,
        last4
    )
}

@Composable
private fun ManualEntrySuccessContent(
    microdepositVerificationMethod: MicrodepositVerificationMethod,
    last4: String?
) {
    Column {
        Text(microdepositVerificationMethod.value)
        Text(last4 ?: "UNKNOWN")
    }
}

@Preview
@Composable
internal fun ManualEntrySuccessScreenPreview() {
    FinancialConnectionsTheme {
    }
}
