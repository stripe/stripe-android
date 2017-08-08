package com.stripe.android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.stripe.android.model.Card;

import java.util.Locale;

import static com.stripe.android.model.Card.CVC_LENGTH_AMERICAN_EXPRESS;
import static com.stripe.android.model.Card.CVC_LENGTH_COMMON;

/**
 * Static utility functions needed for View classes.
 */
class ViewUtils {

    static TypedValue getThemeAccentColor(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorAccent;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context
                    .getResources()
                    .getIdentifier("colorAccent", "attr", context.getPackageName());
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue;
    }

    static TypedValue getThemeColorControlNormal(Context context) {
        int colorAttr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            colorAttr = android.R.attr.colorControlNormal;
        } else {
            //Get colorAccent defined for AppCompat
            colorAttr = context
                    .getResources()
                    .getIdentifier("colorControlNormal", "attr", context.getPackageName());
        }
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, outValue, true);
        return outValue;
    }

    /**
     * Check to see whether the color int is essentially transparent.
     *
     * @param color a {@link ColorInt} integer
     * @return {@code true} if this color is too transparent to be seen
     */
    static boolean isColorTransparent(@ColorInt int color) {
        return Color.alpha(color) < 0x10;
    }

    /**
     * A crude mechanism by which we check whether or not a color is "dark."
     * This is subject to much interpretation, but we attempt to follow traditional
     * design standards.
     *
     * @param color an integer representation of a color
     * @return {@code true} if the color is "dark," else {@link false}
     */
    static boolean isColorDark(@ColorInt int color){
        // Forumla comes from W3C standards and conventional theory
        // about how to calculate the "brightness" of a color, often
        // thought of as how far along the spectrum from white to black the
        // grayscale version would be.
        // See https://www.w3.org/TR/AERT#color-contrast and
        // http://paulbourke.net/texture_colour/colourspace/ for further reading.
        double luminescence = 0.299* Color.red(color)
                + 0.587*Color.green(color)
                + 0.114* Color.blue(color);

        // Because the colors are all hex integers.
        double luminescencePercentage = luminescence / 255;
        if (luminescencePercentage > 0.5) {
            return false;
        } else {
            return true;
        }
    }

    static boolean isCvcMaximalLength(
            @NonNull @Card.CardBrand String cardBrand,
            @Nullable String cvcText) {
        if (cvcText == null) {
            return false;
        }

        if (Card.AMERICAN_EXPRESS.equals(cardBrand)) {
            return cvcText.trim().length() == CVC_LENGTH_AMERICAN_EXPRESS;
        } else {
            return cvcText.trim().length() == CVC_LENGTH_COMMON;
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
