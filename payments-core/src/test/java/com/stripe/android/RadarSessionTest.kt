package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import com.stripe.android.model.RadarSession
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.view.ActivityScenarioFactory
import com.stripe.android.view.PaymentFlowActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RadarSessionTest {
    private val mockApiRepository: StripeApiRepository = mock()
    private val mockPaymentController: PaymentController = mock()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripe: Stripe =
        Stripe(
            mockApiRepository,
            mockPaymentController,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            TEST_STRIPE_ACCOUNT_ID,
            testDispatcher
        )

    private val ephemeralKeyProvider: EphemeralKeyProvider = mock()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
        CustomerSession.initCustomerSession(context, ephemeralKeyProvider)
    }

    @Test
    fun ensureRadarSesssionsAttachHCaptchaToken(): Unit = runTest {
        whenever(mockApiRepository.createRadarSession(any())).thenReturn(
            Result.success(RadarSession("rse_id", "sitekey", "rqdata"))
        )

        whenever(mockApiRepository.attachHCaptchaToRadarSession(any(), any(), any(), any())).thenReturn(
            Result.success(RadarSession("rse_id", "sitekey", "rqdata"))
        )

        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { scenario ->
            scenario.onActivity { activity ->
                runBlocking {
                    stripe.createRadarSession(activity)
                }
            }
        }

        verify(mockApiRepository.attachHCaptchaToRadarSession(eq("rse_id"), any(), any(), any()))
    }

    private companion object {
        const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
    }
}
