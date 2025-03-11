package com.stripe.android.paymentelement.confirmation.lpms

import com.stripe.android.model.CardParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.MerchantCountry
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class CardLpmTest(
    private val testType: TestType,
) : BaseLpmNetworkTest(PaymentMethod.Type.Card) {
    @Test
    fun `Confirm a card with US merchant`() = test(
        testType = testType,
        country = MerchantCountry.US,
        createParams = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 12,
                expYear = 2032,
                cvc = "454",
            )
        ),
    )

    @Test
    fun `Confirm a card with FR merchant`() = test(
        testType = testType,
        country = MerchantCountry.FR,
        createParams = PaymentMethodCreateParams.createCard(
            cardParams = CardParams(
                number = "4242424242424242",
                expMonth = 12,
                expYear = 2032,
                cvc = "454",
            )
        ),
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun testTypes() = listOf(
            arrayOf(TestType.PaymentIntent),
            arrayOf(TestType.PaymentIntentWithSetupFutureUsage),
            arrayOf(TestType.SetupIntent),
            arrayOf(TestType.DeferredPaymentIntent),
            arrayOf(TestType.DeferredPaymentIntentWithSetupFutureUsage),
            arrayOf(TestType.DeferredSetupIntent),
        )
    }
}
