package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;

import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.AccountParams;
import com.stripe.android.model.Address;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.SourceSepaDebitData;
import com.stripe.android.model.Token;
import com.stripe.android.testharness.JsonTestUtils;
import com.stripe.android.view.CardInputTestActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@Config(constants = BuildConfig.class, sdk = 25)
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

    private static final String FUNCTIONAL_PUBLISHABLE_KEY = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";
    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";
    private static final String TEST_CARD_NUMBER = "4242424242424242";
    private static final String TEST_BANK_ACCOUNT_NUMBER = "000123456789";
    private static final String TEST_BANK_ROUTING_NUMBER = "110000000";

    private BankAccount mBankAccount;
    private Card mCard;
    private int mYear;

    private Context mContext;

    @Before
    public void setup() {
        mContext = RuntimeEnvironment.application;
        String cvc = "123";
        int month = 12;
        Calendar rightNow = Calendar.getInstance();
        // Try to make the test card always expire next year
        mYear = rightNow.get(Calendar.YEAR) + 1;
        mCard = new Card(TEST_CARD_NUMBER, month, mYear, cvc);
        mBankAccount = new BankAccount(
                TEST_BANK_ACCOUNT_NUMBER,
                "US",
                "usd",
                TEST_BANK_ROUTING_NUMBER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldFailWithNullPublishableKey() throws AuthenticationException {
        new Stripe(mContext, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldFailWithEmptyPublishableKey() throws AuthenticationException {
        new Stripe(mContext, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldFailWithSecretKey() throws AuthenticationException {
        new Stripe(mContext, DEFAULT_SECRET_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultPublishableKeyShouldFailWhenNull() throws AuthenticationException {
        Stripe stripe = new Stripe(mContext);
        stripe.setDefaultPublishableKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultPublishableKeyShouldFailWhenEmpty() throws AuthenticationException {
        Stripe stripe = new Stripe(mContext);
        stripe.setDefaultPublishableKey("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultPublishableKeyShouldFailWithSecretKey() throws AuthenticationException {
        Stripe stripe = new Stripe(mContext);
        stripe.setDefaultPublishableKey(DEFAULT_SECRET_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void createTokenShouldFailWithNull() {
        Stripe stripe = new Stripe(mContext);
        stripe.createToken(null, null);
    }

    @Test(expected = RuntimeException.class)
    public void createTokenShouldFailWithNullCard() {
        Stripe stripe = new Stripe(mContext);
        stripe.createToken(null, DEFAULT_TOKEN_CALLBACK);
    }

    @Test(expected = RuntimeException.class)
    public void createTokenShouldFailWithNullTokenCallback() {
        Stripe stripe = new Stripe(mContext);
        stripe.createToken(DEFAULT_CARD, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTokenShouldFailWithNullPublishableKey() {
        Stripe stripe = new Stripe(mContext);
        stripe.createToken(DEFAULT_CARD, new TokenCallback() {
            @Override
            public void onError(Exception error) {
                fail("Should not call method");
            }

            @Override
            public void onSuccess(Token token) {
                fail("Should not call method");
            }
        });
    }

    @Test
    public void createTokenShouldCallTokenCreator() {
        final boolean[] tokenCreatorCalled = { false };
        Stripe stripe = getNonLoggingStripe(mContext, DEFAULT_PUBLISHABLE_KEY);
        stripe.mTokenCreator = new Stripe.TokenCreator() {
            @Override
            public void create(Map<String, Object> tokenParams,
                               String publishableKey,
                               String stripeAccount,
                               @NonNull @Token.TokenType String tokenType,
                               Executor executor, TokenCallback callback) {
                tokenCreatorCalled[0] = true;
            }
        };
        stripe.createToken(DEFAULT_CARD, DEFAULT_TOKEN_CALLBACK);
        assertTrue(tokenCreatorCalled[0]);
    }

    @Test
    public void createTokenShouldUseExecutor() {
        final Executor expectedExecutor = new Executor() {
            @Override
            public void execute(Runnable command) {
            }
        };
        Stripe stripe = getNonLoggingStripe(mContext, DEFAULT_PUBLISHABLE_KEY);
        stripe.mTokenCreator = new Stripe.TokenCreator() {
            @Override
            public void create(Map<String, Object> tokenParams,
                               String publishableKey,
                               String stripeAccount,
                               @NonNull @Token.TokenType String tokenType,
                               Executor executor,
                               TokenCallback callback) {
                assertEquals(expectedExecutor, executor);
                assertEquals(DEFAULT_PUBLISHABLE_KEY, publishableKey);
                assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
            }
        };
        stripe.createToken(DEFAULT_CARD, expectedExecutor, DEFAULT_TOKEN_CALLBACK);
    }

    @Test
    public void createTokenShouldUseProvidedKey() {
        final String expectedPublishableKey = "pk_this_one";
        Stripe stripe = getNonLoggingStripe(mContext, DEFAULT_PUBLISHABLE_KEY);
        stripe.mTokenCreator = new Stripe.TokenCreator() {
            @Override
            public void create(Map<String, Object> tokenParams,
                               String publishableKey,
                               String stripeAccount,
                               @NonNull @Token.TokenType String tokenType,
                               Executor executor,
                               TokenCallback callback) {
                assertEquals(expectedPublishableKey, publishableKey);
                assertNull(executor);
                assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
            }
        };
        stripe.createToken(DEFAULT_CARD, expectedPublishableKey, DEFAULT_TOKEN_CALLBACK);
    }

    @Test
    public void createCardTokenSynchronous_withValidData_returnsToken() {
        try {
            Stripe stripe = new Stripe(mContext, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            TestLoggingListener listener = new TestLoggingListener(true);
            stripe.setLoggingResponseListener(listener);

            Token token = stripe.createTokenSynchronous(mCard);

            assertNotNull(token);
            Card returnedCard = token.getCard();
            assertNotNull(returnedCard);
            assertNull(token.getBankAccount());
            assertEquals(Token.TYPE_CARD, token.getType());
            assertEquals(mCard.getLast4(), returnedCard.getLast4());
            assertEquals(Card.VISA, returnedCard.getBrand());
            assertEquals(mCard.getExpYear(), returnedCard.getExpYear());
            assertEquals(mCard.getExpMonth(), returnedCard.getExpMonth());
            assertEquals(Card.FUNDING_CREDIT, returnedCard.getFunding());

            assertAllLogsAreValid(listener, 2);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createCardTokenSynchronous_withValidDataAndConnectAccount_returnsToken() {
        try {
            Stripe stripe = new Stripe(mContext, "pk_test_fdjfCYpGSwAX24KUEiuaAAWX");
            stripe.setStripeAccount("acct_1Acj2PBUgO3KuWzz");
            TestLoggingListener listener = new TestLoggingListener(true);
            stripe.setLoggingResponseListener(listener);

            Token token = stripe.createTokenSynchronous(mCard);

            assertNotNull(token);
            Card returnedCard = token.getCard();
            assertNotNull(returnedCard);
            assertNull(token.getBankAccount());
            assertEquals(Token.TYPE_CARD, token.getType());
            assertEquals(mCard.getLast4(), returnedCard.getLast4());
            assertEquals(Card.VISA, returnedCard.getBrand());
            assertEquals(mCard.getExpYear(), returnedCard.getExpYear());
            assertEquals(mCard.getExpMonth(), returnedCard.getExpMonth());
            assertEquals(Card.FUNDING_CREDIT, returnedCard.getFunding());

            assertAllLogsAreValid(listener, 2);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createToken_createSource_returnsSource() {
        try {
            Stripe stripe = new Stripe(mContext, FUNCTIONAL_PUBLISHABLE_KEY);
            TestLoggingListener listener = new TestLoggingListener(true);
            stripe.setLoggingResponseListener(listener);

            Token token = stripe.createTokenSynchronous(mCard);

            assertNotNull(token);
            SourceParams sourceParams = SourceParams.createCustomParams();
            sourceParams.setType(Source.CARD);
            sourceParams.setToken(token.getId());
            Source source = stripe.createSourceSynchronous(sourceParams,
                    FUNCTIONAL_PUBLISHABLE_KEY);
            assertNotNull(source);
            assertAllLogsAreValid(listener, 4);
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createBankAccountTokenSynchronous_withValidBankAccount_returnsToken() {
        try {
            Stripe stripe = getNonLoggingStripe(mContext, FUNCTIONAL_PUBLISHABLE_KEY);

            Token token = stripe.createBankAccountTokenSynchronous(mBankAccount);
            assertNotNull(token);
            assertEquals(Token.TYPE_BANK_ACCOUNT, token.getType());
            assertNull(token.getCard());

            BankAccount returnedBankAccount = token.getBankAccount();
            String expectedLast4 = TEST_BANK_ACCOUNT_NUMBER
                    .substring(TEST_BANK_ACCOUNT_NUMBER.length() - 4);
            assertEquals(expectedLast4, returnedBankAccount.getLast4());
            assertEquals(mBankAccount.getCountryCode(), returnedBankAccount.getCountryCode());
            assertEquals(mBankAccount.getCurrency(), returnedBankAccount.getCurrency());
            assertEquals(mBankAccount.getRoutingNumber(), returnedBankAccount.getRoutingNumber());
        } catch (AuthenticationException authEx) {
            fail("Unexpected error: " + authEx.getLocalizedMessage());
        } catch (StripeException stripeEx) {
            fail("Unexpected error when connecting to Stripe API: "
                    + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withAlipayReusableParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams alipayParams = SourceParams.createAlipayReusableParams(
                "usd",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        try {
            Source alipaySource =
                    stripe.createSourceSynchronous(alipayParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(alipaySource);
            assertNotNull(alipaySource.getId());
            assertNotNull(alipaySource.getClientSecret());
            assertEquals(Source.ALIPAY, alipaySource.getType());
            assertEquals("redirect", alipaySource.getFlow());
            assertNotNull(alipaySource.getOwner());
            assertEquals("Example Payer", alipaySource.getOwner().getName());
            assertEquals("abc@def.com", alipaySource.getOwner().getEmail());
            assertEquals("usd", alipaySource.getCurrency());
            assertEquals(Source.REUSABLE, alipaySource.getUsage());
            assertNotNull(alipaySource.getRedirect());
            assertEquals("stripe://start", alipaySource.getRedirect().getReturnUrl());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withAlipaySingleUseParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams alipayParams = SourceParams.createAlipaySingleUseParams(
                1000L,
                "usd",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        try {
            Source alipaySource =
                    stripe.createSourceSynchronous(alipayParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(alipaySource);
            assertNotNull(alipaySource.getId());
            assertNotNull(alipaySource.getClientSecret());
            assertNotNull(alipaySource.getAmount());
            assertEquals(1000L, alipaySource.getAmount().longValue());
            assertEquals(Source.ALIPAY, alipaySource.getType());
            assertEquals("redirect", alipaySource.getFlow());
            assertNotNull(alipaySource.getOwner());
            assertEquals("Example Payer", alipaySource.getOwner().getName());
            assertEquals("abc@def.com", alipaySource.getOwner().getEmail());
            assertEquals("usd", alipaySource.getCurrency());
            assertEquals(Source.SINGLE_USE, alipaySource.getUsage());
            assertNotNull(alipaySource.getRedirect());
            assertEquals("stripe://start", alipaySource.getRedirect().getReturnUrl());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withBitcoinParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams bitcoinParams = SourceParams.createBitcoinParams(1000L, "usd", "abc@def.com");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("site", "google");
            put("mood", "sad");
        }};
        bitcoinParams.setMetaData(metamap);
        try {
            Source bitcoinSource =
                    stripe.createSourceSynchronous(bitcoinParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(bitcoinSource);
            assertNotNull(bitcoinSource.getId());
            assertNotNull(bitcoinSource.getClientSecret());
            assertEquals(Source.BITCOIN, bitcoinSource.getType());
            assertEquals(1000L, bitcoinSource.getAmount().longValue());
            assertNotNull(bitcoinSource.getSourceTypeData());
            assertNotNull(bitcoinSource.getOwner());
            assertNull(bitcoinSource.getSourceTypeModel());
            assertEquals("abc@def.com", bitcoinSource.getOwner().getEmail());
            assertEquals("usd", bitcoinSource.getCurrency());
            JsonTestUtils.assertMapEquals(metamap, bitcoinSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withCustomBitcoinParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);

        // This causes us to go through a different code path for the source creation,
        // since the strict "getType" method on bitcoinParams now returns "unknown".
        // Using bitcoin to test full integration, since that is a source type that we
        // accept.
        SourceParams customParams = SourceParams.createCustomParams();
        Map<String, Object> ownerMap = new HashMap<>();
        ownerMap.put("email", "abc@def.com");
        customParams.setTypeRaw("bitcoin")
                .setAmount(1000L)
                .setCurrency("usd")
                .setOwner(ownerMap);
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("site", "google");
            put("mood", "sad");
        }};
        customParams.setMetaData(metamap);

        try {
            Source bitcoinSource =
                    stripe.createSourceSynchronous(customParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(bitcoinSource);
            assertNotNull(bitcoinSource.getId());
            assertNotNull(bitcoinSource.getClientSecret());
            // We'll still get a bitcoin type back, because we'll recognize the string.
            assertEquals(Source.BITCOIN, bitcoinSource.getType());
            assertEquals(1000L, bitcoinSource.getAmount().longValue());
            assertNotNull(bitcoinSource.getSourceTypeData());
            assertNotNull(bitcoinSource.getOwner());
            assertNull(bitcoinSource.getSourceTypeModel());
            assertEquals("abc@def.com", bitcoinSource.getOwner().getEmail());
            assertEquals("usd", bitcoinSource.getCurrency());
            JsonTestUtils.assertMapEquals(metamap, bitcoinSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withBancontactParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams bancontactParams = SourceParams.createBancontactParams(
                1000L, "John Doe", "example://path", "a statement described");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("flavor", "strawberry");
            put("type", "sherbet");
        }};
        bancontactParams.setMetaData(metamap);
        try {
            Source bancontactSource =
                    stripe.createSourceSynchronous(bancontactParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(bancontactSource);
            assertNotNull(bancontactSource.getId());
            assertNotNull(bancontactSource.getClientSecret());
            assertEquals(Source.BANCONTACT, bancontactSource.getType());
            assertEquals(1000L, bancontactSource.getAmount().longValue());
            assertNotNull(bancontactSource.getSourceTypeData());
            assertNull(bancontactSource.getSourceTypeModel());
            assertNotNull(bancontactSource.getOwner());
            assertNotNull(bancontactSource.getRedirect());
            assertEquals("John Doe", bancontactSource.getOwner().getName());
            assertEquals("example://path", bancontactSource.getRedirect().getReturnUrl());
            JsonTestUtils.assertMapEquals(metamap, bancontactSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withCardParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        stripe.setDefaultPublishableKey("pk_test_dCyfhfyeO2CZkcvT5xyIDdJj");
        stripe.setStripeAccount("acct_19YLYlE7cWSP2tMo");

        Card card = new Card(CardInputTestActivity.VALID_VISA_NO_SPACES, 12, 2050, "123");
        card.setAddressCity("Sheboygan");
        card.setAddressCountry("US");
        card.setAddressLine1("123 Main St");
        card.setAddressLine2("#456");
        card.setAddressZip("53081");
        card.setAddressState("WI");
        card.setName("Winnie Hoop");
        SourceParams params = SourceParams.createCardParams(card);
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("addons", "cream");
            put("type", "halfandhalf");
        }};
        params.setMetaData(metamap);

        try {
            Source cardSource =
                    stripe.createSourceSynchronous(params);
            assertNotNull(cardSource);
            assertNotNull(cardSource.getClientSecret());
            assertNotNull(cardSource.getId());
            assertEquals(Source.CARD, cardSource.getType());
            assertNotNull(cardSource.getSourceTypeData());
            assertNotNull(cardSource.getSourceTypeModel());
            assertTrue(cardSource.getSourceTypeModel() instanceof SourceCardData);
            assertNotNull(cardSource.getOwner());
            assertNotNull(cardSource.getOwner().getAddress());
            assertEquals("Sheboygan", cardSource.getOwner().getAddress().getCity());
            assertEquals("WI", cardSource.getOwner().getAddress().getState());
            assertEquals("53081", cardSource.getOwner().getAddress().getPostalCode());
            assertEquals("123 Main St", cardSource.getOwner().getAddress().getLine1());
            assertEquals("#456", cardSource.getOwner().getAddress().getLine2());
            assertEquals("US", cardSource.getOwner().getAddress().getCountry());
            assertEquals("Winnie Hoop", cardSource.getOwner().getName());
            JsonTestUtils.assertMapEquals(metamap, cardSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_with3DSParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        Card card = new Card(CardInputTestActivity.VALID_VISA_NO_SPACES, 12, 2050, "123");
        SourceParams params = SourceParams.createCardParams(card);
        try {
            Source cardSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);

            assertNotNull(cardSource);
            assertNotNull(cardSource.getId());
            SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                    50000L,
                    "brl",
                    "example://return",
                    cardSource.getId());
            Map<String, String> metamap = new HashMap<String, String>() {{
                put("dimensions", "three");
                put("type", "beach ball");
            }};
            threeDParams.setMetaData(metamap);

            Source threeDSource =
                    stripe.createSourceSynchronous(threeDParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(threeDSource);
            assertEquals(50000L, threeDSource.getAmount().longValue());
            assertEquals("brl", threeDSource.getCurrency());
            assertNotNull(threeDSource.getClientSecret());
            assertNotNull(threeDSource.getId());
            assertNull(threeDSource.getSourceTypeModel());
            assertEquals(Source.THREE_D_SECURE, threeDSource.getType());
            assertNotNull(threeDSource.getSourceTypeData());
            JsonTestUtils.assertMapEquals(metamap, threeDSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withGiropayParams_passesIntegrationTest() {
        Stripe stripe = new Stripe(mContext, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
        TestLoggingListener listener = new TestLoggingListener(true);
        stripe.setLoggingResponseListener(listener);
        SourceParams params = SourceParams.createGiropayParams(
                2000L,
                "Mr. X",
                "example://redirect",
                "a well-described statement");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("giro", "with chicken");
            put("type", "wrap");
        }};
        params.setMetaData(metamap);
        try {
            Source giropaySource =
                    stripe.createSourceSynchronous(params);
            assertNotNull(giropaySource);
            assertNotNull(giropaySource.getClientSecret());
            assertNotNull(giropaySource.getId());
            assertEquals("eur", giropaySource.getCurrency());
            assertEquals(2000L, giropaySource.getAmount().longValue());
            assertEquals(Source.GIROPAY, giropaySource.getType());
            assertNotNull(giropaySource.getSourceTypeData());
            assertNull(giropaySource.getSourceTypeModel());
            assertNotNull(giropaySource.getOwner());
            assertNotNull(giropaySource.getRedirect());
            assertEquals("Mr. X", giropaySource.getOwner().getName());
            assertEquals("example://redirect", giropaySource.getRedirect().getReturnUrl());
            assertAllLogsAreValid(listener, 2);
            JsonTestUtils.assertMapEquals(metamap, giropaySource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withP24Params_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams p24Params = SourceParams.createP24Params(
                100,
                "eur",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        try {
            Source p24Source =
                    stripe.createSourceSynchronous(p24Params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(p24Source);
            assertNotNull(p24Source.getId());
            assertNotNull(p24Source.getClientSecret());
            assertEquals(Source.P24, p24Source.getType());
            assertEquals("redirect", p24Source.getFlow());
            assertNotNull(p24Source.getOwner());
            assertEquals("Example Payer", p24Source.getOwner().getName());
            assertEquals("abc@def.com", p24Source.getOwner().getEmail());
            assertEquals("eur", p24Source.getCurrency());
            assertEquals(Source.SINGLE_USE, p24Source.getUsage());
            assertNotNull(p24Source.getRedirect());
            assertEquals("stripe://start", p24Source.getRedirect().getReturnUrl());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withSepaDebitParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        String validIban = "DE89370400440532013000";
        SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                "sepaholder@stripe.com",
                "123 Main St",
                "Eureka",
                "90210",
                "EI");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("water source", "well");
            put("type", "brackish");
            put("value", "100000");
        }};
        params.setMetaData(metamap);
        try {
            Source sepaDebitSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(sepaDebitSource);
            assertNotNull(sepaDebitSource.getClientSecret());
            assertNotNull(sepaDebitSource.getId());
            assertEquals(Source.SEPA_DEBIT, sepaDebitSource.getType());
            assertNotNull(sepaDebitSource.getSourceTypeData());
            assertNotNull(sepaDebitSource.getOwner());
            assertNotNull(sepaDebitSource.getOwner().getAddress());
            assertNotNull(sepaDebitSource.getSourceTypeModel());
            assertTrue(sepaDebitSource.getSourceTypeModel() instanceof SourceSepaDebitData);
            assertEquals("eur", sepaDebitSource.getCurrency());
            assertEquals("Eureka", sepaDebitSource.getOwner().getAddress().getCity());
            assertEquals("90210", sepaDebitSource.getOwner().getAddress().getPostalCode());
            assertEquals("123 Main St", sepaDebitSource.getOwner().getAddress().getLine1());
            assertEquals("EI", sepaDebitSource.getOwner().getAddress().getCountry());
            assertEquals("Sepa Account Holder", sepaDebitSource.getOwner().getName());
            JsonTestUtils.assertMapEquals(metamap ,sepaDebitSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withNoEmail_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        String validIban = "DE89370400440532013000";
        SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                "123 Main St",
                "Eureka",
                "90210",
                "EI");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("water source", "well");
            put("type", "brackish");
            put("value", "100000");
        }};
        params.setMetaData(metamap);
        try {
            Source sepaDebitSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(sepaDebitSource);
            assertNotNull(sepaDebitSource.getClientSecret());
            assertNotNull(sepaDebitSource.getId());
            assertEquals(Source.SEPA_DEBIT, sepaDebitSource.getType());
            assertNotNull(sepaDebitSource.getSourceTypeData());
            assertNotNull(sepaDebitSource.getOwner());
            assertNotNull(sepaDebitSource.getOwner().getAddress());
            assertNotNull(sepaDebitSource.getSourceTypeModel());
            assertTrue(sepaDebitSource.getSourceTypeModel() instanceof SourceSepaDebitData);
            assertEquals("eur", sepaDebitSource.getCurrency());
            assertEquals("Eureka", sepaDebitSource.getOwner().getAddress().getCity());
            assertEquals("90210", sepaDebitSource.getOwner().getAddress().getPostalCode());
            assertEquals("123 Main St", sepaDebitSource.getOwner().getAddress().getLine1());
            assertEquals("EI", sepaDebitSource.getOwner().getAddress().getCountry());
            assertEquals("Sepa Account Holder", sepaDebitSource.getOwner().getName());
            JsonTestUtils.assertMapEquals(metamap ,sepaDebitSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSepaDebitSource_withNoAddress_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        String validIban = "DE89370400440532013000";
        SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                "sepaholder@stripe.com",
                null,
                "Eureka",
                "90210",
                "EI");

        try {
            Source sepaDebitSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(sepaDebitSource);
            assertNotNull(sepaDebitSource.getClientSecret());
            assertNotNull(sepaDebitSource.getId());
            assertEquals(Source.SEPA_DEBIT, sepaDebitSource.getType());
            assertNotNull(sepaDebitSource.getSourceTypeData());
            assertNotNull(sepaDebitSource.getOwner());
            assertNotNull(sepaDebitSource.getOwner().getAddress());
            assertNotNull(sepaDebitSource.getSourceTypeModel());
            assertTrue(sepaDebitSource.getSourceTypeModel() instanceof SourceSepaDebitData);
            assertEquals("eur", sepaDebitSource.getCurrency());
            assertEquals("Eureka", sepaDebitSource.getOwner().getAddress().getCity());
            assertEquals("90210", sepaDebitSource.getOwner().getAddress().getPostalCode());
            assertEquals("EI", sepaDebitSource.getOwner().getAddress().getCountry());
            assertEquals("Sepa Account Holder", sepaDebitSource.getOwner().getName());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

        @Test
    public void createSourceSynchronous_withIDealParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams params = SourceParams.createIdealParams(
                5500L,
                "Bond",
                "example://return",
                "A statement description",
                "rabobank");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("state", "quite ideal");
            put("picture", "17L");
            put("arrows", "what?");
        }};
        params.setMetaData(metamap);
        try {
            Source idealSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(idealSource);
            assertNotNull(idealSource.getClientSecret());
            assertNotNull(idealSource.getId());
            assertEquals(5500L, idealSource.getAmount().longValue());
            assertEquals(Source.IDEAL, idealSource.getType());
            assertEquals("eur", idealSource.getCurrency());
            assertNotNull(idealSource.getSourceTypeData());
            assertNotNull(idealSource.getOwner());
            assertNull(idealSource.getSourceTypeModel());
            assertEquals("Bond", idealSource.getOwner().getName());
            assertNotNull(idealSource.getRedirect());
            assertEquals("example://return", idealSource.getRedirect().getReturnUrl());
            JsonTestUtils.assertMapEquals(metamap, idealSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createSourceSynchronous_withSofortParams_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        SourceParams params = SourceParams.createSofortParams(
                70000L,
                "example://return",
                "NL",
                "a description");
        Map<String, String> metamap = new HashMap<String, String>() {{
            put("state", "soforting");
            put("repetitions", "400");
        }};
        params.setMetaData(metamap);
        try {
            Source sofortSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
            assertNotNull(sofortSource);
            assertNotNull(sofortSource.getClientSecret());
            assertNotNull(sofortSource.getId());
            assertEquals(Source.SOFORT, sofortSource.getType());
            assertEquals("eur", sofortSource.getCurrency());
            assertNotNull(sofortSource.getSourceTypeData());
            assertNull(sofortSource.getSourceTypeModel());
            assertEquals(70000L, sofortSource.getAmount().longValue());
            assertNotNull(sofortSource.getRedirect());
            assertEquals("example://return", sofortSource.getRedirect().getReturnUrl());
            JsonTestUtils.assertMapEquals(metamap, sofortSource.getMetaData());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void retrieveSourceSynchronous_withValidData_passesIntegrationTest() {
        Stripe stripe = getNonLoggingStripe(mContext);
        Card card = new Card(CardInputTestActivity.VALID_VISA_NO_SPACES, 12, 2050, "123");
        SourceParams params = SourceParams.createCardParams(card);
        try {
            Source cardSource =
                    stripe.createSourceSynchronous(params, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);

            assertNotNull(cardSource);
            assertNotNull(cardSource.getId());
            SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                    5000L,
                    "brl",
                    "example://return",
                    cardSource.getId());

            Map<String, String> metamap = new HashMap<String, String>() {{
                put("dimensions", "three");
                put("type", "beach ball");
            }};
            threeDParams.setMetaData(metamap);
            Source threeDSource =
                    stripe.createSourceSynchronous(threeDParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);

            String sourceId = threeDSource.getId();
            String clientSecret = threeDSource.getClientSecret();

            assertNotNull(sourceId);
            assertNotNull(clientSecret);

            Source retrievedSource =
                    stripe.retrieveSourceSynchronous(
                            sourceId,
                            clientSecret,
                            FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);

            // We aren't actually updating the source on the server, so the two sources should
            // be identical.
            JsonTestUtils.assertMapEquals(threeDSource.toMap(), retrievedSource.toMap());
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withValidPersonalId_passesIntegrationTest() {
        try {
            Stripe stripe = new Stripe(mContext, FUNCTIONAL_PUBLISHABLE_KEY);
            TestLoggingListener listener = new TestLoggingListener(true);
            stripe.setLoggingResponseListener(listener);

            Token token = stripe.createPiiTokenSynchronous("0123456789");
            assertNotNull(token);
            assertEquals(Token.TYPE_PII, token.getType());
            assertFalse(token.getLivemode());
            assertFalse(token.getUsed());
            assertNotNull(token.getId());
            assertAllLogsAreValid(listener, 2);
        } catch (StripeException stripeEx) {
            fail("Unexpected exception making PII token");
        }
    }

    @Test
    public void createTokenSynchronous_withValidAccount_passesIntegrationTest() {
        try {
            final Address exampleAddress = new Address
                    .Builder()
                    .setCity("SF")
                    .setCountry("US")
                    .setState("CA").build();
           final Map<String, Object> exampleLegalEntity = new HashMap<String, Object>() {{
                put("personal_address", exampleAddress.toMap());
                put("type", "individual");
                put("ssn_last_4", "1234");
                put("first_name", "Kathy");
                put("last_name", "Sun");
            }};
            Stripe stripe = new Stripe(mContext, FUNCTIONAL_PUBLISHABLE_KEY);
            TestLoggingListener listener = new TestLoggingListener(true);
            stripe.setLoggingResponseListener(listener);
            Token token = stripe.createAccountTokenSynchronous(
                    AccountParams.createAccountParams(true, exampleLegalEntity));
            assertNotNull(token);
            assertEquals(Token.TYPE_ACCOUNT, token.getType());
            assertFalse(token.getLivemode());
            assertFalse(token.getUsed());
            assertNotNull(token.getId());
            assertAllLogsAreValid(listener, 2);
        } catch (StripeException stripeEx) {
            fail(stripeEx.getMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withoutTosShown_failsIntegrationTest() {
        try {
            final Map<String, Object> exampleMapAddress = new HashMap<String, Object>() {{
                put("city", "San Francisco");
                put("country", "US");
                put("line1", "123 Market St");
                put("line2", "#345");
                put("postal_code", "94107");
                put("state", "CA");
            }};

            final Map<String, Object> exampleLegalEntity = new HashMap<String, Object>() {{
                put("personal_address", exampleMapAddress);
                put("type", "individual");
                put("ssn_last_4", "1234");
                put("first_name", "Kathy");
                put("last_name", "Sun");
            }};
            Stripe stripe = new Stripe(mContext, FUNCTIONAL_PUBLISHABLE_KEY);
            TestLoggingListener listener = new TestLoggingListener(true);
            stripe.setLoggingResponseListener(listener);
            Token token = stripe.createAccountTokenSynchronous(
                    AccountParams.createAccountParams(false, exampleLegalEntity));
            assertNotNull(token);
            assertEquals(Token.TYPE_ACCOUNT, token.getType());
            assertFalse(token.getLivemode());
            assertFalse(token.getUsed());
            assertNotNull(token.getId());
            assertAllLogsAreValid(listener, 2);
        } catch (StripeException stripeEx) {
            fail(stripeEx.getMessage());
        }
    }

    @Test
    public void createTokenSynchronous_withValidDataAndBadKey_throwsAuthenticationException() {
        try {
            // This key won't work for a real connection to the api.
            Stripe stripe = getNonLoggingStripe(mContext, DEFAULT_PUBLISHABLE_KEY);
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
    public void createTokenSynchronous_withoutKey_shouldNotLogAnything() {
        Stripe stripe = new Stripe(mContext);
        // This test should not log anything, so we set it to be theoretically capable of logging
        TestLoggingListener listener = new TestLoggingListener(true);
        stripe.setLoggingResponseListener(listener);
        try {
            stripe.createTokenSynchronous(mCard);
            fail("We shouldn't be able to make a token without a key.");
        } catch (Exception exception) {
            // Note: we're not testing the type of exception in this test.
            assertEquals(0, listener.responseList.size());
            assertEquals(0, listener.exceptionList.size());
        }
    }

    @Test
    public void createTokenSynchronous_withInvalidCardNumber_throwsCardException() {
        try {
            // This card is missing quite a few numbers.
            Card card = new Card("42424242", 12, mYear, "123");
            Stripe stripe = getNonLoggingStripe(mContext, FUNCTIONAL_PUBLISHABLE_KEY);
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
            Stripe stripe = getNonLoggingStripe(mContext);
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

    private void assertAllLogsAreValid(@NonNull TestLoggingListener listener, int expectedLogs) {
        assertEquals(expectedLogs, listener.responseList.size());
        assertEquals(0, listener.exceptionList.size());
        for(int i = 0; i < expectedLogs; i++) {
            assertEquals(200, listener.responseList.get(i).getResponseCode());
        }
    }

    private static Stripe getNonLoggingStripe(Context context) {
        Stripe nonLoggingStripe = new Stripe(context);
        nonLoggingStripe.setLoggingResponseListener(new TestLoggingListener(false));
        return nonLoggingStripe;
    }

    private static Stripe getNonLoggingStripe(Context context, String key) {
        Stripe nonLoggingStripe = new Stripe(context, key);
        nonLoggingStripe.setLoggingResponseListener(new TestLoggingListener(false));
        return nonLoggingStripe;
    }

    private static class TestLoggingListener implements StripeApiHandler.LoggingResponseListener {
        boolean mShouldLogTest;
        List<StripeResponse> responseList = new ArrayList<>();
        List<StripeException> exceptionList = new ArrayList<>();

        public TestLoggingListener(boolean shouldLogTest) {
            mShouldLogTest = shouldLogTest;
        }

        @Override
        public boolean shouldLogTest() {
            return mShouldLogTest;
        }

        @Override
        public void onLoggingResponse(StripeResponse response) {
            if (!mShouldLogTest) {
                fail("Test should not be logged.");
            }
            responseList.add(response);
        }

        @Override
        public void onStripeException(StripeException exception) {
            if (!mShouldLogTest) {
                fail("Test should not be logged.");
            }
            exceptionList.add(exception);
        }
    }
}
