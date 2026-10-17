package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import com.stripe.android.utils.ParcelUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/**
 * Test class for [SourceParams].
 */
@RunWith(RobolectricTestRunner::class)
class SourceParamsTest {

    @Test
    fun `create with CardParams object should return expected map`() {
        assertThat(
            SourceParams.createCardParams(CardParamsFixtures.DEFAULT)
                .toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "card",
                "card" to mapOf(
                    "number" to VISA_NO_SPACES,
                    "exp_month" to 12,
                    "exp_year" to 2045,
                    "cvc" to "123"
                ),
                "owner" to mapOf(
                    "address" to mapOf(
                        "line1" to "123 Market St",
                        "line2" to "#345",
                        "city" to "San Francisco",
                        "state" to "CA",
                        "postal_code" to "94107",
                        "country" to "US"
                    ),
                    "name" to "Jenny Rosen"
                ),
                "metadata" to mapOf("fruit" to "orange")
            )
        )
    }

    @Test
    fun createCardParamsFromGooglePay_withNoBillingAddress() {
        assertThat(
            SourceParams.createCardParamsFromGooglePay(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS
            )
        ).isEqualTo(
            SourceParams(
                Source.SourceType.CARD,
                token = "tok_1F4ACMCRMbs6FrXf6fPqLnN7",
                attribution = setOf("GooglePay"),
                owner = SourceParams.OwnerParams()
            )
        )
    }

    @Test
    fun createCardParamsFromGooglePay_withFullBillingAddress() {
        assertThat(
            SourceParams.createCardParamsFromGooglePay(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS
            )
        ).isEqualTo(
            SourceParams(
                Source.SourceType.CARD,
                token = "tok_1F4VSjBbvEcIpqUbSsbEtBap",
                attribution = setOf("GooglePay"),
                owner = SourceParams.OwnerParams(
                    email = "stripe@example.com",
                    name = "Stripe Johnson",
                    phone = "1-888-555-1234",
                    address = Address(
                        line1 = "510 Townsend St",
                        city = "San Francisco",
                        state = "CA",
                        postalCode = "94103",
                        country = "US"
                    )
                )
            )
        )
    }

    @Test
    fun createCustomParamsWithSourceTypeParameters_toParamMap_createsExpectedMap() {
        val dogecoin = "dogecoin"

        val dogecoinParams = mapOf("statement_descriptor" to "stripe descriptor")

        val params = SourceParams.createCustomParams(dogecoin)
            .setApiParameterMap(dogecoinParams)
            .also {
                it.currency = Source.EURO
                it.amount = AMOUNT
                it.owner = SourceParams.OwnerParams(name = "Stripe")
            }

        assertThat(
            params.toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to dogecoin,
                "currency" to Source.EURO,
                "amount" to AMOUNT,
                "owner" to mapOf("name" to "Stripe"),
                dogecoin to mapOf("statement_descriptor" to "stripe descriptor")
            )
        )
    }

    @Test
    fun setCustomType_forEmptyParams_setsTypeToUnknown() {
        val params = SourceParams.createCustomParams("dogecoin")
        assertThat(params.type)
            .isEqualTo(Source.SourceType.UNKNOWN)
        assertThat(params.typeRaw)
            .isEqualTo("dogecoin")
    }

    @Test
    fun createCustomParams_withCustomType() {
        val params = SourceParams.createCustomParams("bar_tab")
            .setApiParameterMap(
                mapOf("card" to "card_id_123")
            )
            .also {
                it.amount = AMOUNT
                it.currency = "brl"
            }

        assertThat(
            params.toParamMap()
        ).isEqualTo(
            mapOf(
                "type" to "bar_tab",
                "amount" to AMOUNT,
                "currency" to "brl",
                "bar_tab" to mapOf("card" to "card_id_123")
            )
        )
    }

    @Test
    fun `verify ApiParams parceling`() {
        val apiParams = SourceParams.ApiParams(
            mapOf(
                "type" to "bar_tab",
                "amount" to 1000,
                "currency" to "brl",
                "redirect" to mapOf("return_url" to "https://example.com"),
                "bar_tab" to mapOf("card" to "card_id_123")
            )
        )
        ParcelUtils.verifyParcelRoundtrip(apiParams)
    }

    private companion object {
        private const val AMOUNT = 1099L
    }
}
