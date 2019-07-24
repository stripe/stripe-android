package com.stripe.android.view;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation rules for postal code for use in {@link ShippingInfoWidget}
 */
final class ShippingPostalCodeValidator {
    private static final Map<String, Pattern> POSTAL_CODE_PATTERNS = new HashMap<>(3);

    static {
        POSTAL_CODE_PATTERNS.put(Locale.US.getCountry(),
                Pattern.compile("^[0-9]{5}(?:-[0-9]{4})?$"));
        POSTAL_CODE_PATTERNS.put(Locale.CANADA.getCountry(),
                Pattern.compile("^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$"));
        POSTAL_CODE_PATTERNS.put(Locale.UK.getCountry(),
                Pattern.compile("^[A-Z]{1,2}[0-9R][0-9A-Z]? [0-9][ABD-HJLNP-UW-Z]{2}$"));
    }

    boolean isValid(@NonNull String postalCode, @NonNull String countryCode,
                          @NonNull List<String> optionalShippingInfoFields,
                          @NonNull List<String> hiddenShippingInfoFields) {
        final Pattern postalCodePattern = POSTAL_CODE_PATTERNS.get(countryCode);
        if (postalCode.isEmpty() &&
                isPostalCodeOptional(optionalShippingInfoFields, hiddenShippingInfoFields)) {
            return true;
        } else if (postalCodePattern != null) {
            return postalCodePattern.matcher(postalCode).matches();
        } else if (CountryUtils.doesCountryUsePostalCode(countryCode)) {
            return !postalCode.isEmpty();
        } else {
            return true;
        }
    }

    private static boolean isPostalCodeOptional(
            @NonNull List<String> optionalShippingInfoFields,
            @NonNull List<String> hiddenShippingInfoFields) {
        return optionalShippingInfoFields
                .contains(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD) ||
                hiddenShippingInfoFields
                        .contains(ShippingInfoWidget.CustomizableShippingField.POSTAL_CODE_FIELD);
    }
}
