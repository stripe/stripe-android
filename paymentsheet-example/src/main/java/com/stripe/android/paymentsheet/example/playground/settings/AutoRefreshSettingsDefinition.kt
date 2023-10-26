package com.stripe.android.paymentsheet.example.playground.settings

internal object AutoRefreshSettingsDefinition : BooleanSettingsDefinition(
    key = "auto_refresh",
    displayName = "Auto Refresh",
    defaultValue = true,
)
