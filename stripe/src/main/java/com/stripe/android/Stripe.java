package com.stripe.android;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.AccountParams;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.StripePaymentSource;
import com.stripe.android.model.Token;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.stripe.android.StripeNetworkUtils.hashMapFromBankAccount;
import static com.stripe.android.StripeNetworkUtils.hashMapFromCard;
import static com.stripe.android.StripeNetworkUtils.hashMapFromPersonalId;

/**
 * Class that handles {@link Token} creation from charges, {@link Card}, and accounts.
 */
public class Stripe {

    SourceCreator mSourceCreator = new SourceCreator() {
        @Override
        public void create(
                @NonNull final SourceParams sourceParams,
                @NonNull final String publishableKey,
                @Nullable final String stripeAccount,
                @Nullable Executor executor,
                @NonNull final SourceCallback sourceCallback) {
            AsyncTask<Void, Void, ResponseWrapper> task =
                    new AsyncTask<Void, Void, ResponseWrapper>() {
                        @Override
                        protected ResponseWrapper doInBackground(Void... params) {
                            try {
                                Source source = StripeApiHandler.createSource(
                                        null,
                                        mContext,
                                        sourceParams,
                                        publishableKey,
                                        stripeAccount,
                                        null);
                                return new ResponseWrapper(source);
                            } catch (StripeException stripeException) {
                                return new ResponseWrapper(stripeException);
                            }
                        }

                        @Override
                        protected void onPostExecute(ResponseWrapper responseWrapper) {
                            if (responseWrapper.source != null) {
                                sourceCallback.onSuccess(responseWrapper.source);
                            } else if (responseWrapper.error != null) {
                                sourceCallback.onError(responseWrapper.error);
                            }
                        }
                    };

            executeTask(executor, task);
        }
    };

    @VisibleForTesting
    TokenCreator mTokenCreator = new TokenCreator() {
        @Override
        public void create(
                final Map<String, Object> tokenParams,
                final String publishableKey,
                final String stripeAccount,
                final @NonNull @Token.TokenType String tokenType,
                final Executor executor,
                final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task =
                    new AsyncTask<Void, Void, ResponseWrapper>() {
                        @Override
                        protected ResponseWrapper doInBackground(Void... params) {
                            try {
                                RequestOptions requestOptions =
                                        RequestOptions.builder(
                                                publishableKey,
                                                stripeAccount,
                                                RequestOptions.TYPE_QUERY).build();
                                Token token = StripeApiHandler.createToken(
                                        mContext,
                                        tokenParams,
                                        requestOptions,
                                        tokenType,
                                        mLoggingResponseListener);
                                return new ResponseWrapper(token);
                            } catch (StripeException e) {
                                return new ResponseWrapper(e);
                            }
                        }

                        @Override
                        protected void onPostExecute(ResponseWrapper result) {
                            tokenTaskPostExecution(result, callback);
                        }
            };

            executeTask(executor, task);
        }
    };

    private Context mContext;
    private StripeApiHandler.LoggingResponseListener mLoggingResponseListener;
    private String mDefaultPublishableKey;
    private String mStripeAccount;

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
     */
    public Stripe(@NonNull Context context, String publishableKey) {
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

        createTokenFromParams(
                hashMapFromBankAccount(mContext, bankAccount),
                publishableKey,
                Token.TYPE_BANK_ACCOUNT,
                executor,
                callback);
    }

    /**
     * The simplest way to create a PII token. This runs on the default
     * {@link Executor} and with the currently set {@link #mDefaultPublishableKey}.
     *
     * @param personalId the personal id used to create this token
     * @param callback a {@link TokenCallback} to receive either the token or an error
     */
    public void createPiiToken(
            @NonNull final String personalId,
            @NonNull final TokenCallback callback) {
        createPiiToken(personalId, mDefaultPublishableKey, null, callback);
    }

    /**
     * Call to create a {@link Token} for PII with the publishable key and
     * {@link Executor} specified.
     *
     * @param personalId the personal id used to create this token
     * @param publishableKey the publishable key to use
     * @param executor an {@link Executor} to run this operation on. If null, this is run on a
     *                 default non-ui executor
     * @param callback a {@link TokenCallback} to receive the result or error message
     */
    public void createPiiToken(
            @NonNull final String personalId,
            @NonNull @Size(min = 1) final String publishableKey,
            @Nullable final Executor executor,
            @NonNull final TokenCallback callback) {
        createTokenFromParams(
                hashMapFromPersonalId(mContext, personalId),
                publishableKey,
                Token.TYPE_PII,
                executor,
                callback);
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
        RequestOptions requestOptions = RequestOptions.builder(
                publishableKey,
                mStripeAccount,
                RequestOptions.TYPE_QUERY).build();
        return StripeApiHandler.createToken(
                mContext,
                hashMapFromBankAccount(mContext, bankAccount),
                requestOptions,
                Token.TYPE_BANK_ACCOUNT,
                mLoggingResponseListener);
    }

