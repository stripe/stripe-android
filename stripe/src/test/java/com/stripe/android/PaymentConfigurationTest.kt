package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.utils.ParcelUtils
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

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @BeforeTest
    fun setup() {
        // Make sure we initialize before each test.
        PaymentConfiguration.clearInstance()
    }

    @Test
    fun getInstance_beforeInit_throwsRuntimeException() {
        assertFailsWith<IllegalStateException> {
            PaymentConfiguration.getInstance(context)
        }
    }

    @Test
    fun getInstance_whenInstanceIsNull_loadsFromPrefs() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )

        PaymentConfiguration.clearInstance()

        assertEquals(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            PaymentConfiguration
                .getInstance(context)
                .publishableKey
        )
    }

    @Test
    fun testParcel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val paymentConfig = PaymentConfiguration.getInstance(context)
        assertEquals(paymentConfig, ParcelUtils.create(paymentConfig))
    }
}
