package com.stripe;

import com.stripe.util.DateUtils;
import com.stripe.util.TextUtils;
import com.stripe.util.URLUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Card {
    public final String number;
    public final String expMonth;
    public final String expYear;
    public final String cvc;
    public final String name;
    public final String addressLine1;
    public final String addressLine2;
    public final String addressCity;
    public final String addressState;
    public final String addressZip;
    public final String addressCountry;

    public final String object;
    public final String last4;
    public final String type;
    public final String fingerprint;
    public final String country;

    static public Card fromJSON(String jsonCard) throws JSONException {
        if (jsonCard == null) {
            return null;
        }

        return fromJSON(new JSONObject(jsonCard));
    }

    static public Card fromJSON(JSONObject cardMap) {
        if (cardMap == null) {
            return null;
        }

        return new Card(
                cardMap.optString("number"),
                cardMap.optString("exp_month"),
                cardMap.optString("exp_year"),
                cardMap.optString("cvc"),
                cardMap.optString("name"),
                cardMap.optString("address_line1"),
                cardMap.optString("address_line2"),
                cardMap.optString("address_city"),
                cardMap.optString("address_state"),
                cardMap.optString("address_zip"),
                cardMap.optString("address_country"),
                cardMap.optString("object"),
                cardMap.optString("last4"),
                cardMap.optString("type"),
                cardMap.optString("fingerprint"),
                cardMap.optString("country")
        );
    }

    public Card(Map<String, String> cardMap) {
        this(cardMap.get("number"),
                cardMap.get("exp_month"),
                cardMap.get("exp_year"),
                cardMap.get("cvc"),
                cardMap.get("name"),
                cardMap.get("address_line1"),
                cardMap.get("address_line2"),
                cardMap.get("address_city"),
                cardMap.get("address_state"),
                cardMap.get("address_zip"),
                cardMap.get("address_country"),
                cardMap.get("object"),
                cardMap.get("last4"),
                cardMap.get("type"),
                cardMap.get("fingerprint"),
                cardMap.get("country")
        );
    }

    public Card(String number, String expMonth, String expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity, String addressState, String addressZip, String addressCountry, String object, String last4, String type, String fingerprint, String country) {
        this.number = TextUtils.nullIfBlank(normalizeCardNumber(number));
        this.expMonth = TextUtils.nullIfBlank(expMonth);
        this.expYear = TextUtils.nullIfBlank(expYear);
        this.cvc = TextUtils.nullIfBlank(cvc);
        this.name = TextUtils.nullIfBlank(name);
        this.addressLine1 = TextUtils.nullIfBlank(addressLine1);
        this.addressLine2 = TextUtils.nullIfBlank(addressLine2);
        this.addressCity = TextUtils.nullIfBlank(addressCity);
        this.addressState = TextUtils.nullIfBlank(addressState);
        this.addressZip = TextUtils.nullIfBlank(addressZip);
        this.addressCountry = TextUtils.nullIfBlank(addressCountry);
        this.object = TextUtils.nullIfBlank(object);
        this.last4 = TextUtils.nullIfBlank(findLast4Digits(last4, this.number));
        this.type = TextUtils.nullIfBlank(findType(type, this.number));
        this.fingerprint = TextUtils.nullIfBlank(fingerprint);
        this.country = TextUtils.nullIfBlank(country);
    }

    public Card(String number, String expMonth, String expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity, String addressState, String addressZip, String addressCountry) {
        this(number, expMonth, expYear, cvc, name, addressLine1, addressLine2, addressCity, addressState, addressZip, addressCountry, null, null, null, null, null);
    }

    public Card(String number, String expMonth, String expYear, String cvc) {
        this(number, expMonth, expYear, cvc, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public Validation validateCard() {
        if (cvc == null) {
            return new Validation(validateNumber(), validateExpiryDate());
        } else {
            return new Validation(validateNumber(), validateExpiryDate(), validateCVC());
        }
    }

    public Validation validateNumber() {
        if (TextUtils.isBlank(number)) {
            return new Validation(StripeError.INVALID_NUMBER);
        }

        String rawNumber = number.trim().replaceAll("\\s+|-", "");
        if (TextUtils.isBlank(rawNumber)
                || rawNumber.length() < 10
                || rawNumber.length() > 19
                || !TextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return new Validation(StripeError.INVALID_NUMBER);
        }
        return new Validation();
    }

    public Validation validateExpiryDate() {
        Validation combined = new Validation(validateExpYear(), validateExpMonth());

        if (combined.isValid) {
            String year = expYear.trim();
            String month = expMonth.trim();

            if (DateUtils.hasMonthPassedOrInvalid(year, month)) {
                return new Validation(StripeError.INVALID_EXP_MONTH);
            }
        }
        return combined;
    }

    public Validation validateExpMonth() {
        if (TextUtils.isBlank(expMonth)) {
            return new Validation(StripeError.INVALID_EXP_MONTH);
        }

        String month = expMonth.trim();
        if (!TextUtils.isWholePositiveNumber(month)) {
            return new Validation(StripeError.INVALID_EXP_MONTH);
        }

        int intMonth = Integer.parseInt(month);
        if (intMonth < 1 || intMonth > 12) {
            return new Validation(StripeError.INVALID_EXP_MONTH);
        }

        return new Validation();
    }

    public Validation validateExpYear() {
        if (TextUtils.isBlank(expYear)) {
            return new Validation(StripeError.INVALID_EXP_YEAR);
        }

        String year = expYear.trim();
        if (!TextUtils.isWholePositiveNumber(year) || DateUtils.hasYearPassedOrInvalid(year)) {
            return new Validation(StripeError.INVALID_EXP_YEAR);
        }

        return new Validation();
    }

    public Validation validateCVC() {
        if (TextUtils.isBlank(cvc)) {
            return new Validation(StripeError.INVALID_CVC);
        }
        String cvcValue = cvc.trim();

        boolean validLength = ((type == null && cvcValue.length() >= 3 && cvcValue.length() <= 4) ||
                ("American Express".equals(type) && cvcValue.length() == 4) ||
                (!"American Express".equals(type) && cvcValue.length() == 3));


        if (!TextUtils.isWholePositiveNumber(cvcValue) || !validLength) {
            return new Validation(StripeError.INVALID_CVC);
        }
        return new Validation();
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

            sum += digitInteger;
        }

        return sum % 10 == 0;
    }

    protected String urlEncode() {
        List<String> params = new ArrayList<String>();
        if (this.number != null) {
            params.add(String.format("card[number]=%s", URLUtils.urlEncode(this.number)));
        }

        if (this.cvc != null) {
            params.add(String.format("card[cvc]=%s", URLUtils.urlEncode(this.cvc)));
        }

        if (this.expYear != null) {
            params.add(String.format("card[exp_year]=%s", URLUtils.urlEncode(this.expYear)));
        }

        if (this.expMonth != null) {
            params.add(String.format("card[exp_month]=%s", URLUtils.urlEncode(this.expMonth)));
        }

        if (this.name != null) {
            params.add(String.format("card[name]=%s", URLUtils.urlEncode(this.name)));
        }

        if (this.addressLine1 != null) {
            params.add(String.format("card[address_line1]=%s", URLUtils.urlEncode(this.addressLine1)));
        }

        if (this.addressLine2 != null) {
            params.add(String.format("card[address_line2]=%s", URLUtils.urlEncode(this.addressLine2)));
        }

        if (this.addressCity != null) {
            params.add(String.format("card[address_city]=%s", URLUtils.urlEncode(this.addressCity)));
        }

        if (this.addressState != null) {
            params.add(String.format("card[address_state]=%s", URLUtils.urlEncode(this.addressState)));
        }

        if (this.addressZip != null) {
            params.add(String.format("card[address_zip]=%s", URLUtils.urlEncode(this.addressZip)));
        }

        if (this.addressCountry != null) {
            params.add(String.format("card[address_country]=%s", URLUtils.urlEncode(this.addressCountry)));
        }
        return TextUtils.join(params, "&");
    }

    private String normalizeCardNumber(String number) {
        if (number == null) {
            return null;
        }
        return number.trim().replaceAll("\\s+|-", "");
    }

    private String findLast4Digits(String last4, String number) {
        if (!TextUtils.isBlank(last4)) {
            return last4;
        }
        if (number != null && number.length() > 4) {
            return number.substring(number.length() - 4, number.length());
        }
        return null;
    }

    private String findType(String type, String number) {
        if (TextUtils.isBlank(type) && !TextUtils.isBlank(number)) {
            if (TextUtils.hasAnyPrefix(number, "34", "37")) {
                return "American Express";
            } else if (TextUtils.hasAnyPrefix(number, "60", "62", "64", "65")) {
                return "Discover";
            } else if (TextUtils.hasAnyPrefix(number, "35")) {
                return "JCB";
            } else if (TextUtils.hasAnyPrefix(number, "30", "36", "38", "39")) {
                return "Diners Club";
            } else if (TextUtils.hasAnyPrefix(number, "4")) {
                return "Visa";
            } else if (TextUtils.hasAnyPrefix(number, "5")) {
                return "MasterCard";
            } else {
                return "Unknown";
            }
        }

        return type;
    }
}
