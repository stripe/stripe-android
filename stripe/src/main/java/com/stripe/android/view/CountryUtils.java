package com.stripe.android.view;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CountryUtils {

    private static final String[] NO_POSTAL_CODE_COUNTRIES = {
            "AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF",
            "CG", "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY",
            "HK", "IE", "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU",
            "MW", "NR", "NU", "PA", "QA", "RW", "SB", "SC", "SL", "SO", "SR", "ST", "SY", "TF",
            "TK", "TL", "TO", "TT", "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW"
    };
    private static final Set<String> NO_POSTAL_CODE_COUNTRIES_SET = new HashSet<>(
            Arrays.asList(NO_POSTAL_CODE_COUNTRIES));

    static boolean doesCountryUsePostalCode(@NonNull String countryCode) {
        return !NO_POSTAL_CODE_COUNTRIES_SET.contains(countryCode);
    }

    @NonNull
    static Map<String, String> getCountryNameToCodeMap() {
        final Map<String, String> displayNameToCountryCode = new HashMap<>();
        for (String countryCode : Locale.getISOCountries()) {
            final Locale locale = new Locale("", countryCode);
            displayNameToCountryCode.put(locale.getDisplayCountry(), countryCode);
        }
        return displayNameToCountryCode;
    }

}
