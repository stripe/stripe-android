package com.stripe.android;

import android.os.AsyncTask;
import android.os.Build;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executor;

import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.util.StripeTextUtils;
import com.stripe.exception.AuthenticationException;
import com.stripe.net.RequestOptions;

/**
 * Class that handles {@link Token} creation from charges and {@link Card} models.
 */
public class Stripe {

    public TokenCreator tokenCreator = new TokenCreator() {
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

    public TokenRequester tokenRequester = new TokenRequester() {
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
     * Call to create a {@link Token}.
     *
     * @param card the {@link Card} used for this transaction
     * @param publishableKey the public key used for this transaction
     * @param callback a {@link TokenCallback} to receive the result of this operation
     */
    public void createToken(
            final Card card,
            final String publishableKey,
            final TokenCallback callback) {
        createToken(card, publishableKey, null, callback);
    }

    public void setDefaultPublishableKey(String publishableKey) throws AuthenticationException {
        validateKey(publishableKey);
        this.defaultPublishableKey = publishableKey;
    }

    private void validateKey(String publishableKey) throws AuthenticationException {
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

    public void requestToken(final String tokenId, final Executor executor, final TokenCallback callback) {
        requestToken(tokenId, defaultPublishableKey, executor, callback);
    }

    public void requestToken(final String tokenId, final String publishableKey, final TokenCallback callback) {
        requestToken(tokenId, publishableKey, null, callback);
    }

    public void requestToken(final String tokenId, final TokenCallback callback) {
        requestToken(tokenId, defaultPublishableKey, callback);
    }

    public void createToken(final Card card, final Executor executor, final TokenCallback callback) {
        createToken(card, defaultPublishableKey, executor, callback);
    }

    public void createToken(final Card card, final TokenCallback callback) {
        createToken(card, defaultPublishableKey, callback);
    }

    public void requestToken(
            final String tokenId,
            final String publishableKey,
            final Executor executor,
            final TokenCallback callback) {
        try {
            if (tokenId == null)
                throw new RuntimeException("Required Parameter: 'tokenId' is required to request a token");

            if (callback == null)
                throw new RuntimeException("Required Parameter: 'callback' is required to use the requested token and handle errors");

            validateKey(publishableKey);

            tokenRequester.request(tokenId, publishableKey, executor, callback);
        }
        catch (AuthenticationException e) {
            callback.onError(e);
        }
    }

    public void createToken(
            final Card card,
            final String publishableKey,
            final Executor executor,
            final TokenCallback callback) {
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

    private Card androidCardFromStripeCard(com.stripe.model.Card stripeCard) {
        return new Card(
                stripeCard.getId()
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
                stripeCard.getLast4(),
                stripeCard.getBrand(),
                stripeCard.getFingerprint(),
                stripeCard.getCountry());
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
        if (executor != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(executor);
        else
            task.execute();
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
        public final Token token;
        public final Exception error;

        private ResponseWrapper(Token token, Exception error) {
            this.error = error;
            this.token = token;
        }
    }

    interface TokenCreator {
        void create(Card card, String publishableKey, Executor executor, TokenCallback callback);
    }

    interface TokenRequester {
        void request(String tokenId, String publishableKey, Executor executor, TokenCallback callback);
    }
}
