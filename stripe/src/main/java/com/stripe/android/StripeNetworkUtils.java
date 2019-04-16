package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Utility class for static functions useful for networking and data transfer. You probably will
 * not need to call functions from this class in your code.
 */
public class StripeNetworkUtils {

    private static final String MUID = "muid";
    private static final String GUID = "guid";

    @NonNull private final String mPackageName;
    @NonNull private final UidProvider mUidProvider;

    StripeNetworkUtils(@NonNull Context context) {
        this(context.getPackageName(), new UidProvider(context));
    }

    @VisibleForTesting
    StripeNetworkUtils(@NonNull String packageName, @NonNull UidProvider uidProvider) {
        mPackageName = packageName;
        mUidProvider = uidProvider;
    }

    /**
     * A utility function to map the fields of a {@link Card} object into a {@link Map} we
     * can use in network communications.
     *
     * @param card the {@link Card} to be read
     * @return a {@link Map} containing the appropriate values read from the card
     */
    @NonNull
    Map<String, Object> hashMapFromCard(@NonNull Card card) {
        final Map<String, Object> tokenParams = new HashMap<>();

        final AbstractMap<String, Object> cardParams = new HashMap<>();
        cardParams.put("number", StripeTextUtils.nullIfBlank(card.getNumber()));
        cardParams.put("cvc", StripeTextUtils.nullIfBlank(card.getCVC()));
        cardParams.put("exp_month", card.getExpMonth());
        cardParams.put("exp_year", card.getExpYear());
        cardParams.put("name", StripeTextUtils.nullIfBlank(card.getName()));
        cardParams.put("currency", StripeTextUtils.nullIfBlank(card.getCurrency()));
        cardParams.put("address_line1", StripeTextUtils.nullIfBlank(card.getAddressLine1()));
        cardParams.put("address_line2", StripeTextUtils.nullIfBlank(card.getAddressLine2()));
        cardParams.put("address_city", StripeTextUtils.nullIfBlank(card.getAddressCity()));
        cardParams.put("address_zip", StripeTextUtils.nullIfBlank(card.getAddressZip()));
        cardParams.put("address_state", StripeTextUtils.nullIfBlank(card.getAddressState()));
        cardParams.put("address_country", StripeTextUtils.nullIfBlank(card.getAddressCountry()));

        // Remove all null values; they cause validation errors
        removeNullAndEmptyParams(cardParams);

        // We store the logging items in this field, which is extracted from the parameters
        // sent to the API.
        tokenParams.put(LoggingUtils.FIELD_PRODUCT_USAGE, card.getLoggingTokens());

        tokenParams.put(Token.TYPE_CARD, cardParams);

        addUidParams(tokenParams);
        return tokenParams;
    }

    @NonNull
    static Map<String, Object> hashMapFromPersonalId(@NonNull String personalId) {
        final Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("personal_id_number", personalId);
        final Map<String, Object> piiParams = new HashMap<>();
        piiParams.put(Token.TYPE_PII, tokenParams);
        return piiParams;
    }

    @NonNull
    static Map<String, Object> mapFromCvc(@NonNull String cvc) {
        final Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("cvc", cvc);
        final Map<String, Object> cvcParams = new HashMap<>();
        cvcParams.put(Token.TYPE_CVC_UPDATE, tokenParams);
        return cvcParams;
    }

    /**
     * Util function for creating parameters for a bank account.
     *
     * @param bankAccount {@link BankAccount} object used to create the paramters
     * @return a map that can be used as parameters to create a bank account object
     */
    @NonNull
    Map<String, Object> hashMapFromBankAccount(@NonNull BankAccount bankAccount) {
        Map<String, Object> tokenParams = new HashMap<>();
        AbstractMap<String, Object> accountParams = new HashMap<>();

        accountParams.put("country", bankAccount.getCountryCode());
        accountParams.put("currency", bankAccount.getCurrency());
        accountParams.put("account_number", bankAccount.getAccountNumber());
        accountParams.put("routing_number",
                StripeTextUtils.nullIfBlank(bankAccount.getRoutingNumber()));
        accountParams.put("account_holder_name",
                StripeTextUtils.nullIfBlank(bankAccount.getAccountHolderName()));
        accountParams.put("account_holder_type",
                StripeTextUtils.nullIfBlank(bankAccount.getAccountHolderType()));

        // Remove all null values; they cause validation errors
        removeNullAndEmptyParams(accountParams);

        tokenParams.put(Token.TYPE_BANK_ACCOUNT, accountParams);
        addUidParams(tokenParams);
        return tokenParams;
    }

    /**
     * Remove null values from a map. This helps with JSON conversion and validation.
     *
     * @param mapToEdit a {@link Map} from which to remove the keys that have {@code null} values
     */
    @SuppressWarnings("unchecked")
    public static void removeNullAndEmptyParams(@NonNull Map<String, Object> mapToEdit) {
        // Remove all null values; they cause validation errors
        for (String key : new HashSet<>(mapToEdit.keySet())) {
            if (mapToEdit.get(key) == null) {
                mapToEdit.remove(key);
            }

            if (mapToEdit.get(key) instanceof CharSequence) {
                CharSequence sequence = (CharSequence) mapToEdit.get(key);
                if (TextUtils.isEmpty(sequence)) {
                    mapToEdit.remove(key);
                }
            }

            if (mapToEdit.get(key) instanceof Map) {
                Map<String, Object> stringObjectMap = (Map<String, Object>) mapToEdit.get(key);
                removeNullAndEmptyParams(stringObjectMap);
            }
        }
    }

    void addUidParamsToPaymentIntent(@NonNull Map<String, Object> params) {
        if (params.containsKey("source_data") && params.get("source_data") instanceof Map) {
            addUidParams((Map) params.get("source_data"));
        }
    }

    void addUidParams(@NonNull Map<String, Object> params) {
        final String guid = mUidProvider.get();
        if (StripeTextUtils.isBlank(guid)) {
            return;
        }

        String hashGuid = StripeTextUtils.shaHashInput(guid);
        String muid = mPackageName + guid;
        String hashMuid = StripeTextUtils.shaHashInput(muid);

        if (!StripeTextUtils.isBlank(hashGuid)) {
            params.put(GUID, hashGuid);
        }

        if (!StripeTextUtils.isBlank(hashMuid)) {
            params.put(MUID, hashMuid);
        }
    }
}
