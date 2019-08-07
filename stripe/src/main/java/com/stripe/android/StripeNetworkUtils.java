package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.ConfirmPaymentIntentParams;
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

    private static final String FIELD_MUID = "muid";
    private static final String FIELD_GUID = "guid";

    @NonNull private final String mPackageName;
    @NonNull private final Supplier<StripeUid> mUidSupplier;

    StripeNetworkUtils(@NonNull Context context) {
        this(context.getPackageName(), new UidSupplier(context));
    }

    @VisibleForTesting
    StripeNetworkUtils(@NonNull String packageName, @NonNull Supplier<StripeUid> uidSupplier) {
        mPackageName = packageName;
        mUidSupplier = uidSupplier;
    }

    /**
     * A utility function to map the fields of a {@link Card} object into a {@link Map} we
     * can use in network communications.
     *
     * @param card the {@link Card} to be read
     * @return a {@link Map} containing the appropriate values read from the card
     */
    @NonNull
    Map<String, Object> createCardTokenParams(@NonNull Card card) {
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
        tokenParams.put(AnalyticsDataFactory.FIELD_PRODUCT_USAGE, card.getLoggingTokens());

        tokenParams.put(Token.TokenType.CARD, cardParams);
        tokenParams.putAll(createUidParams());

        return tokenParams;
    }

    @NonNull
    static Map<String, Object> createPersonalIdTokenParams(@NonNull String personalId) {
        final Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("personal_id_number", personalId);
        final Map<String, Object> piiParams = new HashMap<>();
        piiParams.put(Token.TokenType.PII, tokenParams);
        return piiParams;
    }

    @NonNull
    static Map<String, Object> createUpdateCvcTokenParams(@NonNull String cvc) {
        final Map<String, Object> tokenParams = new HashMap<>();
        tokenParams.put("cvc", cvc);
        final Map<String, Object> cvcParams = new HashMap<>();
        cvcParams.put(Token.TokenType.CVC_UPDATE, tokenParams);
        return cvcParams;
    }

    /**
     * Util function for creating parameters for a bank account.
     *
     * @param bankAccount {@link BankAccount} object used to create the paramters
     * @return a map that can be used as parameters to create a bank account object
     */
    @NonNull
    Map<String, Object> createBankAccountTokenParams(@NonNull BankAccount bankAccount) {
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

        tokenParams.put(Token.TokenType.BANK_ACCOUNT, accountParams);
        tokenParams.putAll(createUidParams());
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
                if (StripeTextUtils.isEmpty(sequence)) {
                    mapToEdit.remove(key);
                }
            }

            if (mapToEdit.get(key) instanceof Map) {
                final Map<String, Object> stringObjectMap =
                        (Map<String, Object>) mapToEdit.get(key);
                if (stringObjectMap != null) {
                    removeNullAndEmptyParams(stringObjectMap);
                }
            }
        }
    }

    void addUidToConfirmPaymentIntentParams(
            @NonNull Map<String, ?> confirmPaymentIntentParams) {
        final Object sourceData =
                confirmPaymentIntentParams.get(ConfirmPaymentIntentParams.API_PARAM_SOURCE_DATA);
        if (sourceData instanceof Map) {
            //noinspection unchecked
            ((Map<String, Object>) sourceData).putAll(createUidParams());
        } else {
            final Object paymentMethodData = confirmPaymentIntentParams
                    .get(ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_DATA);
            if (paymentMethodData instanceof Map) {
                //noinspection unchecked
                ((Map) paymentMethodData).putAll(createUidParams());
            }
        }
    }

    @NonNull
    Map<String, String> createUidParams() {
        final String guid = mUidSupplier.get().value;
        if (StripeTextUtils.isBlank(guid)) {
            return new HashMap<>();
        }

        final Map<String, String> uidParams = new HashMap<>(2);
        final String hashGuid = StripeTextUtils.shaHashInput(guid);
        if (!StripeTextUtils.isBlank(hashGuid)) {
            uidParams.put(FIELD_GUID, hashGuid);
        }

        final String muid = mPackageName + guid;
        final String hashMuid = StripeTextUtils.shaHashInput(muid);
        if (!StripeTextUtils.isBlank(hashMuid)) {
            uidParams.put(FIELD_MUID, hashMuid);
        }

        return uidParams;
    }
}
