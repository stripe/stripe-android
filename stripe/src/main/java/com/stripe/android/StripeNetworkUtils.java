package com.stripe.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.stripe.android.model.Card;
import com.stripe.android.model.ConfirmPaymentIntentParams;

import java.util.Map;

/**
 * Utility class for static functions useful for networking and data transfer.
 */
final class StripeNetworkUtils {
    @NonNull private final UidParamsFactory mUidParamsFactory;

    StripeNetworkUtils(@NonNull Context context) {
        this(
                UidParamsFactory.create(context)
        );
    }

    @VisibleForTesting
    StripeNetworkUtils(
            @NonNull UidParamsFactory uidParamsFactory
    ) {
        mUidParamsFactory = uidParamsFactory;
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
        final Map<String, Object> tokenParams = card.toParamMap();

        // We store the logging items in this field, which is extracted from the parameters
        // sent to the API.
        tokenParams.put(AnalyticsDataFactory.FIELD_PRODUCT_USAGE, card.getLoggingTokens());
        tokenParams.putAll(mUidParamsFactory.create());

        return tokenParams;
    }

    void addUidToConfirmPaymentIntentParams(
            @NonNull Map<String, ?> confirmPaymentIntentParams) {
        final Object sourceData =
                confirmPaymentIntentParams.get(ConfirmPaymentIntentParams.API_PARAM_SOURCE_DATA);
        if (sourceData instanceof Map) {
            //noinspection unchecked
            ((Map<String, Object>) sourceData).putAll(mUidParamsFactory.create());
        } else {
            final Object paymentMethodData = confirmPaymentIntentParams
                    .get(ConfirmPaymentIntentParams.API_PARAM_PAYMENT_METHOD_DATA);
            if (paymentMethodData instanceof Map) {
                //noinspection unchecked
                ((Map) paymentMethodData).putAll(mUidParamsFactory.create());
            }
        }
    }

    @NonNull
    Map<String, String> createUidParams() {
        return mUidParamsFactory.create();
    }
}
