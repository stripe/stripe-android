package com.stripe.android.model

import com.stripe.android.CardNumberFixtures.VISA_NO_SPACES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.json.JSONException

/**
 * Test class for [SourceParams].
 */
class SourceParamsTest {

    @Test
    fun createAlipayReusableParams_withAllFields_hasExpectedFields() {
        val params = SourceParams.createAlipayReusableParams(
            "usd",
            "Jean Valjean",
            "jdog@lesmis.net",
            RETURN_URL
        )

        assertEquals(Source.SourceType.ALIPAY, params.type)
        assertEquals(Source.Usage.REUSABLE, params.usage)
        assertNull(params.amount)
        assertEquals("usd", params.currency)
        assertEquals(RETURN_URL, params.returnUrl)

        val owner = requireNotNull(params.owner)
        assertEquals("Jean Valjean", owner.name)
        assertEquals("jdog@lesmis.net", owner.email)
    }

    @Test
    fun createAlipayReusableParams_withOnlyName_hasOnlyExpectedFields() {
        val params = SourceParams.createAlipayReusableParams(
            currency = "cad",
            name = "Hari Seldon",
            returnUrl = RETURN_URL
        )

        assertEquals(Source.SourceType.ALIPAY, params.type)
        assertEquals(Source.Usage.REUSABLE, params.usage)
        assertNull(params.amount)
        assertEquals("cad", params.currency)
        assertEquals(RETURN_URL, params.returnUrl)

        val owner = requireNotNull(params.owner)
        assertEquals("Hari Seldon", owner.name)
        assertNull(owner.email)
    }

    @Test
    fun createAlipaySingleUseParams_withAllFields_hasExpectedFields() {
        val params = SourceParams.createAlipaySingleUseParams(
            1000L,
            "aud",
            "Jane Tester",
            "jane@test.com",
            RETURN_URL)

        assertEquals(Source.SourceType.ALIPAY, params.type)
        assertEquals(1000L, params.amount)
        assertEquals("aud", params.currency)

        val owner = requireNotNull(params.owner)
        assertEquals("Jane Tester", owner.name)
        assertEquals("jane@test.com", owner.email)

        assertEquals(RETURN_URL, params.returnUrl)
    }

    @Test
    fun createAlipaySingleUseParams_withoutOwner_hasNoOwnerFields() {
        val params = SourceParams.createAlipaySingleUseParams(
            amount = 555L,
            currency = "eur",
            returnUrl = RETURN_URL
        )

        assertEquals(Source.SourceType.ALIPAY, params.type)
        assertEquals(555L, params.amount)
        assertEquals("eur", params.currency)

        assertEquals(SourceParams.OwnerParams(), params.owner)

        assertEquals(RETURN_URL, params.returnUrl)
    }

