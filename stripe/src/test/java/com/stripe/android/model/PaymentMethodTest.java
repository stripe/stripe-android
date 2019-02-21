package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodTest {

    private static final String RAW_SEPA_DEBIT_JSON = "{\n" +
            "\t\"id\": \"pm_123456789\",\n" +
            "\t\"created\": 1550757934255,\n" +
            "\t\"customer\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "\t\"livemode\": true,\n" +
            "\t\"type\": \"sepa_debit\",\n" +
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
            "\t\"sepa_debit\": {\n" +
            "\t\t\"bank_code\": \"sepa bank\",\n" +
            "\t\t\"country\": \"USA\",\n" +
            "\t\t\"last4\": \"6543\"\n" +
            "\t}\n" +
            "}";

    private static final String RAW_CARD_JSON = "{\n" +
            "\t\"id\": \"pm_123456789\",\n" +
            "\t\"created\": 1550757934255,\n" +
            "\t\"customer\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "\t\"livemode\": true,\n" +
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
            "\t\t\"name\": \"J Q Public\",\n" +
            "\t\t\"address_city\": \"San Francisco\",\n" +
            "\t\t\"address_country\": \"US\",\n" +
            "\t\t\"address_line1\": \"123 Main Street\",\n" +
            "\t\t\"address_line2\": \"906\",\n" +
            "\t\t\"address_state\": \"CA\",\n" +
            "\t\t\"address_zip\": \"94107\",\n" +
            "\t\t\"brand\": \"Visa\",\n" +
            "\t\t\"currency\": \"USD\",\n" +
            "\t\t\"exp_month\": 8,\n" +
            "\t\t\"exp_year\": 2019,\n" +
            "\t\t\"last4\": \"4242\",\n" +
            "\t\t\"object\": \"card\"\n" +
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

    private static final PaymentMethod CARD_PAYMENT_METHOD = new PaymentMethod.Builder()
            .setId("pm_123456789")
            .setCreated(1550757934255L)
            .setLiveMode(true)
            .setType("card")
            .setCustomerId("cus_AQsHpvKfKwJDrF")
            .setBillingDetails(BILLING_DETAILS)
            .setCard(CardFixtures.CARD)
            .build();

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
    public void toJson_withSepaDebit_shouldReturnExpectedJson() throws JSONException {
        final JSONObject paymentMethod = new PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("sepa_debit")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(BILLING_DETAILS)
                .setSepaDebit(new SourceSepaDebitData.Builder()
                        .setBankCode("sepa bank")
                        .setCountry("USA")
                        .setLast4("6543")
                        .build())
                .build()
                .toJson();

        assertEquals(new JSONObject(RAW_SEPA_DEBIT_JSON).toString(), paymentMethod.toString());
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
                .setCard(CardFixtures.CARD)
                .build());
    }
}
