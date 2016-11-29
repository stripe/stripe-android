package com.stripe.android;

import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
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
    public void androidCardFromStripeCard_properlyConvertsStandardFields() {
        Stripe stripe;
        try {
            stripe = new Stripe(DEFAULT_PUBLISHABLE_KEY);
        } catch (AuthenticationException e) {
            fail("Unexpected error: " + e.getMessage());
            return;
        }

        com.stripe.model.Card stripeCard = new com.stripe.model.Card();
        final Integer EXP_MONTH = 3;
        final Integer EXP_YEAR = 2020;
        final String NAME = "Anita Cardholder";
        final String ADDR_FIRST_LINE = "123 Burns Street";
        final String ADDR_SECOND_LINE = "Apartment 456";
        final String ADDR_STATE = "CA";
        final String ADDR_CITY = "Eureka";
        final String ADDR_ZIP = "95501";
        final String CARD_BRAND = "Visa";
        final String LAST_FOUR = "6789";
        final String FINGERPRINT = "abc123";
        final String FUNDING = "credit";
        final String COUNTRY = "us";
        final String CURRENCY = "usd";
        stripeCard.setExpMonth(EXP_MONTH);
        stripeCard.setExpYear(EXP_YEAR);
        stripeCard.setName(NAME);
        stripeCard.setAddressLine1(ADDR_FIRST_LINE);
        stripeCard.setAddressLine2(ADDR_SECOND_LINE);
        stripeCard.setAddressCity(ADDR_CITY);
        stripeCard.setAddressState(ADDR_STATE);
        stripeCard.setAddressZip(ADDR_ZIP);
        stripeCard.setAddressCountry(COUNTRY);
        stripeCard.setBrand(CARD_BRAND);
        stripeCard.setLast4(LAST_FOUR);
        stripeCard.setFingerprint(FINGERPRINT);
        stripeCard.setFunding(FUNDING);
        stripeCard.setCurrency(CURRENCY);
        stripeCard.setCountry(COUNTRY);

        Card androidCard = stripe.androidCardFromStripeCard(stripeCard);
        assertEquals(EXP_MONTH, androidCard.getExpMonth());
        assertEquals(EXP_YEAR, androidCard.getExpYear());
        assertEquals(ADDR_FIRST_LINE, androidCard.getAddressLine1());
        assertEquals(ADDR_SECOND_LINE, androidCard.getAddressLine2());
        assertEquals(ADDR_CITY, androidCard.getAddressCity());
        assertEquals(COUNTRY, androidCard.getCountry());
        assertEquals(COUNTRY, androidCard.getCountry());
        assertEquals(ADDR_ZIP, androidCard.getAddressZip());
        assertEquals(ADDR_STATE, androidCard.getAddressState());
        assertEquals(CARD_BRAND, androidCard.getBrand());
        assertEquals(LAST_FOUR, androidCard.getLast4());
        assertEquals(FINGERPRINT, androidCard.getFingerprint());
        assertEquals(FUNDING, androidCard.getFunding());
        assertEquals(CURRENCY, androidCard.getCurrency());
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
