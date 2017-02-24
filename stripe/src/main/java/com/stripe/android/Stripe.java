package com.stripe.android;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.Executor;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.net.RequestOptions;
import com.stripe.android.net.StripeApiHandler;

import static com.stripe.android.util.StripeNetworkUtils.hashMapFromBankAccount;
import static com.stripe.android.util.StripeNetworkUtils.hashMapFromCard;

/**
 * Class that handles {@link Token} creation from charges and {@link Card} models.
 */
public class Stripe {

    @VisibleForTesting
    TokenCreator mTokenCreator = new TokenCreator() {
        @Override
        public void create(
                final Map<String, Object> tokenParams,
                final String publishableKey,
                final Executor executor,
                final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task =
                    new AsyncTask<Void, Void, ResponseWrapper>() {
                        @Override
                        protected ResponseWrapper doInBackground(Void... params) {
                            try {
                                RequestOptions requestOptions =
                                        RequestOptions.builder(publishableKey).build();
                                Token token = StripeApiHandler.createTokenOnServer(
                                        tokenParams,
                                        requestOptions,
                                        mLoggingResponseListener);
                                return new ResponseWrapper(token, null);
                            } catch (StripeException e) {
                                return new ResponseWrapper(null, e);
                            }
                        }

                        @Override
                        protected void onPostExecute(ResponseWrapper result) {
                            tokenTaskPostExecution(result, callback);
                        }
            };

            executeTokenTask(executor, task);
        }
    };

    private Context mContext;
    private StripeApiHandler.LoggingResponseListener mLoggingResponseListener;
    private String mDefaultPublishableKey;

