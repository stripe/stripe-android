package com.stripe.android.common.nfcscan.tapzone

import javax.inject.Inject

internal interface TapZoneResolver {
    fun get(): TapZone
}

internal class DefaultTapZoneResolver @Inject constructor() : TapZoneResolver {
    override fun get(): TapZone {
        return DEFAULT
    }

    private companion object {
        val DEFAULT = TapZone(xBias = 0.5f, yBias = 0.5f)
    }
}
