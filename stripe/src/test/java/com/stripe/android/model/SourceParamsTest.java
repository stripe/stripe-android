package com.stripe.android.model;

import android.support.annotation.NonNull;

import com.stripe.android.testharness.JsonTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.view.CardInputTestActivity.VALID_VISA_NO_SPACES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link SourceParams}.
 */
@RunWith(RobolectricTestRunner.class)
public class SourceParamsTest {

    private static final Card FULL_FIELDS_VISA_CARD;

    static {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("color", "blue");
        metadata.put("animal", "dog");

        FULL_FIELDS_VISA_CARD = new Card(VALID_VISA_NO_SPACES,
                12,
                2050,
                "123",
                "Captain Cardholder",
                "1 ABC Street",
                "Apt. 123",
                "San Francisco",
                "CA",
                "94107",
                "US",
                "usd",
                metadata);
    }

    @Test
    public void createAlipayReusableParams_withAllFields_hasExpectedFields() {
        final SourceParams params = SourceParams.createAlipayReusableParams(
                "usd",
                "Jean Valjean",
                "jdog@lesmis.net",
                "stripe://start");

        assertEquals(Source.ALIPAY, params.getType());
        assertEquals(Source.REUSABLE, params.getUsage());
        assertNull(params.getAmount());
        assertEquals("usd", params.getCurrency());
        assertNotNull(params.getRedirect());
        assertEquals("stripe://start", params.getRedirect().get("return_url"));

        assertNotNull(params.getOwner());
        assertEquals("Jean Valjean", params.getOwner().get("name"));
        assertEquals("jdog@lesmis.net", params.getOwner().get("email"));
    }

    @Test
    public void createAlipayReusableParams_withOnlyName_hasOnlyExpectedFields() {
        final SourceParams params = SourceParams.createAlipayReusableParams(
                "cad",
                "Hari Seldon",
                null,
                "stripe://start");

        assertEquals(Source.ALIPAY, params.getType());
        assertEquals(Source.REUSABLE, params.getUsage());
        assertNull(params.getAmount());
        assertEquals("cad", params.getCurrency());
        assertNotNull(params.getRedirect());
        assertEquals("stripe://start", params.getRedirect().get("return_url"));

        assertNotNull(params.getOwner());
        assertEquals("Hari Seldon", params.getOwner().get("name"));
        assertFalse(params.getOwner().containsKey("email"));
    }

    @Test
    public void createAlipaySingleUseParams_withAllFields_hasExpectedFields() {
        final SourceParams params = SourceParams.createAlipaySingleUseParams(
                1000L,
                "aud",
                "Jane Tester",
                "jane@test.com",
                "stripe://testactivity");

        assertEquals(Source.ALIPAY, params.getType());
        assertNotNull(params.getAmount());
        assertEquals(1000L, params.getAmount().longValue());
        assertEquals("aud", params.getCurrency());
        assertNotNull(params.getOwner());
        assertEquals("Jane Tester", params.getOwner().get("name"));
        assertEquals("jane@test.com", params.getOwner().get("email"));
        assertNotNull(params.getRedirect());
        assertEquals("stripe://testactivity", params.getRedirect().get("return_url"));
    }

    @Test
    public void createAlipaySingleUseParams_withoutOwner_hasNoOwnerFields() {
        final SourceParams params = SourceParams.createAlipaySingleUseParams(
                555L,
                "eur",
                null,
                null,
                "stripe://testactivity2");

        assertEquals(Source.ALIPAY, params.getType());
        assertNotNull(params.getAmount());
        assertEquals(555L, params.getAmount().longValue());
        assertEquals("eur", params.getCurrency());

        assertNull(params.getOwner());

        assertNotNull(params.getRedirect());
        assertEquals("stripe://testactivity2", params.getRedirect().get("return_url"));
    }

    @Test
    public void createBancontactParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createBancontactParams(
                1000L,
                "Stripe",
                "return/url/3000",
                "descriptor",
                "en");

