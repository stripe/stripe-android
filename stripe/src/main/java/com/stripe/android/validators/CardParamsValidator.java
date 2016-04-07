package com.stripe.android.validators;

import com.stripe.android.util.DateUtils;
import com.stripe.android.util.TextUtils;

public class CardParamsValidator {

    private String number;
    private Integer expMonth;
    private Integer expYear;
    private String cvc;

    public static final String AMERICAN_EXPRESS = "American Express";
    public static final String DISCOVER = "Discover";
    public static final String JCB = "JCB";
    public static final String DINERS_CLUB = "Diners Club";
    public static final String VISA = "Visa";
    public static final String MASTERCARD = "MasterCard";
    public static final String UNKNOWN = "Unknown";

    // Based on http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29
    public static final String[] PREFIXES_AMERICAN_EXPRESS = {"34", "37"};
    public static final String[] PREFIXES_DISCOVER = {"60", "62", "64", "65"};
    public static final String[] PREFIXES_JCB = {"35"};
    public static final String[] PREFIXES_DINERS_CLUB = {"300", "301", "302", "303", "304", "305", "309", "36", "38", "39"};
    public static final String[] PREFIXES_VISA = {"4"};
    public static final String[] PREFIXES_MASTERCARD = {"50", "51", "52", "53", "54", "55"};

    public static final int MAX_LENGTH_STANDARD = 16;
    public static final int MAX_LENGTH_AMERICAN_EXPRESS = 15;
    public static final int MAX_LENGTH_DINERS_CLUB = 14;

    protected CardParamsValidator(String number, Integer expMonth, Integer expYear) {
        this.number = number;
        this.expMonth = expMonth;
        this.expYear = expYear;
    }

    protected CardParamsValidator(String number, Integer expMonth, Integer expYear, String cvc) {
        this(number, expMonth, expYear);
        this.cvc = cvc;
    }

    public String getType() {
        if (!TextUtils.isBlank(number)) {
            if (TextUtils.hasAnyPrefix(number, PREFIXES_AMERICAN_EXPRESS)) {
                return AMERICAN_EXPRESS;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_DISCOVER)) {
                return DISCOVER;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_JCB)) {
                return JCB;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_DINERS_CLUB)) {
                return DINERS_CLUB;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_VISA)) {
                return VISA;
            } else if (TextUtils.hasAnyPrefix(number, PREFIXES_MASTERCARD)) {
                return MASTERCARD;
            } else {
                return UNKNOWN;
            }
        }
        return null;
    }

    // Helpers
    protected String normalizeCardNumber(String number) {
        if (number == null) {
            return null;
        }
        return number.trim().replaceAll("\\s+|-", "");
    }

    private boolean isValidLuhnNumber(String number) {
        boolean isOdd = true;
        int sum = 0;

        for (int index = number.length() - 1; index >= 0; index--) {
            char c = number.charAt(index);
            if (!Character.isDigit(c)) {
                return false;
            }
            int digitInteger = Integer.parseInt("" + c);
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

    public boolean validateCardParams() {
        if (cvc == null) {
            return validateNumber() && validateExpiryDate();
        } else {
            return validateNumber() && validateExpiryDate() && validateCVC();
        }
    }

    public boolean validateNumber(String number) {

        if (number == null || TextUtils.isBlank(number)) {
            return false;
        }

        String cardType = getType();

        String rawNumber = normalizeCardNumber(number);
        if (TextUtils.isBlank(rawNumber)
                || !TextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return false;
        }

        if (AMERICAN_EXPRESS.equals(cardType)) {
            return rawNumber.length() == MAX_LENGTH_AMERICAN_EXPRESS;
        } else if (DINERS_CLUB.equals(cardType)) {
            return rawNumber.length() == MAX_LENGTH_DINERS_CLUB;
        } else {
            return rawNumber.length() == MAX_LENGTH_STANDARD;
        }
    }
    public boolean validateNumber() { return validateNumber(number); }

    public boolean validateCVC(String cvc) {

        if (cvc == null || TextUtils.isBlank(cvc)) {
            return false;
        }

        String cardType = getType();
        String cvcValue = cvc.trim();

        boolean validLength = ((cardType == null && cvcValue.length() >= 3 && cvcValue.length() <= 4) ||
                (AMERICAN_EXPRESS.equals(cardType) && cvcValue.length() == 4) ||
                (!AMERICAN_EXPRESS.equals(cardType) && cvcValue.length() == 3));

        return (TextUtils.isWholePositiveNumber(cvcValue) && validLength);

    }
    public boolean validateCVC() { return validateCVC(cvc); }

    public boolean validateExpiryDate(Integer expMonth, Integer expYear) {
        if (!validateExpMonth(expMonth)) {
            return false;
        }
        if (!validateExpYear(expYear)) {
            return false;
        }
        return !DateUtils.hasMonthPassed(expYear, expMonth);
    }
    public boolean validateExpiryDate() { return validateExpiryDate(expMonth, expYear); }

    public boolean validateExpMonth(Integer expMonth) {
        return (expMonth != null && expMonth >= 1 && expMonth <= 12);
    }
    public boolean validateExpMonth() { return validateExpMonth(expMonth); }

    public boolean validateExpYear(Integer expYear) {
        return (expYear != null && !DateUtils.hasYearPassed(expYear));
    }
    public boolean validateExpYear() { return validateExpYear(expYear); }

}
