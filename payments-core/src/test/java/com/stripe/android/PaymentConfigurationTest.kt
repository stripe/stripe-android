package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
    fun `stripeAccountId should be persisted`() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_12345"
        )

        assertThat(
            PaymentConfiguration.getInstance(context)
        ).isEqualTo(
            PaymentConfiguration(
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                "acct_12345"
            )
        )
    }

    @Test
    fun getInstance_whenInstanceIsNull_loadsFromPrefs() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )

        PaymentConfiguration.clearInstance()

        assertThat(
            PaymentConfiguration.getInstance(context)
        ).isEqualTo(
            PaymentConfiguration(
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
            )
        )
    }

    @Test
    fun testParcel() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        val paymentConfig = PaymentConfiguration.getInstance(context)
        assertThat(ParcelUtils.create(paymentConfig))
            .isEqualTo(paymentConfig)
    }

    @Test
    fun `isLiveMode is true when publishable key is live`() {
        assertThat(PaymentConfiguration(publishableKey = "pk_test_123").isLiveMode()).isFalse()
        assertThat(PaymentConfiguration(publishableKey = "pk_live_123").isLiveMode()).isTrue()
        assertThat(
            PaymentConfiguration(
                publishableKey = "pk_test_51HvTI7Lu5o3livep6t5AgBSkMvWoTtA0nyA7pV"
            ).isLiveMode()
        ).isFalse()
    }
}
