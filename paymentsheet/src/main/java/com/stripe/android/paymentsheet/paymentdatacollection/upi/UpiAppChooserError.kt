package com.stripe.android.paymentsheet.paymentdatacollection.upi

import com.stripe.android.payments.core.analytics.ErrorReporter

internal enum class UpiAppChooserError(override val eventName: String) : ErrorReporter.ErrorEvent {
    InvalidArgs("payments.upi_app_chooser.invalid_args"),
    NoCompatibleApp("payments.upi_app_chooser.no_compatible_app"),
    LaunchFailed("payments.upi_app_chooser.launch_failed"),
    MissingLauncher("payments.upi_app_chooser.missing_launcher"),
}
