package com.stripe.android.core.model

import com.stripe.android.core.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CountryCodeTest {

    @Test
    fun `verify CountryCode parcel roundtrip`() {
        ParcelUtils.verifyParcelRoundtrip(CountryCode.US)
    }
}