    @Test
    fun createBancontactParams_hasExpectedFields() {
        val params = SourceParams.createBancontactParams(
            1000L,
            "Stripe",
            "return/url/3000",
            "descriptor",
            "en")

        assertEquals(Source.SourceType.BANCONTACT, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals(1000L, params.amount)
        assertEquals("Stripe", params.owner?.name)
        assertEquals("return/url/3000", params.returnUrl)

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals("descriptor", apiMap["statement_descriptor"])
        assertEquals("en", apiMap["preferred_language"])
    }

    @Test
    fun createBancontactParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createBancontactParams(
            1000L,
            "Stripe",
            "return/url/3000",
            "descriptor",
            "en")

        val expectedMap = mapOf(
            "type" to Source.SourceType.BANCONTACT,
            "currency" to Source.EURO,
            "amount" to 1000L,
            "owner" to mapOf("name" to "Stripe"),
            "redirect" to mapOf("return_url" to "return/url/3000"),
            Source.SourceType.BANCONTACT to mapOf(
                "statement_descriptor" to "descriptor",
                "preferred_language" to "en"
            )
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createBancontactParams_hasExpectedFields_optionalStatementDescriptor() {
        val params = SourceParams.createBancontactParams(
            1000L,
            "Stripe",
            "return/url/3000", null,
            "en")

        val apiMap = params.apiParameterMap
        requireNotNull(apiMap)
        assertNull(apiMap["statement_descriptor"])
        assertEquals("en", apiMap["preferred_language"])
    }

    @Test
    fun createBancontactParams_hasExpectedFields_optionalPreferredLanguage() {
        val params = SourceParams.createBancontactParams(
            1000L,
            "Stripe",
            "return/url/3000",
            "descriptor", null)

        val apiMap = params.apiParameterMap
        requireNotNull(apiMap)
        assertEquals("descriptor", apiMap["statement_descriptor"])
        assertNull(apiMap["preferred_language"])
    }

    @Test
    fun createBancontactParams_hasExpectedFields_optionalEverything() {
        val params = SourceParams.createBancontactParams(
            1000L,
            "Stripe",
            "return/url/3000", null, null)

        assertNull(params.apiParameterMap)
    }

    @Test
    fun createCardParams_hasBothExpectedMaps() {
        val params = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD)

        val apiMap = params.apiParameterMap
        requireNotNull(apiMap)
        assertEquals(VISA_NO_SPACES, apiMap["number"])
        assertEquals(12, apiMap["exp_month"])
        assertEquals(2050, apiMap["exp_year"])
        assertEquals("123", apiMap["cvc"])

        val owner = requireNotNull(params.owner)
        assertEquals("Captain Cardholder", owner.name)
        assertNull(owner.email)
        assertNull(owner.phone)

        val addressMap = owner.address?.toParamMap().orEmpty()
        assertEquals("1 ABC Street", addressMap["line1"])
        assertEquals("Apt. 123", addressMap["line2"])
        assertEquals("San Francisco", addressMap["city"])
        assertEquals("CA", addressMap["state"])
        assertEquals("94107", addressMap["postal_code"])
        assertEquals("US", addressMap["country"])

        assertEquals(METADATA, params.metaData)
    }

    @Test
    fun createCardParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD)

        val totalExpectedMap = mapOf(
            "type" to "card",
            "card" to mapOf(
                "number" to VISA_NO_SPACES,
                "exp_month" to 12,
                "exp_year" to 2050,
                "cvc" to "123"
            ),
            "owner" to mapOf(
                "address" to mapOf(
                    "line1" to "1 ABC Street",
                    "line2" to "Apt. 123",
                    "city" to "San Francisco",
                    "state" to "CA",
                    "postal_code" to "94107",
                    "country" to "US"
                ),
                "name" to "Captain Cardholder"
            ),
            "metadata" to METADATA
        )

