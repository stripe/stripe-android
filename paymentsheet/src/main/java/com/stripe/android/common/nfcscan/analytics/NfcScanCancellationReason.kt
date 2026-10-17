package com.stripe.android.common.nfcscan.analytics

internal enum class NfcScanCancellationReason(val analyticsValue: String) {
    UserInitiated("user_initiated"),
    Timeout("scanning_timeout"),
}
