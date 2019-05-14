package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

import static com.stripe.android.StripeNetworkUtils.removeNullAndEmptyParams;

/**
 * Represents a grouping of parameters needed to create a Token for a Connect account on the server.
 */
public class AccountParams {

    static final String API_BUSINESS_TYPE = "business_type";
    static final String API_TOS_SHOWN_AND_ACCEPTED = "tos_shown_and_accepted";

    private final boolean mTosShownAndAccepted;
    @Nullable private final BusinessType mBusinessType;
    @Nullable private final Map<String, Object> mBusinessData;

    /**
     * Create an {@link AccountParams} instance for a {@link BusinessType#Individual} or
     * {@link BusinessType#Company}
     *
     * Note: API version {@code 2019-02-19} [0] replaced {@code legal_entity} with
     * {@code individual} and {@code company}.
     *
     * @param tosShownAndAccepted Whether the user described by the data in the token has been shown
     *                            the Stripe Connected Account Agreement [1]. When creating an
     *                            account token to create a new Connect account, this value must
     *                            be true.
     * @param businessType        See {@link BusinessType}
     * @param businessData        A map of company [2] or individual [3] params.
     *
     * [0] <a href="https://stripe.com/docs/upgrades#2019-02-19">
     *     https://stripe.com/docs/upgrades#2019-02-19</a>
     * [1] https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted
     * [2] <a href="https://stripe.com/docs/api/accounts/create#create_account-company">
     *     https://stripe.com/docs/api/accounts/create#create_account-company</a>
     * [3] <a href="https://stripe.com/docs/api/accounts/create#create_account-individual">
     *     https://stripe.com/docs/api/accounts/create#create_account-individual</a>
     *
     *
     * @return {@link AccountParams}
     */
    @NonNull
    public static AccountParams createAccountParams(
            boolean tosShownAndAccepted,
            @Nullable BusinessType businessType,
            @Nullable Map<String, Object> businessData) {
        return new AccountParams(businessType, businessData, tosShownAndAccepted);
    }

    private AccountParams(@Nullable BusinessType businessType,
                          @Nullable Map<String, Object> businessData,
                          boolean tosShownAndAccepted) {
        mBusinessType = businessType;
        mBusinessData = businessData;
        mTosShownAndAccepted = tosShownAndAccepted;
    }

    /**
     * Create a string-keyed map representing this object that is ready to be sent over the network.
     *
     * @return a String-keyed map
     */
    @NonNull
    public Map<String, Object> toParamMap() {
        final Map<String, Object> accountData = new HashMap<>();
        if (mBusinessType != null) {
            accountData.put(API_BUSINESS_TYPE, mBusinessType.code);

            if (mBusinessData != null) {
                accountData.put(mBusinessType.code, mBusinessData);
            }
        }
        accountData.put(API_TOS_SHOWN_AND_ACCEPTED, mTosShownAndAccepted);

        final Map<String, Object> params = new HashMap<>();
        params.put("account", accountData);
        removeNullAndEmptyParams(params);
        return params;
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
