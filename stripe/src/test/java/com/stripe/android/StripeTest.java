package com.stripe.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.AccountParams;
import com.stripe.android.model.Address;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.Card;
import com.stripe.android.model.CardBrand;
import com.stripe.android.model.CardFixtures;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.PaymentMethodCreateParamsFixtures;
import com.stripe.android.model.PersonTokenParamsFixtures;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.SourceSepaDebitData;
import com.stripe.android.model.Token;
import com.stripe.android.model.WeChat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import kotlinx.coroutines.CoroutineScope;

import static com.stripe.android.CardNumberFixtures.VALID_VISA_NO_SPACES;
import static kotlinx.coroutines.CoroutineScopeKt.MainScope;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Run integration tests on Stripe. Fires real API requests but does not make logging requests.
 */
@SuppressWarnings({"ConstantConditions"})
@RunWith(RobolectricTestRunner.class)
public class StripeTest {
    // publishable keys
    private static final String NON_LOGGING_PK = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY;
    private static final String DEFAULT_SECRET_KEY = "sk_default";

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

    @Captor private ArgumentCaptor<StripeRequest> stripeRequestArgumentCaptor;
    @Mock private FireAndForgetRequestExecutor fireAndForgetRequestExecutor;
    @Mock private ApiResultCallback<Token> tokenCallback;
    @Mock private ApiResultCallback<Source> sourceCallback;
    @Captor private ArgumentCaptor<Token> tokenArgumentCaptor;
    @Captor private ArgumentCaptor<Source> sourceArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testVersion() {
        assertEquals(
                String.format(Locale.ROOT, "AndroidBindings/%s", BuildConfig.VERSION_NAME),
                Stripe.VERSION
        );
    }

    @Test
    public void testApiVersion() {
        assertEquals("2019-12-03", Stripe.API_VERSION);
    }

