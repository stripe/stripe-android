package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;

/**
 * Represents a grouping of parameters needed to create a Token for a Connect account on the server.
 */
public class AccountParams {

    private static final String API_PARAM_LEGAL_ENTITY = "legal_entity";
    private static final String API_TOS_SHOWN_AND_ACCEPTED = "tos_shown_and_accepted";

    @Nullable private Boolean mTosShownAndAccepted;
    @Nullable private final BusinessType mBusinessType;
    @Nullable private Map<String, Object> mBusinessData;

    private AccountParams(@Nullable BusinessType businessType) {
        mBusinessType = businessType;
    }

    /**
     * @param tosShownAndAccepted indicates that the platform showed the user the appropriate text
     *                            and links to Stripe's terms of service. Tokens will only generated
     *                            when this is true.
     *
     * @param legalEntity         Map that specifies the legal entity for which the connect account
     *                            is being created. Can contain any of the fields specified by
     *                            legal_entity in the API docs.
     *
     *                            See https://stripe.com/docs/api/accounts/create
     *
     *                            The object in the map is expected to be a string or a list or map
     *                            of strings. All {@link StripeJsonModel} types have a toMap()
     *                            function that can be used to convert the {@link StripeJsonModel}
     *                            to map representation that can be passed in here.
     */
    @NonNull
    public static AccountParams createAccountParams(
            boolean tosShownAndAccepted,
            @Nullable Map<String, Object> legalEntity) {
        return new AccountParams(null)
                .setTosShownAndAccepted(tosShownAndAccepted)
                .setLegalEntity(legalEntity);
    }

    /**
     * Create an {@link AccountParams} instance for a {@link BusinessType#Individual} or
     * {@link BusinessType#Company}
     *
     * Note: API version {@code 2019-02-19} [0] replaced {@code legal_entity} with
     * {@code individual} and {@code company}.
     *
     * @param tosShownAndAccepted Indicates that the platform showed the user the appropriate text
     *                            and links to Stripe's terms of service. Tokens will only generated
     *                            when this is true.
     * @param businessType        See {@link BusinessType}
     * @param businessParams      A map of company [1] or individual [2] params.
     *
     * [0] <a href="https://stripe.com/docs/upgrades#2019-02-19">
     *     https://stripe.com/docs/upgrades#2019-02-19</a>
     * [1] <a href="https://stripe.com/docs/api/accounts/create#create_account-company">
     *     https://stripe.com/docs/api/accounts/create#create_account-company</a>
     * [2] <a href="https://stripe.com/docs/api/accounts/create#create_account-individual">
     *     https://stripe.com/docs/api/accounts/create#create_account-individual</a>
     *
     * @return {@link AccountParams}
     */
    @NonNull
    public static AccountParams createAccountParams(
            boolean tosShownAndAccepted,
            @NonNull BusinessType businessType,
            @Nullable Map<String, Object> businessParams) {
        return new AccountParams(businessType)
                .setTosShownAndAccepted(tosShownAndAccepted)
                .setLegalEntity(businessParams);
    }

    /**
     * @param tosShownAndAccepted whether the platform showed the user the appropriate text and
     *                            links to Stripe's terms of service. Tokens will only generated
     *                            when this is true.
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public AccountParams setTosShownAndAccepted(boolean tosShownAndAccepted) {
        mTosShownAndAccepted = tosShownAndAccepted;
        return this;
    }

    /**
     * @param legalEntity see documentation on {@link #createAccountParams}
     * @return {@code this}, for chaining purposes
     */
    @NonNull
    public AccountParams setLegalEntity(@Nullable Map<String, Object> legalEntity) {
        mBusinessData = legalEntity;
        return this;
    }

    /**
     * Create a string-keyed map representing this object that is ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    public Map<String, Object> toParamMap() {
        final Map<String, Object> networkReadyMap = new HashMap<>();
        final Map<String, Object> tokenMap = new HashMap<>();
        if (mTosShownAndAccepted != null) {
            tokenMap.put(API_TOS_SHOWN_AND_ACCEPTED, mTosShownAndAccepted);
        }

        if (mBusinessData != null) {
            if (mBusinessType != null) {
                tokenMap.put(mBusinessType.code, mBusinessData);
            } else {
                tokenMap.put(API_PARAM_LEGAL_ENTITY, mBusinessData);
            }
        }

        networkReadyMap.put("account", tokenMap);
        removeNullAndEmptyParams(networkReadyMap);
        return networkReadyMap;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mTosShownAndAccepted, mBusinessType, mBusinessData);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof AccountParams && typedEquals((AccountParams) obj));
    }

    private boolean typedEquals(@NonNull AccountParams accountParams) {
        return ObjectUtils.equals(mTosShownAndAccepted, accountParams.mTosShownAndAccepted)
                && ObjectUtils.equals(mBusinessType, accountParams.mBusinessType)
                && ObjectUtils.equals(mBusinessData, accountParams.mBusinessData);
    }

    /**
     * See <a href="https://stripe.com/docs/api/accounts/create#create_account-business_type">
     *     Account creation API docs</a>
     */
    public enum BusinessType {
        Individual("individual"),
        Company("company");

        @NonNull public final String code;

        BusinessType(@NonNull String code) {
            this.code = code;
        }
    }
}