        assertEquals(Source.BANCONTACT, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertNotNull(params.getAmount());
        assertEquals(1000L, params.getAmount().longValue());
        assertNotNull(params.getOwner());
        assertEquals("Stripe", params.getOwner().get("name"));
        assertNotNull(params.getRedirect());
        assertEquals("return/url/3000", params.getRedirect().get("return_url"));

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertNotNull(apiMap);
        assertEquals("descriptor", apiMap.get("statement_descriptor"));
        assertEquals("en", apiMap.get("preferred_language"));
    }

    @Test
    public void createBancontactParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createBancontactParams(
                1000L,
                "Stripe",
                "return/url/3000",
                "descriptor",
                "en");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.BANCONTACT);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 1000L);
        expectedMap.put("owner", new HashMap<String, Object>() {{ put("name", "Stripe"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "return/url/3000"); }});
        expectedMap.put(Source.BANCONTACT,
                new HashMap<String, Object>() {{
                    put("statement_descriptor", "descriptor");
                    put("preferred_language", "en");
        }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createBancontactParams_hasExpectedFields_optionalStatementDescriptor() {
        final SourceParams params = SourceParams.createBancontactParams(
                1000L,
                "Stripe",
                "return/url/3000",
                null,
                "en");

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertNotNull(apiMap);
        assertNull(apiMap.get("statement_descriptor"));
        assertEquals("en", apiMap.get("preferred_language"));
    }

    @Test
    public void createBancontactParams_hasExpectedFields_optionalPreferredLanguage() {
        final SourceParams params = SourceParams.createBancontactParams(
                1000L,
                "Stripe",
                "return/url/3000",
                "descriptor",
                null);

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertNotNull(apiMap);
        assertEquals("descriptor", apiMap.get("statement_descriptor"));
        assertNull(apiMap.get("preferred_language"));
    }

    @Test
    public void createBancontactParams_hasExpectedFields_optionalEverything() {
        final SourceParams params = SourceParams.createBancontactParams(
                1000L,
                "Stripe",
                "return/url/3000",
                null,
                null);

        assertNull(params.getApiParameterMap());
    }

    @Test
    public void createCardParams_hasBothExpectedMaps() {
        final SourceParams params = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD);

        final Map<String, Object> apiMap = params.getApiParameterMap();
        assertNotNull(apiMap);
        assertEquals(VALID_VISA_NO_SPACES, apiMap.get("number"));
        assertEquals(12, apiMap.get("exp_month"));
        assertEquals(2050, apiMap.get("exp_year"));
        assertEquals("123", apiMap.get("cvc"));

        assertNotNull(params.getOwner());
        assertEquals("Captain Cardholder", params.getOwner().get("name"));
        assertEquals(2, params.getOwner().size());

        final Map<String, Object> addressMap = getMapFromOwner(params, "address");
        assertEquals("1 ABC Street", addressMap.get("line1"));
        assertEquals("Apt. 123", addressMap.get("line2"));
        assertEquals("San Francisco", addressMap.get("city"));
        assertEquals("CA", addressMap.get("state"));
        assertEquals("94107", addressMap.get("postal_code"));
        assertEquals("US", addressMap.get("country"));

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("color", "blue");
        metadata.put("animal", "dog");
        assertEquals(metadata, params.getMetaData());
    }

    @Test
    public void createCardParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createCardParams(FULL_FIELDS_VISA_CARD);

        final Map<String, Object> expectedCardMap = new HashMap<>();
        expectedCardMap.put("number", VALID_VISA_NO_SPACES);
        expectedCardMap.put("exp_month", 12);
        expectedCardMap.put("exp_year", 2050);
        expectedCardMap.put("cvc", "123");

        final Map<String, Object> expectedAddressMap = new HashMap<>();
        expectedAddressMap.put("line1", "1 ABC Street");
        expectedAddressMap.put("line2", "Apt. 123");
        expectedAddressMap.put("city", "San Francisco");
        expectedAddressMap.put("state", "CA");
        expectedAddressMap.put("postal_code", "94107");
        expectedAddressMap.put("country", "US");

        final Map<String, Object> totalExpectedMap = new HashMap<>();
        totalExpectedMap.put("type", "card");
        totalExpectedMap.put("card", expectedCardMap);
        totalExpectedMap.put("owner",
                new HashMap<String, Object>() {{
                    put("address", expectedAddressMap);
                    put("name", "Captain Cardholder");
                }});

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("color", "blue");
        metadata.put("animal", "dog");
        totalExpectedMap.put("metadata", metadata);

        JsonTestUtils.assertMapEquals(totalExpectedMap, params.toParamMap());
    }

    @Test
    public void createEPSParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createEPSParams(
                150L,
                "Stripe",
                "stripe://return",
                "stripe descriptor");

        assertEquals(Source.EPS, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertEquals("Stripe", params.getOwner().get("name"));
        assertEquals("stripe://return", params.getRedirect().get("return_url"));

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertEquals("stripe descriptor", apiMap.get("statement_descriptor"));
    }

    @Test
    public void createEPSParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createEPSParams(
                150L,
                "Stripe",
                "stripe://return",
                "stripe descriptor");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.EPS);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 150L);
        expectedMap.put("owner", new HashMap<String, Object>() {{ put("name", "Stripe"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://return"); }});
        expectedMap.put(Source.EPS,
                new HashMap<String, Object>() {{
                    put("statement_descriptor", "stripe descriptor");
                }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createEPSParams_toParamMap_createsExpectedMap_noStatementDescriptor() {
        final SourceParams params = SourceParams.createEPSParams(
                150L,
                "Stripe",
                "stripe://return",
                null);

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.EPS);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 150L);
        expectedMap.put("owner", new HashMap<String, Object>() {{ put("name", "Stripe"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://return"); }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createGiropayParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createGiropayParams(
                150L,
                "Stripe",
                "stripe://return",
                "stripe descriptor");

        assertEquals(Source.GIROPAY, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertNotNull(params.getAmount());
        assertEquals(150L, params.getAmount().longValue());
        assertNotNull(params.getOwner());
        assertEquals("Stripe", params.getOwner().get("name"));
        assertNotNull(params.getRedirect());
        assertEquals("stripe://return", params.getRedirect().get("return_url"));

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertEquals("stripe descriptor", apiMap.get("statement_descriptor"));
    }

    @Test
    public void createGiropayParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createGiropayParams(
                150L,
                "Stripe",
                "stripe://return",
                "stripe descriptor");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.GIROPAY);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 150L);
        expectedMap.put("owner", new HashMap<String, Object>() {{ put("name", "Stripe"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://return"); }});
        expectedMap.put(Source.GIROPAY,
                new HashMap<String, Object>() {{
                    put("statement_descriptor", "stripe descriptor");
                }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createGiropayParams_withNullStatementDescriptor_hasExpectedFieldsButNoApiParams() {
        final SourceParams params = SourceParams.createGiropayParams(
                150L,
                "Stripe",
                "stripe://return",
                null);

        assertEquals(Source.GIROPAY, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertNotNull(params.getAmount());
        assertEquals(150L, params.getAmount().longValue());
        assertNotNull(params.getOwner());
        assertEquals("Stripe", params.getOwner().get("name"));
        assertNotNull(params.getRedirect());
        assertEquals("stripe://return", params.getRedirect().get("return_url"));
        assertNull(params.getApiParameterMap());
    }

    @Test
    public void createIdealParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createIdealParams(
                900L,
                "Default Name",
                "stripe://anotherurl",
                "something you bought",
                "SVB");
        assertEquals(Source.IDEAL, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertNotNull(params.getAmount());
        assertEquals(900L, params.getAmount().longValue());
        assertNotNull(params.getOwner());
        assertEquals("Default Name", params.getOwner().get("name"));
        assertNotNull(params.getRedirect());
        assertEquals("stripe://anotherurl", params.getRedirect().get("return_url"));
        Map<String, Object> apiMap = params.getApiParameterMap();
        assertEquals("something you bought", apiMap.get("statement_descriptor"));
        assertEquals("SVB", apiMap.get("bank"));
    }

    @Test
    public void createIdealParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createIdealParams(
                900L,
                "Default Name",
                "stripe://anotherurl",
                "something you bought",
                "SVB");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.IDEAL);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 900L);
        expectedMap.put("owner",
                new HashMap<String, Object>() {{ put("name", "Default Name"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://anotherurl"); }});
        expectedMap.put(Source.IDEAL,
                new HashMap<String, Object>() {{
                    put("statement_descriptor", "something you bought");
                    put("bank", "SVB");
                }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createP24Params_withAllFields_hasExpectedFields() {
        final SourceParams params = SourceParams.createP24Params(
                1000L,
                "eur",
                "Jane Tester",
                "jane@test.com",
                "stripe://testactivity");

        assertEquals(Source.P24, params.getType());
        assertNotNull(params.getAmount());
        assertEquals(1000L, params.getAmount().longValue());
        assertEquals("eur", params.getCurrency());
        assertNotNull(params.getOwner());
        assertEquals("Jane Tester", params.getOwner().get("name"));
        assertEquals("jane@test.com", params.getOwner().get("email"));
        assertNotNull(params.getRedirect());
        assertEquals("stripe://testactivity", params.getRedirect().get("return_url"));
    }

    @Test
    public void createP24Params_withNullName_hasExpectedFields() {
        final SourceParams params = SourceParams.createP24Params(
                1000L,
                "eur",
                null,
                "jane@test.com",
                "stripe://testactivity");

        assertEquals(Source.P24, params.getType());
        assertNotNull(params.getAmount());
        assertEquals(1000L, params.getAmount().longValue());
        assertEquals("eur", params.getCurrency());
        assertNotNull(params.getOwner());
        assertNull(params.getOwner().get("name"));
        assertEquals("jane@test.com", params.getOwner().get("email"));
        assertNotNull(params.getRedirect());
        assertEquals("stripe://testactivity", params.getRedirect().get("return_url"));
    }

    @Test
    public void createMultibancoParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createMultibancoParams(
                150L,
                "stripe://testactivity",
                "multibancoholder@stripe.com");

        assertEquals(Source.MULTIBANCO, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertEquals(150L, params.getAmount().longValue());
        assertEquals("stripe://testactivity", params.getRedirect().get("return_url"));
        assertEquals("multibancoholder@stripe.com", params.getOwner().get("email"));
    }

    @Test
    public void createMultibancoParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createMultibancoParams(
                150L,
                "stripe://testactivity",
                "multibancoholder@stripe.com");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.MULTIBANCO);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 150L);
        expectedMap.put("owner",
                new HashMap<String, Object>() {{ put("email", "multibancoholder@stripe.com"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://testactivity"); }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createSepaDebitParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createSepaDebitParams(
                "Jai Testa",
                "ibaniban",
                "sepaholder@stripe.com",
                "44 Fourth Street",
                "Test City",
                "90210",
                "EI");

        assertEquals(Source.SEPA_DEBIT, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertNotNull(params.getOwner());
        assertEquals("Jai Testa", params.getOwner().get("name"));
        Map<String, Object> addressMap = getMapFromOwner(params, "address");
        assertEquals("44 Fourth Street", addressMap.get("line1"));
        assertEquals("Test City", addressMap.get("city"));
        assertEquals("90210", addressMap.get("postal_code"));
        assertEquals("EI", addressMap.get("country"));

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertEquals("ibaniban", apiMap.get("iban"));
    }

    @Test
    public void createSepaDebitParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createSepaDebitParams(
                "Jai Testa",
                "ibaniban",
                "sepaholder@stripe.com",
                "44 Fourth Street",
                "Test City",
                "90210",
                "EI");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.SEPA_DEBIT);
        expectedMap.put("currency", Source.EURO);

        final Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("line1", "44 Fourth Street");
        addressMap.put("city", "Test City");
        addressMap.put("postal_code", "90210");
        addressMap.put("country", "EI");

        expectedMap.put("owner",
                new HashMap<String, Object>() {{
                    put("name", "Jai Testa");
                    put("email", "sepaholder@stripe.com");
                    put("address", addressMap);
                }});

        expectedMap.put(Source.SEPA_DEBIT,
                new HashMap<String, Object>() {{ put("iban", "ibaniban"); }});
                assertEquals(Source.SEPA_DEBIT, params.getType());

        Map<String, Object> actualMap = params.toParamMap();
        JsonTestUtils.assertMapEquals(expectedMap, actualMap);
    }

    @Test
    public void createSofortParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createSofortParams(
                50000L,
                "example://return",
                "UK",
                "a thing you bought");

        assertEquals(Source.SOFORT, params.getType());
        assertEquals(Source.EURO, params.getCurrency());
        assertNotNull(params.getAmount());
        assertEquals(50000L, params.getAmount().longValue());
        assertNotNull(params.getRedirect());
        assertEquals("example://return", params.getRedirect().get("return_url"));
        Map<String, Object> apiMap = params.getApiParameterMap();
        assertEquals("UK", apiMap.get("country"));
        assertEquals("a thing you bought", apiMap.get("statement_descriptor"));
    }

    @Test
    public void createSofortParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createSofortParams(
                50000L,
                "example://return",
                "UK",
                "a thing you bought");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.SOFORT);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 50000L);
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "example://return"); }});
        expectedMap.put(Source.SOFORT,
                new HashMap<String, Object>() {{
                    put("country", "UK");
                    put("statement_descriptor", "a thing you bought");
                }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createThreeDSecureParams_hasExpectedFields() {
        final SourceParams params = SourceParams.createThreeDSecureParams(
                99000L,
                "brl",
                "stripe://returnaddress",
                "card_id_123");

        assertEquals(Source.THREE_D_SECURE, params.getType());
        // Brazilian Real
        assertEquals("brl", params.getCurrency());
        assertNotNull(params.getAmount());
        assertEquals(99000L, params.getAmount().longValue());
        assertNotNull(params.getRedirect());
        assertEquals("stripe://returnaddress", params.getRedirect().get("return_url"));

        Map<String, Object> apiMap = params.getApiParameterMap();
        assertNotNull(apiMap);
        assertEquals(1, apiMap.size());
        assertEquals("card_id_123", apiMap.get("card"));
    }

    @Test
    public void createThreeDSecureParams_toParamMap_createsExpectedMap() {
        final SourceParams params = SourceParams.createThreeDSecureParams(
                99000L,
                "brl",
                "stripe://returnaddress",
                "card_id_123");

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", Source.THREE_D_SECURE);
        expectedMap.put("currency", "brl");
        expectedMap.put("amount", 99000L);
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://returnaddress"); }});
        expectedMap.put(Source.THREE_D_SECURE,
                new HashMap<String, Object>() {{ put("card", "card_id_123"); }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void createCustomParamsWithSourceTypeParameters_toParamMap_createsExpectedMap() {
        // Using the Giropay constructor to add some free params and expected values,
        // including a source type params
        final String DOGECOIN = "dogecoin";
        final SourceParams params = SourceParams.createGiropayParams(
                150L,
                "Stripe",
                "stripe://return",
                "stripe descriptor");
        params.setTypeRaw(DOGECOIN);

        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("type", DOGECOIN);
        expectedMap.put("currency", Source.EURO);
        expectedMap.put("amount", 150L);
        expectedMap.put("owner", new HashMap<String, Object>() {{ put("name", "Stripe"); }});
        expectedMap.put("redirect",
                new HashMap<String, Object>() {{ put("return_url", "stripe://return"); }});
        expectedMap.put(DOGECOIN,
                new HashMap<String, Object>() {{
                    put("statement_descriptor", "stripe descriptor");
                }});

        JsonTestUtils.assertMapEquals(expectedMap, params.toParamMap());
    }

    @Test
    public void setCustomType_forEmptyParams_setsTypeToUnknown() {
        final SourceParams params = SourceParams.createCustomParams();
        params.setTypeRaw("dogecoin");
        assertEquals(Source.UNKNOWN, params.getType());
        assertEquals("dogecoin", params.getTypeRaw());
    }

    @Test
    public void setCustomType_forStandardParams_overridesStandardType() {
        final SourceParams params = SourceParams.createThreeDSecureParams(
                99000L,
                "brl",
                "stripe://returnaddress",
                "card_id_123");
        params.setTypeRaw("bar_tab");
        assertEquals(Source.UNKNOWN, params.getType());
        assertEquals("bar_tab", params.getTypeRaw());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromOwner(
            @NonNull SourceParams params,
            @NonNull String mapName) {
        assertNotNull(params.getOwner());
        assertTrue(params.getOwner() instanceof Map);
        return (Map<String, Object>) params.getOwner().get(mapName);
    }
}