    @Test
    public void constructorShouldFailWithNullPublishableKey() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new Stripe(mContext, null);
            }
        });
    }

    @Test
    public void constructorShouldFailWithEmptyPublishableKey() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new Stripe(mContext, "");
            }
        });
    }

    @Test
    public void constructorShouldFailWithSecretKey() {
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                new Stripe(mContext, DEFAULT_SECRET_KEY);
            }
        });
    }

    @Test
    public void createCardTokenShouldCreateRealToken() {
        final Stripe stripe = createStripe(MainScope());
        stripe.createCardToken(CARD, tokenCallback);
        verify(tokenCallback).onSuccess(tokenArgumentCaptor.capture());
        final Token token = tokenArgumentCaptor.getValue();
        final String tokenId = token.getId();
        assertTrue(tokenId.startsWith("tok_"));
    }

    @Test
    public void createCardTokenSynchronous_withValidData_returnsToken()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createCardTokenSynchronous(CARD);

        assertNotNull(token);
        Card returnedCard = token.getCard();
        assertNotNull(returnedCard);
        assertNull(token.getBankAccount());
        assertEquals(Token.TokenType.CARD, token.getType());
        assertEquals(CARD.getLast4(), returnedCard.getLast4());
        assertEquals(CardBrand.Visa, returnedCard.getBrand());
        assertEquals(CARD.getExpYear(), returnedCard.getExpYear());
        assertEquals(CARD.getExpMonth(), returnedCard.getExpMonth());
        assertEquals(Card.FundingType.CREDIT, returnedCard.getFunding());
    }

    @Test
    public void createCardTokenSynchronous_withValidDataAndConnectAccount_returnsToken()
            throws StripeException {
        final Stripe stripe = new Stripe(
                mContext,
                ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY,
                "acct_1Acj2PBUgO3KuWzz"
        );

        final Token token = stripe.createCardTokenSynchronous(CARD);

        assertNotNull(token);
        Card returnedCard = token.getCard();
        assertNotNull(returnedCard);
        assertNull(token.getBankAccount());
        assertEquals(Token.TokenType.CARD, token.getType());
        assertEquals(CARD.getLast4(), returnedCard.getLast4());
        assertEquals(CardBrand.Visa, returnedCard.getBrand());
        assertEquals(CARD.getExpYear(), returnedCard.getExpYear());
        assertEquals(CARD.getExpMonth(), returnedCard.getExpMonth());
        assertEquals(Card.FundingType.CREDIT, returnedCard.getFunding());
    }

    @Test
    public void createToken_createSource_returnsSource()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createCardTokenSynchronous(CARD);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId());
        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
    }

    @Test
    public void createToken_createSourceWithTokenToSourceParams_returnsSource()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createCardTokenSynchronous(CARD);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId());

        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
    }

    @Test
    public void createBankAccountToken() {
        createStripe()
                .createBankAccountToken(new BankAccount(
                        "Jane Austen",
                                BankAccount.BankAccountType.INDIVIDUAL,
                        "STRIPE TEST BANK",
                        "US",
                        "usd",
                        "1JWtPxqbdX5Gamtc",
                        "6789",
                        "110000000"),
                new ApiResultCallback<Token>() {
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
        createStripe().createPiiToken("123-45-6789",
                new ApiResultCallback<Token>() {
                    @Override
                    public void onSuccess(@NonNull Token result) {

                    }

                    @Override
                    public void onError(@NonNull Exception e) {

                    }
                });
    }

    @Test
    public void testCreateSource() {
        createStripe().createSource(CARD_SOURCE_PARAMS,
                new ApiResultCallback<Source>() {
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
        createStripe().createCvcUpdateToken("123",
                new ApiResultCallback<Token>() {
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
        final Stripe stripe = createStripe();

        final Token token = stripe.createBankAccountTokenSynchronous(BANK_ACCOUNT);
        assertNotNull(token);
        assertEquals(Token.TokenType.BANK_ACCOUNT, token.getType());
        assertNull(token.getCard());

        final BankAccount returnedBankAccount = token.getBankAccount();
        assertNotNull(returnedBankAccount);
        final String expectedLast4 = TEST_BANK_ACCOUNT_NUMBER
                .substring(TEST_BANK_ACCOUNT_NUMBER.length() - 4);
        assertEquals(expectedLast4, returnedBankAccount.getLast4());
        assertEquals(BANK_ACCOUNT.getCountryCode(), returnedBankAccount.getCountryCode());
        assertEquals(BANK_ACCOUNT.getCurrency(), returnedBankAccount.getCurrency());
        assertEquals(BANK_ACCOUNT.getRoutingNumber(), returnedBankAccount.getRoutingNumber());
    }

    @Test
    public void createSourceSynchronous_withAlipayReusableParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
        SourceParams alipayParams = SourceParams.createAlipayReusableParams(
                "usd",
                "Example Payer",
                "abc@def.com",
                "stripe://start");

        final Source alipaySource = stripe.createSourceSynchronous(alipayParams);
        assertNotNull(alipaySource);
        assertNotNull(alipaySource.getId());
        assertNotNull(alipaySource.getClientSecret());
        assertEquals(Source.SourceType.ALIPAY, alipaySource.getType());
        assertEquals("redirect", alipaySource.getFlow());
        assertNotNull(alipaySource.getOwner());
        assertEquals("Example Payer", alipaySource.getOwner().getName());
        assertEquals("abc@def.com", alipaySource.getOwner().getEmail());
        assertEquals("usd", alipaySource.getCurrency());
        assertEquals(Source.Usage.REUSABLE, alipaySource.getUsage());
        assertNotNull(alipaySource.getRedirect());
        assertEquals("stripe://start", alipaySource.getRedirect().getReturnUrl());
    }

    @Test
    public void createSourceSynchronous_withAlipaySingleUseParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
        final SourceParams alipayParams = SourceParams.createAlipaySingleUseParams(
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
        assertEquals(Source.SourceType.ALIPAY, alipaySource.getType());
        assertEquals("redirect", alipaySource.getFlow());
        assertNotNull(alipaySource.getOwner());
        assertEquals("Example Payer", alipaySource.getOwner().getName());
        assertEquals("abc@def.com", alipaySource.getOwner().getEmail());
        assertEquals("usd", alipaySource.getCurrency());
        assertEquals(Source.Usage.SINGLE_USE, alipaySource.getUsage());
        assertNotNull(alipaySource.getRedirect());
        assertEquals("stripe://start", alipaySource.getRedirect().getReturnUrl());
    }

    @Test
    public void createSourceSynchronous_withWeChatPayParams_passesIntegrationTest()
            throws StripeException {
        final String weChatAppId = "wx65997d6307c3827d";
        final Stripe stripe = createStripe("pk_live_L4KL0pF017Jgv9hBaWzk4xoB");
        final SourceParams weChatPaySourceParams = SourceParams.createWeChatPayParams(
                1000L,
                "USD",
                weChatAppId,
                "WIDGET STORE"
        );
        final Source weChatPaySource = stripe.createSourceSynchronous(weChatPaySourceParams);
        assertNotNull(weChatPaySource);
        final WeChat weChatData = weChatPaySource.getWeChat();
        assertEquals(weChatAppId, weChatData.getAppId());
        assertEquals("WIDGET STORE", weChatData.getStatementDescriptor());
    }

    @Test
    public void createSourceSynchronous_withWeChatPayParams_onUnactivatedAccount_throwsException() {
        final Stripe stripe = createStripe("pk_live_8fFXnTeQrVqExmub4gQPOzgG001iJLyZwl");
        final SourceParams weChatPaySourceParams = SourceParams.createWeChatPayParams(
                1000L,
                "USD",
                "wx65997d6307c3827d",
                "WIDGET STORE"
        );
        final InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.createSourceSynchronous(weChatPaySourceParams);
                    }
                });
        assertEquals("payment_method_unactivated", ex.getErrorCode());
        assertEquals(
                "This payment method (wechat) is not activated for your account. You can only create testmode wechat sources. You can learn more about this here https://support.stripe.com/questions/i-am-having-trouble-activating-a-payment-method",
                ex.getMessage()
        );
    }

    @Test
    public void createSourceSynchronous_withBancontactParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
        final SourceParams bancontactParams = SourceParams.createBancontactParams(
                1000L,
                "John Doe",
                "example://path",
                "a statement described",
                "en"
        );
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
        assertEquals(Source.SourceType.BANCONTACT, bancontactSource.getType());
        assertEquals(1000L, bancontactSource.getAmount().longValue());
        assertNotNull(bancontactSource.getSourceTypeData());
        assertNull(bancontactSource.getSourceTypeModel());
        assertNotNull(bancontactSource.getOwner());
        assertNotNull(bancontactSource.getRedirect());
        assertEquals("John Doe", bancontactSource.getOwner().getName());
        assertEquals("example://path", bancontactSource.getRedirect().getReturnUrl());
        assertEquals(metamap, bancontactSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withCardParams_passesIntegrationTest()
            throws StripeException {
        final Card card = new Card.Builder(VALID_VISA_NO_SPACES, 12, 2050, "123")
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

        final Source cardSource = createStripe().createSourceSynchronous(params);
        assertNotNull(cardSource);
        assertNotNull(cardSource.getClientSecret());
        assertNotNull(cardSource.getId());
        assertEquals(Source.SourceType.CARD, cardSource.getType());
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
        assertEquals(metamap, cardSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_with3DSParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Card card = Card.create(VALID_VISA_NO_SPACES, 12, 2050, "123");
        final SourceParams params = SourceParams.createCardParams(card);

        final Source cardSource = stripe.createSourceSynchronous(params);
        assertNotNull(cardSource);
        assertNotNull(cardSource.getId());
        final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                50000L,
                "brl",
                "example://return",
                cardSource.getId());
        final Map<String, String> metadata = new HashMap<String, String>() {{
            put("dimensions", "three");
            put("type", "beach ball");
        }};
        threeDParams.setMetaData(metadata);

        final Source threeDSource = stripe.createSourceSynchronous(threeDParams);
        assertNotNull(threeDSource);
        assertNotNull(threeDSource.getAmount());
        assertEquals(50000L, threeDSource.getAmount().longValue());
        assertEquals("brl", threeDSource.getCurrency());
        assertNotNull(threeDSource.getClientSecret());
        assertNotNull(threeDSource.getId());
        assertNull(threeDSource.getSourceTypeModel());
        assertEquals(Source.SourceType.THREE_D_SECURE, threeDSource.getType());
        assertNotNull(threeDSource.getSourceTypeData());
        assertEquals(metadata, threeDSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withGiropayParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.GIROPAY, giropaySource.getType());
        assertNotNull(giropaySource.getSourceTypeData());
        assertNull(giropaySource.getSourceTypeModel());
        assertNotNull(giropaySource.getOwner());
        assertNotNull(giropaySource.getRedirect());
        assertEquals("Mr. X", giropaySource.getOwner().getName());
        assertEquals("example://redirect", giropaySource.getRedirect().getReturnUrl());
        assertEquals(metamap, giropaySource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withP24Params_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.P24, p24Source.getType());
        assertEquals("redirect", p24Source.getFlow());
        assertNotNull(p24Source.getOwner());
        assertEquals("Example Payer", p24Source.getOwner().getName());
        assertEquals("abc@def.com", p24Source.getOwner().getEmail());
        assertEquals("eur", p24Source.getCurrency());
        assertEquals(Source.Usage.SINGLE_USE, p24Source.getUsage());
        assertNotNull(p24Source.getRedirect());
        assertEquals("stripe://start", p24Source.getRedirect().getReturnUrl());
    }

    @Test
    public void createSourceSynchronous_withSepaDebitParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.SEPA_DEBIT, sepaDebitSource.getType());
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
        assertEquals(metamap ,sepaDebitSource.getMetaData());
    }


    @Test
    public void createSourceSynchronous_withSepaDebitParamsWithMinimalValues_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.SEPA_DEBIT, sepaDebitSource.getType());
        assertEquals(metamap ,sepaDebitSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withNoEmail_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
        String validIban = "DE89370400440532013000";
        SourceParams params = SourceParams.createSepaDebitParams(
                "Sepa Account Holder",
                validIban,
                "123 Main St",
                "Eureka",
                "90210",
                "EI"
        );
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
        assertEquals(Source.SourceType.SEPA_DEBIT, sepaDebitSource.getType());
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
        assertEquals(metamap ,sepaDebitSource.getMetaData());
    }

    @Test
    public void createSepaDebitSource_withNoAddress_passesIntegrationTest() throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.SEPA_DEBIT, sepaDebitSource.getType());
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
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.IDEAL, idealSource.getType());
        assertEquals("eur", idealSource.getCurrency());
        assertNotNull(idealSource.getSourceTypeData());
        assertNotNull(idealSource.getOwner());
        assertNull(idealSource.getSourceTypeModel());
        assertEquals("Bond", idealSource.getOwner().getName());
        assertNotNull(idealSource.getRedirect());
        assertEquals("example://return", idealSource.getRedirect().getReturnUrl());
        assertEquals(metamap, idealSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withiDEALParamsNoStatement_doesNotIgnoreBank()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.IDEAL, idealSource.getType());
        assertEquals("eur", idealSource.getCurrency());
        assertNotNull(idealSource.getSourceTypeData());
        assertNotNull(idealSource.getOwner());
        assertNull(idealSource.getSourceTypeModel());
        assertEquals("Bond", idealSource.getOwner().getName());
        assertNotNull(idealSource.getRedirect());
        assertEquals(bankName, idealSource.getSourceTypeData().get("bank"));
        assertEquals("example://return", idealSource.getRedirect().getReturnUrl());
        assertEquals(metamap, idealSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withiDEALParamsNoName_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.IDEAL, idealSource.getType());
        assertEquals("eur", idealSource.getCurrency());
        assertNotNull(idealSource.getSourceTypeData());
        assertNotNull(idealSource.getOwner());
        assertNull(idealSource.getSourceTypeModel());
        assertNotNull(idealSource.getRedirect());
        assertEquals(bankName, idealSource.getSourceTypeData().get("bank"));
        assertNull(idealSource.getOwner().getName());
        assertEquals("example://return", idealSource.getRedirect().getReturnUrl());
        assertEquals(metamap, idealSource.getMetaData());
    }

    @Test
    public void createSourceSynchronous_withSofortParams_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
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
        assertEquals(Source.SourceType.SOFORT, sofortSource.getType());
        assertEquals("eur", sofortSource.getCurrency());
        assertNotNull(sofortSource.getSourceTypeData());
        assertNull(sofortSource.getSourceTypeModel());
        assertEquals(70000L, sofortSource.getAmount().longValue());
        assertNotNull(sofortSource.getRedirect());
        assertEquals("example://return", sofortSource.getRedirect().getReturnUrl());
        assertEquals(metamap, sofortSource.getMetaData());
    }

    @Test
    public void retrieveSourceAsync_withValidData_passesIntegrationTest() throws StripeException {
        final Source source = createSource();

        final Stripe stripe = createStripe(MainScope());
        stripe.retrieveSource(source.getId(), source.getClientSecret(), sourceCallback);
        verify(sourceCallback).onSuccess(sourceArgumentCaptor.capture());

        final Source capturedSource = sourceArgumentCaptor.getValue();
        assertEquals(
                source.getId(),
                capturedSource.getId()
        );
    }

    @Test
    public void retrieveSourceSynchronous_withValidData_passesIntegrationTest()
            throws StripeException {
        final Source source = createSource();

        final String sourceId = source.getId();
        final String clientSecret = source.getClientSecret();
        assertNotNull(sourceId);
        assertNotNull(clientSecret);

        final Source retrievedSource = createStripe()
                .retrieveSourceSynchronous(sourceId, clientSecret);

        // We aren't actually updating the source on the server, so the two sources should
        // be identical.
        assertEquals(source, retrievedSource);
    }

    @Test
    public void createSourceFromTokenParams_withExtraParams_succeeds()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createCardTokenSynchronous(CARD);
        assertNotNull(token);

        final Map<String, String> map = new HashMap<>();
        map.put("usage", Source.Usage.SINGLE_USE);
        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId())
                .setExtraParams(map);

        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
        assertNotNull(Source.Usage.SINGLE_USE, source.getUsage());
    }

    @Test
    public void createVisaCheckoutParams_whenUnactivated_throwsException() {
        final Stripe stripe = createStripe();
        final SourceParams sourceParams = SourceParams.createVisaCheckoutParams(
                UUID.randomUUID().toString()
        );
        final InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.createSourceSynchronous(sourceParams);
                    }
                }
        );
        assertEquals("visa_checkout must be activated before use.", ex.getMessage());
    }

    @Test
    public void createMasterpassParams_whenUnactivated_throwsException() {
        final Stripe stripe = createStripe();
        final SourceParams sourceParams = SourceParams.createMasterpassParams(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        final InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.createSourceSynchronous(sourceParams);
                    }
                }
        );
        assertEquals("masterpass must be activated before use.", ex.getMessage());
    }

    @Test
    public void createTokenSynchronous_withValidPersonalId_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createPiiTokenSynchronous("0123456789");
        assertNotNull(token);
        assertEquals(Token.TokenType.PII, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createTokenSynchronous_withValidCvc_passesIntegrationTest()
            throws StripeException {
        final Stripe stripe = createStripe();

        final Token token = stripe.createCvcUpdateTokenSynchronous("1234");
        assertNotNull(token);
        assertEquals(Token.TokenType.CVC_UPDATE, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
        assertTrue(token.getId().startsWith("cvctok_"));
    }

    @Test
    public void testCreatePersonToken() {
        final Stripe stripe = createStripe(MainScope());
        stripe.createPersonToken(PersonTokenParamsFixtures.PARAMS, tokenCallback);
        verify(tokenCallback).onSuccess(tokenArgumentCaptor.capture());
        final Token token = tokenArgumentCaptor.getValue();
        assertEquals(Token.TokenType.PERSON, Objects.requireNonNull(token).getType());
        assertTrue(token.getId().startsWith("cpt_"));
    }

    @Test
    public void testCreatePersonTokenSynchronous() throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createPersonTokenSynchronous(PersonTokenParamsFixtures.PARAMS);
        assertEquals(Token.TokenType.PERSON, Objects.requireNonNull(token).getType());
        assertTrue(token.getId().startsWith("cpt_"));
    }

    @Test
    public void createAccountTokenSynchronous_withIndividualEntity_passesIntegrationTest()
            throws StripeException {
        final Address exampleAddress = new Address
                .Builder()
                .setCity("SF")
                .setCountry("US")
                .setState("CA")
                .build();
        final Map<String, Object> businessData = new HashMap<String, Object>() {{
            put("address", exampleAddress.toParamMap());
            put("ssn_last_4", "1234");
            put("first_name", "Kathy");
            put("last_name", "Sun");
        }};

        final Stripe stripe = createStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(true,
                        AccountParams.BusinessType.Individual, businessData));
        assertNotNull(token);
        assertEquals(Token.TokenType.ACCOUNT, token.getType());
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
            put("address", exampleAddress.toParamMap());
            put("tax_id", "123-23-1234");
            put("name", "My Corp.");
        }};

        final Stripe stripe = createStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(true,
                        AccountParams.BusinessType.Company, businessData));
        assertNotNull(token);
        assertEquals(Token.TokenType.ACCOUNT, token.getType());
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
            put("address", address.toParamMap());
            put("ssn_last_4", "1234");
            put("first_name", "Kathy");
            put("last_name", "Sun");
        }};

        final Stripe stripe = createStripe();
        Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(false,
                        AccountParams.BusinessType.Individual, businessData));
        assertNotNull(token);
        assertEquals(Token.TokenType.ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutBusinessData_isValid()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(false,
                        AccountParams.BusinessType.Individual, null));
        assertNotNull(token);
        assertEquals(Token.TokenType.ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutBusinessType_isValid()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createAccountTokenSynchronous(
                AccountParams.createAccountParams(false,
                        null, null));
        assertNotNull(token);
        assertEquals(Token.TokenType.ACCOUNT, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createTokenSynchronous_withValidDataAndBadKey_throwsAuthenticationException() {
        // This key won't work for a real connection to the api.
        final Stripe stripe = createStripe(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);
        final AuthenticationException authenticationException = assertThrows(
                AuthenticationException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.createCardTokenSynchronous(CARD);
                    }
                }
        );
        assertEquals("Invalid API Key provided: " + ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                authenticationException.getMessage());
    }

    @Test
    public void createTokenSynchronous_withInvalidCardNumber_throwsCardException() {
        // This card is missing quite a few numbers.
        final Card card = Card.create("42424242", 12, YEAR, "123");
        final Stripe stripe = createStripe();
        final CardException cardException = assertThrows(
                CardException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.createCardTokenSynchronous(card);
                    }
                }
        );
        assertEquals("Your card number is incorrect.", cardException.getMessage());
    }

    @Test
    public void retrievePaymentIntent_withInvalidClientSecret_shouldThrowException() {
        Locale.setDefault(Locale.GERMANY);

        final Stripe stripe = createStripe();
        assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                stripe.retrievePaymentIntentSynchronous("invalid");
            }
        });
    }

    @Test
    public void createTokenSynchronous_withExpiredCard_throwsCardException() {
        // This card is missing quite a few numbers.
        final Card card = Card.create("4242424242424242", 11, 2015, "123");
        final Stripe stripe = createStripe();
        final CardException cardException = assertThrows(
                CardException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        stripe.createCardTokenSynchronous(card);
                    }
                }
        );
        assertEquals("Your card's expiration year is invalid.",
                cardException.getMessage());
    }

    @Test
    public void createTokenSynchronousTwice_withIdempotencyKey_returnsSameToken()
            throws StripeException {
        final Stripe stripe = createStripe();
        final String idempotencyKey = UUID.randomUUID().toString();
        assertEquals(
                stripe.createCardTokenSynchronous(CardFixtures.MINIMUM_CARD, idempotencyKey),
                stripe.createCardTokenSynchronous(CardFixtures.MINIMUM_CARD, idempotencyKey)
        );
    }

    @Test
    public void createTokenSynchronousTwice_withoutIdempotencyKey_returnsDifferentToken()
            throws StripeException {
        final Stripe stripe = createStripe();
        assertNotEquals(
                stripe.createCardTokenSynchronous(CardFixtures.MINIMUM_CARD),
                stripe.createCardTokenSynchronous(CardFixtures.MINIMUM_CARD)
        );
    }

    @Test
    public void createPaymentMethodSynchronous_withCard()
            throws StripeException {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD;
        final Stripe stripe = createStripe();
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(PaymentMethodCreateParamsFixtures.BILLING_DETAILS,
                createdPaymentMethod.billingDetails);
        assertNotNull(createdPaymentMethod.card);
        assertEquals("4242", createdPaymentMethod.card.last4);
    }

    @Test
    public void createPaymentMethod_withCardToken()
            throws StripeException {
        final Stripe stripe = createStripe();
        final Token token = stripe.createCardTokenSynchronous(CARD);

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Card.create(token.getId()),
                        null);
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertNotNull(createdPaymentMethod.card);
        assertEquals("visa", createdPaymentMethod.card.brand);
        assertEquals("4242", createdPaymentMethod.card.last4);
    }

    @Test
    public void createPaymentMethodSynchronous_withCardAndMetadata()
            throws StripeException {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", "123456789");

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParamsFixtures.createWith(metadata);
        final Stripe stripe = createStripe(fireAndForgetRequestExecutor);
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(PaymentMethodCreateParamsFixtures.BILLING_DETAILS,
                createdPaymentMethod.billingDetails);
        assertNotNull(createdPaymentMethod.card);
        assertEquals("4242", createdPaymentMethod.card.last4);
        assertEquals(metadata, createdPaymentMethod.metadata);

        verify(fireAndForgetRequestExecutor, times(2))
                .executeAsync(stripeRequestArgumentCaptor.capture());
        final List<StripeRequest> fireAndForgetRequests =
                stripeRequestArgumentCaptor.getAllValues();
        final StripeRequest analyticsRequest = fireAndForgetRequests.get(1);
        assertEquals(AnalyticsRequest.HOST, analyticsRequest.getBaseUrl());
        assertEquals(createdPaymentMethod.id,
                analyticsRequest.getParams().get(AnalyticsDataFactory.FIELD_PAYMENT_METHOD_ID));
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
                        new PaymentMethodCreateParams.Ideal("ing"),
                        expectedBillingDetails
                );
        final Stripe stripe = createStripe(fireAndForgetRequestExecutor);
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(expectedBillingDetails, createdPaymentMethod.billingDetails);
        assertNull(createdPaymentMethod.card);
        assertEquals("INGBNL2A", createdPaymentMethod.ideal.bankIdentifierCode);

        verify(fireAndForgetRequestExecutor, times(2))
                .executeAsync(stripeRequestArgumentCaptor.capture());
        final List<StripeRequest> fireAndForgetRequests =
                stripeRequestArgumentCaptor.getAllValues();
        final StripeRequest analyticsRequest = fireAndForgetRequests.get(1);
        assertEquals(AnalyticsRequest.HOST, analyticsRequest.getBaseUrl());
        assertEquals(createdPaymentMethod.id,
                analyticsRequest.getParams().get(AnalyticsDataFactory.FIELD_PAYMENT_METHOD_ID));
    }

    @Test
    public void createPaymentMethodSynchronous_withFpx()
            throws StripeException {
        final FireAndForgetRequestExecutor fireAndForgetRequestExecutor =
                mock(FireAndForgetRequestExecutor.class);

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
                        new PaymentMethodCreateParams.Fpx("hsbc"),
                        expectedBillingDetails
                );
        final Stripe stripe = createStripe(
                ApiKeyFixtures.FPX_PUBLISHABLE_KEY,
                fireAndForgetRequestExecutor
        );
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(expectedBillingDetails, createdPaymentMethod.billingDetails);
        assertNull(createdPaymentMethod.card);
        assertEquals("hsbc", createdPaymentMethod.fpx.bank);

        verify(fireAndForgetRequestExecutor, times(2))
                .executeAsync(stripeRequestArgumentCaptor.capture());
        final List<StripeRequest> fireAndForgetRequests =
                stripeRequestArgumentCaptor.getAllValues();
        final StripeRequest analyticsRequest = fireAndForgetRequests.get(1);
        assertEquals(AnalyticsRequest.HOST, analyticsRequest.getBaseUrl());
        assertEquals(createdPaymentMethod.id,
                analyticsRequest.getParams().get(AnalyticsDataFactory.FIELD_PAYMENT_METHOD_ID));
    }

    @Test
    public void setAppInfo() {
        assertNull(Stripe.getAppInfo());

        final AppInfo appInfo = AppInfo.create("myapp");
        Stripe.setAppInfo(appInfo);
        assertEquals(appInfo, Stripe.getAppInfo());
    }

    @NonNull
    private Source createSource() throws StripeException {
        final Stripe stripe = createStripe();
        final SourceParams params = SourceParams.createCardParams(
                Card.create(VALID_VISA_NO_SPACES, 12, 2050, "123")
        );

        final Source cardSource = stripe.createSourceSynchronous(params);

        assertNotNull(cardSource);
        assertNotNull(cardSource.getId());
        SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                5000L,
                "brl",
                "example://return",
                cardSource.getId()
        );

        final Map<String, String> metamap = new HashMap<String, String>() {{
            put("dimensions", "three");
            put("type", "beach ball");
        }};
        threeDParams.setMetaData(metamap);

        final Source source = stripe.createSourceSynchronous(threeDParams);
        assertNotNull(source);
        return source;
    }

    @NonNull
    private Stripe createStripe() {
        return createStripe(NON_LOGGING_PK);
    }

    @NonNull
    private Stripe createStripe(
            @NonNull FireAndForgetRequestExecutor fireAndForgetRequestExecutor) {
        return createStripe(NON_LOGGING_PK, fireAndForgetRequestExecutor);
    }

    @NonNull
    private Stripe createStripe(@NonNull String publishableKey) {
        return createStripe(publishableKey, new FakeFireAndForgetRequestExecutor());
    }

    @NonNull
    private Stripe createStripe(
            @NonNull String publishableKey,
            @NonNull FireAndForgetRequestExecutor fireAndForgetRequestExecutor) {
        final StripeRepository stripeRepository = createStripeRepository(fireAndForgetRequestExecutor);
        return new Stripe(
                stripeRepository,
                new StripeNetworkUtils(mContext),
                StripePaymentController.create(mContext, stripeRepository),
                publishableKey,
                null
        );
    }

    @NonNull
    private Stripe createStripe(@NonNull CoroutineScope workScope) {
        final StripeRepository stripeRepository = createStripeRepository(
                new FakeFireAndForgetRequestExecutor());
        return new Stripe(
                stripeRepository,
                new StripeNetworkUtils(mContext),
                StripePaymentController.create(mContext, stripeRepository),
                NON_LOGGING_PK,
                null,
                workScope
        );
    }

    @NonNull
    private StripeRepository createStripeRepository(
            @NonNull FireAndForgetRequestExecutor fireAndForgetRequestExecutor) {
        return new StripeApiRepository(
                mContext,
                null,
                new FakeLogger(),
                new StripeApiRequestExecutor(),
                fireAndForgetRequestExecutor,
                new FingerprintRequestFactory(
                        new TelemetryClientUtil(mContext, new FakeUidSupplier())
                )
        );
    }
}
