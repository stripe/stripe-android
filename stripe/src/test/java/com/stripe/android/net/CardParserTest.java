package com.stripe.android.net;

import com.stripe.android.model.Card;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test class for {@link CardParser}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class CardParserTest {

    private static final String JSON_CARD = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": null,\n" +
            "    \"address_country\": null,\n" +
            "    \"address_line1\": null,\n" +
            "    \"address_line1_check\": null,\n" +
            "    \"address_line2\": null,\n" +
            "    \"address_state\": null,\n" +
            "    \"address_zip\": null,\n" +
            "    \"address_zip_check\": null,\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"cvc_check\": null,\n" +
            "    \"dynamic_last4\": null,\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"metadata\": {\n" +
            "    },\n" +
            "    \"name\": null,\n" +
            "    \"tokenization_method\": null\n" +
            "  }";

    private static final String JSON_NO_EXP_MONTH = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": null,\n" +
            "    \"address_country\": null,\n" +
            "    \"address_line1\": null,\n" +
            "    \"address_line1_check\": null,\n" +
            "    \"address_line2\": null,\n" +
            "    \"address_state\": null,\n" +
            "    \"address_zip\": null,\n" +
            "    \"address_zip_check\": null,\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"cvc_check\": null,\n" +
            "    \"dynamic_last4\": null,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"metadata\": {\n" +
            "    },\n" +
            "    \"name\": null,\n" +
            "    \"tokenization_method\": null\n" +
            "  }";

    private static final String JSON_NO_EXP_YEAR = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": null,\n" +
            "    \"address_country\": null,\n" +
            "    \"address_line1\": null,\n" +
            "    \"address_line1_check\": null,\n" +
            "    \"address_line2\": null,\n" +
            "    \"address_state\": null,\n" +
            "    \"address_zip\": null,\n" +
            "    \"address_zip_check\": null,\n" +
            "    \"brand\": \"Visa\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"cvc_check\": null,\n" +
            "    \"dynamic_last4\": null,\n" +
            "    \"exp_month\": 8,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"last4\": \"4242\",\n" +
            "    \"metadata\": {\n" +
            "    },\n" +
            "    \"name\": null,\n" +
            "    \"tokenization_method\": null\n" +
            "  }";

    private static final String BAD_JSON = "{ \"id\": ";

    @Test
    public void parseSampleCard_returnsExpectedValue() {
        Card expectedCard = new Card(
                null,
                8,
                2017,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Card.VISA,
                "4242",
                null,
                Card.FUNDING_CREDIT,
                "US",
                null);
        try {
            Card cardFromJson = CardParser.parseCard(JSON_CARD);
            assertEquals(expectedCard.getBrand(), cardFromJson.getBrand());
            assertEquals(expectedCard.getFunding(), cardFromJson.getFunding());
            assertEquals(expectedCard.getCountry(), cardFromJson.getCountry());
            assertEquals(expectedCard.getLast4(), cardFromJson.getLast4());
            assertEquals(expectedCard.getExpMonth(), cardFromJson.getExpMonth());
            assertEquals(expectedCard.getExpYear(), cardFromJson.getExpYear());
            assertNull(cardFromJson.getAddressCity());
            assertNull(cardFromJson.getFingerprint());
        } catch (JSONException jex) {
            fail();
        }
    }

    @Test(expected = JSONException.class)
    public void parseCard_withBadJson_throwsJsonException() throws JSONException {
        CardParser.parseCard(BAD_JSON);
        fail("Expected an exception.");
    }

    @Test(expected = JSONException.class)
    public void parseCard_withNoExpirationMonth_throwsJsonException() throws JSONException {
        CardParser.parseCard(JSON_NO_EXP_MONTH);
        fail("Expected an exception.");
    }

    @Test(expected = JSONException.class)
    public void parseCard_withNoExpirationYear_throwsJsonException() throws JSONException {
        CardParser.parseCard(JSON_NO_EXP_YEAR);
        fail("Expected an exception.");
    }
}
