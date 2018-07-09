package com.stripe.android;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

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

    /**
     * A utility function to map the fields of a {@link Card} object into a {@link Map} we
     * can use in network communications.
     *
     * @param context the {@link Context} used to resolve resources
     * @param card the {@link Card} to be read
     * @return a {@link Map} containing the appropriate values read from the card
     */
    @NonNull
    static Map<String, Object> hashMapFromCard(@NonNull Context context, Card card) {
        return hashMapFromCard(null, context, card);
    }

    @NonNull
    static Map<String, Object> hashMapFromPersonalId(@NonNull Context context,
                                                            @NonNull String personalId) {
        Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("personal_id_number", personalId);
        Map<String, Object> piiParams = new HashMap<>();
        piiParams.put(Token.TYPE_PII, tokenParams);
        return piiParams;
    }

    @NonNull
    private static Map<String, Object> hashMapFromCard(
            @Nullable UidProvider provider,
            @NonNull Context context,
            Card card) {
        Map<String, Object> tokenParams = new HashMap<>();

        Map<String, Object> cardParams = new HashMap<>();
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

        addUidParams(provider, context, tokenParams);
        return tokenParams;
    }

    /**
     * Util function for creating parameters for a bank account.
     *
     * @param context {@link Context} used to determine resources
     * @param bankAccount {@link BankAccount} object used to create the paramters
     * @return a map that can be used as parameters to create a bank account object
     */
    @NonNull
    static Map<String, Object> hashMapFromBankAccount(@NonNull Context context,
                                                             @NonNull BankAccount bankAccount) {
        return hashMapFromBankAccount(null, context, bankAccount);
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

    static void addUidParamsToPaymentIntent(@Nullable UidProvider provider,
                                            @NonNull Context context,
                                            @NonNull Map<String, Object> params) {
        if (params.containsKey("source_data") && params.get("source_data") instanceof Map) {
            addUidParams(provider, context, (Map) params.get("source_data"));
        }
    }

    @SuppressWarnings("HardwareIds")
    static void addUidParams(
            @Nullable UidProvider provider,
            @NonNull Context context,
            @NonNull Map<String, Object> params) {
        String guid =
                provider == null
                        ? Settings.Secure.getString(context.getContentResolver(),
                        Settings.Secure.ANDROID_ID)
                        : provider.getUid();

        if (StripeTextUtils.isBlank(guid)) {
            return;
        }

        String hashGuid = StripeTextUtils.shaHashInput(guid);
        String muid =
                provider == null
                        ? context.getApplicationContext().getPackageName() + guid
                        : provider.getPackageName() + guid;
        String hashMuid = StripeTextUtils.shaHashInput(muid);

        if (!StripeTextUtils.isBlank(hashGuid)) {
            params.put(GUID, hashGuid);
        }

        if (!StripeTextUtils.isBlank(hashMuid)) {
            params.put(MUID, hashMuid);
        }
    }

    @NonNull
    private static Map<String, Object> hashMapFromBankAccount(
            @Nullable UidProvider provider,
            @NonNull Context context,
            @NonNull BankAccount bankAccount) {
        Map<String, Object> tokenParams = new HashMap<>();
        Map<String, Object> accountParams = new HashMap<>();

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
        addUidParams(provider, context, tokenParams);
        return tokenParams;
    }

    @VisibleForTesting
    interface UidProvider {
        String getUid();

        String getPackageName();
    }
}
