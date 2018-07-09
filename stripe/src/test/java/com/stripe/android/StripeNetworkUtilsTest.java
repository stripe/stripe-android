package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link StripeNetworkUtils}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
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

    private static final String BANK_ACCOUNT_NUMBER = "000123456789";
    private static final String BANK_ROUTING_NUMBER = "110000000";
    private static final String BANK_ACCOUNT_HOLDER_NAME = "Lily Thomas";

    @Test
    public void hashMapFromCard_mapsCorrectFields() {
        Card card = new Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC)
                .addressCity(CARD_CITY)
                .addressLine1(CARD_ADDRESS_L1)
                .addressLine2(CARD_ADDRESS_L2)
                .addressCountry(CARD_COUNTRY)
                .addressState(CARD_STATE)
                .addressZip(CARD_ZIP)
                .currency(CARD_CURRENCY)
                .name(CARD_NAME)
                .build();

        Map<String, Object> cardMap = getCardMapFromHashMappedCard(card);

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
    public void hashMapFromBankAccount_mapsCorrectFields() {
        BankAccount bankAccount = new BankAccount(
                BANK_ACCOUNT_NUMBER, "US", "usd", BANK_ROUTING_NUMBER)
                .setAccountHolderName(BANK_ACCOUNT_HOLDER_NAME)
                .setAccountHolderType(BankAccount.TYPE_INDIVIDUAL);
        Map<String, Object> bankAccountMap = getMapFromHashMappedBankAccount(bankAccount);

        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap.get("account_number"));
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap.get("routing_number"));
        assertEquals("US", bankAccountMap.get("country"));
        assertEquals("usd", bankAccountMap.get("currency"));
        assertEquals(BANK_ACCOUNT_HOLDER_NAME, bankAccountMap.get("account_holder_name"));
        assertEquals(BankAccount.TYPE_INDIVIDUAL, bankAccountMap.get("account_holder_type"));
    }

    @Test
    public void hashMapFromCard_omitsEmptyFields() {
        Card card = new Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC).build();

        Map<String, Object> cardMap = getCardMapFromHashMappedCard(card);

        assertEquals(CARD_NUMBER, cardMap.get("number"));
        assertEquals(CARD_CVC, cardMap.get("cvc"));
        assertEquals(8, cardMap.get("exp_month"));
        assertEquals(2019, cardMap.get("exp_year"));
        assertFalse(cardMap.containsKey("name"));
        assertFalse(cardMap.containsKey("currency"));
        assertFalse(cardMap.containsKey("address_line1"));
        assertFalse(cardMap.containsKey("address_line2"));
        assertFalse(cardMap.containsKey("address_city"));
        assertFalse(cardMap.containsKey("address_zip"));
        assertFalse(cardMap.containsKey("address_state"));
        assertFalse(cardMap.containsKey("address_country"));
    }

    @Test
    public void hashMapFromBankAccount_omitsEmptyFields() {
        BankAccount bankAccount = new BankAccount(
                BANK_ACCOUNT_NUMBER, "US", "usd", BANK_ROUTING_NUMBER);
        Map<String, Object> bankAccountMap = getMapFromHashMappedBankAccount(bankAccount);

        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap.get("account_number"));
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap.get("routing_number"));
        assertEquals("US", bankAccountMap.get("country"));
        assertEquals("usd", bankAccountMap.get("currency"));
        assertFalse(bankAccountMap.containsKey("account_holder_name"));
        assertFalse(bankAccountMap.containsKey("account_holder_type"));
    }

    @Test
    public void removeNullAndEmptyParams_removesNullParams() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("a", null);
        testMap.put("b", "not null");
        StripeNetworkUtils.removeNullAndEmptyParams(testMap);
        assertEquals(1, testMap.size());
        assertTrue(testMap.containsKey("b"));
    }

    @Test
    public void removeNullAndEmptyParams_removesEmptyStringParams() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("a", "fun param");
        testMap.put("b", "not null");
        testMap.put("c", "");
        StripeNetworkUtils.removeNullAndEmptyParams(testMap);
        assertEquals(2, testMap.size());
        assertTrue(testMap.containsKey("a"));
        assertTrue(testMap.containsKey("b"));
    }

    @Test
    public void removeNullAndEmptyParams_removesNestedEmptyParams() {
        Map<String, Object> testMap = new HashMap<>();
        Map<String, Object> firstNestedMap = new HashMap<>();
        Map<String, Object> secondNestedMap = new HashMap<>();
        testMap.put("a", "fun param");
        testMap.put("b", "not null");
        firstNestedMap.put("1a", "something");
        firstNestedMap.put("1b", null);
        secondNestedMap.put("2a", "");
        secondNestedMap.put("2b", "hello world");
        firstNestedMap.put("1c", secondNestedMap);
        testMap.put("c", firstNestedMap);

        StripeNetworkUtils.removeNullAndEmptyParams(testMap);
        assertEquals(3, testMap.size());
        assertTrue(testMap.containsKey("a"));
        assertTrue(testMap.containsKey("b"));
        assertTrue(testMap.containsKey("c"));
        assertEquals(2, firstNestedMap.size());
        assertTrue(firstNestedMap.containsKey("1a"));
        assertTrue(firstNestedMap.containsKey("1c"));
        assertEquals(1, secondNestedMap.size());
        assertTrue(secondNestedMap.containsKey("2b"));
    }

    @Test
    public void addUidParams_addsParams() {
        Map<String, Object> existingMap = new HashMap<>();
        StripeNetworkUtils.UidProvider provider = new StripeNetworkUtils.UidProvider() {
            @Override
            public String getUid() {
                return "abc123";
            }

            @Override
            public String getPackageName() {
                return "com.example.main";
            }

        };
        StripeNetworkUtils.addUidParams(provider, RuntimeEnvironment.application, existingMap);
        assertEquals(2, existingMap.size());
        assertTrue(existingMap.containsKey("muid"));
        assertTrue(existingMap.containsKey("guid"));
    }

    @Test
    public void addUidParamsToPaymentIntent_addParamsAtRightLevel() {
        Map<String, Object> existingMap = new HashMap<>();
        Map<String, Object> sourceDataMap = new HashMap<>();
        existingMap.put("source_data", sourceDataMap);

        StripeNetworkUtils.UidProvider provider = new StripeNetworkUtils.UidProvider() {
            @Override
            public String getUid() {
                return "abc123";
            }

            @Override
            public String getPackageName() {
                return "com.example.main";
            }

        };
        StripeNetworkUtils.addUidParamsToPaymentIntent(provider, RuntimeEnvironment.application, existingMap);
        assertEquals(1, existingMap.size());
        assertTrue(sourceDataMap.containsKey("muid"));
        assertTrue(sourceDataMap.containsKey("guid"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCardMapFromHashMappedCard(@NonNull Card card) {
        Map<String, Object> tokenMap = StripeNetworkUtils.hashMapFromCard(
                RuntimeEnvironment.application, card);
        assertNotNull(tokenMap);
        return (Map<String, Object>) tokenMap.get("card");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromHashMappedBankAccount(@NonNull BankAccount bankAccount) {
        Map<String, Object> tokenMap = StripeNetworkUtils.hashMapFromBankAccount(
                RuntimeEnvironment.application, bankAccount);
        assertNotNull(tokenMap);
        return (Map<String, Object>) tokenMap.get("bank_account");
    }
}
