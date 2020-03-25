package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiFingerprintParamsFactoryTest {

    private val factory = ApiFingerprintParamsFactory(
        store = ClientFingerprintDataStore.Default(
            ApplicationProvider.getApplicationContext()
        )
    )

    @Test
    fun testCreate() {
        val uidParams = factory.createParams(
            guid = UUID.randomUUID().toString()
        )
        assertThat(uidParams.keys)
            .containsExactly("muid", "guid")
    }
}
