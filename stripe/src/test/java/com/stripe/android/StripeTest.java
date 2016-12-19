package com.stripe.android;

import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for {@link Stripe}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StripeTest {


    private static final String DEFAULT_PUBLISHABLE_KEY = "pk_default";
    private static final String DEFAULT_SECRET_KEY = "sk_default";
    private static final Card DEFAULT_CARD = new Card(null, null, null, null);
    private static final TokenCallback DEFAULT_TOKEN_CALLBACK = new TokenCallback() {
        @Override
        public void onError(Exception error) {
        }
        @Override
        public void onSuccess(Token token) {
        }
    };

    private static final String DEFAULT_TOKEN_ID = "tok_default";

    private static final String FUNCTIONAL_PUBLISHABLE_KEY = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";
    private static final String TEST_CARD_NUMBER = "4242424242424242";

    private Card mCard;
    private int mYear;

    @Before
    public void setup() {
        String cvc = "123";
        int month = 12;
        Calendar rightNow = Calendar.getInstance();
        // Try to make the test card always expire next year
        mYear = rightNow.get(Calendar.YEAR) + 1;
        mCard = new Card(TEST_CARD_NUMBER, month, mYear, cvc);
    }

    @Test(expected = AuthenticationException.class)
    public void constructorShouldFailWithNullPublishableKey() throws AuthenticationException {
        new Stripe(null);
    }

    @Test(expected = AuthenticationException.class)
    public void constructorShouldFailWithEmptyPublishableKey() throws AuthenticationException {
        new Stripe("");
    }

    @Test(expected = AuthenticationException.class)
    public void constructorShouldFailWithSecretKey() throws AuthenticationException {
        new Stripe(DEFAULT_SECRET_KEY);
    }

    @Test(expected = AuthenticationException.class)
    public void setDefaultPublishableKeyShouldFailWhenNull() throws AuthenticationException {
        Stripe stripe = new Stripe();
        stripe.setDefaultPublishableKey(null);
    }

    @Test(expected = AuthenticationException.class)
    public void setDefaultPublishableKeyShouldFailWhenEmpty() throws AuthenticationException {
        Stripe stripe = new Stripe();
        stripe.setDefaultPublishableKey("");
    }

    @Test(expected = AuthenticationException.class)
    public void setDefaultPublishableKeyShouldFailWithSecretKey() throws AuthenticationException {
        Stripe stripe = new Stripe();
        stripe.setDefaultPublishableKey(DEFAULT_SECRET_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void requestTokenShouldFailWithNull() {
        Stripe stripe = new Stripe();
        stripe.requestToken(null, null);
    }

    @Test(expected = RuntimeException.class)
    public void requestTokenShouldFailWithNullCard() {
        Stripe stripe = new Stripe();
        stripe.requestToken(null, DEFAULT_TOKEN_CALLBACK);
    }

    @Test(expected = RuntimeException.class)
    public void requestTokenShouldFailWithNullTokencallback() {
        Stripe stripe = new Stripe();
        stripe.requestToken(DEFAULT_TOKEN_ID, null);
    }

    @Test
    public void requestTokenShouldFailWithNullPublishableKey() {
        Stripe stripe = new Stripe();
        stripe.requestToken(
                DEFAULT_TOKEN_ID, new ErrorTokenCallback(AuthenticationException.class));
    }

    @Test
    public void requestTokenShouldCallTokenRequester() {
        final boolean[] tokenRequesterCalled = { false };
        try {
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.tokenRequester = new Stripe.TokenRequester() {
                @Override
                public void request(String tokenId, String publishableKey,
                                    Executor executor, TokenCallback callback) {
                    tokenRequesterCalled[0] = true;
                }
            };
            stripe.requestToken(DEFAULT_TOKEN_ID, DEFAULT_TOKEN_CALLBACK);
            assertTrue(tokenRequesterCalled[0]);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void requestTokenShouldUseExecutor() {
        final Executor expectedExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
            }
        };

        try {
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.tokenRequester = new Stripe.TokenRequester() {
                @Override
                public void request(String tokenId, String publishableKey, Executor executor,
                                    TokenCallback callback) {
                    assertEquals(expectedExecutor, executor);
                    assertEquals(DEFAULT_TOKEN_ID, tokenId);
                    assertEquals(DEFAULT_PUBLISHABLE_KEY, publishableKey);
                    assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
                }
            };
            stripe.requestToken(DEFAULT_TOKEN_ID, expectedExecutor, DEFAULT_TOKEN_CALLBACK);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void requestTokenShouldUseProvidedKey() {
        final String expectedPublishableKey = "pk_this_one";

        try {
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.tokenRequester = new Stripe.TokenRequester() {
                @Override
                public void request(String tokenId, String publishableKey, Executor executor,
                                    TokenCallback callback) {
                    assertEquals(expectedPublishableKey, publishableKey);
                    assertEquals(DEFAULT_TOKEN_ID, tokenId);
                    assertNull(executor);
                    assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
                }
            };
            stripe.requestToken(DEFAULT_TOKEN_ID, expectedPublishableKey, DEFAULT_TOKEN_CALLBACK);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test(expected = RuntimeException.class)
    public void createTokenShouldFailWithNull() {
        Stripe stripe = new Stripe();
        stripe.createToken(null, null);
    }

    @Test(expected = RuntimeException.class)
    public void createTokenShouldFailWithNullCard() {
        Stripe stripe = new Stripe();
        stripe.createToken(null, DEFAULT_TOKEN_CALLBACK);
    }

    @Test(expected = RuntimeException.class)
    public void createTokenShouldFailWithNullTokencallback() {
        Stripe stripe = new Stripe();
        stripe.createToken(DEFAULT_CARD, null);
    }

    @Test
    public void createTokenShouldFailWithNullPublishableKey() {
        Stripe stripe = new Stripe();
        stripe.createToken(DEFAULT_CARD, new ErrorTokenCallback(AuthenticationException.class));
    }

    @Test
    public void createTokenShouldCallTokenCreator() {
        final boolean[] tokenCreatorCalled = { false };
        try {
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.tokenCreator = new Stripe.TokenCreator() {
                @Override
                public void create(Card card, String publishableKey,
                                   Executor executor, TokenCallback callback) {
                    tokenCreatorCalled[0] = true;
                }
            };
            stripe.createToken(DEFAULT_CARD, DEFAULT_TOKEN_CALLBACK);
            assertTrue(tokenCreatorCalled[0]);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void createTokenShouldUseExecutor() {
        final Executor expectedExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
            }
        };

        try {
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.tokenCreator = new Stripe.TokenCreator() {
                @Override
                public void create(Card card, String publishableKey,
                                   Executor executor, TokenCallback callback) {
                    assertEquals(expectedExecutor, executor);
                    assertEquals(DEFAULT_CARD, card);
                    assertEquals(DEFAULT_PUBLISHABLE_KEY, publishableKey);
                    assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
                }
            };
            stripe.createToken(DEFAULT_CARD, expectedExecutor, DEFAULT_TOKEN_CALLBACK);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void createTokenShouldUseProvidedKey() {
        final String expectedPublishableKey = "pk_this_one";
        try {
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.tokenCreator = new Stripe.TokenCreator() {
                @Override
                public void create(Card card, String publishableKey,
                                   Executor executor, TokenCallback callback) {
                    assertEquals(expectedPublishableKey, publishableKey);
                    assertEquals(DEFAULT_CARD, card);
                    assertNull(executor);
                    assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
                }
            };
            stripe.createToken(DEFAULT_CARD, expectedPublishableKey, DEFAULT_TOKEN_CALLBACK);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withValidData_returnsToken() {
        try {
            Stripe stripe = new Stripe(FUNCTIONAL_PUBLISHABLE_KEY);
            Token token = stripe.createTokenSynchronous(mCard);

            assertNotNull(token);
            Card returnedCard = token.getCard();
            assertNotNull(returnedCard);
            assertEquals(mCard.getLast4(), returnedCard.getLast4());
            assertEquals(Card.VISA, returnedCard.getBrand());
            assertEquals(mCard.getExpYear(), returnedCard.getExpYear());
            assertEquals(mCard.getExpMonth(), returnedCard.getExpMonth());
            assertEquals(Card.FUNDING_CREDIT, returnedCard.getFunding());
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withValidDataAndBadKey_throwsAuthenticationException() {
        try {
            // This key won't work for a real connection to the api.
            Stripe stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
            stripe.createTokenSynchronous(mCard);
            fail("Expecting an error, but did not get one.");
        } catch (AuthenticationException authEx) {
            String message = authEx.getMessage();
            assertTrue(message.startsWith("Invalid API Key provided"));
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withInvalidCardNumber_throwsCardException() {
        try {
            // This card is missing quite a few numbers.
            Card card = new Card("42424242", 12, mYear, "123");
            Stripe stripe = new Stripe(FUNCTIONAL_PUBLISHABLE_KEY);
            Token token = stripe.createTokenSynchronous(card);
            fail("Expecting an exception, but created a token instead: " + token.toString());
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (CardException cardException) {
            assertTrue(cardException.getMessage().startsWith("Your card number is incorrect."));
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withExpiredCard_throwsCardException() {
        try {
            // This card is missing quite a few numbers.
            Card card = new Card("4242424242424242", 11, 2015, "123");
            Stripe stripe = new Stripe();
            Token token = stripe.createTokenSynchronous(card, FUNCTIONAL_PUBLISHABLE_KEY);
            fail("Expecting an exception, but created a token instead: " + token.toString());
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (CardException cardException) {
            assertTrue(cardException.getMessage()
                    .startsWith("Your card's expiration year is invalid."));
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    private static class ErrorTokenCallback implements TokenCallback {
        final Class<?> expectedError;

        public ErrorTokenCallback(Class<?> expectedError) {
            this.expectedError = expectedError;
        }

        @Override
        public void onError(Exception error) {
            assertEquals(expectedError, error.getClass());
        }

        @Override
        public void onSuccess(Token token) {
            fail("onSuccess should not be called");
        }
    }
}
