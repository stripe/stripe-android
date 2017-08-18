package com.stripe.android.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

class CountryUtils {

    static final String[] NO_POSTAL_CODE_COUNTRIES = {
            "AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF",
            "CG", "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY",
            "HK", "IE", "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU",
            "MW", "NR", "NU", "PA", "QA", "RW", "SA", "SB", "SC", "SL", "SO", "SR", "ST", "SY",
            "TF", "TK", "TL", "TO", "TT", "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW"};
    static final Set<String> NO_POSTAL_CODE_COUNTRIES_SET = new HashSet<>(Arrays.asList(NO_POSTAL_CODE_COUNTRIES));

    static boolean doesCountryUsePostalCode(String countryCode) {
        return !NO_POSTAL_CODE_COUNTRIES_SET.contains(countryCode);
    }

    static boolean isUSZipCodeValid(String zipCode) {
        return Pattern.matches("^[0-9]{5}(?:-[0-9]{4})?$", zipCode);
    }

    static boolean isCanadianPostalCodeValid(String postalCode) {
        return Pattern.matches("^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$", postalCode);
    }

    static boolean isUKPostcodeValid(String postalCode) {
        return Pattern.matches("^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][ABD-HJLNP-UW-Z]{2}$", postalCode);
    }

}
