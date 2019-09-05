package com.stripe.android.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

final class GooglePayFixtures {
    @NonNull private static final String GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS_RAW = "{\n" +
            "\t\"apiVersionMinor\": 0,\n" +
            "\t\"apiVersion\": 2,\n" +
            "\t\"paymentMethodData\": {\n" +
            "\t\t\"description\": \"Visa •••• 1234\",\n" +
            "\t\t\"tokenizationData\": {\n" +
            "\t\t\t\"type\": \"PAYMENT_GATEWAY\",\n" +
            "\t\t\t\"token\": \"{\\n  \\\"id\\\": \\\"tok_1F4ACMCRMbs6FrXf6fPqLnN7\\\",\\n  \\\"object\\\": \\\"token\\\",\\n  \\\"card\\\": {\\n    \\\"id\\\": \\\"card_1F4AzKCRMbs6FrXf1nX87nde\\\",\\n    \\\"object\\\": \\\"card\\\",\\n    \\\"address_city\\\": null,\\n    \\\"address_country\\\": null,\\n    \\\"address_line1\\\": null,\\n    \\\"address_line1_check\\\": null,\\n    \\\"address_line2\\\": null,\\n    \\\"address_state\\\": null,\\n    \\\"address_zip\\\": null,\\n    \\\"address_zip_check\\\": null,\\n    \\\"brand\\\": \\\"Visa\\\",\\n    \\\"country\\\": \\\"US\\\",\\n    \\\"cvc_check\\\": null,\\n    \\\"dynamic_last4\\\": \\\"4242\\\",\\n    \\\"exp_month\\\": 12,\\n    \\\"exp_year\\\": 2024,\\n    \\\"funding\\\": \\\"credit\\\",\\n    \\\"last4\\\": \\\"1234\\\",\\n    \\\"metadata\\\": {\\n    },\\n    \\\"name\\\": \\\"Stripe Johnson\\\",\\n    \\\"tokenization_method\\\": \\\"android_pay\\\"\\n  },\\n  \\\"client_ip\\\": \\\"74.125.113.96\\\",\\n  \\\"created\\\": 1565029974,\\n  \\\"livemode\\\": false,\\n  \\\"type\\\": \\\"card\\\",\\n  \\\"used\\\": false\\n}\\n\"\n" +
            "\t\t},\n" +
            "\t\t\"type\": \"CARD\",\n" +
            "\t\t\"info\": {\n" +
            "\t\t\t\"cardNetwork\": \"VISA\",\n" +
            "\t\t\t\"cardDetails\": \"1234\"\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    @NonNull private static final String GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS_RAW = "{\n" +
            "\t\"apiVersionMinor\": 0,\n" +
            "\t\"apiVersion\": 2,\n" +
            "\t\"paymentMethodData\": {\n" +
            "\t\t\"description\": \"Visa •••• 1234\",\n" +
            "\t\t\"tokenizationData\": {\n" +
            "\t\t\t\"type\": \"PAYMENT_GATEWAY\",\n" +
            "\t\t\t\"token\": \"{\\n  \\\"id\\\": \\\"tok_1F4VSjBbvEcIpqUbSsbEtBap\\\",\\n  \\\"object\\\": \\\"token\\\",\\n  \\\"card\\\": {\\n    \\\"id\\\": \\\"card_1F4B7Q\\\",\\n    \\\"object\\\": \\\"card\\\",\\n    \\\"address_city\\\": \\\"San Francisco\\\",\\n    \\\"address_country\\\": \\\"US\\\",\\n    \\\"address_line1\\\": \\\"510 Townsend Street\\\",\\n    \\\"address_line1_check\\\": \\\"unchecked\\\",\\n    \\\"address_line2\\\": null,\\n    \\\"address_state\\\": \\\"CA\\\",\\n    \\\"address_zip\\\": \\\"20895\\\",\\n    \\\"address_zip_check\\\": \\\"unchecked\\\",\\n    \\\"brand\\\": \\\"Visa\\\",\\n    \\\"country\\\": \\\"US\\\",\\n    \\\"cvc_check\\\": null,\\n    \\\"dynamic_last4\\\": \\\"4242\\\",\\n    \\\"exp_month\\\": 12,\\n    \\\"exp_year\\\": 2024,\\n    \\\"funding\\\": \\\"credit\\\",\\n    \\\"last4\\\": \\\"1234\\\",\\n    \\\"metadata\\\": {\\n    },\\n    \\\"name\\\": \\\"Stripe Johnson\\\",\\n    \\\"tokenization_method\\\": \\\"android_pay\\\"\\n  },\\n  \\\"client_ip\\\": \\\"74.125.113.98\\\",\\n  \\\"created\\\": 1565030476,\\n  \\\"livemode\\\": false,\\n  \\\"type\\\": \\\"card\\\",\\n  \\\"used\\\": false\\n}\\n\"\n" +
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

    @NonNull
    static final JSONObject GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS;

    @NonNull
    static final JSONObject GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS;

    static {
        try {
            GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS =
                    new JSONObject(GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS_RAW);
            GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS =
                    new JSONObject(GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS_RAW);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private GooglePayFixtures() {}
}