        assertEquals(totalExpectedMap, params.toParamMap())
    }

    @Test
    @Throws(JSONException::class)
    fun createCardParamsFromGooglePay_withNoBillingAddress() {
        val createdParams = SourceParams.createCardParamsFromGooglePay(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS)
        val expectedParams = SourceParams
            .createSourceFromTokenParams("tok_1F4ACMCRMbs6FrXf6fPqLnN7")
            .setOwner(SourceParams.OwnerParams())
        assertEquals(expectedParams, createdParams)
    }

    @Test
    @Throws(JSONException::class)
    fun createCardParamsFromGooglePay_withFullBillingAddress() {
        val createdParams = SourceParams.createCardParamsFromGooglePay(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS)

        val ownerParams = SourceParams.OwnerParams(
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

        val expectedParams = SourceParams.createSourceFromTokenParams("tok_1F4VSjBbvEcIpqUbSsbEtBap")
            .setOwner(ownerParams)

        assertEquals(expectedParams, createdParams)
    }

    @Test
    fun createEPSParams_hasExpectedFields() {
        val params = SourceParams.createEPSParams(
            150L,
            "Stripe",
            RETURN_URL,
            "stripe descriptor")

        assertEquals(Source.SourceType.EPS, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals("Stripe", params.owner?.name)
        assertEquals(RETURN_URL, params.returnUrl)

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals("stripe descriptor", apiMap["statement_descriptor"])
    }

    @Test
    fun createEPSParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createEPSParams(
            150L,
            "Stripe",
            RETURN_URL,
            "stripe descriptor")

        val expectedMap = mapOf(
            "type" to Source.SourceType.EPS,
            "currency" to Source.EURO,
            "amount" to 150L,
            "owner" to mapOf("name" to "Stripe"),
            "redirect" to mapOf("return_url" to RETURN_URL),
            Source.SourceType.EPS to mapOf("statement_descriptor" to "stripe descriptor")
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createEPSParams_toParamMap_createsExpectedMap_noStatementDescriptor() {
        val params = SourceParams.createEPSParams(
            150L,
            "Stripe",
            RETURN_URL, null)

        val expectedMap = mapOf(
            "type" to Source.SourceType.EPS,
            "currency" to Source.EURO,
            "amount" to 150L,
            "owner" to mapOf("name" to "Stripe"),
            "redirect" to mapOf("return_url" to RETURN_URL)
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createGiropayParams_hasExpectedFields() {
        val params = SourceParams.createGiropayParams(
            150L,
            "Stripe",
            RETURN_URL,
            "stripe descriptor")

        assertEquals(Source.SourceType.GIROPAY, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals(150L, params.amount)

        assertEquals("Stripe", params.owner?.name)

        assertEquals(RETURN_URL, params.returnUrl)

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals("stripe descriptor", apiMap["statement_descriptor"])
    }

    @Test
    fun createGiropayParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createGiropayParams(
            150L,
            "Stripe",
            RETURN_URL,
            "stripe descriptor")

        val expectedMap = mapOf(
            "type" to Source.SourceType.GIROPAY,
            "currency" to Source.EURO,
            "amount" to 150L,
            "owner" to mapOf(
                "name" to "Stripe"
            ),
            "redirect" to mapOf(
                "return_url" to RETURN_URL
            ),
            Source.SourceType.GIROPAY to mapOf(
                "statement_descriptor" to "stripe descriptor"
            )
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createGiropayParams_withNullStatementDescriptor_hasExpectedFieldsButNoApiParams() {
        val params = SourceParams.createGiropayParams(
            150L,
            "Stripe",
            RETURN_URL, null)

        assertEquals(Source.SourceType.GIROPAY, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals(150L, params.amount)

        assertEquals("Stripe", params.owner?.name)

        assertEquals(RETURN_URL, params.returnUrl)
        assertNull(params.apiParameterMap)
    }

    @Test
    fun createIdealParams_hasExpectedFields() {
        val params = SourceParams.createIdealParams(
            900L,
            "Default Name",
            RETURN_URL,
            "something you bought",
            "SVB"
        )
        assertEquals(Source.SourceType.IDEAL, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals(900L, params.amount)

        assertEquals("Default Name", params.owner?.name)

        assertEquals(RETURN_URL, params.returnUrl)

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals("something you bought", apiMap["statement_descriptor"])
        assertEquals("SVB", apiMap["bank"])
    }

    @Test
    fun createIdealParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createIdealParams(
            900L,
            "Default Name",
            RETURN_URL,
            "something you bought",
            "SVB")

        val expectedMap = mapOf(
            "type" to Source.SourceType.IDEAL,
            "currency" to Source.EURO,
            "amount" to 900L,
            "owner" to mapOf("name" to "Default Name"),
            "redirect" to mapOf("return_url" to RETURN_URL),
            Source.SourceType.IDEAL to mapOf(
                "statement_descriptor" to "something you bought",
                "bank" to "SVB"
            )
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createP24Params_withAllFields_hasExpectedFields() {
        val params = SourceParams.createP24Params(
            1000L,
            "eur",
            "Jane Tester",
            "jane@test.com",
            RETURN_URL)

        assertEquals(Source.SourceType.P24, params.type)
        assertEquals(1000L, params.amount)
        assertEquals("eur", params.currency)

        val owner = requireNotNull(params.owner)
        assertEquals("Jane Tester", owner.name)
        assertEquals("jane@test.com", owner.email)

        assertEquals(RETURN_URL,
            params.returnUrl)
    }

    @Test
    fun createP24Params_withNullName_hasExpectedFields() {
        val params = SourceParams.createP24Params(
            1000L,
            "eur", null,
            "jane@test.com",
            RETURN_URL)

        assertEquals(Source.SourceType.P24, params.type)
        assertEquals(1000L, params.amount)
        assertEquals("eur", params.currency)

        val owner = requireNotNull(params.owner)
        assertNull(owner.name)
        assertEquals("jane@test.com", owner.email)

        assertEquals(RETURN_URL,
            params.returnUrl)
    }

    @Test
    fun createMultibancoParams_hasExpectedFields() {
        val params = SourceParams.createMultibancoParams(
            150L,
            RETURN_URL,
            "multibancoholder@stripe.com")

        assertEquals(Source.SourceType.MULTIBANCO, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals(150L, params.amount)
        assertEquals(RETURN_URL,
            params.returnUrl)
        assertEquals("multibancoholder@stripe.com", params.owner?.email)
    }

    @Test
    fun createMultibancoParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createMultibancoParams(
            150L,
            RETURN_URL,
            "multibancoholder@stripe.com")

        val expectedMap = mapOf(
            "type" to Source.SourceType.MULTIBANCO,
            "currency" to Source.EURO,
            "amount" to 150L,
            "owner" to mapOf("email" to "multibancoholder@stripe.com"),
            "redirect" to mapOf("return_url" to RETURN_URL)
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createSepaDebitParams_hasExpectedFields() {
        val params = SourceParams.createSepaDebitParams(
            "Jai Testa",
            "ibaniban",
            "sepaholder@stripe.com",
            "44 Fourth Street",
            "Test City",
            "90210",
            "EI")

        assertEquals(Source.SourceType.SEPA_DEBIT, params.type)
        assertEquals(Source.EURO, params.currency)

        assertEquals("Jai Testa", params.owner?.name)

        val addressMap = params.owner?.address?.toParamMap().orEmpty()
        assertEquals("44 Fourth Street", addressMap["line1"])
        assertEquals("Test City", addressMap["city"])
        assertEquals("90210", addressMap["postal_code"])
        assertEquals("EI", addressMap["country"])

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals("ibaniban", apiMap["iban"])
    }

    @Test
    fun createSepaDebitParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createSepaDebitParams(
            "Jai Testa",
            "ibaniban",
            "sepaholder@stripe.com",
            "44 Fourth Street",
            "Test City",
            "90210",
            "EI")

        val expectedMap = mapOf(
            "type" to Source.SourceType.SEPA_DEBIT,
            "currency" to Source.EURO,
            "owner" to mapOf(
                "name" to "Jai Testa",
                "email" to "sepaholder@stripe.com",
                "address" to mapOf(
                    "line1" to "44 Fourth Street",
                    "city" to "Test City",
                    "postal_code" to "90210",
                    "country" to "EI"
                )
            ),
            Source.SourceType.SEPA_DEBIT to mapOf("iban" to "ibaniban")
        )

        assertEquals(Source.SourceType.SEPA_DEBIT, params.type)

        val actualMap = params.toParamMap()
        assertEquals(expectedMap, actualMap)
    }

    @Test
    fun createSofortParams_hasExpectedFields() {
        val params = SourceParams.createSofortParams(
            50000L,
            RETURN_URL,
            "UK",
            "a thing you bought"
        )

        assertEquals(Source.SourceType.SOFORT, params.type)
        assertEquals(Source.EURO, params.currency)
        assertEquals(50000L, params.amount)
        assertEquals(RETURN_URL, params.returnUrl)

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals("UK", apiMap["country"])
        assertEquals("a thing you bought", apiMap["statement_descriptor"])
    }

    @Test
    fun createSofortParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createSofortParams(
            50000L,
            RETURN_URL,
            "UK",
            "a thing you bought")

        val expectedMap = mapOf(
            "type" to Source.SourceType.SOFORT,
            "currency" to Source.EURO,
            "amount" to 50000L,
            "redirect" to mapOf("return_url" to RETURN_URL),
            Source.SourceType.SOFORT to mapOf(
                "country" to "UK",
                "statement_descriptor" to "a thing you bought"
            )
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createThreeDSecureParams_hasExpectedFields() {
        val params = SourceParams.createThreeDSecureParams(
            99000L,
            "brl",
            RETURN_URL,
            "card_id_123"
        )

        assertEquals(Source.SourceType.THREE_D_SECURE, params.type)
        // Brazilian Real
        assertEquals("brl", params.currency)
        assertEquals(99000L, params.amount)
        assertEquals(RETURN_URL, params.returnUrl)

        val apiMap = requireNotNull(params.apiParameterMap)
        assertEquals(1, apiMap.size)
        assertEquals("card_id_123", apiMap["card"])
    }

    @Test
    fun createThreeDSecureParams_toParamMap_createsExpectedMap() {
        val params = SourceParams.createThreeDSecureParams(
            99000L,
            "brl",
            RETURN_URL,
            "card_id_123")

        val expectedMap = mapOf(
            "type" to Source.SourceType.THREE_D_SECURE,
            "currency" to "brl",
            "amount" to 99000L,
            "redirect" to mapOf("return_url" to RETURN_URL),
            Source.SourceType.THREE_D_SECURE to mapOf("card" to "card_id_123")
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createCustomParamsWithSourceTypeParameters_toParamMap_createsExpectedMap() {
        // Using the Giropay constructor to add some free params and expected values,
        // including a source type params
        val dogecoin = "dogecoin"

        val dogecoinParams = mapOf("statement_descriptor" to "stripe descriptor")

        val params = SourceParams.createCustomParams(dogecoin)
            .setCurrency(Source.EURO)
            .setAmount(150L)
            .setReturnUrl(RETURN_URL)
            .setOwner(SourceParams.OwnerParams(name = "Stripe"))
            .setApiParameterMap(dogecoinParams)

        val expectedMap = mapOf(
            "type" to dogecoin,
            "currency" to Source.EURO,
            "amount" to 150L,
            "owner" to mapOf("name" to "Stripe"),
            "redirect" to mapOf("return_url" to RETURN_URL),
            dogecoin to mapOf("statement_descriptor" to "stripe descriptor")
        )

        assertEquals(expectedMap, params.toParamMap())
    }

    @Test
    fun createWeChatPayParams_shouldCreateExpectedParams() {
        val expectedWeChatParams = SourceParams.WeChatParams(
            "wxa0df51ec63e578ce",
            "WIDGET STORE"
        )

        val paramMap = SourceParams
            .createWeChatPayParams(
                150L,
                "USD",
                "wxa0df51ec63e578ce",
                "WIDGET STORE"
            )
            .toParamMap()
        assertEquals(expectedWeChatParams.toParamMap(), paramMap["wechat"])
    }

    @Test
    fun setCustomType_forEmptyParams_setsTypeToUnknown() {
        val params = SourceParams.createCustomParams("dogecoin")
        assertEquals(Source.SourceType.UNKNOWN, params.type)
        assertEquals("dogecoin", params.typeRaw)
    }

    @Test
    fun createCustomParams_withCustomType() {
        val apiParamMap = mapOf("card" to "card_id_123")

        val sourceParams = SourceParams.createCustomParams("bar_tab")
            .setAmount(99000L)
            .setCurrency("brl")
            .setReturnUrl(RETURN_URL)
            .setApiParameterMap(apiParamMap)
        assertEquals(Source.SourceType.UNKNOWN, sourceParams.type)
        assertEquals("bar_tab", sourceParams.typeRaw)
    }

    private companion object {

        private val METADATA = mapOf(
            "color" to "blue",
            "animal" to "dog"
        )

        private const val RETURN_URL = "stripe://return"

        private val FULL_FIELDS_VISA_CARD =
            Card.Builder(VISA_NO_SPACES, 12, 2050, "123")
                .name("Captain Cardholder")
                .addressLine1("1 ABC Street")
                .addressLine2("Apt. 123")
                .addressCity("San Francisco")
                .addressState("CA")
                .addressZip("94107")
                .addressCountry("US")
                .currency("usd")
                .metadata(METADATA)
                .build()
    }
}
