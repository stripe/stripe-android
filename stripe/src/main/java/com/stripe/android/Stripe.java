package com.stripe.android;

import android.os.AsyncTask;
import android.os.Build;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executor;

import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.util.StripeTextUtils;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.net.RequestOptions;

/**
 * Class that handles {@link Token} creation from charges and {@link Card} models.
 */
public class Stripe {

    @VisibleForTesting
    TokenCreator tokenCreator = new TokenCreator() {
        @Override
        public void create(
                final Card card,
                final String publishableKey,
                final Executor executor,
                final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task =
                    new AsyncTask<Void, Void, ResponseWrapper>() {
                        @Override
                        protected ResponseWrapper doInBackground(Void... params) {
                            try {
                                RequestOptions requestOptions = RequestOptions.builder()
                                        .setApiKey(publishableKey).build();
                                com.stripe.model.Token stripeToken = com.stripe.model.Token.create(
                                        hashMapFromCard(card), requestOptions);
                                com.stripe.model.Card stripeCard = stripeToken.getCard();
                                Card card = androidCardFromStripeCard(stripeCard);
                                Token token = androidTokenFromStripeToken(card, stripeToken);
                                return new ResponseWrapper(token, null);
                            } catch (Exception e) {
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

    @VisibleForTesting
    TokenRequester tokenRequester = new TokenRequester() {
          @Override
          public void request(final String tokenId, final String publishableKey,
                  final Executor executor, final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task = new AsyncTask<Void, Void, ResponseWrapper>() {
                protected ResponseWrapper doInBackground(Void... params) {
                    try {
                        com.stripe.model.Token stripeToken = com.stripe.model.Token.retrieve(
                                tokenId, publishableKey);
                        com.stripe.model.Card stripeCard = stripeToken.getCard();
                        Card card = androidCardFromStripeCard(stripeCard);
                        Token token = androidTokenFromStripeToken(card, stripeToken);
                        return new ResponseWrapper(token, null);
                    } catch (Exception e) {
                        return new ResponseWrapper(null, e);
                    }
                }

                protected void onPostExecute(ResponseWrapper result) {
                    tokenTaskPostExecution(result, callback);
               }
            };

            executeTokenTask(executor, task);
          }
    };

    private String defaultPublishableKey;

    /**
     * A blank constructor to set the key later.
     */
    public Stripe() { }

    /**
     * Constructor with publishable key.
     *
     * @param publishableKey the client's publishable key
     * @throws AuthenticationException if the key is invalid
     */
    public Stripe(String publishableKey) throws AuthenticationException {
        setDefaultPublishableKey(publishableKey);
    }

    /**
     * The simplest way to create a token, using a {@link Card} and {@link TokenCallback}. This
     * runs on the default {@link Executor} and with the
     * currently set {@link #defaultPublishableKey}.
     *
     * @param card the {@link Card} used to create this payment token
     * @param callback a {@link TokenCallback} to receive either the token or an error
     */
    public void createToken(@NonNull final Card card, @NonNull final TokenCallback callback) {
        createToken(card, defaultPublishableKey, callback);
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
     *                 specify an executor, use one of the other createToken methods.
     * @param callback a {@link TokenCallback} to receive the result of this operation
     */
    public void createToken(
            @NonNull final Card card,
            @NonNull final Executor executor,
            @NonNull final TokenCallback callback) {
        createToken(card, defaultPublishableKey, executor, callback);
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
        try {
            if (card == null) {
                throw new RuntimeException(
                        "Required Parameter: 'card' is required to create a token");
            }

            if (callback == null) {
                throw new RuntimeException(
                        "Required Parameter: 'callback' is required to use the created " +
                                "token and handle errors");
            }

            validateKey(publishableKey);

            tokenCreator.create(card, publishableKey, executor, callback);
        }
        catch (AuthenticationException e) {
            callback.onError(e);
        }
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
        return createTokenSynchronous(card, defaultPublishableKey);
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
     * Stripe's servers
     */
    public Token createTokenSynchronous(Card card, String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {
        validateKey(publishableKey);

        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(publishableKey).build();
        com.stripe.model.Token stripeToken = com.stripe.model.Token.create(
                hashMapFromCard(card), requestOptions);
        com.stripe.model.Card stripeCard = stripeToken.getCard();
        Card resultCard = androidCardFromStripeCard(stripeCard);
        return androidTokenFromStripeToken(resultCard, stripeToken);
    }

    /**
     * Retrieve a token for inspection, using the token's id.
     *
     * @param tokenId the id of the {@link Token} being requested
     * @param callback a {@link TokenCallback} to receive the result
     *
     * @deprecated the requestToken endpoint is not guaranteed to work with a public key, as that
     * ability has been turned off for accounts using API versions later than 2014-10-07. Secret
     * keys should not be included in mobile applications.
     */
    @Deprecated
    public void requestToken(
            @NonNull final String tokenId,
            @NonNull final TokenCallback callback) {
        requestToken(tokenId, defaultPublishableKey, callback);
    }

    /**
     * Retrieve a token for inspection, using the token's id and a publishable key.
     *
     * @param tokenId the id of the {@link Token} being requested
     * @param publishableKey the publishable key used to create this token
     * @param callback a {@link TokenCallback} to receive the result
     *
     * @deprecated the requestToken endpoint is not guaranteed to work with a public key, as that
     * ability has been turned off for accounts using API versions later than 2014-10-07. Secret
     * keys should not be included in mobile applications.
     */
    @Deprecated
    public void requestToken(
            @NonNull final String tokenId,
            @NonNull @Size(min = 1) final String publishableKey,
            @NonNull final TokenCallback callback) {
        requestToken(tokenId, publishableKey, null, callback);
    }

    /**
     * Retrieve a token for inspection on a specific {@link Executor}, using the token's id.
     *
     * @param tokenId the id of the {@link Token} being requested
     * @param executor an {@link Executor} on which to run this request
     * @param callback a {@link TokenCallback} to receive the result
     *
     * @deprecated the requestToken endpoint is not guaranteed to work with a public key, as that
     * ability has been turned off for accounts using API versions later than 2014-10-07. Secret
     * keys should not be included in mobile applications.
     */
    @Deprecated
    public void requestToken(
            @NonNull final String tokenId,
            @NonNull final Executor executor,
            @NonNull final TokenCallback callback) {
        requestToken(tokenId, defaultPublishableKey, executor, callback);
    }


    /**
     * Retrieve a token for inspection on a specific {@link Executor}, using the publishable key
     * that was used to create the token.
     *
     * @param tokenId the id of the token being requested
     * @param publishableKey the key used to create the token
     * @param executor an {@link Executor} on which to run this operation, or {@code null} to run
     *                 on a default background executor
     * @param callback a {@link TokenCallback} to receive the result
     *
     * @deprecated the requestToken endpoint is not guaranteed to work with a public key, as that
     * ability has been turned off for accounts using API versions later than 2014-10-07. Secret
     * keys should not be included in mobile applications.
     */
    @Deprecated
    public void requestToken(
            @NonNull final String tokenId,
            @NonNull @Size(min = 1) final String publishableKey,
            @Nullable final Executor executor,
            @NonNull final TokenCallback callback) {
        try {
            if (tokenId == null) {
                throw new RuntimeException("Required Parameter: 'tokenId' " +
                        "is required to request a token");
            }

            if (callback == null) {
                throw new RuntimeException("Required Parameter: 'callback' " +
                        "is required to use the requested token and handle errors");
            }

            validateKey(publishableKey);

            tokenRequester.request(tokenId, publishableKey, executor, callback);
        } catch (AuthenticationException e) {
            callback.onError(e);
        }
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
        this.defaultPublishableKey = publishableKey;
    }

    /**
     * Converts a stripe-java card model into a {@link Card} model.
     *
     * @param stripeCard the server-returned {@link com.stripe.model.Card}.
     * @return an equivalent {@link Card}.
     */
    @VisibleForTesting
    Card androidCardFromStripeCard(com.stripe.model.Card stripeCard) {
        return new Card(
                null,
                stripeCard.getExpMonth(),
                stripeCard.getExpYear(),
                null,
                stripeCard.getName(),
                stripeCard.getAddressLine1(),
                stripeCard.getAddressLine2(),
                stripeCard.getAddressCity(),
                stripeCard.getAddressState(),
                stripeCard.getAddressZip(),
                stripeCard.getAddressCountry(),
                stripeCard.getBrand(),
                stripeCard.getLast4(),
                stripeCard.getFingerprint(),
                stripeCard.getFunding(),
                stripeCard.getCountry(),
                stripeCard.getCurrency());
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

    private Token androidTokenFromStripeToken(
            Card androidCard,
            com.stripe.model.Token stripeToken) {
        return new Token(
                stripeToken.getId(),
                stripeToken.getLivemode(),
                new Date(stripeToken.getCreated() * 1000),
                stripeToken.getUsed(),
                androidCard);
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

    private Map<String, Object> hashMapFromCard(Card card) {
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
        for (String key : new HashSet<>(cardParams.keySet())) {
            if (cardParams.get(key) == null) {
                cardParams.remove(key);
            }
        }

        tokenParams.put("card", cardParams);
        return tokenParams;
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
        void create(Card card, String publishableKey, Executor executor, TokenCallback callback);
    }

    @VisibleForTesting
    interface TokenRequester {
        void request(String tokenId, String publishableKey, Executor executor, TokenCallback callback);
    }
}
