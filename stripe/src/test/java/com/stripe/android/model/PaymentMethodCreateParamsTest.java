package com.stripe.android.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaymentMethodCreateParamsTest {

    private static final String GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS = "{\n" +
            "\t\"apiVersionMinor\": 0,\n" +
            "\t\"apiVersion\": 2,\n" +
            "\t\"paymentMethodData\": {\n" +
            "\t\t\"description\": \"Visa •••• 1234\",\n" +
            "\t\t\"tokenizationData\": {\n" +
            "\t\t\t\"type\": \"PAYMENT_GATEWAY\",\n" +
            "\t\t\t\"token\": \"{\\n  \\\"id\\\": \\\"tok_1F4AzK\\\",\\n  \\\"object\\\": \\\"token\\\",\\n  \\\"card\\\": {\\n    \\\"id\\\": \\\"card_1F4AzKCRMbs6FrXf1nX87nde\\\",\\n    \\\"object\\\": \\\"card\\\",\\n    \\\"address_city\\\": null,\\n    \\\"address_country\\\": null,\\n    \\\"address_line1\\\": null,\\n    \\\"address_line1_check\\\": null,\\n    \\\"address_line2\\\": null,\\n    \\\"address_state\\\": null,\\n    \\\"address_zip\\\": null,\\n    \\\"address_zip_check\\\": null,\\n    \\\"brand\\\": \\\"Visa\\\",\\n    \\\"country\\\": \\\"US\\\",\\n    \\\"cvc_check\\\": null,\\n    \\\"dynamic_last4\\\": \\\"4242\\\",\\n    \\\"exp_month\\\": 12,\\n    \\\"exp_year\\\": 2024,\\n    \\\"funding\\\": \\\"credit\\\",\\n    \\\"last4\\\": \\\"1234\\\",\\n    \\\"metadata\\\": {\\n    },\\n    \\\"name\\\": \\\"Stripe Johnson\\\",\\n    \\\"tokenization_method\\\": \\\"android_pay\\\"\\n  },\\n  \\\"client_ip\\\": \\\"74.125.113.96\\\",\\n  \\\"created\\\": 1565029974,\\n  \\\"livemode\\\": false,\\n  \\\"type\\\": \\\"card\\\",\\n  \\\"used\\\": false\\n}\\n\"\n" +
            "\t\t},\n" +
            "\t\t\"type\": \"CARD\",\n" +
            "\t\t\"info\": {\n" +
            "\t\t\t\"cardNetwork\": \"VISA\",\n" +
            "\t\t\t\"cardDetails\": \"1234\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    private static final String GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS = "{\n" +
            "\t\"apiVersionMinor\": 0,\n" +
            "\t\"apiVersion\": 2,\n" +
            "\t\"paymentMethodData\": {\n" +
            "\t\t\"description\": \"Visa •••• 1234\",\n" +
            "\t\t\"tokenizationData\": {\n" +
            "\t\t\t\"type\": \"PAYMENT_GATEWAY\",\n" +
            "\t\t\t\"token\": \"{\\n  \\\"id\\\": \\\"tok_1F4B7Q\\\",\\n  \\\"object\\\": \\\"token\\\",\\n  \\\"card\\\": {\\n    \\\"id\\\": \\\"card_1F4B7Q\\\",\\n    \\\"object\\\": \\\"card\\\",\\n    \\\"address_city\\\": \\\"San Francisco\\\",\\n    \\\"address_country\\\": \\\"US\\\",\\n    \\\"address_line1\\\": \\\"510 Townsend Street\\\",\\n    \\\"address_line1_check\\\": \\\"unchecked\\\",\\n    \\\"address_line2\\\": null,\\n    \\\"address_state\\\": \\\"CA\\\",\\n    \\\"address_zip\\\": \\\"20895\\\",\\n    \\\"address_zip_check\\\": \\\"unchecked\\\",\\n    \\\"brand\\\": \\\"Visa\\\",\\n    \\\"country\\\": \\\"US\\\",\\n    \\\"cvc_check\\\": null,\\n    \\\"dynamic_last4\\\": \\\"4242\\\",\\n    \\\"exp_month\\\": 12,\\n    \\\"exp_year\\\": 2024,\\n    \\\"funding\\\": \\\"credit\\\",\\n    \\\"last4\\\": \\\"1234\\\",\\n    \\\"metadata\\\": {\\n    },\\n    \\\"name\\\": \\\"Stripe Johnson\\\",\\n    \\\"tokenization_method\\\": \\\"android_pay\\\"\\n  },\\n  \\\"client_ip\\\": \\\"74.125.113.98\\\",\\n  \\\"created\\\": 1565030476,\\n  \\\"livemode\\\": false,\\n  \\\"type\\\": \\\"card\\\",\\n  \\\"used\\\": false\\n}\\n\"\n" +
            "\t\t},\n" +
            "\t\t\"type\": \"CARD\",\n" +
            "\t\t\"info\": {\n" +
            "\t\t\t\"cardNetwork\": \"VISA\",\n" +
            "\t\t\t\"cardDetails\": \"1234\",\n" +
            "\t\t\t\"billingAddress\": {\n" +
            "\t\t\t\t\"phoneNumber\": \"1-888-555-1234\",\n" +
            "\t\t\t\t\"address3\": \"\",\n" +
            "\t\t\t\t\"sortingCode\": \"\",\n" +
            "\t\t\t\t\"address2\": \"\",\n" +
            "\t\t\t\t\"countryCode\": \"US\",\n" +
            "\t\t\t\t\"address1\": \"510 Townsend St\",\n" +
            "\t\t\t\t\"postalCode\": \"94103\",\n" +
            "\t\t\t\t\"name\": \"Stripe Johnson\",\n" +
            "\t\t\t\t\"locality\": \"San Francisco\",\n" +
            "\t\t\t\t\"administrativeArea\": \"CA\"\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t},\n" +
            "\t\"email\": \"stripe@example.com\"" +
            "}";

    @Test
    public void card_toPaymentMethodParamsCard() {
        final PaymentMethodCreateParams.Card expectedCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4242424242424242")
                        .setCvc("123")
                        .setExpiryMonth(8)
                        .setExpiryYear(2019)
                        .build();
        assertEquals(expectedCard, CardFixtures.CARD.toPaymentMethodParamsCard());
    }

    @Test
    public void createFromGooglePay_withNoBillingAddress() throws JSONException {
        final PaymentMethodCreateParams createdParams =
                PaymentMethodCreateParams.createFromGooglePay(
                        new JSONObject(GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS));

        final PaymentMethodCreateParams expectedParams = PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.create("tok_1F4AzK"),
                new PaymentMethod.BillingDetails.Builder()
                        .setEmail("")
                        .build()
        );
        assertEquals(expectedParams, createdParams);
    }

    @Test
    public void createFromGooglePay_withFullBillingAddress() throws JSONException {
        final PaymentMethodCreateParams createdParams =
                PaymentMethodCreateParams.createFromGooglePay(
                        new JSONObject(GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS));

        final PaymentMethodCreateParams expectedParams = PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.create("tok_1F4B7Q"),
                new PaymentMethod.BillingDetails.Builder()
                        .setPhone("1-888-555-1234")
                        .setEmail("stripe@example.com")
                        .setName("Stripe Johnson")
                        .setAddress(new Address.Builder()
                                .setLine1("510 Townsend St")
                                .setLine2("")
                                .setCity("San Francisco")
                                .setState("CA")
                                .setPostalCode("94103")
                                .setCountry("US")
                                .build())
                        .build()
        );
        assertEquals(expectedParams, createdParams);
    }
}
