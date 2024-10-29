package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.RadarSessionWithHCaptcha
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.AbsPaymentController
import com.stripe.android.view.ActivityScenarioFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.lang.RuntimeException

class RadarSessionTest {
    private val mockApiRepository: StripeRepository = object : AbsFakeStripeRepository() {
        override suspend fun createRadarSession(requestOptions: ApiRequest.Options): Result<RadarSessionWithHCaptcha> {
            return Result.success(RadarSessionWithHCaptcha("rse_id", HCAPTCHA_SITE_KEY, "rqdata"))
        }

        override suspend fun attachHCaptchaToRadarSession(
            radarSessionToken: String,
            hcaptchaToken: String,
            hcaptchaEKey: String?,
            requestOptions: ApiRequest.Options
        ): Result<RadarSessionWithHCaptcha> {
            if (hcaptchaToken == "10000000-aaaa-bbbb-cccc-000000000001") {
                return Result.success(RadarSessionWithHCaptcha("rse_id", HCAPTCHA_SITE_KEY, "rqdata"))
            } else {
                throw RuntimeException("Incorrect hCaptcha token: $hcaptchaToken")
            }
        }
    }

    private val mockPaymentController: PaymentController = object : AbsPaymentController() { }

    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripe: Stripe =
        Stripe(
            mockApiRepository,
            mockPaymentController,
            FAKE_PUBLISHABLE_KEY,
            TEST_STRIPE_ACCOUNT_ID,
            testDispatcher
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @Test
    fun ensureRadarSessionsAttachHCaptchaToken(): Unit = runTest {
        activityScenarioFactory.create<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                launch(Dispatchers.Main) {
                    stripe.createRadarSession(activity)
                }
            }
        }
    }

    private companion object {
        private const val FAKE_PUBLISHABLE_KEY = "pk_test_123"
        private const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
        private const val HCAPTCHA_SITE_KEY = "10000000-ffff-ffff-ffff-000000000001"
    }
}
