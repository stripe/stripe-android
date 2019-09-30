package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [PaymentConfiguration].
 */
@RunWith(RobolectricTestRunner::class)
class PaymentConfigurationTest {

    @BeforeTest
    fun setup() {
        // Make sure we initialize before each test.
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun getInstance_beforeInit_throwsRuntimeException() {
        assertFailsWith<IllegalStateException> {
            PaymentConfiguration.getInstance(ApplicationProvider.getApplicationContext<Context>())
        }
    }

    @Test
    fun getInstance_whenInstanceIsNull_loadsFromPrefs() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext<Context>(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )

        PaymentConfiguration.clearInstance()

        assertEquals(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            PaymentConfiguration
                .getInstance(ApplicationProvider.getApplicationContext<Context>())
                .publishableKey
        )
    }
}
