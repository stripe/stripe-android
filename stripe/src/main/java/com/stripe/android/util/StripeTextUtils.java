package com.stripe.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.view.KeyEvent;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.stripe.android.model.BankAccount.BankAccountType;
import static com.stripe.android.model.Card.CardBrand;
import static com.stripe.android.model.Card.FundingType;
import static com.stripe.android.model.Token.TokenType;

/**
 * Utility class for common text-related operations on Stripe data coming from the server.
 */
public class StripeTextUtils {

    private static int[] NON_DELETE_KEYS = {
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_SLASH
    };

    /**
     * Util Array for converting bytes to a hex string.
     * {@url http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java}
     */
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Check to see if the input number has any of the given prefixes.
     *
     * @param number the number to test
     * @param prefixes the prefixes to test against
     * @return {@code true} if number begins with any of the input prefixes
     */
    public static boolean hasAnyPrefix(String number, String... prefixes) {
        if (number == null) {
            return false;
        }

        for (String prefix : prefixes) {
            if (number.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check to see whether the input string is a whole, positive number.
     *
     * @param value the input string to test
     * @return {@code true} if the input value consists entirely of integers
     */
    public static boolean isWholePositiveNumber(String value) {
        if (value == null) {
            return false;
        }

        // Refraining from using android's TextUtils in order to avoid
        // depending on another package.
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Swap {@code null} for blank text values.
     *
     * @param value an input string that may or may not be entirely whitespace
     * @return {@code null} if the string is entirely whitespace, or the original value if not
     */
    public static String nullIfBlank(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value;
    }

    /**
     * A checker for whether or not the input value is entirely whitespace. This is slightly more
     * aggressive than the android TextUtils#isEmpty method, which only returns true for
     * {@code null} or {@code ""}.
     *
     * @param value a possibly blank input string value
     * @return {@code true} if and only if the value is all whitespace, {@code null}, or empty
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    /**
     * Converts a String value into the appropriate {@link BankAccountType}.
     *
     * @param possibleAccountType a String that might match a {@link BankAccountType} or be empty.
     * @return {@code null} if the input is blank or of unknown type, else the appropriate
     *         {@link BankAccountType}.
     */
    @Nullable
    @BankAccountType
    public static String asBankAccountType(@Nullable String possibleAccountType) {
        if (BankAccount.TYPE_COMPANY.equals(possibleAccountType)) {
            return BankAccount.TYPE_COMPANY;
        } else if (BankAccount.TYPE_INDIVIDUAL.equals(possibleAccountType)) {
            return BankAccount.TYPE_INDIVIDUAL;
        }

        return null;
    }

    /**
     * Converts a card number that may have spaces between the numbers into one without any spaces.
     * Note: method does not check that all characters are digits or spaces.
     *
     * @param cardNumberWithSpaces a card number, for instance "4242 4242 4242 4242"
     * @return the input number minus any spaces, for instance "4242424242424242".
     * Returns {@code null} if the input was {@code null} or all spaces.
     */
    @Nullable
    public static String removeSpacesAndHyphens(@Nullable String cardNumberWithSpaces) {
        if (isBlank(cardNumberWithSpaces)) {
            return null;
        }
        return cardNumberWithSpaces.replaceAll("\\s|-", "");
    }

    /**
     * Converts an unchecked String value to a {@link CardBrand} or {@code null}.
     *
     * @param possibleCardType a String that might match a {@link CardBrand} or be empty.
     * @return {@code null} if the input is blank, else the appropriate {@link CardBrand}.
     */
    @Nullable
    @CardBrand
    public static String asCardBrand(@Nullable String possibleCardType) {
        if (isBlank(possibleCardType)) {
            return null;
        }

        if (Card.AMERICAN_EXPRESS.equalsIgnoreCase(possibleCardType)) {
            return Card.AMERICAN_EXPRESS;
        } else if (Card.MASTERCARD.equalsIgnoreCase(possibleCardType)) {
            return Card.MASTERCARD;
        } else if (Card.DINERS_CLUB.equalsIgnoreCase(possibleCardType)) {
            return Card.DINERS_CLUB;
        } else if (Card.DISCOVER.equalsIgnoreCase(possibleCardType)) {
            return Card.DISCOVER;
        } else if (Card.JCB.equalsIgnoreCase(possibleCardType)) {
            return Card.JCB;
        } else if (Card.VISA.equalsIgnoreCase(possibleCardType)) {
            return Card.VISA;
        } else {
            return Card.UNKNOWN;
        }
    }

    /**
     * Converts an unchecked String value to a {@link FundingType} or {@code null}.
     *
     * @param possibleFundingType a String that might match a {@link FundingType} or be empty
     * @return {@code null} if the input is blank, else the appropriate {@link FundingType}
     */
    @Nullable
    @FundingType
    public static String asFundingType(@Nullable String possibleFundingType) {
        if (isBlank(possibleFundingType)) {
            return null;
        }

        if (Card.FUNDING_CREDIT.equalsIgnoreCase(possibleFundingType)) {
            return Card.FUNDING_CREDIT;
        } else if (Card.FUNDING_DEBIT.equalsIgnoreCase(possibleFundingType)) {
            return Card.FUNDING_DEBIT;
        } else if (Card.FUNDING_PREPAID.equalsIgnoreCase(possibleFundingType)) {
            return Card.FUNDING_PREPAID;
        } else {
            return Card.FUNDING_UNKNOWN;
        }
    }

    /**
     * Converts an unchecked String value to a {@link TokenType} or {@code null}.
     *
     * @param possibleTokenType a String that might match a {@link TokenType} or be empty
     * @return {@code null} if the input is blank or otherwise does not match a {@link TokenType},
     * else the appropriate {@link TokenType}.
     */
    @Nullable
    @TokenType
    public static String asTokenType(@Nullable String possibleTokenType) {
        if (isBlank(possibleTokenType)) {
            return null;
        }

        if (Token.TYPE_CARD.equals(possibleTokenType)) {
            return Token.TYPE_CARD;
        } else if (Token.TYPE_BANK_ACCOUNT.equals(possibleTokenType)) {
            return Token.TYPE_BANK_ACCOUNT;
        }

        return null;
    }

    /**
     * Calculate a hash value of a String input and convert the result to a hex string.
     *
     * @param toHash a value to hash
     * @return a hexadecimal string
     */
    @Nullable
    public static String shaHashInput(@NonNull @Size(min = 1) String toHash) {
        String hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();
            hash = bytesToHex( bytes );
        } catch(NoSuchAlgorithmException noSuchAlgorithm) {
            return null;
        } catch (UnsupportedEncodingException unsupportedCoding) {
            return null;
        }

        return hash;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[ bytes.length * 2 ];
        for(int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[ i * 2 ] = hexArray[ v >>> 4 ];
            hexChars[ i * 2 + 1 ] = hexArray[ v & 0x0F ];
        }
        return new String( hexChars );
    }
}
