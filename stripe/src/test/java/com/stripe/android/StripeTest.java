package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.AccountParams;
import com.stripe.android.model.Address;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
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

import java.util.Calendar;
import java.util.HashMap;
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
@SuppressWarnings({"ConstantConditions", "deprecation"})
@RunWith(RobolectricTestRunner.class)
public class StripeTest {
    // publishable keys
    private static final String CONNECT_ACCOUNT_PK = "pk_test_dCyfhfyeO2CZkcvT5xyIDdJj";
    private static final String LOGGING_PK = "pk_test_6pRNASCoBOKtIshFeQd4XMUh";
    private static final String NON_LOGGING_PK = "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";
    private static final String DEFAULT_SECRET_KEY = "sk_default";

    private static final Card DEFAULT_CARD = Card.create(null, null, null, null);
    private static final TokenCallback DEFAULT_TOKEN_CALLBACK = new TokenCallback() {
        @Override
        public void onError(@NonNull Exception error) {
        }
        @Override
        public void onSuccess(@NonNull Token token) {
        }
    };

    private static final String TEST_CARD_NUMBER = "4242424242424242";
    private static final String TEST_BANK_ACCOUNT_NUMBER = "000123456789";
    private static final String TEST_BANK_ROUTING_NUMBER = "110000000";

