package com.stripe.android

import com.stripe.android.model.Card
import com.stripe.android.model.CardFixtures
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [StripeNetworkUtils]
 */
@RunWith(RobolectricTestRunner::class)
class StripeNetworkUtilsTest {

    private val networkUtils = StripeNetworkUtils(
        UidParamsFactory("com.example.app", FakeUidSupplier())
    )

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun hashMapFromCard_mapsCorrectFields() {
        val cardMap = requireNotNull(getCardTokenParamData(CardFixtures.CARD))
        assertEquals(CARD_NUMBER, cardMap["number"])
        assertEquals(CARD_CVC, cardMap["cvc"])
        assertEquals(8, cardMap["exp_month"])
        assertEquals(2019, cardMap["exp_year"])
        assertEquals(CARD_NAME, cardMap["name"])
        assertEquals(CARD_CURRENCY, cardMap["currency"])
        assertEquals(CARD_ADDRESS_L1, cardMap["address_line1"])
        assertEquals(CARD_ADDRESS_L2, cardMap["address_line2"])
        assertEquals(CARD_CITY, cardMap["address_city"])
        assertEquals(CARD_ZIP, cardMap["address_zip"])
        assertEquals(CARD_STATE, cardMap["address_state"])
        assertEquals(CARD_COUNTRY, cardMap["address_country"])
    }

    @Test
    fun createCardTokenParams_hasExpectedEntries() {
        val card = Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC)
            .build()

        val cardMap = requireNotNull(getCardTokenParamData(card))
        assertEquals(CARD_NUMBER, cardMap["number"])
        assertEquals(CARD_CVC, cardMap["cvc"])
        assertEquals(8, cardMap["exp_month"])
        assertEquals(2019, cardMap["exp_year"])
    }

    @Test
    fun addUidParamsToPaymentIntent_withSource_addsParamsAtRightLevel() {
        val updatedParams = networkUtils.paramsWithUid(
            mapOf(ConfirmPaymentIntentParams.API_PARAM_SOURCE_DATA to emptyMap<String, Any>())
        )

        val updatedData =
            updatedParams[ConfirmPaymentIntentParams.API_PARAM_SOURCE_DATA] as Map<String, *>
        assertEquals(1, updatedParams.size)
        assertTrue(updatedData.containsKey("muid"))
        assertTrue(updatedData.containsKey("guid"))
    }

    @Test
    fun addUidParamsToPaymentIntent_withPaymentMethodParams_addsUidAtRightLevel() {
        val updatedParams = networkUtils.paramsWithUid(
            mapOf(ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_DATA to
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD.toParamMap())
        )
        val updatedData =
            updatedParams[ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_DATA] as Map<String, *>
        assertEquals(1, updatedParams.size)
        assertTrue(updatedData.containsKey("muid"))
        assertTrue(updatedData.containsKey("guid"))
    }

    private fun getCardTokenParamData(card: Card): Map<String, Any>? {
        val cardTokenParams = networkUtils.createCardTokenParams(card)
        return cardTokenParams["card"] as Map<String, Any>?
    }

    companion object {
        private const val CARD_ADDRESS_L1 = "123 Main Street"
        private const val CARD_ADDRESS_L2 = "906"
        private const val CARD_CITY = "San Francisco"
        private const val CARD_COUNTRY = "US"
        private const val CARD_CURRENCY = "USD"
        private const val CARD_CVC = "123"
        private const val CARD_NAME = "J Q Public"
        private const val CARD_NUMBER = "4242424242424242"
        private const val CARD_STATE = "CA"
        private const val CARD_ZIP = "94107"
    }
}
