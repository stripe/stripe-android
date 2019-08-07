package com.stripe.android.model;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
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

    @Test
    public void toJson_withIdeal_shouldReturnExpectedJson() {
        final PaymentMethod paymentMethod = new PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("ideal")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
                .setIdeal(new PaymentMethod.Ideal.Builder()
                        .setBank("my bank")
                        .setBankIdentifierCode("bank id")
                        .build())
                .build();

        assertEquals(paymentMethod, PaymentMethod.fromString(RAW_IDEAL_JSON));
    }

    @Test
    public void equals_withEqualPaymentMethods_shouldReturnTrue() {
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                new PaymentMethod.Builder()
                        .setId("pm_123456789")
                        .setCreated(1550757934255L)
                        .setLiveMode(true)
                        .setType("card")
                        .setCustomerId("cus_AQsHpvKfKwJDrF")
                        .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
                        .setCard(PaymentMethodFixtures.CARD)
                        .build());
    }

    @Test
    public void fromString_shouldReturnExpectedPaymentMethod() {
        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                PaymentMethod.fromString(RAW_CARD_JSON));
    }

    @Test
    public void fromString_withIdeal_returnsExpectedObject() {
        final PaymentMethod paymentMethod = PaymentMethod.fromString(RAW_IDEAL_JSON);
        assertNotNull(paymentMethod);
        assertEquals("ideal", paymentMethod.type);
    }

    @Test
    public void billingDetails_toParamMap_removesNullValues() {
        final Map<String, Object> billingDetails =
                new PaymentMethod.BillingDetails.Builder()
                        .setName("name")
                        .build()
                        .toParamMap();
        assertEquals(1, billingDetails.size());
        assertFalse(billingDetails.containsKey(PaymentMethod.BillingDetails.FIELD_ADDRESS));
        assertTrue(billingDetails.containsKey(PaymentMethod.BillingDetails.FIELD_NAME));
    }

    @Test
    public void testParcelable_shouldBeEqualAfterParcel() {
        final Map<String, String> metadata = new HashMap<>(2);
        metadata.put("meta", "data");
        metadata.put("meta2", "data2");
        final PaymentMethod paymentMethod = new PaymentMethod.Builder()
                .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
                .setCard(PaymentMethodFixtures.CARD)
                .setCardPresent(PaymentMethod.CardPresent.EMPTY)
                .setCreated(1550757934255L)
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setId("pm_123456789")
                .setType("card")
                .setLiveMode(true)
                .setMetadata(metadata)
                .setIdeal(new PaymentMethod.Ideal.Builder()
                        .setBank("my bank")
                        .setBankIdentifierCode("bank id")
                        .build())
                .build();

        final Parcel parcel = Parcel.obtain();
        paymentMethod.writeToParcel(parcel, paymentMethod.describeContents());
        parcel.setDataPosition(0);

        final PaymentMethod parcelPaymentMethod = PaymentMethod.CREATOR.createFromParcel(parcel);

        assertEquals(paymentMethod, parcelPaymentMethod);
    }
}
