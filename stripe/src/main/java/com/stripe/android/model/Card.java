package com.stripe.android.model;

import com.stripe.android.util.DateUtils;
import com.stripe.android.util.TextUtils;

public class Card extends com.stripe.model.StripeObject {
    String number;
    String cvc;
    Integer expMonth;
    Integer expYear;
    String name;
    String addressLine1;
    String addressLine2;
    String addressCity;
    String addressState;
    String addressZip;
    String addressCountry;
    String last4;
    String type;
    String fingerprint;
    String country;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCVC() {
        return cvc;
    }

    public void setCVC(String cvc) {
        this.cvc = cvc;
    }

    public Integer getExpMonth() {
        return expMonth;
    }

    public void setExpMonth(Integer expMonth) {
        this.expMonth = expMonth;
    }

    public Integer getExpYear() {
        return expYear;
    }

    public void setExpYear(Integer expYear) {
        this.expYear = expYear;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressZip() {
        return addressZip;
    }

    public void setAddressZip(String addressZip) {
        this.addressZip = addressZip;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getLast4() {
        if (!TextUtils.isBlank(last4)) {
            return last4;
        }
        if (number != null && number.length() > 4) {
            return number.substring(number.length() - 4, number.length());
        }
        return null;
    }

    public String getType() {
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

    public String getFingerprint() {
        return fingerprint;
    }

    public String getCountry() {
        return country;
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity, String addressState, String addressZip, String addressCountry, String last4, String type, String fingerprint, String country) {
        this.number = TextUtils.nullIfBlank(normalizeCardNumber(number));
        this.expMonth = expMonth;
        this.expYear = expYear;
        this.cvc = TextUtils.nullIfBlank(cvc);
        this.name = TextUtils.nullIfBlank(name);
        this.addressLine1 = TextUtils.nullIfBlank(addressLine1);
        this.addressLine2 = TextUtils.nullIfBlank(addressLine2);
        this.addressCity = TextUtils.nullIfBlank(addressCity);
        this.addressState = TextUtils.nullIfBlank(addressState);
        this.addressZip = TextUtils.nullIfBlank(addressZip);
        this.addressCountry = TextUtils.nullIfBlank(addressCountry);
        this.last4 = TextUtils.nullIfBlank(last4);
        this.type = TextUtils.nullIfBlank(type);
        this.fingerprint = TextUtils.nullIfBlank(fingerprint);
        this.country = TextUtils.nullIfBlank(country);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc, String name, String addressLine1, String addressLine2, String addressCity, String addressState, String addressZip, String addressCountry) {
        this(number, expMonth, expYear, cvc, name, addressLine1, addressLine2, addressCity, addressState, addressZip, addressCountry, null, null, null, null);
    }

    public Card(String number, Integer expMonth, Integer expYear, String cvc) {
        this(number, expMonth, expYear, cvc, null, null, null, null, null, null, null, null, null, null, null);
        this.type = getType();
    }

    public boolean validateCard() {
        if (cvc == null) {
            return validateNumber() && validateExpiryDate();
        } else {
            return validateNumber() && validateExpiryDate() && validateCVC();
        }
    }

    public boolean validateNumber() {
        if (TextUtils.isBlank(number)) {
            return false;
        }

        String rawNumber = number.trim().replaceAll("\\s+|-", "");
        if (TextUtils.isBlank(rawNumber)
                || !TextUtils.isWholePositiveNumber(rawNumber)
                || !isValidLuhnNumber(rawNumber)) {
            return false;
        }

        if (!"American Express".equals(type) && rawNumber.length() != 16) {
        	return false;
        }

        if ("American Express".equals(type) && rawNumber.length() != 15) {
        	return false;
        }

        return true;
    }

    public boolean validateExpiryDate() {
    	if (!validateExpMonth()) {
    		return false;
    	}
    	if (!validateExpYear()) {
    		return false;
    	}
    	return !DateUtils.hasMonthPassed(expYear, expMonth);
    }

    public boolean validateExpMonth() {
    	if (expMonth == null) {
    		return false;
    	}
    	return (expMonth >= 1 && expMonth <= 12);
    }

    public boolean validateExpYear() {
    	if (expYear == null) {
    		return false;
    	}
    	return !DateUtils.hasYearPassed(expYear);
    }

    public boolean validateCVC() {
        if (TextUtils.isBlank(cvc)) {
            return false;
        }
        String cvcValue = cvc.trim();

        boolean validLength = ((type == null && cvcValue.length() >= 3 && cvcValue.length() <= 4) ||
                ("American Express".equals(type) && cvcValue.length() == 4) ||
                (!"American Express".equals(type) && cvcValue.length() == 3));


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