package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a grouping of parameters needed to create a Token for a Connect account on the server.
 */
public final class AccountParams implements StripeParamsModel {

    static final String API_BUSINESS_TYPE = "business_type";
    static final String API_TOS_SHOWN_AND_ACCEPTED = "tos_shown_and_accepted";

    private final boolean mTosShownAndAccepted;
    @Nullable private final BusinessType mBusinessType;
    @Nullable private final Map<String, Object> mBusinessData;

    /**
     * Create an {@link AccountParams} instance for a {@link BusinessType#Individual} or
     * {@link BusinessType#Company}.
     *
     * @param tosShownAndAccepted Whether the user described by the data in the token has been shown
     *                            the <a href="https://stripe.com/docs/api/tokens/create_account#create_account_token-account-tos_shown_and_accepted">Stripe Connected Account Agreement</a>.
     *                            When creating an account token to create a new Connect account,
     *                            this value must be <code>true</code>.
     * @param businessType        See {@link BusinessType}
     * @param businessData        A map of <a href="https://stripe.com/docs/api/accounts/create#create_account-company">company</a>
     *                            or <a href="https://stripe.com/docs/api/accounts/create#create_account-individual">individual</a> params.
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
    @Override
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
        return params;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTosShownAndAccepted, mBusinessType, mBusinessData);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof AccountParams && typedEquals((AccountParams) obj));
    }

    private boolean typedEquals(@NonNull AccountParams accountParams) {
        return Objects.equals(mTosShownAndAccepted, accountParams.mTosShownAndAccepted)
                && Objects.equals(mBusinessType, accountParams.mBusinessType)
                && Objects.equals(mBusinessData, accountParams.mBusinessData);
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
