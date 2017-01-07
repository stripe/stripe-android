package com.stripe.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import static com.stripe.android.model.BankAccount.BankAccountType;
import static com.stripe.android.model.Card.CardBrand;
import static com.stripe.android.model.Card.FundingType;
import static com.stripe.android.model.Token.TokenType;

/**
 * Utility class for common text-related operations on Stripe data coming from the server.
 */
public class StripeTextUtils {

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
     * Returns a {@link CardBrand} corresponding to a partial card number, or {@link Card#UNKNOWN}
     * if the card brand can't be determined from the input value.
     *
     * @param cardNumber a credit card number or partial card number
     * @return the {@link CardBrand} corresponding to that number, or {@link Card#UNKNOWN} if it
     * can't be determined
     */
    @NonNull
    @CardBrand
    public static String getPossibleCardType(@Nullable String cardNumber) {
        if (isBlank(cardNumber)) {
            return Card.UNKNOWN;
        }

        String spacelessCardNumber = convertToSpacelessNumber(cardNumber);

        if (hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_AMERICAN_EXPRESS)) {
            return Card.AMERICAN_EXPRESS;
        } else if (hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_DISCOVER)) {
            return Card.DISCOVER;
        } else if (hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_JCB)) {
            return Card.JCB;
        } else if (hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_DINERS_CLUB)) {
            return Card.DINERS_CLUB;
        } else if (hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_VISA)) {
            return Card.VISA;
        } else if (hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_MASTERCARD)) {
            return Card.MASTERCARD;
        } else {
            return Card.UNKNOWN;
        }
    }

    /**
     * Converts a card number that may have spaces between the numbers into one without any spaces.
     * @param cardNumberWithSpaces a card number, for instance "4242 4242 4242 4242"
     * @return the input number minus any spaces, for instance "4242424242424242".
     * Returns {@code null} if the input was {@code null} or all spaces.
     */
    @Nullable
    public static String convertToSpacelessNumber(@Nullable String cardNumberWithSpaces) {
        if (isBlank(cardNumberWithSpaces)) {
            return null;
        }
        return cardNumberWithSpaces.replaceAll("\\s", "");
    }

    /**
     * Separates a card number according to the brand requirements, including prefixes of card
     * numbers, so that the groups can be easily displayed if the user is typing them in.
     *
     * @param spacelessCardNumber the raw card number, without spaces
     * @param brand the {@link CardBrand} to use as a separating scheme
     * @return an array of strings with the number groups, in order. If the number is not complete,
     * some of the array entries may be {@code null}.
     */
    @NonNull
    public static String[] separateCardNumberGroups(@NonNull String spacelessCardNumber,
                                                    @NonNull @CardBrand String brand) {
        String[] numberGroups;
        if (brand.equals(Card.AMERICAN_EXPRESS)) {
            numberGroups = new String[3];

            int length = spacelessCardNumber.length();
            int lastUsedIndex = 0;
            if (length > 4) {
                numberGroups[0] = spacelessCardNumber.substring(0, 4);
                lastUsedIndex = 4;
            }

            if (length > 10) {
                numberGroups[1] = spacelessCardNumber.substring(4, 10);
                lastUsedIndex = 10;
            }

            for (int i = 0; i < 3; i++) {
                if (numberGroups[i] != null) {
                    continue;
                }
                numberGroups[i] = spacelessCardNumber.substring(lastUsedIndex);
                break;
            }

        } else {
            numberGroups = new String[4];
            int i = 0;
            int previousStart = 0;
            while((i + 1) * 4 < spacelessCardNumber.length()) {
                String group = spacelessCardNumber.substring(previousStart, (i + 1) * 4);
                numberGroups[i] = group;
                previousStart = (i + 1) * 4;
                i++;
            }
            // Always stuff whatever is left into the next available array entry. This handles
            // incomplete numbers, full 16-digit numbers, and full 14-digit numbers
            numberGroups[i] = spacelessCardNumber.substring(previousStart);
        }
        return numberGroups;
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
}
