package com.stripe.android.view;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

class CountryUtils {

    static final String[] NO_POSTAL_CODE_COUNTRIES = {
            "AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF",
            "CG", "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY",
            "HK", "IE", "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU",
            "MW", "NR", "NU", "PA", "QA", "RW", "SA", "SB", "SC", "SL", "SO", "SR", "ST", "SY",
            "TF", "TK", "TL", "TO", "TT", "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW"};
    static final Set<String> NO_POSTAL_CODE_COUNTRIES_SET = new HashSet<>(Arrays.asList
            (NO_POSTAL_CODE_COUNTRIES));

    static boolean doesCountryUsePostalCode(@NonNull String countryCode) {
        return !NO_POSTAL_CODE_COUNTRIES_SET.contains(countryCode);
    }

    static boolean isUSZipCodeValid(@NonNull String zipCode) {
        return Pattern.matches("^[0-9]{5}(?:-[0-9]{4})?$", zipCode);
    }

    static boolean isCanadianPostalCodeValid(@NonNull String postalCode) {
        return Pattern.matches("^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$", postalCode);
    }

    static boolean isUKPostcodeValid(@NonNull String postcode) {
        return Pattern.matches("^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][ABD-HJLNP-UW-Z]{2}$", postcode);
    }

    static Map<String, String> getCountryNameToCodeMap() {
        Map<String, String> displayNameToCountryCode = new HashMap<>();
        for (String countryCode : Locale.getISOCountries()) {
            Locale locale = new Locale("", countryCode);
            displayNameToCountryCode.put(locale.getDisplayCountry(), countryCode);
        }
        return displayNameToCountryCode;
    }

}
