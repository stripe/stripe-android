package com.stripe.android

import androidx.test.core.app.launchActivity
import com.stripe.android.model.RadarSession
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.view.PaymentFlowActivity
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class StripeKxtTest {
    private val mockApiRepository: StripeApiRepository = mock()
    private val mockPaymentController: PaymentController = mock()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripe: Stripe =
        Stripe(
            mockApiRepository,
            mockPaymentController,
            "FAKE_PUBLISHABLE_KEY",
            TEST_STRIPE_ACCOUNT_ID,
            testDispatcher
        )

    @Test
    fun ensureRadarSesssionsAttachHCaptchaToken(): Unit = runTest {
        whenever(mockApiRepository.createRadarSession(any())).thenReturn(
            Result.success(RadarSession("rse_id", "sitekey", "rqdata"))
        )

        launchActivity<PaymentFlowActivity>().use { scenario ->
            scenario.onActivity { activity ->
                runTest {
                    whenever(mockApiRepository.attachHCaptchaToRadarSession(any(), any(), any(), any())).thenReturn(
                        Result.success(RadarSession("rse_id", "sitekey", "rqdata"))
                    )

                    stripe.createRadarSession(activity)

                    verify(mockApiRepository.attachHCaptchaToRadarSession(eq("rse_id"), any(), any(), any()))
                }
            }
        }
    }

    private companion object {
        const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
    }
}
