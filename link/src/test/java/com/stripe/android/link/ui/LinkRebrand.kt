package com.stripe.android.link.ui

import app.cash.paparazzi.DeviceConfig
import com.stripe.android.link.utils.screenshots.PaparazziConfigOption

internal enum class LinkRebrand(val useLinkRebrand: Boolean) : PaparazziConfigOption {
    Legacy(useLinkRebrand = false),
    New(useLinkRebrand = true);

    override fun apply(deviceConfig: DeviceConfig): DeviceConfig {
        LinkUi.useNewBrand = useLinkRebrand
        return deviceConfig
    }
}
