package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UidParamsFactoryTest {

    private val factory = UidParamsFactory(
        store = ClientFingerprintDataStore.Default(
            ApplicationProvider.getApplicationContext()
        ),
        uidSupplier = FakeUidSupplier()
    )

    @Test
    fun testCreate() {
        val uidParams = factory.createParams()
        assertThat(uidParams.keys)
            .containsExactly("muid", "guid")
    }
}
