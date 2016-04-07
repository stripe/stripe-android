package com.stripe.android;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executor;

import com.stripe.android.compat.AsyncTask;
import com.stripe.android.model.Card;
import com.stripe.android.model.CardParams;
import com.stripe.android.model.Token;
import com.stripe.android.model.TokenBuilder;
import com.stripe.android.util.TextUtils;
import com.stripe.exception.AuthenticationException;
import com.stripe.net.RequestOptions;

public class Stripe {
    private String defaultPublishableKey;

    public TokenCreator tokenCreator = new TokenCreator() {
        @Override
        public void create(final CardParams cardParams, final String publishableKey, final Executor executor, final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task = new AsyncTask<Void, Void, ResponseWrapper>() {

                protected ResponseWrapper doInBackground(Void... params) {
                    try {
                        RequestOptions requestOptions = RequestOptions.builder().setApiKey(publishableKey).build();
                        com.stripe.model.Token stripeToken = com.stripe.model.Token.create(hashMapFromCardParams(cardParams), requestOptions);
                        Token token = androidTokenFromStripeToken(stripeToken);
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

    public TokenRequester tokenRequester = new TokenRequester() {
          @Override
          public void request(final String tokenId, final String publishableKey,
                  final Executor executor, final TokenCallback callback) {
            AsyncTask<Void, Void, ResponseWrapper> task = new AsyncTask<Void, Void, ResponseWrapper>() {
                protected ResponseWrapper doInBackground(Void... params) {
                    try {
                        RequestOptions requestOptions = RequestOptions.builder().setApiKey(publishableKey).build();
                        com.stripe.model.Token stripeToken = com.stripe.model.Token.retrieve(tokenId, requestOptions);
                        Token token = androidTokenFromStripeToken(stripeToken);
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
            throw new AuthenticationException("Invalid Publishable Key: You must use a valid publishable key to create a token.  For more info, see https://stripe.com/docs/stripe.js.", null, 0);
        }
        if (publishableKey.startsWith("sk_")) {
            throw new AuthenticationException("Invalid Publishable Key: You are using a secret key to create a token, instead of the publishable one. For more info, see https://stripe.com/docs/stripe.js", null, 0);
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

    public void createToken(final CardParams cardParams, final Executor executor, final TokenCallback callback) {
        createToken(cardParams, defaultPublishableKey, executor, callback);
    }

    public void createToken(final CardParams cardParams, final String publishableKey, final TokenCallback callback) {
        createToken(cardParams, publishableKey, null, callback);
    }

    public void createToken(final CardParams cardParams, final TokenCallback callback) {
        createToken(cardParams, defaultPublishableKey, callback);
    }

    public void createToken(final CardParams cardParams, final String publishableKey, final Executor executor, final TokenCallback callback) {
        try {
            if (cardParams == null)
                throw new RuntimeException("Required Parameter: 'cardParams' is required to create a token");

            if (callback == null)
                throw new RuntimeException("Required Parameter: 'callback' is required to use the created token and handle errors");

            validateKey(publishableKey);

            tokenCreator.create(cardParams, publishableKey, executor, callback);
        }
        catch (AuthenticationException e) {
            callback.onError(e);
        }
    }

    private Card androidCardFromStripeCard(com.stripe.model.Card stripeCard) {
        return new Card(stripeCard.getId(), stripeCard.getStatus(), stripeCard.getExpMonth(), stripeCard.getExpYear(), stripeCard.getLast4(), stripeCard.getDynamicLast4(), stripeCard.getCountry(), stripeCard.getType(), stripeCard.getName(), stripeCard.getAddressLine1(), stripeCard.getAddressLine2(), stripeCard.getAddressZip(), stripeCard.getAddressCity(), stripeCard.getAddressState(), stripeCard.getAddressCountry(), stripeCard.getAddressZipCheck(), stripeCard.getAddressLine1Check(), stripeCard.getCvcCheck(), stripeCard.getFingerprint(), stripeCard.getBrand(), stripeCard.getFunding(), stripeCard.getCurrency(), stripeCard.getTokenizationMethod());
    }

    private Token androidTokenFromStripeToken(com.stripe.model.Token stripeToken) {
        TokenBuilder tb = new TokenBuilder(stripeToken.getId(), new Date(stripeToken.getCreated() * 1000), stripeToken.getLivemode())
            .setUsed(stripeToken.getUsed())
            .setCurrency(stripeToken.getCurrency());
        if (stripeToken.getCard() != null) {
            tb.setCard(androidCardFromStripeCard(stripeToken.getCard()));
        }
        return tb.createToken();
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

    // TODO: Move to CardParams?
    private Map<String, Object> hashMapFromCardParams(CardParams cardParams) {
        Map<String, Object> tokenParamsMap = new HashMap<String, Object>();

        Map<String, Object> cardParamsMap = new HashMap<String, Object>();
        cardParamsMap.put("number", TextUtils.nullIfBlank(cardParams.getNumber()));
        cardParamsMap.put("exp_month", cardParams.getExpMonth());
        cardParamsMap.put("exp_year", cardParams.getExpYear());

        cardParamsMap.put("cvc", TextUtils.nullIfBlank(cardParams.getCvc()));
        cardParamsMap.put("currency", TextUtils.nullIfBlank(cardParams.getCurrency()));
        cardParamsMap.put("name", TextUtils.nullIfBlank(cardParams.getName()));
        cardParamsMap.put("address_line1", TextUtils.nullIfBlank(cardParams.getAddressLine1()));
        cardParamsMap.put("address_line2", TextUtils.nullIfBlank(cardParams.getAddressLine2()));
        cardParamsMap.put("address_city", TextUtils.nullIfBlank(cardParams.getAddressCity()));
        cardParamsMap.put("address_state", TextUtils.nullIfBlank(cardParams.getAddressState()));
        cardParamsMap.put("address_zip", TextUtils.nullIfBlank(cardParams.getAddressZip()));
        cardParamsMap.put("address_country", TextUtils.nullIfBlank(cardParams.getAddressCountry()));

        // Remove all null values; they cause validation errors
        for (String key : new HashSet<String>(cardParamsMap.keySet())) {
            if (cardParamsMap.get(key) == null) {
                cardParamsMap.remove(key);
            }
        }

        tokenParamsMap.put("card", cardParamsMap);
        return tokenParamsMap;
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
        public void create(CardParams cardParams, String publishableKey, Executor executor, TokenCallback callback);
    }

    interface TokenRequester {
        public void request(String tokenId, String publishableKey, Executor executor, TokenCallback callback);
    }
}