    private static final int YEAR = Calendar.getInstance().get(Calendar.YEAR) + 1;
    private static final Card CARD = Card.create(TEST_CARD_NUMBER, 12, YEAR, "123");
    private static final BankAccount BANK_ACCOUNT = new BankAccount(
            TEST_BANK_ACCOUNT_NUMBER,
            "US",
            "usd",
            TEST_BANK_ROUTING_NUMBER);
    private static final SourceParams CARD_SOURCE_PARAMS = SourceParams.createCardParams(CARD);

    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldFailWithNullPublishableKey() {
        new Stripe(mContext, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldFailWithEmptyPublishableKey() {
        new Stripe(mContext, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldFailWithSecretKey() {
        new Stripe(mContext, DEFAULT_SECRET_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultPublishableKeyShouldFailWhenNull() {
        Stripe stripe = new Stripe(mContext);
        stripe.setDefaultPublishableKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultPublishableKeyShouldFailWhenEmpty() {
        Stripe stripe = new Stripe(mContext);
        stripe.setDefaultPublishableKey("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setDefaultPublishableKeyShouldFailWithSecretKey() {
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
            public void onError(@NonNull Exception error) {
                fail("Should not call method");
            }

            @Override
            public void onSuccess(@NonNull Token token) {
                fail("Should not call method");
            }
        });
    }

    @Test
    public void createTokenShouldCallTokenCreator() {
        final boolean[] tokenCreatorCalled = { false };
        final Stripe stripe = createNonLoggingStripe(
                new Stripe.TokenCreator() {
                    @Override
                    public void create(@NonNull Map<String, Object> tokenParams,
                                       @NonNull RequestOptions requestOptions,
                                       @NonNull @Token.TokenType String tokenType,
                                       @Nullable Executor executor,
                                       @NonNull TokenCallback callback) {
                        tokenCreatorCalled[0] = true;
                    }
                });
        stripe.createToken(DEFAULT_CARD, DEFAULT_TOKEN_CALLBACK);
        assertTrue(tokenCreatorCalled[0]);
    }

    @Test
    public void createTokenShouldUseExecutor() {
        final Executor expectedExecutor = new Executor() {
            @Override
            public void execute(@NonNull Runnable command) {
            }
        };
        final Stripe stripe = createNonLoggingStripe(
                new Stripe.TokenCreator() {
                    @Override
                    public void create(@NonNull Map<String, Object> tokenParams,
                                       @NonNull RequestOptions requestOptions,
                                       @NonNull @Token.TokenType String tokenType,
                                       @Nullable Executor executor,
                                       @NonNull TokenCallback callback) {
                        assertEquals(expectedExecutor, executor);
                        assertEquals(NON_LOGGING_PK, requestOptions.getPublishableApiKey());
                        assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
                    }
                });
        stripe.createToken(DEFAULT_CARD, expectedExecutor, DEFAULT_TOKEN_CALLBACK);
    }

    @Test
    public void createTokenShouldUseProvidedKey() {
        final Stripe stripe = createNonLoggingStripe(
                new Stripe.TokenCreator() {
                    @Override
                    public void create(@NonNull Map<String, Object> tokenParams,
                                       @NonNull RequestOptions requestOptions,
                                       @NonNull @Token.TokenType String tokenType,
                                       @Nullable Executor executor,
                                       @NonNull TokenCallback callback) {
                        assertEquals(NON_LOGGING_PK, requestOptions.getPublishableApiKey());
                        assertNull(executor);
                        assertEquals(DEFAULT_TOKEN_CALLBACK, callback);
                    }
                });
        stripe.createToken(DEFAULT_CARD, DEFAULT_TOKEN_CALLBACK);
    }

    @Test
    public void createCardTokenSynchronous_withValidData_returnsToken()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createTokenSynchronous(CARD);

        assertNotNull(token);
        Card returnedCard = token.getCard();
        assertNotNull(returnedCard);
        assertNull(token.getBankAccount());
        assertEquals(Token.TYPE_CARD, token.getType());
        assertEquals(CARD.getLast4(), returnedCard.getLast4());
        assertEquals(Card.VISA, returnedCard.getBrand());
        assertEquals(CARD.getExpYear(), returnedCard.getExpYear());
        assertEquals(CARD.getExpMonth(), returnedCard.getExpMonth());
        assertEquals(Card.FUNDING_CREDIT, returnedCard.getFunding());
    }

    @Test
    public void createCardTokenSynchronous_withValidDataAndConnectAccount_returnsToken()
            throws StripeException {
        final Stripe stripe = new Stripe(mContext, "pk_test_fdjfCYpGSwAX24KUEiuaAAWX");
        stripe.setStripeAccount("acct_1Acj2PBUgO3KuWzz");

        final Token token = stripe.createTokenSynchronous(CARD);

        assertNotNull(token);
        Card returnedCard = token.getCard();
        assertNotNull(returnedCard);
        assertNull(token.getBankAccount());
        assertEquals(Token.TYPE_CARD, token.getType());
        assertEquals(CARD.getLast4(), returnedCard.getLast4());
        assertEquals(Card.VISA, returnedCard.getBrand());
        assertEquals(CARD.getExpYear(), returnedCard.getExpYear());
        assertEquals(CARD.getExpMonth(), returnedCard.getExpMonth());
        assertEquals(Card.FUNDING_CREDIT, returnedCard.getFunding());
    }

    @Test
    public void createToken_createSource_returnsSource()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createTokenSynchronous(CARD);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createCustomParams();
        sourceParams.setType(Source.CARD);
        sourceParams.setToken(token.getId());

        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
    }

    @Test
    public void createToken_createSourceWithTokenToSourceParams_returnsSource()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createTokenSynchronous(CARD);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId());

        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
    }

    @Test
    public void createBankAccountToken() {
        createNonLoggingStripe()
                .createBankAccountToken(new BankAccount(
                        "Jane Austen",
                        BankAccount.TYPE_INDIVIDUAL,
                        "STRIPE TEST BANK",
                        "US",
                        "usd",
                        "1JWtPxqbdX5Gamtc",
                        "6789",
                        "110000000"),
                new TokenCallback() {
                    @Override
                    public void onSuccess(@NonNull Token result) {
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                    }
                });
    }

    @Test
    public void createPiiToken() {
        createNonLoggingStripe().createPiiToken("123-45-6789",
                new TokenCallback() {
                    @Override
                    public void onSuccess(@NonNull Token result) {

                    }

                    @Override
                    public void onError(@NonNull Exception e) {

                    }
                });
    }

    @Test
    public void createSource() {
        createNonLoggingStripe().createSource(CARD_SOURCE_PARAMS,
                new SourceCallback() {
                    @Override
                    public void onSuccess(@NonNull Source result) {

                    }

                    @Override
                    public void onError(@NonNull Exception e) {

                    }
                });
    }

    @Test
    public void createCvcUpdateToken() {
        createNonLoggingStripe().createCvcUpdateToken("123",
                new TokenCallback() {
                    @Override
                    public void onSuccess(@NonNull Token result) {

                    }

                    @Override
                    public void onError(@NonNull Exception e) {

                    }
                });
    }

    @Test
    public void createBankAccountTokenSynchronous_withValidBankAccount_returnsToken()
            throws StripeException {
        Stripe stripe = createNonLoggingStripe();

        Token token = stripe.createBankAccountTokenSynchronous(BANK_ACCOUNT);
        assertNotNull(token);
        assertEquals(Token.TYPE_BANK_ACCOUNT, token.getType());
        assertNull(token.getCard());

        BankAccount returnedBankAccount = token.getBankAccount();
        assertNotNull(returnedBankAccount);
        String expectedLast4 = TEST_BANK_ACCOUNT_NUMBER
                .substring(TEST_BANK_ACCOUNT_NUMBER.length() - 4);
        assertEquals(expectedLast4, returnedBankAccount.getLast4());
        assertEquals(BANK_ACCOUNT.getCountryCode(), returnedBankAccount.getCountryCode());
        assertEquals(BANK_ACCOUNT.getCurrency(), returnedBankAccount.getCurrency());
        assertEquals(BANK_ACCOUNT.getRoutingNumber(), returnedBankAccount.getRoutingNumber());
    }

    @Test
    public void createSourceSynchronous_withAlipayReusableParams_passesIntegrationTest()
            throws StripeException {
        Stripe stripe = createNonLoggingStripe();
        SourceParams alipayParams = SourceParams.createAlipayReusableParams(
                "usd",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        final Source alipaySource = stripe.createSourceSynchronous(alipayParams);
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
    }

    @Test
    public void createSourceSynchronous_withAlipaySingleUseParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        SourceParams alipayParams = SourceParams.createAlipaySingleUseParams(
                1000L,
                "usd",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        final Source alipaySource = stripe.createSourceSynchronous(alipayParams);
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
    }

    @Test
    public void createSourceSynchronous_withBancontactParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final SourceParams bancontactParams = SourceParams.createBancontactParams(
                1000L,
                "John Doe",
                "example://path",
                "a statement described",
                "en");
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("flavor", "strawberry");
            put("type", "sherbet");
        }};
        bancontactParams.setMetaData(metamap);

        final Source bancontactSource = stripe.createSourceSynchronous(bancontactParams);
        assertNotNull(bancontactSource);
        assertNotNull(bancontactSource.getId());
        assertNotNull(bancontactSource.getClientSecret());
        assertNotNull(bancontactSource.getAmount());
        assertEquals(Source.BANCONTACT, bancontactSource.getType());
        assertEquals(1000L, bancontactSource.getAmount().longValue());
        assertNotNull(bancontactSource.getSourceTypeData());
        assertNull(bancontactSource.getSourceTypeModel());
        assertNotNull(bancontactSource.getOwner());
        assertNotNull(bancontactSource.getRedirect());
        assertEquals("John Doe", bancontactSource.getOwner().getName());
        assertEquals("example://path", bancontactSource.getRedirect().getReturnUrl());
        JsonTestUtils.assertMapEquals(metamap, bancontactSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withCardParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe(CONNECT_ACCOUNT_PK);
        stripe.setStripeAccount("acct_28DT589O8KAxCGbLmxyZ");

        final Card card = new Card.Builder(CardInputTestActivity.VALID_VISA_NO_SPACES, 12, 2050, "123")
                .addressCity("Sheboygan")
                .addressCountry("US")
                .addressLine1("123 Main St")
                .addressLine2("#456")
                .addressZip("53081")
                .addressState("WI")
                .name("Winnie Hoop")
                .build();
        final SourceParams params = SourceParams.createCardParams(card);
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("addons", "cream");
            put("type", "halfandhalf");
        }};
        params.setMetaData(metamap);

