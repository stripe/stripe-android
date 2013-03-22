package com.stripe.android;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import com.stripe.android.compat.AsyncTask;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

public class Stripe {
    private String defaultPublishableKey;

    TokenCreator tokenCreator = new TokenCreator() {
        @Override
        public void create(final Card card, final String publishableKey, final Executor executor,
                final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task = new AsyncTask<Void, Void, ResponseWrapper>() {
                protected ResponseWrapper doInBackground(Void... params) {
                    try {
                        com.stripe.model.Token stripeToken = com.stripe.model.Token.create(
                                hashMapFromCard(card), publishableKey);
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

    public Stripe() {
    }

    public Stripe(String publishableKey) throws AuthenticationException {
        setDefaultPublishableKey(publishableKey);
    }

    public void setDefaultPublishableKey(String publishableKey) throws AuthenticationException {
        validateKey(publishableKey);
        this.defaultPublishableKey = publishableKey;
    }

    private void validateKey(String publishableKey) throws AuthenticationException {
        if (publishableKey == null || publishableKey.length() == 0) {
            throw new AuthenticationException("Invalid Publishable Key: You must use a valid publishable key to create a token.  For more info, see https://stripe.com/docs/stripe.js.");
        }
        if (publishableKey.startsWith("sk_")) {
            throw new AuthenticationException("Invalid Publishable Key: You are using a secret key to create a token, instead of the publishable one. For more info, see https://stripe.com/docs/stripe.js");
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

    public void requestToken(final String tokenId, final String publishableKey, final Executor executor, final TokenCallback callback) {
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

    public void createToken(final Card card, final Executor executor, final TokenCallback callback) {
        createToken(card, defaultPublishableKey, executor, callback);
    }

    public void createToken(final Card card, final String publishableKey, final TokenCallback callback) {
        createToken(card, publishableKey, null, callback);
    }

    public void createToken(final Card card, final TokenCallback callback) {
        createToken(card, defaultPublishableKey, callback);
    }

    public void createToken(final Card card, final String publishableKey, final Executor executor, final TokenCallback callback) {
        try {
            if (card == null)
                throw new RuntimeException("Required Parameter: 'card' is required to create a token");

            if (callback == null)
                throw new RuntimeException("Required Parameter: 'callback' is required to use the created token and handle errors");

            validateKey(publishableKey);

            tokenCreator.create(card, publishableKey, executor, callback);
        }
        catch (AuthenticationException e) {
            callback.onError(e);
        }
    }

    private Card androidCardFromStripeCard(com.stripe.model.Card stripeCard) {
        return new Card(null, stripeCard.getExpMonth(), stripeCard.getExpYear(), null, stripeCard.getName(), stripeCard.getAddressLine1(), stripeCard.getAddressLine2(), stripeCard.getAddressCity(), stripeCard.getAddressState(), stripeCard.getAddressZip(), stripeCard.getAddressCountry(), stripeCard.getLast4(), stripeCard.getType(), stripeCard.getFingerprint(), stripeCard.getCountry());
    }

    private Token androidTokenFromStripeToken(Card androidCard, com.stripe.model.Token stripeToken) {
        return new Token(stripeToken.getId(), stripeToken.getLivemode(), new Date(stripeToken.getCreated() * 1000), stripeToken.getUsed(), androidCard);
    }

    private void tokenTaskPostExecution(ResponseWrapper result, TokenCallback callback) {
        if (result.token != null)
            callback.onSuccess(result.token);
        else if (result.error != null)
            callback.onError(result.error);
        else
            callback.onError(new RuntimeException("Somehow got neither a token response or an error response"));
    }

    private void executeTokenTask(Executor executor, AsyncTask<Void, Void, ResponseWrapper> task) {
        if (executor != null)
            task.executeOnExecutor(executor);
        else
            task.execute();
    }

    private Map<String, Object> hashMapFromCard(Card card) {
        Map<String, Object> tokenParams = new HashMap<String, Object>();
        Map<String, Object> cardParams = new HashMap<String, Object>();
        cardParams.put("number", card.getNumber());
        cardParams.put("cvc", card.getCVC());
        cardParams.put("exp_month", card.getExpMonth());
        cardParams.put("exp_year", card.getExpYear());
        cardParams.put("name", card.getName());
        cardParams.put("address_line_1", card.getAddressLine1());
        cardParams.put("address_line_2", card.getAddressLine2());
        cardParams.put("address_line_city", card.getAddressCity());
        cardParams.put("address_line_zip", card.getAddressZip());
        cardParams.put("address_line_state", card.getAddressState());
        cardParams.put("address_line_country", card.getAddressCountry());
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
        public void create(Card card, String publishableKey, Executor executor, TokenCallback callback);
    }

    interface TokenRequester {
        public void request(String tokenId, String publishableKey, Executor executor, TokenCallback callback);
    }
}