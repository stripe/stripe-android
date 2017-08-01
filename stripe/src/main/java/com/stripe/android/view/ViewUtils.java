package com.stripe.android.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.Card;

import static com.stripe.android.model.Card.CVC_LENGTH_AMERICAN_EXPRESS;
import static com.stripe.android.model.Card.CVC_LENGTH_COMMON;

/**
 * Static utility functions needed for View classes.
 */
class ViewUtils {

    static boolean isCvcMaximalLength(
            @NonNull @Card.CardBrand String cardBrand,
            @Nullable String cvcText) {
        if (cvcText == null) {
            return false;
        }

        if (Card.AMERICAN_EXPRESS.equals(cardBrand)) {
            return cvcText.length() == CVC_LENGTH_AMERICAN_EXPRESS;
        } else {
            return cvcText.length() == CVC_LENGTH_COMMON;
        }
    }

    /**
     * Separates a card number according to the brand requirements, including prefixes of card
     * numbers, so that the groups can be easily displayed if the user is typing them in.
     * Note that this does not verify that the card number is valid, or even that it is a number.
     *
     * @param spacelessCardNumber the raw card number, without spaces
     * @param brand the {@link Card.CardBrand} to use as a separating scheme
     * @return an array of strings with the number groups, in order. If the number is not complete,
     * some of the array entries may be {@code null}.
     */
    @NonNull
    static String[] separateCardNumberGroups(@NonNull String spacelessCardNumber,
                                                    @NonNull @Card.CardBrand String brand) {
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