        final Source cardSource = stripe.createSourceSynchronous(params);
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
    }

    @Test
    public void createSourceSynchronous_with3DSParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        Card card = Card.create(CardInputTestActivity.VALID_VISA_NO_SPACES, 12, 2050, "123");
        SourceParams params = SourceParams.createCardParams(card);

        final Source cardSource = stripe.createSourceSynchronous(params);
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

        final Source threeDSource = stripe.createSourceSynchronous(threeDParams);
        assertNotNull(threeDSource);
        assertNotNull(threeDSource.getAmount());
        assertEquals(50000L, threeDSource.getAmount().longValue());
        assertEquals("brl", threeDSource.getCurrency());
        assertNotNull(threeDSource.getClientSecret());
        assertNotNull(threeDSource.getId());
        assertNull(threeDSource.getSourceTypeModel());
        assertEquals(Source.THREE_D_SECURE, threeDSource.getType());
        assertNotNull(threeDSource.getSourceTypeData());
        JsonTestUtils.assertMapEquals(metamap, threeDSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withGiropayParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final SourceParams params = SourceParams.createGiropayParams(
                2000L,
                "Mr. X",
                "example://redirect",
                "a well-described statement");
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("giro", "with chicken");
            put("type", "wrap");
        }};
        params.setMetaData(metamap);

        final Source giropaySource = stripe.createSourceSynchronous(params);
        assertNotNull(giropaySource);
        assertNotNull(giropaySource.getClientSecret());
        assertNotNull(giropaySource.getId());
        assertNotNull(giropaySource.getAmount());
        assertEquals("eur", giropaySource.getCurrency());
        assertEquals(2000L, giropaySource.getAmount().longValue());
        assertEquals(Source.GIROPAY, giropaySource.getType());
        assertNotNull(giropaySource.getSourceTypeData());
        assertNull(giropaySource.getSourceTypeModel());
        assertNotNull(giropaySource.getOwner());
        assertNotNull(giropaySource.getRedirect());
        assertEquals("Mr. X", giropaySource.getOwner().getName());
        assertEquals("example://redirect", giropaySource.getRedirect().getReturnUrl());
        JsonTestUtils.assertMapEquals(metamap, giropaySource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withP24Params_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final SourceParams p24Params = SourceParams.createP24Params(
                100,
                "eur",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        final Source p24Source = stripe.createSourceSynchronous(p24Params);
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
    }

    @Test
    public void createSourceSynchronous_withSepaDebitParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final String validIban = "DE89370400440532013000";
        final SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                "sepaholder@stripe.com",
                "123 Main St",
                "Eureka",
                "90210",
                "EI");
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("water source", "well");
            put("type", "brackish");
            put("value", "100000");
        }};
        params.setMetaData(metamap);

        final Source sepaDebitSource = stripe.createSourceSynchronous(params);
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
    }


    @Test
    public void createSourceSynchronous_withSepaDebitParamsWithMinimalValues_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final String validIban = "DE89370400440532013000";
        final SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                null,
                null,
                null,
                null,
                null);
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("water source", "well");
            put("type", "brackish");
            put("value", "100000");
        }};
        params.setMetaData(metamap);
        final Source sepaDebitSource = stripe.createSourceSynchronous(params);
        assertNotNull(sepaDebitSource);
        assertNotNull(sepaDebitSource.getClientSecret());
        assertNotNull(sepaDebitSource.getId());
        assertEquals(Source.SEPA_DEBIT, sepaDebitSource.getType());
        JsonTestUtils.assertMapEquals(metamap ,sepaDebitSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withNoEmail_passesIntegrationTest()
            throws StripeException {
        Stripe stripe = createNonLoggingStripe();
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

        final Source sepaDebitSource = stripe.createSourceSynchronous(params);
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
    }

    @Test
    public void createSepaDebitSource_withNoAddress_passesIntegrationTest() throws StripeException {
        Stripe stripe = createNonLoggingStripe();
        String validIban = "DE89370400440532013000";
        SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                "sepaholder@stripe.com",
                null,
                "Eureka",
                "90210",
                "EI");

        final Source sepaDebitSource = stripe.createSourceSynchronous(params);
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
    }

    @Test
    public void createSourceSynchronous_withiDEALParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final SourceParams params = SourceParams.createIdealParams(
                5500L,
                "Bond",
                "example://return",
                "A statement description",
                "rabobank");
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("state", "quite ideal");
            put("picture", "17L");
            put("arrows", "what?");
        }};
        params.setMetaData(metamap);

        final Source idealSource = stripe.createSourceSynchronous(params);
        assertNotNull(idealSource);
        assertNotNull(idealSource.getClientSecret());
        assertNotNull(idealSource.getId());
        assertNotNull(idealSource.getAmount());
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
    }

    @Test
    public void createSourceSynchronous_withiDEALParamsNoStatement_doesNotIgnoreBank()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final String bankName = "rabobank";
        final SourceParams params = SourceParams.createIdealParams(
                5500L,
                "Bond",
                "example://return",
                null,
                bankName);
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("state", "quite ideal");
            put("picture", "17L");
            put("arrows", "what?");
        }};
        params.setMetaData(metamap);

        final Source idealSource = stripe.createSourceSynchronous(params);
        assertNotNull(idealSource);
        assertNotNull(idealSource.getClientSecret());
        assertNotNull(idealSource.getId());
        assertNotNull(idealSource.getAmount());
        assertEquals(5500L, idealSource.getAmount().longValue());
        assertEquals(Source.IDEAL, idealSource.getType());
        assertEquals("eur", idealSource.getCurrency());
        assertNotNull(idealSource.getSourceTypeData());
        assertNotNull(idealSource.getOwner());
        assertNull(idealSource.getSourceTypeModel());
        assertEquals("Bond", idealSource.getOwner().getName());
        assertNotNull(idealSource.getRedirect());
        assertEquals(bankName, idealSource.getSourceTypeData().get("bank"));
        assertEquals("example://return", idealSource.getRedirect().getReturnUrl());
        JsonTestUtils.assertMapEquals(metamap, idealSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withiDEALParamsNoName_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        final String bankName = "rabobank";
        final SourceParams params = SourceParams.createIdealParams(
                5500L,
                null,
                "example://return",
                null,
                bankName);
        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("state", "quite ideal");
            put("picture", "17L");
            put("arrows", "what?");
        }};
        params.setMetaData(metamap);

        final Source idealSource = stripe.createSourceSynchronous(params);
        assertNotNull(idealSource);
        assertNotNull(idealSource.getClientSecret());
        assertNotNull(idealSource.getId());
        assertNotNull(idealSource.getAmount());
        assertEquals(5500L, idealSource.getAmount().longValue());
        assertEquals(Source.IDEAL, idealSource.getType());
        assertEquals("eur", idealSource.getCurrency());
        assertNotNull(idealSource.getSourceTypeData());
        assertNotNull(idealSource.getOwner());
        assertNull(idealSource.getSourceTypeModel());
        assertNotNull(idealSource.getRedirect());
        assertEquals(bankName, idealSource.getSourceTypeData().get("bank"));
        assertNull(idealSource.getOwner().getName());
        assertEquals("example://return", idealSource.getRedirect().getReturnUrl());
        JsonTestUtils.assertMapEquals(metamap, idealSource.getMetaData());
    }


    @Test
    public void createSourceSynchronous_withSofortParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
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

        final Source sofortSource = stripe.createSourceSynchronous(params);
        assertNotNull(sofortSource);
        assertNotNull(sofortSource.getClientSecret());
        assertNotNull(sofortSource.getId());
        assertNotNull(sofortSource.getAmount());
        assertEquals(Source.SOFORT, sofortSource.getType());
        assertEquals("eur", sofortSource.getCurrency());
        assertNotNull(sofortSource.getSourceTypeData());
        assertNull(sofortSource.getSourceTypeModel());
        assertEquals(70000L, sofortSource.getAmount().longValue());
        assertNotNull(sofortSource.getRedirect());
        assertEquals("example://return", sofortSource.getRedirect().getReturnUrl());
        JsonTestUtils.assertMapEquals(metamap, sofortSource.getMetaData());
    }

    @Test
    public void retrieveSourceSynchronous_withValidData_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createNonLoggingStripe();
        Card card = Card.create(CardInputTestActivity.VALID_VISA_NO_SPACES, 12, 2050, "123");
        SourceParams params = SourceParams.createCardParams(card);

        final Source cardSource = stripe.createSourceSynchronous(params);

        assertNotNull(cardSource);
        assertNotNull(cardSource.getId());
        SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                5000L,
                "brl",
                "example://return",
                cardSource.getId());

        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("dimensions", "three");
            put("type", "beach ball");
        }};
        threeDParams.setMetaData(metamap);
        final Source threeDSource = stripe.createSourceSynchronous(threeDParams);
        assertNotNull(threeDSource);

        final String sourceId = threeDSource.getId();
        final String clientSecret = threeDSource.getClientSecret();
        assertNotNull(sourceId);
        assertNotNull(clientSecret);

        final Source retrievedSource = stripe.retrieveSourceSynchronous(sourceId, clientSecret);

        // We aren't actually updating the source on the server, so the two sources should
        // be identical.
        JsonTestUtils.assertMapEquals(threeDSource.toMap(), retrievedSource.toMap());
    }

    @Test
    public void createTokenSynchronous_withValidPersonalId_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createPiiTokenSynchronous("0123456789");
        assertNotNull(token);
        assertEquals(Token.TYPE_PII, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createTokenSynchronous_withValidCvc_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();

        final Token token = stripe.createCvcUpdateTokenSynchronous("1234");
        assertNotNull(token);
        assertEquals(Token.TYPE_CVC_UPDATE, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
        assertTrue(token.getId().startsWith("cvctok_"));
    }

    @Test
    public void createAccountTokenSynchronous_withIndividualEntity_passesIntegrationTest()
            throws StripeException {
        final Address exampleAddress = new Address
                .Builder()
                .setCity("SF")
                .setCountry("US")
                .setState("CA").build();
        final Map<String, Object> businessData = new HashMap<String, Object>() {{
            put("address", exampleAddress.toMap());
            put("ssn_last_4", "1234");
            put("first_name", "Kathy");
            put("last_name", "Sun");
        }};

        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(true,
                        AccountParams.BusinessType.Individual, businessData));
        assertNotNull(token);
        assertEquals(Token.TYPE_ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withCompanyEntity_isSuccessful()
            throws StripeException {
        final Address exampleAddress = new Address
                .Builder()
                .setCity("SF")
                .setCountry("US")
                .setState("CA").build();
        final Map<String, Object> businessData = new HashMap<String, Object>() {{
            put("address", exampleAddress.toMap());
            put("tax_id", "123-23-1234");
            put("name", "My Corp.");
        }};

        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(true,
                        AccountParams.BusinessType.Company, businessData));
        assertNotNull(token);
        assertEquals(Token.TYPE_ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutTosShown_isSuccessful()
            throws StripeException {
        final Address address = new Address.Builder()
                .setCity("SF")
                .setCountry("US")
                .setState("CA").build();
        final Map<String, Object> businessData = new HashMap<String, Object>() {{
            put("address", address.toMap());
            put("ssn_last_4", "1234");
            put("first_name", "Kathy");
            put("last_name", "Sun");
        }};

        Stripe stripe = createLoggingStripe();
        Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(false,
                        AccountParams.BusinessType.Individual, businessData));
        assertNotNull(token);
        assertEquals(Token.TYPE_ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutBusinessData_isValid()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(false,
                        AccountParams.BusinessType.Individual, null));
        assertNotNull(token);
        assertEquals(Token.TYPE_ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutBusinessType_isValid()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(false,
                        null, null));
        assertNotNull(token);
        assertEquals(Token.TYPE_ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createTokenSynchronous_withValidDataAndBadKey_throwsAuthenticationException() {
        try {
            // This key won't work for a real connection to the api.
            Stripe stripe = createNonLoggingStripe("pk_test");
            stripe.createTokenSynchronous(CARD);
            fail("Expecting an error, but did not get one.");
        } catch (AuthenticationException authEx) {
            String message = authEx.getMessage();
            assertTrue(message.startsWith("Invalid API Key provided"));
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void createTokenSynchronous_withoutKey_shouldThrowException()
            throws StripeException {
        final Stripe stripe = new Stripe(mContext);
        // This test should not log anything, so we set it to be theoretically capable of logging
        stripe.createTokenSynchronous(CARD);
    }

    @Test
    public void createTokenSynchronous_withInvalidCardNumber_throwsCardException() {
        try {
            // This card is missing quite a few numbers.
            Card card = Card.create("42424242", 12, YEAR, "123");
            Stripe stripe = createNonLoggingStripe();
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
            Card card = Card.create("4242424242424242", 11, 2015, "123");
            Stripe stripe = createNonLoggingStripe();
            Token token = stripe.createTokenSynchronous(card);
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

    @Test
    public void createPaymentMethodSynchronous_withCard()
            throws StripeException {
        final PaymentMethod.BillingDetails expectedBillingDetails =
                new PaymentMethod.BillingDetails.Builder()
                        .setName("Home")
                        .setEmail("me@example.com")
                        .setPhone("1-800-555-1234")
                        .setAddress(new Address.Builder()
                                .setLine1("123 Main St")
                                .setCity("Los Angeles")
                                .setState("CA")
                                .setCountry("US")
                                .build())
                        .build();
        final PaymentMethod.Card expectedCard = new PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setChecks(new PaymentMethod.Card.Checks.Builder()
                        .setAddressLine1Check("unchecked")
                        .setAddressPostalCodeCheck(null)
                        .setCvcCheck("unchecked")
                        .build())
                .setCountry("US")
                .setExpiryMonth(8)
                .setExpiryYear(2022)
                .setFunding("credit")
                .setLast4("4242")
                .setThreeDSecureUsage(new PaymentMethod.Card.ThreeDSecureUsage.Builder()
                        .setSupported(true)
                        .build())
                .setWallet(null)
                .build();

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        new PaymentMethodCreateParams.Card.Builder()
                                .setNumber("4242424242424242")
                                .setExpiryMonth(8)
                                .setExpiryYear(2022)
                                .setCvc("123")
                                .build(),
                        expectedBillingDetails);
        final Stripe stripe = createNonLoggingStripe();
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(expectedBillingDetails, createdPaymentMethod.billingDetails);
        assertEquals(expectedCard, createdPaymentMethod.card);
    }

    @Test
    public void createPaymentMethod_withCardToken()
            throws StripeException {
        final Stripe stripe = createLoggingStripe();
        final Token token = stripe.createTokenSynchronous(CARD);

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Card.create(token.getId()),
                        null);
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);

        final PaymentMethod.Card expectedPaymentMethodCard = new PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setCountry("US")
                .setExpiryMonth(12)
                .setExpiryYear(2020)
                .setFunding("credit")
                .setLast4("4242")
                .setChecks(new PaymentMethod.Card.Checks.Builder()
                        .setCvcCheck("unchecked")
                        .build())
                .setThreeDSecureUsage(new PaymentMethod.Card.ThreeDSecureUsage.Builder()
                        .setSupported(true)
                        .build())
                .build();
        assertEquals(expectedPaymentMethodCard, createdPaymentMethod.card);
    }

    @Test
    public void createPaymentMethodSynchronous_withCardAndMetadata()
            throws StripeException {
        final PaymentMethod.BillingDetails expectedBillingDetails =
                new PaymentMethod.BillingDetails.Builder()
                        .setName("Home")
                        .setEmail("me@example.com")
                        .setPhone("1-800-555-1234")
                        .setAddress(new Address.Builder()
                                .setLine1("123 Main St")
                                .setCity("Los Angeles")
                                .setState("CA")
                                .setCountry("US")
                                .build())
                        .build();
        final PaymentMethod.Card expectedCard = new PaymentMethod.Card.Builder()
                .setBrand("visa")
                .setChecks(new PaymentMethod.Card.Checks.Builder()
                        .setAddressLine1Check("unchecked")
                        .setAddressPostalCodeCheck(null)
                        .setCvcCheck("unchecked")
                        .build())
                .setCountry("US")
                .setExpiryMonth(8)
                .setExpiryYear(2022)
                .setFunding("credit")
                .setLast4("4242")
                .setThreeDSecureUsage(new PaymentMethod.Card.ThreeDSecureUsage.Builder()
                        .setSupported(true)
                        .build())
                .setWallet(null)
                .build();

        final Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", "123456789");

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        new PaymentMethodCreateParams.Card.Builder()
                                .setNumber("4242424242424242")
                                .setExpiryMonth(8)
                                .setExpiryYear(2022)
                                .setCvc("123")
                                .build(),
                        expectedBillingDetails,
                        metadata);
        final Stripe stripe = createNonLoggingStripe();
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(expectedBillingDetails, createdPaymentMethod.billingDetails);
        assertEquals(expectedCard, createdPaymentMethod.card);
        assertEquals(metadata, createdPaymentMethod.metadata);
    }

    @Test
    public void createPaymentMethodSynchronous_withIdeal()
            throws StripeException {
        final PaymentMethod.BillingDetails expectedBillingDetails =
                new PaymentMethod.BillingDetails.Builder()
                        .setName("Home")
                        .setEmail("me@example.com")
                        .setPhone("1-800-555-1234")
                        .setAddress(new Address.Builder()
                                .setLine1("123 Main St")
                                .setCity("Los Angeles")
                                .setState("CA")
                                .setCountry("US")
                                .build())
                        .build();

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        new PaymentMethodCreateParams.Ideal.Builder()
                                .setBank("ing")
                                .build(),
                        expectedBillingDetails);
        final Stripe stripe = createNonLoggingStripe();
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(expectedBillingDetails, createdPaymentMethod.billingDetails);
        assertNull(createdPaymentMethod.card);
        assertEquals(new PaymentMethod.Ideal.Builder()
                        .setBank("ing")
                        .setBankIdentifierCode("INGBNL2A")
                        .build(),
                createdPaymentMethod.ideal);
    }

    @NonNull
    private Stripe createNonLoggingStripe() {
        return createNonLoggingStripe(NON_LOGGING_PK);
    }

    @NonNull
    private Stripe createNonLoggingStripe(@NonNull String publishableKey) {
        final StripeApiHandler apiHandler =
                new StripeApiHandler(mContext, new RequestExecutor(), false);
        return new Stripe(
                apiHandler,
                new StripeNetworkUtils(mContext),
                new PaymentAuthenticationController(mContext, apiHandler),
                publishableKey);
    }

    @NonNull
    private Stripe createNonLoggingStripe(@NonNull Stripe.TokenCreator tokenCreator) {
        final StripeApiHandler apiHandler =
                new StripeApiHandler(mContext, new RequestExecutor(), false);
        return new Stripe(
                apiHandler,
                new StripeNetworkUtils(mContext),
                new PaymentAuthenticationController(mContext, apiHandler),
                NON_LOGGING_PK,
                tokenCreator
        );
    }

    @NonNull
    private Stripe createLoggingStripe() {
        return new Stripe(mContext, LOGGING_PK);
    }
}
