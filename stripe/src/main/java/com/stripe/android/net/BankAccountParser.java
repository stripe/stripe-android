package com.stripe.android.net;

import com.stripe.android.model.BankAccount;
import com.stripe.android.util.StripeJsonUtils;
import com.stripe.android.util.StripeTextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for parsing {@link BankAccount} objects.
 */
class BankAccountParser {

    private static final String FIELD_ACCOUNT_HOLDER_NAME = "account_holder_name";
    private static final String FIELD_ACCOUNT_HOLDER_TYPE = "account_holder_type";
    private static final String FIELD_BANK_NAME = "bank_name";
    private static final String FIELD_COUNTRY = "country";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_FINGERPRINT = "fingerprint";
    private static final String FIELD_LAST4 = "last4";
    private static final String FIELD_ROUTING_NUMBER = "routing_number";

    static BankAccount parseBankAccount(String jsonBankAccount) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonBankAccount);
        return parseBankAccount(jsonObject);
    }

    static BankAccount parseBankAccount(JSONObject objectAccount) {
        return new BankAccount(
                StripeJsonUtils.optString(objectAccount, FIELD_ACCOUNT_HOLDER_NAME),
                StripeTextUtils.asBankAccountType(
                        StripeJsonUtils.optString(objectAccount, FIELD_ACCOUNT_HOLDER_TYPE)),
                StripeJsonUtils.optString(objectAccount, FIELD_BANK_NAME),
                StripeJsonUtils.optCountryCode(objectAccount, FIELD_COUNTRY),
                StripeJsonUtils.optCurrency(objectAccount, FIELD_CURRENCY),
                StripeJsonUtils.optString(objectAccount, FIELD_FINGERPRINT),
                StripeJsonUtils.optString(objectAccount, FIELD_LAST4),
                StripeJsonUtils.optString(objectAccount, FIELD_ROUTING_NUMBER));
    }

}
