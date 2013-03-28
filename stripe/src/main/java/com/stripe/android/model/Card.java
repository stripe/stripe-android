package com.stripe.android.model;

import com.stripe.android.util.DateUtils;
import com.stripe.android.util.TextUtils;

public class Card extends com.stripe.model.StripeObject {
    private String mNumber;
    private String mCvc;
    private Integer mExpMonth;
    private Integer mExpYear;
    private String mName;
    private String mAddressLine1;
    private String mAddressLine2;
    private String mAddressCity;
    private String mAddressState;
    private String mAddressZip;
    private String mAddressCountry;
    private String mLast4;
    private String mType;
    private String mFingerprint;
    private String mCountry;

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }

    public String getCVC() {
        return mCvc;
    }

    public void setCVC(String cvc) {
        mCvc = cvc;
    }

    public Integer getExpMonth() {
        return mExpMonth;
    }

    public void setExpMonth(Integer expMonth) {
        mExpMonth = expMonth;
    }

    public Integer getExpYear() {
        return mExpYear;
    }

    public void setExpYear(Integer expYear) {
        mExpYear = expYear;
    }

    public String getName() {
        return mName;
    }
    public void setName(String name) {
        mName = name;
    }

    public String getAddressLine1() {
        return mAddressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        mAddressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return mAddressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        mAddressLine2 = addressLine2;
    }

    public String getAddressCity() {
        return mAddressCity;
    }

    public void setAddressCity(String addressCity) {
        mAddressCity = addressCity;
    }

    public String getAddressZip() {
        return mAddressZip;
    }

    public void setAddressZip(String addressZip) {
        mAddressZip = addressZip;
    }

    public String getAddressState() {
        return mAddressState;
    }

    public void setAddressState(String addressState) {
        mAddressState = addressState;
    }

    public String getAddressCountry() {
        return mAddressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        mAddressCountry = addressCountry;
    }

    public String getLast4() {
        if (!TextUtils.isBlank(mLast4)) {
            return mLast4;
        }
        if (mNumber != null && mNumber.length() > 4) {
            return mNumber.substring(mNumber.length() - 4, mNumber.length());
        }
        return null;
    }

    public String getType() {
        if (TextUtils.isBlank(mType) && !TextUtils.isBlank(mNumber)) {
            if (TextUtils.hasAnyPrefix(mNumber, "34", "37")) {
                return "American Express";
            } else if (TextUtils.hasAnyPrefix(mNumber, "60", "62", "64", "65")) {
                return "Discover";
            } else if (TextUtils.hasAnyPrefix(mNumber, "35")) {
                return "JCB";
            } else if (TextUtils.hasAnyPrefix(mNumber, "30", "36", "38", "39")) {
                return "Diners Club";
            } else if (TextUtils.hasAnyPrefix(mNumber, "4")) {
                return "Visa";
            } else if (TextUtils.hasAnyPrefix(mNumber, "5")) {
                return "MasterCard";
            } else {
                return "Unknown";
            }
        }
        return mType;
    }

    public String getFingerprint() {
        return mFingerprint;
    }

    public String getCountry() {
        return mCountry;
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name,
            String addressLine1, String addressLine2, String addressCity, String addressState,
            String addressZip, String addressCountry, String last4, String type, String fingerprint,
            String country) {
        mNumber = TextUtils.nullIfBlank(normalizeCardNumber(number));
        mExpMonth = expMonth;
        mExpYear = expYear;
        mCvc = TextUtils.nullIfBlank(cvc);
        mName = TextUtils.nullIfBlank(name);
        mAddressLine1 = TextUtils.nullIfBlank(addressLine1);
        mAddressLine2 = TextUtils.nullIfBlank(addressLine2);
        mAddressCity = TextUtils.nullIfBlank(addressCity);
        mAddressState = TextUtils.nullIfBlank(addressState);
        mAddressZip = TextUtils.nullIfBlank(addressZip);
        mAddressCountry = TextUtils.nullIfBlank(addressCountry);
        mLast4 = TextUtils.nullIfBlank(last4);
        mType = TextUtils.nullIfBlank(type);
        mFingerprint = TextUtils.nullIfBlank(fingerprint);
        mCountry = TextUtils.nullIfBlank(country);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name,
            String addressLine1, String addressLine2, String addressCity, String addressState,
            String addressZip, String addressCountry) {
        this(number, expMonth, expYear, cvc, name, addressLine1, addressLine2, addressCity,
                addressState, addressZip, addressCountry, null, null, null, null);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc) {
        this(number, expMonth, expYear, cvc, null, null, null, null, null, null, null, null, null,
                null, null);
    }

    public boolean validateCard() {
        if (mCvc == null) {
            return validateNumber() && validateExpiryDate();
        } else {
            return validateNumber() && validateExpiryDate() && validateCVC();
        }
    }

    public boolean validateNumber() {
        if (TextUtils.isBlank(mNumber)) {
            return false;
        }

        String rawNumber = mNumber.trim().replaceAll("\\s+|-", "");
        if (TextUtils.isBlank(rawNumber)
                || !TextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return false;
        }

        if (!validateNumberLength()) {
            return false;
        }

        return true;
    }

    public boolean validateNumberLength() {
        String rawNumber = mNumber.trim().replaceAll("\\s+|-", "");

        String cardType = getType();

        if ("American Express".equals(cardType)) {
            return (rawNumber.length() == 15);
        }

        return (rawNumber.length() == 16);
    }

    public boolean validateExpiryDate() {
        if (!validateExpMonth()) {
            return false;
        }
        if (!validateExpYear()) {
            return false;
        }
        return !DateUtils.hasMonthPassed(mExpYear, mExpMonth);
    }

    public boolean validateExpMonth() {
        if (mExpMonth == null) {
            return false;
        }
        return (mExpMonth >= 1 && mExpMonth <= 12);
    }

    public boolean validateExpYear() {
        if (mExpYear == null) {
            return false;
        }
        return !DateUtils.hasYearPassed(mExpYear);
    }

    public boolean validateCVC() {
        if (TextUtils.isBlank(mCvc)) {
            return false;
        }
        String cvcValue = mCvc.trim();
        String cardType = getType();

        boolean validLength = (
                (cardType == null && cvcValue.length() >= 3 && cvcValue.length() <= 4) ||
                ("American Express".equals(cardType) && cvcValue.length() == 4) ||
                (!"American Express".equals(cardType) && cvcValue.length() == 3));


        if (!TextUtils.isWholePositiveNumber(cvcValue) || !validLength) {
            return false;
        }
        return true;
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

    private String normalizeCardNumber(String number) {
      if (number == null) {
        return null;
      }
      return number.trim().replaceAll("\\s+|-", "");
    }
}