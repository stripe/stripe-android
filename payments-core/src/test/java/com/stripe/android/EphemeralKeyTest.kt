package com.stripe.android

import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class EphemeralKeyTest {
    @Test
    fun toParcel_fromParcel_createsExpectedObject() {
        assertEquals(EphemeralKeyFixtures.FIRST, ParcelUtils.create(EphemeralKeyFixtures.FIRST))
    }
}
