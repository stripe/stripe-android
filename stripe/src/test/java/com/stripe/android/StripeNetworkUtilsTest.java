package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.Card;
import com.stripe.android.model.CardFixtures;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentMethodCreateParamsFixtures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link StripeNetworkUtils}
 */
@RunWith(RobolectricTestRunner.class)
public class StripeNetworkUtilsTest {

    private static final String CARD_ADDRESS_L1 = "123 Main Street";
    private static final String CARD_ADDRESS_L2 = "906";
    private static final String CARD_CITY = "San Francisco";
    private static final String CARD_COUNTRY = "US";
    private static final String CARD_CURRENCY = "USD";
    private static final String CARD_CVC = "123";
    private static final String CARD_NAME = "J Q Public";
    private static final String CARD_NUMBER = "4242424242424242";
    private static final String CARD_STATE = "CA";
    private static final String CARD_ZIP = "94107";

    @NonNull private final StripeNetworkUtils mNetworkUtils =
            new StripeNetworkUtils("com.example.app", new FakeUidSupplier());

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void hashMapFromCard_mapsCorrectFields() {
        final Map<String, Object> cardMap = getCardTokenParamData(CardFixtures.CARD);
        assertNotNull(cardMap);

        assertEquals(CARD_NUMBER, cardMap.get("number"));
        assertEquals(CARD_CVC, cardMap.get("cvc"));
        assertEquals(8, cardMap.get("exp_month"));
        assertEquals(2019, cardMap.get("exp_year"));
        assertEquals(CARD_NAME, cardMap.get("name"));
        assertEquals(CARD_CURRENCY, cardMap.get("currency"));
        assertEquals(CARD_ADDRESS_L1, cardMap.get("address_line1"));
        assertEquals(CARD_ADDRESS_L2, cardMap.get("address_line2"));
        assertEquals(CARD_CITY, cardMap.get("address_city"));
        assertEquals(CARD_ZIP, cardMap.get("address_zip"));
        assertEquals(CARD_STATE, cardMap.get("address_state"));
        assertEquals(CARD_COUNTRY, cardMap.get("address_country"));
    }

    @Test
    public void createCardTokenParams_hasExpectedEntries() {
        final Card card = new Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC)
                .build();

        final Map<String, Object> cardMap = getCardTokenParamData(card);
        assertNotNull(cardMap);

        assertEquals(CARD_NUMBER, cardMap.get("number"));
        assertEquals(CARD_CVC, cardMap.get("cvc"));
        assertEquals(8, cardMap.get("exp_month"));
        assertEquals(2019, cardMap.get("exp_year"));
    }

    @Test
    public void addUidParams_addsParams() {
        final Map<String, String> uidParams = mNetworkUtils.createUidParams();
        assertEquals(2, uidParams.size());
        assertTrue(uidParams.containsKey("muid"));
        assertTrue(uidParams.containsKey("guid"));
    }

    @Test
    public void addUidParamsToPaymentIntent_withSource_addsParamsAtRightLevel() {
        final Map<String, Object> existingMap = new HashMap<>();
        final Map<String, Object> sourceDataMap = new HashMap<>();
        existingMap.put(ConfirmPaymentIntentParams.API_PARAM_SOURCE_DATA, sourceDataMap);

        mNetworkUtils.addUidToConfirmPaymentIntentParams(existingMap);
        assertEquals(1, existingMap.size());
        assertTrue(sourceDataMap.containsKey("muid"));
        assertTrue(sourceDataMap.containsKey("guid"));
    }

    @Test
    public void addUidParamsToPaymentIntent_withPaymentMethodParams_addsUidAtRightLevel() {
        final Map<String, Object> existingMap = new HashMap<>();
        final Map<String, Object> paymentMethodCreateParamsMap =
                PaymentMethodCreateParamsFixtures.DEFAULT.toParamMap();
        existingMap.put(ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_DATA,
                paymentMethodCreateParamsMap);

        mNetworkUtils.addUidToConfirmPaymentIntentParams(existingMap);
        assertEquals(1, existingMap.size());
        assertTrue(paymentMethodCreateParamsMap.containsKey("muid"));
        assertTrue(paymentMethodCreateParamsMap.containsKey("guid"));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Map<String, Object> getCardTokenParamData(@NonNull Card card) {
        final Map<String, Object> cardTokenParams = mNetworkUtils.createCardTokenParams(card);
        assertNotNull(cardTokenParams);
        return (Map<String, Object>) cardTokenParams.get("card");
    }
}
