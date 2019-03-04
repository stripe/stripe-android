package com.stripe.android.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link BankAccount}.
 */
@RunWith(RobolectricTestRunner.class)
public class BankAccountTest {

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
                BankAccount.TYPE_INDIVIDUAL,
                "STRIPE TEST BANK",
                "US",
                "usd",
                "1JWtPxqbdX5Gamtc",
                "6789",
                "110000000");
        final BankAccount actualAccount = BankAccount.fromString(RAW_BANK_ACCOUNT);
        assertEquals(expectedAccount, actualAccount);
    }
}
