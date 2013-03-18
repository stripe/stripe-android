package com.stripe.android;

import com.stripe.exception.AuthenticationException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.compat.AsyncTask;
import com.stripe.android.TokenCallback;
import java.util.concurrent.Executor;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class Stripe {
    private String defaultPublishableKey;

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

    public void createToken(Card card, Executor executor, TokenCallback callback) throws AuthenticationException {
        createToken(card, defaultPublishableKey, executor, callback);
    }

    public void createToken(Card card, String publishableKey, TokenCallback callback) throws AuthenticationException {
        createToken(card, publishableKey, null, callback);
    }

    public void createToken(Card card, TokenCallback callback) throws AuthenticationException {
        createToken(card, defaultPublishableKey, callback);
    }

    public void createToken(final Card card, final String publishableKey, Executor executor, final TokenCallback callback) throws AuthenticationException {
        try {
            if (card == null)
                throw new RuntimeException("Required Parameter: 'card' is required to create a token.");

            if (callback == null)
               throw new RuntimeException("Required Parameter: 'callback' is required to use the created token.");

            validateKey(publishableKey);

            AsyncTask<Void, Void, ResponseWrapper> task = new AsyncTask<Void, Void, ResponseWrapper>() {
                protected ResponseWrapper doInBackground(Void... params) {
                    try {
                        com.stripe.model.Token stripeToken = com.stripe.model.Token.create(hashMapFromCard(card), publishableKey);
                        com.stripe.model.Card stripeCard = stripeToken.getCard();
                        Card card = new Card(null, stripeCard.getExpMonth(), stripeCard.getExpYear(), null, stripeCard.getName(), stripeCard.getAddressLine1(), stripeCard.getAddressLine2(), stripeCard.getAddressCity(), stripeCard.getAddressState(), stripeCard.getAddressZip(), stripeCard.getAddressCountry(), stripeCard.getLast4(), stripeCard.getType(), stripeCard.getFingerprint(), stripeCard.getCountry());
                        Token token = new Token(stripeToken.getId(), stripeToken.getLivemode(), new Date(stripeToken.getCreated() * 1000), stripeToken.getUsed(), card);
                        return new ResponseWrapper(token, null);
                    } catch (Exception e) {
                        return new ResponseWrapper(null, e);
                    }
                }

                protected void onPostExecute(ResponseWrapper result) {
                    if (result.token != null)
                        callback.onSuccess(result.token);
                    else if (result.error != null)
                        callback.onError(result.error);
                    else
                        callback.onError(new RuntimeException("Creating a token somehow led to neither a token response or an error response"));
               }
            };
            if (executor != null)
                task.executeOnExecutor(executor);
            else
                task.execute();

        } catch (Exception e) {
            if (callback != null)
                callback.onError(e);
            else
                throw new RuntimeException(e);
        }
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
}