    /**
     * A constructor with only context, to set the key later.
     *
     * @param context {@link Context} for resolving resources
     */
    public Stripe(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Constructor with publishable key.
     *
     * @param context {@link Context} for resolving resources
     * @param publishableKey the client's publishable key
     * @throws AuthenticationException if the key is invalid
     */
    public Stripe(@NonNull Context context, String publishableKey) throws AuthenticationException {
        mContext = context;
        setDefaultPublishableKey(publishableKey);
    }

    /**
     * The simplest way to create a {@link BankAccount} token. This runs on the default
     * {@link Executor} and with the currently set {@link #mDefaultPublishableKey}.
     *
     * @param bankAccount the {@link BankAccount} used to create this token
     * @param callback a {@link TokenCallback} to receive either the token or an error
     */
    public void createBankAccountToken(
            @NonNull final BankAccount bankAccount,
            @NonNull final TokenCallback callback) {
        createBankAccountToken(bankAccount, mDefaultPublishableKey, null, callback);
    }

    /**
     * Call to create a {@link Token} for a {@link BankAccount} with the publishable key and
     * {@link Executor} specified.
     *
     * @param bankAccount the {@link BankAccount} for which to create a {@link Token}
     * @param publishableKey the publishable key to use
     * @param executor an {@link Executor} to run this operation on. If null, this is run on a
     *                 default non-ui executor
     * @param callback a {@link TokenCallback} to receive the result or error message
     */
    public void createBankAccountToken(
            @NonNull final BankAccount bankAccount,
            @NonNull @Size(min = 1) final String publishableKey,
            @Nullable final Executor executor,
            @NonNull final TokenCallback callback) {
        if (bankAccount == null) {
            throw new RuntimeException(
                    "Required parameter: 'bankAccount' is requred to create a token");
        }

        createTokenFromParams(hashMapFromBankAccount(mContext, bankAccount), publishableKey, executor, callback);
    }

    /**
     * Blocking method to create a {@link Token} for a {@link BankAccount}. Do not call this on
     * the UI thread or your app will crash.
     *
     * This method uses the default publishable key for this {@link Stripe} instance.
     *
     * @param bankAccount the {@link Card} to use for this token
     * @return a {@link Token} that can be used for this {@link BankAccount}
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException should not be thrown with this type of token, but is theoretically
     * possible given the underlying methods called
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    public Token createBankAccountTokenSynchronous(final BankAccount bankAccount)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return createBankAccountTokenSynchronous(bankAccount, mDefaultPublishableKey);
    }

    /**
     * Blocking method to create a {@link Token} using a {@link BankAccount}. Do not call this on
     * the UI thread or your app will crash.
     *
     * @param bankAccount the {@link BankAccount} to use for this token
     * @param publishableKey the publishable key to use with this request
     * @return a {@link Token} that can be used for this {@link BankAccount}
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException should not be thrown with this type of token, but is theoretically
     * possible given the underlying methods called
     * @throws APIException any other type of problem
     */
    public Token createBankAccountTokenSynchronous(
                final BankAccount bankAccount,
                String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        validateKey(publishableKey);
        RequestOptions requestOptions = RequestOptions.builder(publishableKey).build();
        return StripeApiHandler.createTokenOnServer(
                hashMapFromBankAccount(mContext, bankAccount), requestOptions, mLoggingResponseListener);
    }

    /**
     * The simplest way to create a token, using a {@link Card} and {@link TokenCallback}. This
     * runs on the default {@link Executor} and with the
     * currently set {@link #mDefaultPublishableKey}.
     *
     * @param card the {@link Card} used to create this payment token
     * @param callback a {@link TokenCallback} to receive either the token or an error
     */
    public void createToken(@NonNull final Card card, @NonNull final TokenCallback callback) {
        createToken(card, mDefaultPublishableKey, callback);
    }

    /**
     * Call to create a {@link Token} with a specific public key.
     *
     * @param card the {@link Card} used for this transaction
     * @param publishableKey the public key used for this transaction
     * @param callback a {@link TokenCallback} to receive the result of this operation
     */
    public void createToken(
            @NonNull final Card card,
            @NonNull final String publishableKey,
            @NonNull final TokenCallback callback) {
        createToken(card, publishableKey, null, callback);
    }

    /**
     * Call to create a {@link Token} on a specific {@link Executor}.
     * @param card the {@link Card} to use for this token creation
     * @param executor An {@link Executor} on which to run this operation. If you don't wish to
     *                 specify an executor, use one of the other createTokenFromParams methods.
     * @param callback a {@link TokenCallback} to receive the result of this operation
     */
    public void createToken(
            @NonNull final Card card,
            @NonNull final Executor executor,
            @NonNull final TokenCallback callback) {
        createToken(card, mDefaultPublishableKey, executor, callback);
    }

    /**
     * Call to create a {@link Token} with the publishable key and {@link Executor} specified.
     *
     * @param card the {@link Card} used for this token
     * @param publishableKey the publishable key to use
     * @param executor an {@link Executor} to run this operation on. If null, this is run on a
     *                 default non-ui executor
     * @param callback a {@link TokenCallback} to receive the result or error message
     */
    public void createToken(
            @NonNull final Card card,
            @NonNull @Size(min = 1) final String publishableKey,
            @Nullable final Executor executor,
            @NonNull final TokenCallback callback) {
        if (card == null) {
            throw new RuntimeException(
                    "Required Parameter: 'card' is required to create a token");
        }

        createTokenFromParams(hashMapFromCard(mContext, card), publishableKey, executor, callback);
    }

    /**
     * Blocking method to create a {@link Token}. Do not call this on the UI thread or your app
     * will crash. This method uses the default publishable key for this {@link Stripe} instance.
     *
     * @param card the {@link Card} to use for this token
     * @return a {@link Token} that can be used for this card
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    public Token createTokenSynchronous(final Card card)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return createTokenSynchronous(card, mDefaultPublishableKey);
    }

    /**
     * Blocking method to create a {@link Token}. Do not call this on the UI thread or your app
     * will crash.
     *
     * @param card the {@link Card} to use for this token
     * @param publishableKey the publishable key to use with this request
     * @return a {@link Token} that can be used for this card
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Token createTokenSynchronous(final Card card, String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        validateKey(publishableKey);

        RequestOptions requestOptions = RequestOptions.builder(publishableKey).build();
        return StripeApiHandler.createTokenOnServer(
                hashMapFromCard(mContext, card),
                requestOptions,
                mLoggingResponseListener);
    }

    /**
     * Set the default publishable key to use with this {@link Stripe} instance.
     *
     * @param publishableKey the key to be set
     * @throws AuthenticationException if the key is null, empty, or a secret key
     */
    public void setDefaultPublishableKey(@NonNull @Size(min = 1) String publishableKey)
            throws AuthenticationException {
        validateKey(publishableKey);
        this.mDefaultPublishableKey = publishableKey;
    }

    @VisibleForTesting
    void setLoggingResponseListener(StripeApiHandler.LoggingResponseListener listener) {
        mLoggingResponseListener = listener;
    }

    private void createTokenFromParams(
            @NonNull final Map<String, Object> tokenParams,
            @NonNull @Size(min = 1) final String publishableKey,
            @Nullable final Executor executor,
            @NonNull final TokenCallback callback) {
        try {
            if (callback == null) {
                throw new RuntimeException(
                        "Required Parameter: 'callback' is required to use the created " +
                                "token and handle errors");
            }

            validateKey(publishableKey);
            mTokenCreator.create(tokenParams, publishableKey, executor, callback);
        }
        catch (AuthenticationException e) {
            callback.onError(e);
        }
    }

    private void validateKey(@NonNull @Size(min = 1) String publishableKey)
            throws AuthenticationException {
        if (publishableKey == null || publishableKey.length() == 0) {
            throw new AuthenticationException("Invalid Publishable Key: " +
                    "You must use a valid publishable key to create a token.  " +
                    "For more info, see https://stripe.com/docs/stripe.js.", null, 0);
        }

        if (publishableKey.startsWith("sk_")) {
            throw new AuthenticationException("Invalid Publishable Key: " +
                    "You are using a secret key to create a token, " +
                    "instead of the publishable one. For more info, " +
                    "see https://stripe.com/docs/stripe.js", null, 0);
        }
    }

    private void tokenTaskPostExecution(ResponseWrapper result, TokenCallback callback) {
        if (result.token != null) {
            callback.onSuccess(result.token);
        }
        else if (result.error != null) {
            callback.onError(result.error);
        }
        else {
            callback.onError(new RuntimeException(
                    "Somehow got neither a token response or an error response"));
        }
    }

    private void executeTokenTask(Executor executor, AsyncTask<Void, Void, ResponseWrapper> task) {
        if (executor != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(executor);
        } else {
            task.execute();
        }
    }

    private class ResponseWrapper {
        final Token token;
        final Exception error;

        private ResponseWrapper(Token token, Exception error) {
            this.error = error;
            this.token = token;
        }
    }

    @VisibleForTesting
    interface TokenCreator {
        void create(Map<String, Object> params,
                    String publishableKey,
                    Executor executor,
                    TokenCallback callback);
    }
}
