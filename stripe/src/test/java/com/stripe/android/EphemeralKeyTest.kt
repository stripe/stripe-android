package com.stripe.android

import com.stripe.android.utils.ParcelUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EphemeralKeyTest {
    @Test
    fun toParcel_fromParcel_createsExpectedObject() {
        assertEquals(EphemeralKeyFixtures.FIRST, ParcelUtils.create(EphemeralKeyFixtures.FIRST))
    }
}