    /**
     * Create a {@link Source} using an {@link AsyncTask} on the default {@link Executor} with a
     * publishable api key that has already been set on this {@link Stripe} instance.
     *
     * @param sourceParams the {@link SourceParams} to be used
     * @param callback a {@link SourceCallback} to receive a result or an error message
     */
    public void createSource(@NonNull SourceParams sourceParams, @NonNull SourceCallback callback) {
        createSource(sourceParams, callback, null, null);
    }

    /**
     * Create a {@link Source} using an {@link AsyncTask}.
     *
     * @param sourceParams the {@link SourceParams} to be used
     * @param callback a {@link SourceCallback} to receive a result or an error message
     * @param publishableKey the publishable api key to be used
     * @param executor an {@link Executor} on which to execute the task, or {@link null} for default
     */
    public void createSource(
            @NonNull SourceParams sourceParams,
            @NonNull SourceCallback callback,
            @Nullable String publishableKey,
            @Nullable Executor executor) {
        String apiKey = publishableKey == null ? mDefaultPublishableKey : publishableKey;
        if (apiKey == null) {
            return;
        }
        mSourceCreator.create(sourceParams, apiKey, mStripeAccount, executor, callback);
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

        createTokenFromParams(
                hashMapFromCard(mContext, card),
                publishableKey,
                Token.TYPE_CARD,
                executor,
                callback);
    }

    /**
     * Blocking method to create a {@link Source} object using this object's
     * {@link Stripe#mDefaultPublishableKey key}.
     *
     * Do not call this on the UI thread or your app will crash.
     *
     * @param params a set of {@link SourceParams} with which to create the source
     * @return a {@link Source}, or {@code null} if a problem occurred
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws CardException the card cannot be charged for some reason
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    @Nullable
    public Source createSourceSynchronous(@NonNull SourceParams params)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return createSourceSynchronous(params, null);
    }

    /**
     * Blocking method to create a {@link Source} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * @param params a set of {@link SourceParams} with which to create the source
     * @param publishableKey a publishable API key to use
     * @return a {@link Source}, or {@code null} if a problem occurred
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers
     */
    public Source createSourceSynchronous(
            @NonNull SourceParams params,
            @Nullable String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        String apiKey = publishableKey == null ? mDefaultPublishableKey : publishableKey;
        if (apiKey == null) {
            return null;
        }
        return StripeApiHandler.createSource(
                null, mContext, params, apiKey, mStripeAccount, mLoggingResponseListener);
    }

