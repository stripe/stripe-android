package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link BankAccount}.
 */
public class BankAccountTest {
    private static final String BANK_ACCOUNT_NUMBER = "000123456789";
    private static final String BANK_ROUTING_NUMBER = "110000000";
    private static final String BANK_ACCOUNT_HOLDER_NAME = "Lily Thomas";

    private static final Map<String, String> GUID_PARAMS;

    static {
        GUID_PARAMS = new HashMap<>();
        GUID_PARAMS.put("guid", UUID.randomUUID().toString());
        GUID_PARAMS.put("muid", UUID.randomUUID().toString());
    }

    private static final String RAW_BANK_ACCOUNT = "{\n" +
            "    \"id\": \"ba_19d8Fh2eZvKYlo2C9qw8RwpV\",\n" +
            "    \"object\": \"bank_account\",\n" +
            "    \"account_holder_name\": \"Jane Austen\",\n" +
            "    \"account_holder_type\": \"individual\",\n" +
            "    \"bank_name\": \"STRIPE TEST BANK\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"fingerprint\": \"1JWtPxqbdX5Gamtc\",\n" +
            "    \"last4\": \"6789\",\n" +
            "    \"routing_number\": \"110000000\",\n" +
            "    \"status\": \"new\"\n" +
            "  }";

    @Test
    public void parseSampleAccount_returnsExpectedValue() {
        final BankAccount expectedAccount = new BankAccount(
                "Jane Austen",
                BankAccount.BankAccountType.INDIVIDUAL,
                "STRIPE TEST BANK",
                "US",
                "usd",
                "1JWtPxqbdX5Gamtc",
                "6789",
                "110000000");
        final BankAccount actualAccount = BankAccount.fromString(RAW_BANK_ACCOUNT);
        assertEquals(expectedAccount, actualAccount);
    }

    @Test
    public void createBankTokenParams_hasExpectedEntries() {
        final BankAccount bankAccount = new BankAccount(BANK_ACCOUNT_NUMBER, "US",
                "usd", BANK_ROUTING_NUMBER);
        final Map<String, Object> bankAccountMap = getBankAccountTokenParamData(bankAccount);
        assertNotNull(bankAccountMap);

        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap.get("account_number"));
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap.get("routing_number"));
        assertEquals("US", bankAccountMap.get("country"));
        assertEquals("usd", bankAccountMap.get("currency"));
    }

    @Test
    public void hashMapFromBankAccount_mapsCorrectFields() {
        final BankAccount bankAccount = new BankAccount(BANK_ACCOUNT_NUMBER,
                BANK_ACCOUNT_HOLDER_NAME, BankAccount.BankAccountType.INDIVIDUAL, null, "US",
                "usd", null, null, BANK_ROUTING_NUMBER);
        final Map<String, Object> bankAccountMap = getBankAccountTokenParamData(bankAccount);
        assertNotNull(bankAccountMap);

        assertEquals(BANK_ACCOUNT_NUMBER, bankAccountMap.get("account_number"));
        assertEquals(BANK_ROUTING_NUMBER, bankAccountMap.get("routing_number"));
        assertEquals("US", bankAccountMap.get("country"));
        assertEquals("usd", bankAccountMap.get("currency"));
        assertEquals(BANK_ACCOUNT_HOLDER_NAME, bankAccountMap.get("account_holder_name"));
        assertEquals(BankAccount.BankAccountType.INDIVIDUAL, bankAccountMap.get("account_holder_type"));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private Map<String, Object> getBankAccountTokenParamData(@NonNull BankAccount bankAccount) {
        final Map<String, Object> params = bankAccount.toParamMap();
        params.putAll(GUID_PARAMS);
        assertNotNull(params);
        return (Map<String, Object>) params.get("bank_account");
    }
}
