package com.stripe;

import com.stripe.http.AsyncHTTPInterface;
import com.stripe.http.ResponseHandler;
import com.stripe.util.URLUtils;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Executor;

import static org.junit.Assert.*;

public class StripeTest {

    private Stripe stripe;
    private final StripeErrorHandler failingErrorHandler = new StripeErrorHandler() {
        @Override
        public void onError(StripeError error) {
            fail(error.developerMessage);
        }
    };
    private StripeSuccessHandler failingSuccessHandler;
    private final boolean[] asyncCalled = {false};
    private final String tokenId = "134";
    private final String tokenJSON = "{id: \"" + tokenId + "\", livemode: true}";
    private final String errorJSON = "{\n" +
            "  \"error\": {\n" +
            "    \"message\": \"Received unknown parameter: number\",\n" +
            "    \"param\": \"number\",\n" +
            "    \"type\": \"invalid_request_error\"\n" +
            "  }\n" +
            "}";
    private final String publishableKey = "pk_something123456789";
    private Card emptyCard = new Card(new HashMap<String, String>());

    @Before
    public void setup() {
        asyncCalled[0] = false;
        stripe = new Stripe();
        stripe.setDefaultPublishableKey(publishableKey);
        failingSuccessHandler = new StripeSuccessHandler() {
            @Override
            public void onSuccess(Token token) {
                fail("Did not expect request to succeed");
            }
        };
    }

