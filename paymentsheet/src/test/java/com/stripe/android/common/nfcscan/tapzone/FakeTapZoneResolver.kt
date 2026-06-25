package com.stripe.android.common.nfcscan.tapzone

internal class FakeTapZoneResolver(
    private val tapZone: TapZone = TapZone(xBias = 0.5f, yBias = 0.5f),
) : TapZoneResolver {
    override fun get(): TapZone = tapZone
}
