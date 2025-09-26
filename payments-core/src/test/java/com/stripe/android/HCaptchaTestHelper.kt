package com.stripe.android

import com.stripe.android.challenge.FakeHCaptchaService
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.hcaptcha.HCaptchaService
import com.stripe.android.model.RadarSessionWithHCaptcha
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.AbsPaymentController
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Helper class to create test Stripe instances with mocked HCaptcha behavior for Java tests.
 */
object HCaptchaTestHelper {

    val radarSessionWithHCaptcha = RadarSessionWithHCaptcha(
        id = "rse_id",
        passiveCaptchaSiteKey = "10000000-ffff-ffff-ffff-000000000001",
        passiveCaptchaRqdata = "rqdata"
    )
    private const val PASSIVE_CAPTCHA_TOKEN = "10000000-aaaa-bbbb-cccc-000000000001"

    /**
     * Creates a Stripe instance configured for HCaptcha success testing.
     */
    @JvmStatic
    fun createStripeWithHCaptchaSuccess(
        publishableKey: String,
        dispatcher: CoroutineDispatcher
    ): Stripe {
        val mockRepository = object : AbsFakeStripeRepository() {
            override suspend fun createRadarSession(
                requestOptions: ApiRequest.Options
            ): Result<RadarSessionWithHCaptcha> {
                return Result.success(radarSessionWithHCaptcha)
            }

            override suspend fun attachHCaptchaToRadarSession(
                radarSessionToken: String,
                hcaptchaToken: String,
                hcaptchaEKey: String?,
                requestOptions: ApiRequest.Options
            ): Result<RadarSessionWithHCaptcha> {
                return Result.success(radarSessionWithHCaptcha)
            }
        }

        val fakeHCaptchaService = FakeHCaptchaService().apply {
            result = HCaptchaService.Result.Success(PASSIVE_CAPTCHA_TOKEN)
        }

        return Stripe(
            mockRepository,
            object : AbsPaymentController() {},
            publishableKey,
            null,
            dispatcher,
            fakeHCaptchaService
        )
    }

    /**
     * Creates a Stripe instance configured for HCaptcha failure testing.
     */
    @JvmStatic
    fun createStripeWithHCaptchaFailure(
        publishableKey: String,
        dispatcher: CoroutineDispatcher,
        errorMessage: String = "HCaptcha failed"
    ): Stripe {
        val mockRepository = object : AbsFakeStripeRepository() {
            override suspend fun createRadarSession(
                requestOptions: ApiRequest.Options
            ): Result<RadarSessionWithHCaptcha> {
                return Result.success(radarSessionWithHCaptcha)
            }

            override suspend fun attachHCaptchaToRadarSession(
                radarSessionToken: String,
                hcaptchaToken: String,
                hcaptchaEKey: String?,
                requestOptions: ApiRequest.Options
            ): Result<RadarSessionWithHCaptcha> {
                throw IllegalArgumentException(errorMessage)
            }
        }

        val fakeHCaptchaService = FakeHCaptchaService().apply {
            result = HCaptchaService.Result.Failure(Exception(errorMessage))
        }

        return Stripe(
            mockRepository,
            object : AbsPaymentController() {},
            publishableKey,
            null,
            dispatcher,
            fakeHCaptchaService
        )
    }
}
