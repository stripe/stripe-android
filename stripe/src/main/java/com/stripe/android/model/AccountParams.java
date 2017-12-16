package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;

/**
 * Represents a grouping of parameters needed to create a Token for a Connect account on the server.
 */
public class AccountParams {

    static final String API_PARAM_LEGAL_ENTITY = "legal_entity";
    static final String API_TOS_SHOWN_AND_ACCEPTED = "tos_shown_and_accepted";
    private Boolean mTosShownAndAccepted;
    private Map<String, Object> mLegalEntity;

    /**
     * @param tosShownAndAccepted indicates that the platform showed the user the appropriate text
     *                            and links to Stripe's terms of service. Tokens will only generated
     *                            when this is true.
     * @param legalEntity map that specifies the legal entity for which the connect account is being
     *                    created. Can contain any of the fields specified by legal_entity in the
     *                    API docs.
     *
     *                    See {@linktourl https://stripe.com/docs/api#account_object-legal_entity}
     *
     *                    The object in the map is expected to be a string or a list or map of
     *                    strings. All {@link StripeJsonModel} types have a toMap() function that
     *                    can be used to convert the {@link StripeJsonModel} to map representation
     *                    that can be passed in here.
     */
    public static AccountParams createAccountParams(
            boolean tosShownAndAccepted,
            Map<String, Object> legalEntity) {
        AccountParams accountParams = new AccountParams()
                .setTosShownAndAccepted(tosShownAndAccepted)
                .setLegalEntity(legalEntity);
        return accountParams;
    }

    /**
     * @param tosShownAndAccepted whether the platform showed the user the appropriate text
     *                            and links to Stripe's terms of service. Tokens will only generated
     *                            when this is true.
     * @return {@code this}, for chaining purposes
     */
    public AccountParams setTosShownAndAccepted(boolean tosShownAndAccepted) {
        mTosShownAndAccepted = tosShownAndAccepted;
        return this;
    }

    /**
     * @param legalEntity map that specifies the legal entity for which the connect account is being
     *                    created. Can contain any of the fields specified by legal_entity in the
     *                    API docs.
     *
     *                    See {@linktourl https://stripe.com/docs/api#account_object-legal_entity}
     *
     *                    The object in the map is expected to be a string or a list or map of
     *                    strings. All {@link StripeJsonModel} types have a toMap() function that
     *                    can be used to convert the {@link StripeJsonModel} to map representation
     *                    that can be passed in here.
     * @return {@code this}, for chaining purposes
     */
    public AccountParams setLegalEntity(Map<String, Object> legalEntity) {
        mLegalEntity = legalEntity;
        return this;
    }

    /**
     * Create a string-keyed map representing this object that is
     * ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    public Map<String, Object> toParamMap() {
        Map<String, Object> networkReadyMap = new HashMap<>();
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(API_TOS_SHOWN_AND_ACCEPTED, mTosShownAndAccepted);
        tokenMap.put(API_PARAM_LEGAL_ENTITY, mLegalEntity);
        networkReadyMap.put("account", tokenMap);
        removeNullAndEmptyParams(networkReadyMap);
        return networkReadyMap;
    }

}
