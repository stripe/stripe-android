package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class CashAppLpmTest(
    private val testType: TestType,
) : BaseLpmNetworkTest(PaymentMethod.Type.CashAppPay) {
    @Test
    fun `Confirm Cash App LPM`() = test(
        testType = testType,
        createParams = PaymentMethodCreateParams.createCashAppPay(),
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun testTypes() = TestType.default().map {
            arrayOf(it)
        }
    }
}