    /**
     * Blocking method to retrieve a {@link PaymentIntent} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * @param paymentIntentParams a set of params with which to retrieve the Payment Intent
     * @param publishableKey a publishable API key to use
     * @return a {@link PaymentIntent} or {@code null} if a problem occurred
     *
     * @throws AuthenticationException
     * @throws InvalidRequestException
     * @throws APIConnectionException
     * @throws APIException
     */
    public PaymentIntent retrievePaymentIntentSynchronous(
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull String publishableKey) throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return StripeApiHandler.retrievePaymentIntent(
                null,
                mContext,
                paymentIntentParams,
                publishableKey,
                mStripeAccount,
                mLoggingResponseListener);
    }

    /**
     * Blocking method to confirm a {@link PaymentIntent} object.
     * Do not call this on the UI thread or your app will crash.
     *
     * @param paymentIntentParams a set of params with which to confirm the Payment Intent
     * @param publishableKey a publishable API key to use
     * @return a {@link PaymentIntent} or {@code null} if a problem occurred
     *
     * @throws AuthenticationException
     * @throws InvalidRequestException
     * @throws APIConnectionException
     * @throws APIException
     */
    public PaymentIntent confirmPaymentIntentSynchronous(
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull String publishableKey) throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return StripeApiHandler.confirmPaymentIntent(
                null,
                mContext,
                paymentIntentParams,
                publishableKey,
                mStripeAccount,
                mLoggingResponseListener);
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

        RequestOptions requestOptions = RequestOptions.builder(
                publishableKey,
                mStripeAccount,
                RequestOptions.TYPE_QUERY).build();
        return StripeApiHandler.createToken(
                mContext,
                hashMapFromCard(mContext, card),
                requestOptions,
                Token.TYPE_CARD,
                mLoggingResponseListener);
    }

    /**
     * Blocking method to create a {@link Token} for PII. Do not call this on the UI thread
     * or your app will crash. The method uses the currently set {@link #mDefaultPublishableKey}.
     *
     * @param personalId the personal ID to use for this token
     * @return a {@link Token} that can be used for this card
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Token createPiiTokenSynchronous(@NonNull String personalId)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return createPiiTokenSynchronous(personalId, mDefaultPublishableKey);
    }

    /**
     * Blocking method to create a {@link Token} for PII. Do not call this on the UI thread
     * or your app will crash.
     *
     * @param personalId the personal ID to use for this token
     * @param publishableKey the publishable key to use with this request
     * @return a {@link Token} that can be used for this card
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Token createPiiTokenSynchronous(@NonNull String personalId, String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        validateKey(publishableKey);
        RequestOptions requestOptions = RequestOptions.builder(
                publishableKey,
                mStripeAccount,
                RequestOptions.TYPE_QUERY).build();
        return StripeApiHandler.createToken(
                mContext,
                hashMapFromPersonalId(mContext, personalId),
                requestOptions,
                Token.TYPE_PII,
                mLoggingResponseListener);
    }

    /**
     * Blocking method to create a {@link Token} for a Connect Account. Do not call this on the UI
     * thread or your app will crash. The method uses the currently set
     * {@link #mDefaultPublishableKey}.
     *
     * @param accountParams params to use for this token.
     * @return a {@link Token} that can be used for this account.
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Token createAccountTokenSynchronous(@NonNull final AccountParams accountParams)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return createAccountTokenSynchronous(accountParams, mDefaultPublishableKey);
    }

    /**
     * Blocking method to create a {@link Token} for a Connect Account. Do not call this on the UI
     * thread.
     *
     * @param accountParams params to use for this token.
     * @param publishableKey the publishable key to use with this request. If null is passed in as
     *                       the publishable key, we will use the default publishable key.
     * @return a {@link Token} that can be used for this account.
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Token createAccountTokenSynchronous(
            @NonNull final AccountParams accountParams,
            @Nullable String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        String apiKey = publishableKey == null ? mDefaultPublishableKey : publishableKey;
        if (apiKey == null) {
            return null;
        }
        validateKey(publishableKey);
        RequestOptions requestOptions = RequestOptions.builder(
                publishableKey,
                mStripeAccount,
                RequestOptions.TYPE_QUERY).build();
        try {
            return StripeApiHandler.createToken(
                    mContext,
                    accountParams.toParamMap(),
                    requestOptions,
                    Token.TYPE_ACCOUNT,
                    mLoggingResponseListener);
        } catch (CardException exception) {
            // Should never occur. CardException is only for card related requests.
        }
        return null;
    }

    public void logEventSynchronous(
            @NonNull List<String> productUsageTokens,
            @NonNull StripePaymentSource paymentSource) {
        RequestOptions.RequestOptionsBuilder builder =
                RequestOptions.builder(mDefaultPublishableKey);
        if (mStripeAccount != null) {
            builder.setStripeAccount(mStripeAccount);
        }
        RequestOptions options = builder.build();

        Map<String, Object> loggingMap = null;
        if (paymentSource instanceof Token) {
            Token token = (Token) paymentSource;
            loggingMap = LoggingUtils.getTokenCreationParams(
                    mContext,
                    productUsageTokens,
                    mDefaultPublishableKey,
                    token.getType());
        } else {
            Source source = (Source) paymentSource;
            loggingMap = LoggingUtils.getSourceCreationParams(
                    mContext,
                    productUsageTokens,
                    mDefaultPublishableKey,
                    source.getType());
        }
        StripeApiHandler.logApiCall(loggingMap, options, mLoggingResponseListener);
    }

    /**
     * Retrieve an existing {@link Source} from the Stripe API. Note that this is a
     * synchronous method, and cannot be called on the main thread. Doing so will cause your app
     * to crash. This method uses the default publishable key for this {@link Stripe} instance.
     *
     * @param sourceId the {@link Source#mId} field of the desired Source object
     * @param clientSecret the {@link Source#mClientSecret} field of the desired Source object
     * @return a {@link Source} if one could be found based on the input params, or {@code null} if
     * no such Source could be found.
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Source retrieveSourceSynchronous(
            @NonNull @Size(min = 1) String sourceId,
            @NonNull @Size(min = 1) String clientSecret)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        return retrieveSourceSynchronous(sourceId, clientSecret, null);
    }

    /**
     * Retrieve an existing {@link Source} from the Stripe API. Note that this is a
     * synchronous method, and cannot be called on the main thread. Doing so will cause your app
     * to crash.
     *
     * @param sourceId the {@link Source#mId} field of the desired Source object
     * @param clientSecret the {@link Source#mClientSecret} field of the desired Source object
     * @param publishableKey a publishable API key to use
     * @return a {@link Source} if one could be found based on the input params, or {@code null} if
     * no such Source could be found.
     *
     * @throws AuthenticationException failure to properly authenticate yourself (check your key)
     * @throws InvalidRequestException your request has invalid parameters
     * @throws APIConnectionException failure to connect to Stripe's API
     * @throws APIException any other type of problem (for instance, a temporary issue with
     * Stripe's servers)
     */
    public Source retrieveSourceSynchronous(
            @NonNull @Size(min = 1) String sourceId,
            @NonNull @Size(min = 1) String clientSecret,
            @Nullable String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        String apiKey = publishableKey == null ? mDefaultPublishableKey : publishableKey;
        if (apiKey == null) {
            return null;
        }
        return StripeApiHandler.retrieveSource(sourceId, clientSecret, apiKey);
    }

    /**
     * Set the default publishable key to use with this {@link Stripe} instance.
     *
     * @param publishableKey the key to be set
     */
    public void setDefaultPublishableKey(@NonNull @Size(min = 1) String publishableKey) {
        validateKey(publishableKey);
        mDefaultPublishableKey = publishableKey;
    }

    /**
     * Set the Stripe Connect account to use with this Stripe instance.
     *
     * @see <a href=https://stripe.com/docs/connect/authentication#authentication-via-the-stripe-account-header>
     *     Authentication via the stripe account header</a>
     * @param stripeAccount the account ID to be set
     */
    public void setStripeAccount(@NonNull @Size(min = 1) String stripeAccount) {
        mStripeAccount = stripeAccount;
    }

    @VisibleForTesting
    void setLoggingResponseListener(StripeApiHandler.LoggingResponseListener listener) {
        mLoggingResponseListener = listener;
    }

    private void createTokenFromParams(
            @NonNull final Map<String, Object> tokenParams,
            @NonNull @Size(min = 1) final String publishableKey,
            @NonNull @Token.TokenType final String tokenType,
            @Nullable final Executor executor,
            @NonNull final TokenCallback callback) {
        if (callback == null) {
            throw new RuntimeException(
                    "Required Parameter: 'callback' is required to use the created " +
                            "token and handle errors");
        }

        validateKey(publishableKey);
        mTokenCreator.create(
                tokenParams,
                publishableKey,
                mStripeAccount,
                tokenType,
                executor,
                callback);
    }

    private void validateKey(@NonNull @Size(min = 1) String publishableKey) {
        if (publishableKey == null || publishableKey.length() == 0) {
            throw new IllegalArgumentException("Invalid Publishable Key: " +
                    "You must use a valid publishable key to create a token.  " +
                    "For more info, see https://stripe.com/docs/stripe.js.");
        }

        if (publishableKey.startsWith("sk_")) {
            throw new IllegalArgumentException("Invalid Publishable Key: " +
                    "You are using a secret key to create a token, " +
                    "instead of the publishable one. For more info, " +
                    "see https://stripe.com/docs/stripe.js");
        }
    }

    private void tokenTaskPostExecution(ResponseWrapper result, TokenCallback callback) {
        if (result.token != null) {
            callback.onSuccess(result.token);
        } else if (result.error != null) {
            callback.onError(result.error);
        } else {
            callback.onError(new RuntimeException("Somehow got neither a token response or an " +
                    "error response"));
        }
    }

    private void executeTask(Executor executor, AsyncTask<Void, Void, ResponseWrapper> task) {
        if (executor != null) {
            task.executeOnExecutor(executor);
        } else {
            task.execute();
        }
    }

    private class ResponseWrapper {
        final Source source;
        final Token token;
        final Exception error;

        private ResponseWrapper(Token token) {
            this.token = token;
            this.source = null;
            this.error = null;
        }

        private ResponseWrapper(Source source) {
            this.source = source;
            this.error = null;
            this.token = null;
        }

        private ResponseWrapper(Exception error) {
            this.error = error;
            this.source = null;
            this.token = null;
        }
    }

    interface SourceCreator {
        void create(
                @NonNull SourceParams params,
                @NonNull String publishableKey,
                @Nullable String stripeAccount,
                @Nullable Executor executor,
                @NonNull SourceCallback sourceCallback);
    }

    @VisibleForTesting
    interface TokenCreator {
        void create(Map<String, Object> params,
                    String publishableKey,
                    String stripeAccount,
                    @NonNull @Token.TokenType String tokenType,
                    Executor executor,
                    TokenCallback callback);
    }
}
