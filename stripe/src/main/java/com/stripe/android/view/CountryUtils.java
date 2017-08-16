package com.stripe.android.view;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ksun on 8/15/17.
 */

public class CountryUtils {

    static final String[] NO_POSTAL_CODE_COUNTRIES = {
            "AE", "AG", "AN", "AO", "AW", "BF", "BI", "BJ", "BO", "BS", "BW", "BZ", "CD", "CF",
            "CG", "CI", "CK", "CM", "DJ", "DM", "ER", "FJ", "GD", "GH", "GM", "GN", "GQ", "GY",
            "HK", "IE", "JM", "KE", "KI", "KM", "KN", "KP", "LC", "ML", "MO", "MR", "MS", "MU",
            "MW", "NR", "NU", "PA", "QA", "RW", "SA", "SB", "SC", "SL", "SO", "SR", "ST", "SY",
            "TF", "TK", "TL", "TO", "TT", "TV", "TZ", "UG", "VU", "YE", "ZA", "ZW"};
    static final Set<String> NO_POSTAL_CODE_COUNTRIES_SET = new HashSet<>(Arrays.asList(NO_POSTAL_CODE_COUNTRIES));

    static boolean doesCountryUsePostalCode(String countryCode) {
        return ! NO_POSTAL_CODE_COUNTRIES_SET.contains(countryCode);
    }
}