    @Test
    public void requestToken_should_work() {

        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;

                assertEquals("https", url.getProtocol());
                assertEquals("api.stripe.com", url.getHost());
                assertEquals("/v1/tokens/" + tokenId, url.getPath());
                assertEquals(publishableKey + ":", url.getUserInfo());

                assertEquals("GET", method);
                assertEquals(null, body);
            }
        };
        stripe.requestToken(tokenId, null, null);
        assertAsyncCalled();
    }

    @Test
    public void requestToken_should_call_success_handler() {
        stripe.asyncHTTPTask = createMockHTTPTask(200, tokenJSON, null);
        stripe.requestToken(tokenId,
                new StripeSuccessHandler() {
                    @Override
                    public void onSuccess(Token token) {
                        assertEquals(tokenId, token.tokenId);
                        assertTrue(token.livemode);
                        asyncCalled[0] = true;
                    }
                },
                failingErrorHandler
        );
        assertAsyncCalled();
    }

    @Test
    public void requestToken_should_use_publishable_key_and_encode_user_input() {
        final String key = "://abc&%!@";
        final String tokenId = "/xyz://&%==!@";
        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;

                assertEquals("/v1/tokens/" + URLUtils.urlEncode(tokenId), url.getPath());
                assertEquals(URLUtils.urlEncode(key) + ":", url.getUserInfo());
            }
        };
        stripe.requestToken(tokenId, "://abc&%!@", null, null);
        assertAsyncCalled();
    }

    @Test
    public void createToken_should_use_publishable_key_and_encode_user_input() {
        final String key = "://abc&%!@";
        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;
                assertEquals(URLUtils.urlEncode(key) + ":", url.getUserInfo());
            }
        };
        stripe.createToken(emptyCard, "://abc&%!@", null, null);
        assertAsyncCalled();
    }

    @Test
    public void createToken_should_use_executor() {
        final Executor myExecutor = new Executor() {
            public void execute(Runnable command) {
            }
        };

        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;
                assertEquals(myExecutor, executor);
            }
        };
        stripe.createToken(emptyCard, myExecutor, null, null);
        assertAsyncCalled();
    }

    @Test
    public void requestToken_should_use_executor() {
        final Executor myExecutor = new Executor() {
            public void execute(Runnable command) {
            }
        };

        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;
                assertEquals(myExecutor, executor);
            }
        };
        stripe.requestToken(tokenId, myExecutor, null, null);
        assertAsyncCalled();
    }

    @Test
    public void createToken_should_work() {

        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;

                assertEquals("https", url.getProtocol());
                assertEquals("api.stripe.com", url.getHost());
                assertEquals("/v1/tokens", url.getPath());
                assertEquals(publishableKey + ":", url.getUserInfo());

                assertEquals("POST", method);
                assertEquals("", body);
            }
        };
        stripe.createToken(emptyCard, null, null);
        assertAsyncCalled();
    }

    @Test
    public void requestToken_should_fail_if_card_is_null() {
        RuntimeException exception = new RuntimeException("Required Parameter: 'tokenId' is required to retrieve a token.");
        stripe.asyncHTTPTask = createMockHTTPTask(200, tokenJSON, null);
        stripe.requestToken(null, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, exception));
    }

    @Test
    public void createToken_should_fail_if_card_is_null() {
        RuntimeException exception = new RuntimeException("Required Parameter: 'card' is required to create a token.");
        stripe.asyncHTTPTask = createMockHTTPTask(200, tokenJSON, null);
        stripe.createToken(null, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, exception));
    }

    @Test
    public void createToken_should_fail_gracefully_on_random_exception() {
        RuntimeException exception = new RuntimeException("TEST");
        stripe.asyncHTTPTask = createMockHTTPTask(200, tokenJSON, exception);
        stripe.createToken(emptyCard, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, exception));
        assertAsyncCalled();
    }

    @Test
    public void validateKey_should_fail_if_key_is_null() {
        try {
            stripe.setDefaultPublishableKey(null);
            fail("Expected exception");
        } catch (Stripe.InvalidTokenException e) {
            assertEquals("Invalid Publishable Key: You must use a valid publishable key to create a token.  For more info, see https://stripe.com/docs/stripe.js.", e.getMessage());
        }
    }

    @Test
    public void validateKey_should_fail_if_key_is_blank() {
        try {
            stripe.setDefaultPublishableKey("");
            fail("Expected exception");
        } catch (Stripe.InvalidTokenException e) {
            assertEquals("Invalid Publishable Key: You must use a valid publishable key to create a token.  For more info, see https://stripe.com/docs/stripe.js.", e.getMessage());
        }
    }

    @Test
    public void validateKey_should_fail_if_key_is_secret() {
        try {
            stripe.setDefaultPublishableKey("sk_foobar");
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertEquals("Invalid Publishable Key: You are using a secret key to create a token, instead of the publishable one. For more info, see https://stripe.com/docs/stripe.js", e.getMessage());
        }
    }

    @Test
    public void constructor_sets_publishable_key_and_fails_if_invalid() {
        try {
            new Stripe("sk_foobar");
            fail("Expected exception");
        } catch (RuntimeException e) {
            assertEquals("Invalid Publishable Key: You are using a secret key to create a token, instead of the publishable one. For more info, see https://stripe.com/docs/stripe.js", e.getMessage());
        }
    }

    @Test
    public void constructor_sets_publishable_key() {
        Stripe stripe = new Stripe("pk_foobar");

        stripe.asyncHTTPTask = new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                asyncCalled[0] = true;

                assertEquals("pk_foobar:", url.getUserInfo());
            }
        };
        stripe.createToken(emptyCard, null, null);
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_on_201() {
        stripe.asyncHTTPTask = createMockHTTPTask(201, tokenJSON, null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.UNKNOWN_ERROR));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_gracefully_on_empty_response_200() {
        stripe.asyncHTTPTask = createMockHTTPTask(200, null, null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, null));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_gracefully_on_bad_json_200() {
        stripe.asyncHTTPTask = createMockHTTPTask(200, "<xml>OK</xml>", null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, null));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_parse_error_json() {
        stripe.asyncHTTPTask = createMockHTTPTask(400, errorJSON, null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.InvalidRequestError, null));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_gracefully_on_bad_json_500() {
        stripe.asyncHTTPTask = createMockHTTPTask(500, "<html>Web Server Error 500</html>", null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, null));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_gracefully_on_empty_response_500() {
        stripe.asyncHTTPTask = createMockHTTPTask(500, null, null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, null));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_gracefully_on_random_exception() {
        RuntimeException exception = new RuntimeException("TEST");
        stripe.asyncHTTPTask = createMockHTTPTask(200, tokenJSON, exception);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.UnexpectedError, exception));
        assertAsyncCalled();
    }

    @Test
    public void response_handler_should_fail_with_unauthorized() {
        stripe.asyncHTTPTask = createMockHTTPTask(401, tokenJSON, null);
        stripe.requestToken(tokenId, failingSuccessHandler, expectError(StripeError.StripeErrorCode.Unauthorized, null));
        assertAsyncCalled();
    }

    private StripeErrorHandler expectError(final StripeError.StripeErrorCode errorCode, final Exception exception) {
        return new StripeErrorHandler() {
            @Override
            public void onError(StripeError actual) {
                assertEquals(errorCode, actual.errorCode);
                if (exception != null) {
                    assertEquals(exception.getMessage(), actual.developerMessage);
                }
                asyncCalled[0] = true;
            }
        };
    }

    private StripeErrorHandler expectError(final StripeError expected) {
        return new StripeErrorHandler() {
            @Override
            public void onError(StripeError actual) {
                assertEquals(expected, actual);
                asyncCalled[0] = true;
            }
        };
    }

    private void assertAsyncCalled() {
        assertTrue(asyncCalled[0]);
    }

    private AsyncHTTPInterface createMockHTTPTask(final int responseCode, final String responseBody, final RuntimeException exception) {
        return new AsyncHTTPInterface() {
            public void sendAsynchronousRequest(URL url, String method, String body, Executor executor, ResponseHandler responseHandler) {
                if (exception != null) {
                    throw exception;
                }
                responseHandler.handle(responseCode, responseBody);
            }
        };
    }

}
