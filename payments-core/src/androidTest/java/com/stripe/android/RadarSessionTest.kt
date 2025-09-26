package com.stripe.android

import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.RadarSessionWithHCaptcha
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.AbsPaymentController
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.RetryRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

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

    private val mockPaymentController: PaymentController = object : AbsPaymentController() {}

    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripe: Stripe =
        Stripe(
            mockApiRepository,
            mockPaymentController,
            FAKE_PUBLISHABLE_KEY,
            TEST_STRIPE_ACCOUNT_ID,
            testDispatcher
        )

    private val scenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(CoroutineTestRule(testDispatcher))
        .around(scenarioRule)
        .around(RetryRule(3))

    @Test
    fun ensureRadarSessionsAttachHCaptchaToken(): Unit = runTest(testDispatcher) {
        val result = CompletableDeferred<String>()
        scenarioRule.scenario.onActivity { activity ->
            launch(testDispatcher) {
                val session = stripe.createRadarSession(activity)
                result.complete(session.id)
            }
        }
        testScheduler.advanceUntilIdle()
        assertThat(result.await()).isEqualTo("rse_id")
    }

    private companion object {
        private const val FAKE_PUBLISHABLE_KEY = "pk_test_123"
        private const val TEST_STRIPE_ACCOUNT_ID = "test_account_id"
        private const val HCAPTCHA_SITE_KEY = "10000000-ffff-ffff-ffff-000000000001"
    }
}
