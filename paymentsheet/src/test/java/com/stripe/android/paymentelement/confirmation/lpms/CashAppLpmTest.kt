package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.lpms.foundations.CreateIntentFactory
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmAssertionParams
import com.stripe.android.paymentelement.confirmation.lpms.foundations.LpmNetworkTestActivity
import com.stripe.android.paymentelement.confirmation.lpms.foundations.assertIntentConfirmed
import com.stripe.android.paymentelement.confirmation.lpms.foundations.assertMandateDataAttached
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class CashAppLpmTest(
    private val testType: TestType,
) : BaseLpmNetworkTest(PaymentMethod.Type.CashAppPay) {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enablePaymentMethodOptionsSetupFutureUsage,
        isEnabled = true
    )

    @Test
    fun `Confirm Cash App LPM`() = test(
        testType = testType,
        createParams = PaymentMethodCreateParams.createCashAppPay(),
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun testTypes() = TestType.all() + PaymentIntentWithSfu
    }

     private data object PaymentIntentWithSfu : TestType {
        override suspend fun createIntent(
            country: MerchantCountry,
            factory: CreateIntentFactory,
        ): Result<CreateIntentFactory.CreateIntentData> {
            return factory.createPaymentIntent(
                amount = 5050, currency = "USD", createWithSetupFutureUsage = false, createWithPmoSfu = true, country = MerchantCountry.US
            )
        }
        override suspend fun assert(activity: LpmNetworkTestActivity, params: LpmAssertionParams) {
            assertMandateDataAttached(activity, params)
        }
    }
}

