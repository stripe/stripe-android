package com.stripe;

import com.stripe.http.AsyncHTTPInterface;
import com.stripe.http.AsyncHTTPTask;
import com.stripe.http.ResponseHandler;
import com.stripe.util.StripeLog;
import com.stripe.util.URLUtils;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;

public class Stripe {

    private static final String API_BASE_URL = "api.stripe.com";
    private static final String API_VERSION = "v1";
    private static final String TOKEN_ENDPOINT = "tokens";

    private String defaultPublishableKey;
    protected AsyncHTTPInterface asyncHTTPTask = null;

    public Stripe() {
    }

    public Stripe(String publishableKey) {
        setDefaultPublishableKey(publishableKey);
    }

    public void setDefaultPublishableKey(String publishableKey) {
        validateKey(publishableKey);
        this.defaultPublishableKey = publishableKey;
    }

    private void validateKey(String publishableKey) {
        StripeLog.d("Client side validation of publishable key: %s", publishableKey);
        if (publishableKey == null || publishableKey.length() == 0) {
            StripeLog.e("Invalid Publishable Key: You must use a valid publishable key to create a token.");
            throw new InvalidTokenException("Invalid Publishable Key: You must use a valid publishable key to create a token.  For more info, see https://stripe.com/docs/stripe.js.");
        }
        if (publishableKey.startsWith("sk_")) {
            StripeLog.e("Invalid Publishable Key: You are using a secret key to create a token, instead of the publishable one.");
            throw new InvalidTokenException("Invalid Publishable Key: You are using a secret key to create a token, instead of the publishable one. For more info, see https://stripe.com/docs/stripe.js");
        }
    }

    private URL getApiUrl(String publishableKey, String tokenID) {
        try {
            return new URL(String.format("https://%s:@%s/%s/%s/%s",
                    URLUtils.urlEncode(publishableKey),
                    API_BASE_URL,
                    API_VERSION,
                    TOKEN_ENDPOINT,
                    URLUtils.urlEncode(tokenID)));
        } catch (Exception e) {
            StripeLog.e(e);
            throw new RuntimeException(e);
        }
    }

    private URL getApiUrl(String publishableKey) {
        try {
            return new URL(String.format("https://%s:@%s/%s/%s",
                    URLUtils.urlEncode(publishableKey),
                    API_BASE_URL,
                    API_VERSION,
                    TOKEN_ENDPOINT));
        } catch (Exception e) {
            StripeLog.e(e);
            throw new RuntimeException(e);
        }
    }

    private AsyncHTTPInterface getHTTPTask() {
        if (asyncHTTPTask == null) {
            asyncHTTPTask = new AsyncHTTPTask();
            return asyncHTTPTask;
        } else {
            return asyncHTTPTask;
        }
    }

    private ResponseHandler getResponseHandler(final StripeSuccessHandler successHandler, final StripeErrorHandler errorHandler) {
        return new ResponseHandler() {
            @Override
            public void handle(int responseCode, String responseBody) {
                StripeLog.d("Handling response...");

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Token token;
                    try {
                        StripeLog.d("Parsing token.");
                        JSONObject tokenMap = new JSONObject(responseBody);
                        token = Token.fromJSON(tokenMap);
                    } catch (Exception e) {
                        StripeLog.e(e);
                        errorHandler.onError(StripeError.fromException(e));
                        return;
                    }

                    StripeLog.d("Calling successHandler with token.");
                    successHandler.onSuccess(token);

                } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    StripeLog.e("Received 401 Unauthorized; calling errorHandler.");
                    errorHandler.onError(StripeError.UNAUTHORIZED);
                } else {
                    StripeError error;
                    try {
                        StripeLog.d("Received error from Stripe, parsing.");
                        JSONObject errorMap = new JSONObject(responseBody);
                        error = StripeError.fromJSON(errorMap);
                        StripeLog.d("Error: " + error.developerMessage);
                        StripeLog.d("Parsed Stripe error; calling errorHandler.");
                    } catch (Exception e) {
                        StripeLog.e(e);
                        error = StripeError.fromException(e);
                    }
                    errorHandler.onError(error);
                }
            }
        };
    }

    public void requestToken(String tokenId, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        requestToken(tokenId, defaultPublishableKey, null, successHandler, errorHandler);
    }

    public void requestToken(String tokenId, String publishableKey, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        requestToken(tokenId, publishableKey, null, successHandler, errorHandler);
    }

    public void requestToken(String tokenId, Executor executor, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        requestToken(tokenId, defaultPublishableKey, executor, successHandler, errorHandler);
    }

    public void requestToken(String tokenId, String publishableKey, Executor executor, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        StripeLog.d("createToken for id: %s", tokenId);
        try {
            if (tokenId == null) {
                StripeLog.e("Required Parameter: 'tokenId' is required to retrieve a token.");
                throw new RuntimeException("Required Parameter: 'tokenId' is required to retrieve a token.");
            }

            validateKey(publishableKey);

            URL url = getApiUrl(publishableKey, tokenId);

            getHTTPTask().sendAsynchronousRequest(url, "GET", null, executor, getResponseHandler(successHandler, errorHandler));
        } catch (Exception exception) {
            StripeLog.e(exception);
            errorHandler.onError(StripeError.fromException(exception));
        }
    }

    public void createToken(Card card, Executor executor, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        createToken(card, defaultPublishableKey, executor, successHandler, errorHandler);
    }

    public void createToken(Card card, String publishableKey, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        createToken(card, publishableKey, null, successHandler, errorHandler);
    }

    public void createToken(Card card, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        createToken(card, defaultPublishableKey, null, successHandler, errorHandler);
    }

    public void createToken(Card card, String publishableKey, Executor executor, StripeSuccessHandler successHandler, StripeErrorHandler errorHandler) {
        StripeLog.d("createToken for card: %s", card.last4);
        try {
            if (card == null) {
                StripeLog.e("Required Parameter: 'card' is required to create a token.");
                throw new RuntimeException("Required Parameter: 'card' is required to create a token.");
            }

            validateKey(publishableKey);

            String params = card.urlEncode();
            StripeLog.d("Setting request body to card.urlEncode()");

            URL url = getApiUrl(publishableKey);
            getHTTPTask().sendAsynchronousRequest(url, "POST", params, executor, getResponseHandler(successHandler, errorHandler));
        } catch (Exception exception) {
            StripeLog.e(exception);
            errorHandler.onError(StripeError.fromException(exception));
        }
    }

    public class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
