package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PaymentMethodTest {
    public static final String RAW_CARD_JSON = "{\n" +
            "\t\"id\": \"pm_123456789\",\n" +
            "\t\"created\": 1550757934255,\n" +
            "\t\"customer\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "\t\"livemode\": true,\n" +
            "\t\"metadata\": {\n" +
            "\t\t\"order_id\": \"123456789\"\n" +
            "\t}," +
            "\t\"type\": \"card\",\n" +
            "\t\"billing_details\": {\n" +
            "\t\t\"address\": {\n" +
            "\t\t\t\"city\": \"San Francisco\",\n" +
            "\t\t\t\"country\": \"USA\",\n" +
            "\t\t\t\"line1\": \"510 Townsend St\",\n" +
            "\t\t\t\"postal_code\": \"94103\",\n" +
            "\t\t\t\"state\": \"CA\"\n" +
            "\t\t},\n" +
            "\t\t\"email\": \"patrick@example.com\",\n" +
            "\t\t\"name\": \"Patrick\",\n" +
            "\t\t\"phone\": \"123-456-7890\"\n" +
            "\t},\n" +
            "\t\"card\": {\n" +
            "\t\t\"brand\": \"visa\",\n" +
            "\t\t\"checks\": {\n" +
            "\t\t\t\"address_line1_check\": \"unchecked\",\n" +
            "\t\t\t\"cvc_check\": \"unchecked\"\n" +
            "\t\t},\n" +
            "\t\t\"country\": \"US\",\n" +
            "\t\t\"exp_month\": 8,\n" +
            "\t\t\"exp_year\": 2022,\n" +
            "\t\t\"funding\": \"credit\",\n" +
            "\t\t\"last4\": \"4242\",\n" +
            "\t\t\"three_d_secure_usage\": {\n" +
            "\t\t\t\"supported\": true\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    private static final String RAW_IDEAL_JSON = "{\n" +
            "\t\"id\": \"pm_123456789\",\n" +
            "\t\"created\": 1550757934255,\n" +
            "\t\"customer\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "\t\"livemode\": true,\n" +
            "\t\"type\": \"ideal\",\n" +
            "\t\"billing_details\": {\n" +
            "\t\t\"address\": {\n" +
            "\t\t\t\"city\": \"San Francisco\",\n" +
            "\t\t\t\"country\": \"USA\",\n" +
            "\t\t\t\"line1\": \"510 Townsend St\",\n" +
            "\t\t\t\"postal_code\": \"94103\",\n" +
            "\t\t\t\"state\": \"CA\"\n" +
            "\t\t},\n" +
            "\t\t\"email\": \"patrick@example.com\",\n" +
            "\t\t\"name\": \"Patrick\",\n" +
            "\t\t\"phone\": \"123-456-7890\"\n" +
            "\t},\n" +
            "\t\"ideal\": {\n" +
            "\t\t\"bank\": \"my bank\",\n" +
            "\t\t\"bic\": \"bank id\"\n" +
            "\t}\n" +
            "}";

    private static final PaymentMethod.Card CARD = new PaymentMethod.Card.Builder()
            .setBrand(PaymentMethod.Card.VISA)
            .setChecks(new PaymentMethod.Card.Checks.Builder()
                    .setAddressLine1Check("unchecked")
                    .setAddressPostalCodeCheck(null)
                    .setCvcCheck("unchecked")
                    .build())
            .setCountry("US")
            .setExpiryMonth(8)
            .setExpiryYear(2022)
            .setFunding("credit")
            .setLast4("4242")
            .setThreeDSecureUsage(new PaymentMethod.Card.ThreeDSecureUsage.Builder()
                    .setSupported(true)
                    .build())
            .setWallet(null)
            .build();

    private static final PaymentMethod.BillingDetails BILLING_DETAILS =
            new PaymentMethod.BillingDetails.Builder()
                    .setAddress(new Address.Builder()
                            .setLine1("510 Townsend St")
                            .setCity("San Francisco")
                            .setState("CA")
                            .setPostalCode("94103")
                            .setCountry("USA")
                            .build())
                    .setEmail("patrick@example.com")
                    .setName("Patrick")
                    .setPhone("123-456-7890")
                    .build();

    private static final PaymentMethod CARD_PAYMENT_METHOD;

    static {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", "123456789");

        CARD_PAYMENT_METHOD = new PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("card")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(BILLING_DETAILS)
                .setCard(CARD)
                .setMetadata(metadata)
                .build();
    }

    @Test
    public void toJson_withIdeal_shouldReturnExpectedJson() throws JSONException {
        final JSONObject paymentMethod = new PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("ideal")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(BILLING_DETAILS)
                .setIdeal(new PaymentMethod.Ideal.Builder()
                        .setBank("my bank")
                        .setBankIdentifierCode("bank id")
                        .build())
                .build()
                .toJson();

        assertEquals(new JSONObject(RAW_IDEAL_JSON).toString(), paymentMethod.toString());
    }

    @Test
    public void toJson_withCard_shouldReturnExpectedJson() throws JSONException {
        assertEquals(new JSONObject(RAW_CARD_JSON).toString(),
                CARD_PAYMENT_METHOD.toJson().toString());
    }


    @Test
    public void equals_withEqualPaymentMethods_shouldReturnTrue() {
        assertEquals(CARD_PAYMENT_METHOD, new PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("card")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(BILLING_DETAILS)
                .setCard(CARD)
                .build());
    }

    @Test
    public void fromString_shouldReturnExpectedPaymentMethod() {
        assertEquals(CARD_PAYMENT_METHOD, PaymentMethod.fromString(RAW_CARD_JSON));
    }
}
