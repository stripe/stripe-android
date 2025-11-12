package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.SourceParams
import com.stripe.android.model.SourceTypeModel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class SourceEndToEndTest {
    @Test
    fun `Source objects should be populated with the expected CardBrand value`() {
        val stripe = createStripe(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        assertThat(
            listOf(
                CardNumberFixtures.AMEX_NO_SPACES to CardBrand.AmericanExpress,
                CardNumberFixtures.VISA_NO_SPACES to CardBrand.Visa,
                CardNumberFixtures.MASTERCARD_NO_SPACES to CardBrand.MasterCard,
                CardNumberFixtures.JCB_NO_SPACES to CardBrand.JCB,
                CardNumberFixtures.UNIONPAY_16_NO_SPACES to CardBrand.UnionPay,
                CardNumberFixtures.DISCOVER_NO_SPACES to CardBrand.Discover,
                CardNumberFixtures.DINERS_CLUB_14_NO_SPACES to CardBrand.DinersClub
            ).all { (cardNumber, cardBrand) ->
                val source = requireNotNull(
                    stripe.createSourceSynchronous(
                        SourceParams.createCardParams(
                            CardParams(
                                number = cardNumber,
                                expMonth = 10,
                                expYear = 2030,
                                cvc = "123"
                            )
                        )
                    )
                )
                val cardModel = requireNotNull(
                    source.sourceTypeModel as? SourceTypeModel.Card
                )
                cardModel.brand == cardBrand
            }
        ).isTrue()
    }

    private fun createStripe(publishableKey: String): Stripe {
        return Stripe(
            ApplicationProvider.getApplicationContext(),
            publishableKey
        )
    }
}
