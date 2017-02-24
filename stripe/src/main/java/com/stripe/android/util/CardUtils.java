package com.stripe.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.Card;

import static com.stripe.android.model.Card.CardBrand;

/**
 * Utility class for functions to do with cards.
 */
public class CardUtils {

    public static final int LENGTH_COMMON_CARD = 16;
    public static final int LENGTH_AMERICAN_EXPRESS = 15;
    public static final int LENGTH_DINERS_CLUB = 14;

    public static final int CVC_LENGTH_COMMON = 3;
    public static final int CVC_LENGTH_AMEX = 4;

    public static boolean isValidCardNumber(@Nullable String cardNumber) {
        String normalizedNumber = StripeTextUtils.removeSpacesAndHyphens(cardNumber);
        return isValidLuhnNumber(normalizedNumber) && isValidCardLength(normalizedNumber);
    }

    /**
     * Checks the input string to see whether or not it is a valid Luhn number.
     *
     * @param cardNumber a String that may or may not represent a valid Luhn number
     * @return {@code true} if and only if the input value is a valid Luhn number
     */
    public static boolean isValidLuhnNumber(@Nullable String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        boolean isOdd = true;
        int sum = 0;

        for (int index = cardNumber.length() - 1; index >= 0; index--) {
            char c = cardNumber.charAt(index);
            if (!Character.isDigit(c)) {
                return false;
            }

            int digitInteger = Character.getNumericValue(c);
            isOdd = !isOdd;

            if (isOdd) {
                digitInteger *= 2;
            }

            if (digitInteger > 9) {
                digitInteger -= 9;
            }

            sum += digitInteger;
        }

        return sum % 10 == 0;
    }

    /**
     * Checks to see whether the input number is of the correct length, after determining its brand.
     * This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @return {@code true} if the card number is of known type and the correct length
     */
    public static boolean isValidCardLength(@Nullable String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        return isValidCardLength(cardNumber, getPossibleCardType(cardNumber, false));
    }

    /**
     * Checks to see whether the input number is of the correct length, given the assumed brand of
     * the card. This function does not perform a Luhn check.
     *
     * @param cardNumber the card number with no spaces or dashes
     * @param cardBrand a {@link CardBrand} used to get the correct size
     * @return {@code true} if the card number is the correct length for the assumed brand
     */
    public static boolean isValidCardLength(
            @Nullable String cardNumber,
            @NonNull @CardBrand String cardBrand) {
        if(cardNumber == null || Card.UNKNOWN.equals(cardBrand)) {
            return false;
        }

        int length = cardNumber.length();
        switch (cardBrand) {
            case Card.AMERICAN_EXPRESS:
                return length == LENGTH_AMERICAN_EXPRESS;
            case Card.DINERS_CLUB:
                return length == LENGTH_DINERS_CLUB;
            default:
                return length == LENGTH_COMMON_CARD;
        }
    }

    /**
     * Returns a {@link CardBrand} corresponding to a partial card number,
     * or {@link Card#UNKNOWN} if the card brand can't be determined from the input value.
     *
     * @param cardNumber a credit card number or partial card number
     * @return the {@link CardBrand} corresponding to that number,
     * or {@link Card#UNKNOWN} if it can't be determined
     */
    @NonNull
    @CardBrand
    public static String getPossibleCardType(@Nullable String cardNumber) {
        return getPossibleCardType(cardNumber, true);
    }

    @NonNull
    @CardBrand
    private static String getPossibleCardType(@Nullable String cardNumber,
                                              boolean shouldNormalize) {
        if (StripeTextUtils.isBlank(cardNumber)) {
            return Card.UNKNOWN;
        }

        String spacelessCardNumber = cardNumber;
        if (shouldNormalize) {
            spacelessCardNumber = StripeTextUtils.removeSpacesAndHyphens(cardNumber);
        }

        if (StripeTextUtils.hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_AMERICAN_EXPRESS)) {
            return Card.AMERICAN_EXPRESS;
        } else if (StripeTextUtils.hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_DISCOVER)) {
            return Card.DISCOVER;
        } else if (StripeTextUtils.hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_JCB)) {
            return Card.JCB;
        } else if (StripeTextUtils.hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_DINERS_CLUB)) {
            return Card.DINERS_CLUB;
        } else if (StripeTextUtils.hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_VISA)) {
            return Card.VISA;
        } else if (StripeTextUtils.hasAnyPrefix(spacelessCardNumber, Card.PREFIXES_MASTERCARD)) {
            return Card.MASTERCARD;
        } else {
            return Card.UNKNOWN;
        }
    }


    /**
     * Separates a card number according to the brand requirements, including prefixes of card
     * numbers, so that the groups can be easily displayed if the user is typing them in.
     * Note that this does not verify that the card number is valid, or even that it is a number.
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

}
