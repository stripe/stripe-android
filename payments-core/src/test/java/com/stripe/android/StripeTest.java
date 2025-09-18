package com.stripe.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory;
import com.stripe.android.core.AppInfo;
import com.stripe.android.core.exception.AuthenticationException;
import com.stripe.android.core.exception.InvalidRequestException;
import com.stripe.android.core.exception.StripeException;
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository;
import com.stripe.android.core.model.StripeFile;
import com.stripe.android.core.model.StripeFileParams;
import com.stripe.android.core.model.StripeFilePurpose;
import com.stripe.android.core.networking.AnalyticsRequest;
import com.stripe.android.core.networking.AnalyticsRequestExecutor;
import com.stripe.android.core.networking.DefaultStripeNetworkClient;
import com.stripe.android.core.version.StripeSdkVersion;
import com.stripe.android.exception.CardException;
import com.stripe.android.model.AccountParams;
import com.stripe.android.model.AddressFixtures;
import com.stripe.android.model.BankAccount;
import com.stripe.android.model.BankAccountTokenParamsFixtures;
import com.stripe.android.model.Card;
import com.stripe.android.model.CardBrand;
import com.stripe.android.model.CardFunding;
import com.stripe.android.model.CardParams;
import com.stripe.android.model.CardParamsFixtures;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.PaymentMethodCreateParamsFixtures;
import com.stripe.android.model.PaymentMethodFixtures;
import com.stripe.android.model.PersonTokenParamsFixtures;
import com.stripe.android.model.PossibleBrands;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.stripe.android.networking.PaymentAnalyticsRequestFactory;
import com.stripe.android.networking.RequestSurface;
import com.stripe.android.networking.StripeApiRepository;
import com.stripe.android.networking.StripeRepository;
import com.stripe.android.testing.FakeLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.test.TestCoroutineDispatcher;

import static com.google.common.truth.Truth.assertThat;
import static com.stripe.android.PaymentsFraudDetectionDataRepositoryFactoryKt.DefaultFraudDetectionDataRepository;
import static com.stripe.android.utils.TestUtils.idleLooper;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Run integration tests on Stripe.
 */
@SuppressWarnings("KotlinInternalInJava")
@RunWith(RobolectricTestRunner.class)
public class StripeTest {
    private static final CardParams CARD_PARAMS = CardParamsFixtures.MINIMUM;

    private final CoroutineDispatcher testDispatcher = new TestCoroutineDispatcher();

    @NonNull
    private final Context context = ApplicationProvider.getApplicationContext();
    @NonNull
    private final FraudDetectionDataRepository defaultFraudDetectionDataRepository =
            DefaultFraudDetectionDataRepository(context);
    @NonNull
    private final Stripe defaultStripe = createStripe();

    @Mock
    private AnalyticsRequestExecutor analyticsRequestExecutor;
    @Mock
    private ApiResultCallback<Token> tokenCallback;
    @Mock
    private ApiResultCallback<Source> sourceCallback;
    @Mock
    private ApiResultCallback<StripeFile> stripeFileCallback;

    @Mock
    private ApiResultCallback<PossibleBrands> possibleBrands;

    @Captor
    private ArgumentCaptor<AnalyticsRequest> analyticsRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<Token> tokenArgumentCaptor;
    @Captor
    private ArgumentCaptor<Source> sourceArgumentCaptor;
    @Captor
    private ArgumentCaptor<StripeFile> stripeFileArgumentCaptor;

    @Captor
    private ArgumentCaptor<PossibleBrands> possibleBrandsArgumentCaptor;

    @Captor
    private ArgumentCaptor<Exception> errorArgumentCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testVersion() {
        assertEquals(
                String.format(Locale.ROOT, "AndroidBindings/%s", StripeSdkVersion.VERSION_NAME),
                StripeSdkVersion.VERSION
        );
    }

    @Test
    public void testApiVersion() {
        assertEquals("2020-03-02", Stripe.API_VERSION);
    }

    @Test
    public void constructorShouldFailWithNullPublishableKey() {
        assertThrows(NullPointerException.class, () -> {
            //noinspection ConstantConditions
            new Stripe(context, null);
        });
    }

    @Test
    public void constructorShouldFailWithEmptyPublishableKey() {
        assertThrows(IllegalArgumentException.class, () -> new Stripe(context, ""));
    }

    @Test
    public void constructorShouldFailWithSecretKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Stripe(context, ApiKeyFixtures.FAKE_SECRET_KEY))
        ;
    }

    @Test
    public void createCardTokenShouldCreateRealToken() {
        final Stripe stripe = createStripe(testDispatcher);
        stripe.createCardToken(CARD_PARAMS, tokenCallback);
        idleLooper();

        verify(tokenCallback).onSuccess(tokenArgumentCaptor.capture());
        final Token token = tokenArgumentCaptor.getValue();
        final String tokenId = token.getId();
        assertTrue(tokenId.startsWith("tok_"));
    }

    @Test
    public void createCardTokenSynchronous_withValidData_returnsToken()
            throws StripeException {
        final Token token = defaultStripe.createCardTokenSynchronous(CARD_PARAMS);

        assertNotNull(token);
        Card returnedCard = token.getCard();
        assertNotNull(returnedCard);
        assertNull(token.getBankAccount());
        assertEquals(Token.Type.Card, token.getType());
        assertEquals(CardBrand.Visa, returnedCard.getBrand());
        assertThat(returnedCard.getLast4())
                .isEqualTo("4242");
        assertThat(returnedCard.getExpYear())
                .isEqualTo(2050);
        assertThat(returnedCard.getExpMonth())
                .isEqualTo(1);
        assertEquals(CardFunding.Credit, returnedCard.getFunding());
    }

    @Test
    public void createCardTokenSynchronous_withValidDataAndConnectAccount_returnsToken()
            throws StripeException {
        final Stripe stripe = new Stripe(
                context,
                ApiKeyFixtures.CONNECTED_ACCOUNT_PUBLISHABLE_KEY,
                "acct_1Acj2PBUgO3KuWzz"
        );

        final Token token = stripe.createCardTokenSynchronous(CARD_PARAMS);

        assertNotNull(token);
        Card returnedCard = token.getCard();
        assertNotNull(returnedCard);
        assertNull(token.getBankAccount());
        assertEquals(Token.Type.Card, token.getType());
        assertEquals(CardBrand.Visa, returnedCard.getBrand());
        assertThat(returnedCard.getLast4())
                .isEqualTo("4242");
        assertThat(returnedCard.getExpYear())
                .isEqualTo(2050);
        assertThat(returnedCard.getExpMonth())
                .isEqualTo(1);
        assertEquals(CardFunding.Credit, returnedCard.getFunding());
    }

    @Test
    public void createToken_createSource_returnsSource()
            throws StripeException {
        final Stripe stripe = defaultStripe;
        final Token token = stripe.createCardTokenSynchronous(CARD_PARAMS);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId());
        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
    }

    @Test
    public void createToken_createSourceWithTokenToSourceParams_returnsSource()
            throws StripeException {
        final Stripe stripe = defaultStripe;
        final Token token = stripe.createCardTokenSynchronous(CARD_PARAMS);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId());

        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
    }

    @Test
    public void createBankAccountToken() {
        createStripe().createBankAccountToken(
                BankAccountTokenParamsFixtures.DEFAULT,
                new ApiResultCallback<Token>() {
                    @Override
                    public void onSuccess(@NonNull Token result) {
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                    }
                }
        );
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
        createStripe().createSource(
                SourceParams.createCardParams(CARD_PARAMS),
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
        final Token token = defaultStripe.createBankAccountTokenSynchronous(
                BankAccountTokenParamsFixtures.DEFAULT
        );
        assertNotNull(token);
        assertEquals(Token.Type.BankAccount, token.getType());
        assertNull(token.getCard());

        final BankAccount returnedBankAccount = token.getBankAccount();
        assertNotNull(returnedBankAccount);
        final String bankAccountId = Objects.requireNonNull(returnedBankAccount.getId());
        assertTrue(bankAccountId.startsWith("ba_"));
        assertEquals("Jenny Rosen", returnedBankAccount.getAccountHolderName());
        assertEquals(
                BankAccount.Type.Individual,
                returnedBankAccount.getAccountHolderType()
        );
        assertEquals("US", returnedBankAccount.getCountryCode());
        assertEquals("usd", returnedBankAccount.getCurrency());
        assertNull(returnedBankAccount.getFingerprint());
        assertEquals("6789", returnedBankAccount.getLast4());
        assertEquals("110000000", returnedBankAccount.getRoutingNumber());
        assertEquals(BankAccount.Status.New, returnedBankAccount.getStatus());
    }

    @Test
    public void createSourceFromTokenParams_withExtraParams_succeeds()
            throws StripeException {
        final Stripe stripe = defaultStripe;
        final Token token = stripe.createCardTokenSynchronous(CARD_PARAMS);
        assertNotNull(token);

        final SourceParams sourceParams = SourceParams.createSourceFromTokenParams(token.getId());
        sourceParams.setUsage(Source.Usage.SingleUse);

        final Source source = stripe.createSourceSynchronous(sourceParams);
        assertNotNull(source);
        assertEquals(Source.Usage.SingleUse, source.getUsage());
    }

    @Test
    public void createMasterpassParams_whenUnactivated_throwsException() {
        final SourceParams sourceParams = SourceParams.createMasterpassParams(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        final InvalidRequestException ex = assertThrows(
                InvalidRequestException.class,
                () -> defaultStripe.createSourceSynchronous(sourceParams)
        );
        assertEquals("masterpass must be activated before use.", ex.getMessage());
    }

    @Test
    public void createTokenSynchronous_withValidPersonalId_passesIntegrationTest()
            throws StripeException {
        final Token token = defaultStripe.createPiiTokenSynchronous("0123456789");
        assertNotNull(token);
        assertEquals(Token.Type.Pii, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createTokenSynchronous_withValidCvc_passesIntegrationTest()
            throws StripeException {

        final Token token = defaultStripe.createCvcUpdateTokenSynchronous("1234");
        assertNotNull(token);
        assertEquals(Token.Type.CvcUpdate, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
        assertTrue(token.getId().startsWith("cvctok_"));
    }

    @Test
    public void testCreatePersonToken() {
        final Stripe stripe = createStripe(testDispatcher);
        stripe.createPersonToken(PersonTokenParamsFixtures.PARAMS, tokenCallback);
        idle();

        verify(tokenCallback).onSuccess(tokenArgumentCaptor.capture());
        final Token token = tokenArgumentCaptor.getValue();
        assertEquals(Token.Type.Person, Objects.requireNonNull(token).getType());
        assertTrue(token.getId().startsWith("cpt_"));
    }

    @Test
    public void testCreatePersonTokenSynchronous() throws StripeException {
        final Token token =
                defaultStripe.createPersonTokenSynchronous(PersonTokenParamsFixtures.PARAMS);
        assertEquals(Token.Type.Person, Objects.requireNonNull(token).getType());
        assertTrue(token.getId().startsWith("cpt_"));
    }

    @Test
    public void createAccountTokenSynchronous_withIndividualEntity_passesIntegrationTest()
            throws StripeException {
        final Token token = defaultStripe.createAccountTokenSynchronous(
                AccountParams.create(
                        true,
                        new AccountParams.BusinessTypeParams.Individual.Builder()
                                .setAddress(AddressFixtures.ADDRESS)
                                .setFirstName("Kathy")
                                .setLastName("Sun")
                                .setSsnLast4("1234")
                                .build()
                )
        );
        assertNotNull(token);
        assertEquals(Token.Type.Account, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withCompanyEntity_isSuccessful()
            throws StripeException {
        final Token token = defaultStripe.createAccountTokenSynchronous(
                AccountParams.create(
                        true,
                        new AccountParams.BusinessTypeParams.Company.Builder()
                                .setAddress(AddressFixtures.ADDRESS)
                                .setTaxId("123-23-1234")
                                .setName("My Corp.")
                                .build()
                )
        );
        assertNotNull(token);
        assertEquals(Token.Type.Account, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutTosShown_isSuccessful()
            throws StripeException {
        final Token token = defaultStripe.createAccountTokenSynchronous(
                AccountParams.create(false,
                        new AccountParams.BusinessTypeParams.Individual.Builder()
                                .setAddress(AddressFixtures.ADDRESS)
                                .setFirstName("Kathy")
                                .setLastName("Sun")
                                .setSsnLast4("1234")
                                .build()
                )
        );
        assertNotNull(token);
        assertEquals(Token.Type.Account, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutBusinessData_isValid()
            throws StripeException {
        final Token token = defaultStripe.createAccountTokenSynchronous(
                AccountParams.create(
                        false,
                        AccountParams.BusinessType.Individual
                )
        );
        assertNotNull(token);
        assertEquals(Token.Type.Account, token.getType());
        assertFalse(token.getLivemode());
        assertFalse(token.getUsed());
        assertNotNull(token.getId());
    }

    @Test
    public void createAccountTokenSynchronous_withoutBusinessType_isValid()
            throws StripeException {
        final Token token = defaultStripe.createAccountTokenSynchronous(AccountParams.create(false));
        assertNotNull(token);
        assertEquals(Token.Type.Account, token.getType());
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
                () -> stripe.createCardTokenSynchronous(CARD_PARAMS)
        );
        assertEquals("Invalid API Key provided: " + ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                authenticationException.getMessage());
    }

    @Test
    public void createTokenSynchronous_withInvalidCardNumber_throwsCardException() {
        // This card is missing quite a few numbers.
        final CardParams cardParams = new CardParams("42424242", 12, 2050, "123");
        final CardException cardException = assertThrows(
                CardException.class,
                () -> defaultStripe.createCardTokenSynchronous(cardParams)
        );
        assertEquals("Your card's number is invalid.", cardException.getMessage());
    }

    @Test
    public void retrievePaymentIntent_withInvalidClientSecret_shouldThrowException() {
        Locale.setDefault(Locale.GERMANY);

        assertThrows(
                IllegalArgumentException.class,
                () -> defaultStripe.retrievePaymentIntentSynchronous("invalid")
        );
    }

    @Test
    public void createTokenSynchronous_withExpiredCard_throwsCardException() {
        // This card is missing quite a few numbers.
        final CardParams cardParams = new CardParams("4242424242424242", 11, 2015, "123");
        final CardException cardException = assertThrows(
                CardException.class,
                () -> defaultStripe.createCardTokenSynchronous(cardParams)
        );
        assertEquals("Your card's expiration year is invalid.",
                cardException.getMessage());
    }

    @Test
    public void createTokenSynchronousTwice_withIdempotencyKey_returnsSameToken()
            throws StripeException {
        final Stripe stripe = createStripe(
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                mock(AnalyticsRequestExecutor.class),
                new FakeFraudDetectionDataRepository()
        );

        final String idempotencyKey = UUID.randomUUID().toString();
        assertEquals(
                stripe.createCardTokenSynchronous(CardParamsFixtures.MINIMUM, idempotencyKey),
                stripe.createCardTokenSynchronous(CardParamsFixtures.MINIMUM, idempotencyKey)
        );
    }

    @Test
    public void createTokenSynchronousTwice_withoutIdempotencyKey_returnsDifferentToken()
            throws StripeException {
        final Stripe stripe = defaultStripe;
        assertNotEquals(
                stripe.createCardTokenSynchronous(CardParamsFixtures.MINIMUM),
                stripe.createCardTokenSynchronous(CardParamsFixtures.MINIMUM)
        );
    }

    @Test
    public void createPaymentMethodSynchronous_withCard()
            throws StripeException {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD;
        final PaymentMethod createdPaymentMethod = defaultStripe.createPaymentMethodSynchronous(
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
        final Stripe stripe = defaultStripe;
        final Token token = Objects.requireNonNull(stripe.createCardTokenSynchronous(CARD_PARAMS));

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        PaymentMethodCreateParams.Card.create(token.getId()),
                        null
                );
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertNotNull(createdPaymentMethod.card);
        assertEquals(CardBrand.Visa, createdPaymentMethod.card.brand);
        assertEquals("4242", createdPaymentMethod.card.last4);
    }

    @Test
    public void createPaymentMethodSynchronous_withCardAndMetadata()
            throws StripeException {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", "123456789");

        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParamsFixtures.createWith(metadata);
        final Stripe stripe = createStripe(
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                analyticsRequestExecutor
        );
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(PaymentMethodCreateParamsFixtures.BILLING_DETAILS,
                createdPaymentMethod.billingDetails);
        assertNotNull(createdPaymentMethod.card);
        assertEquals("4242", createdPaymentMethod.card.last4);

        verify(analyticsRequestExecutor)
                .executeAsync(analyticsRequestArgumentCaptor.capture());
        final AnalyticsRequest analyticsRequest = analyticsRequestArgumentCaptor.getValue();
        assertTrue(analyticsRequest.getUrl().startsWith(AnalyticsRequest.HOST));
        assertThat(
                Objects.requireNonNull(analyticsRequest.getParams())
                        .get(PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE)
        ).isEqualTo("card");
    }

    @Test
    public void createPaymentMethodSynchronous_withIdeal()
            throws StripeException {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        new PaymentMethodCreateParams.Ideal("ing"),
                        PaymentMethodFixtures.BILLING_DETAILS
                );
        final Stripe stripe = createStripe(
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                analyticsRequestExecutor
        );
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(PaymentMethodFixtures.BILLING_DETAILS, createdPaymentMethod.billingDetails);
        assertNull(createdPaymentMethod.card);
        assertEquals(
                "INGBNL2A",
                Objects.requireNonNull(createdPaymentMethod.ideal).bankIdentifierCode
        );

        verify(analyticsRequestExecutor)
                .executeAsync(analyticsRequestArgumentCaptor.capture());

        final AnalyticsRequest analyticsRequest = analyticsRequestArgumentCaptor.getValue();
        assertTrue(analyticsRequest.getUrl().startsWith(AnalyticsRequest.HOST));

        assertThat(
                Objects.requireNonNull(analyticsRequest.getParams())
                        .get(PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE)
        ).isEqualTo("ideal");
    }

    @Test
    public void createPaymentMethodSynchronous_withFpx()
            throws StripeException {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(
                        new PaymentMethodCreateParams.Fpx("hsbc"),
                        PaymentMethodFixtures.BILLING_DETAILS
                );
        final Stripe stripe = createStripe(
                ApiKeyFixtures.FPX_PUBLISHABLE_KEY,
                analyticsRequestExecutor
        );
        final PaymentMethod createdPaymentMethod = stripe.createPaymentMethodSynchronous(
                paymentMethodCreateParams);
        assertNotNull(createdPaymentMethod);
        assertEquals(PaymentMethodFixtures.BILLING_DETAILS, createdPaymentMethod.billingDetails);
        assertNull(createdPaymentMethod.card);
        assertEquals("hsbc", Objects.requireNonNull(createdPaymentMethod.fpx).bank);

        verify(analyticsRequestExecutor)
                .executeAsync(analyticsRequestArgumentCaptor.capture());
        final AnalyticsRequest analyticsRequest = analyticsRequestArgumentCaptor.getValue();
        assertTrue(analyticsRequest.getUrl().startsWith(AnalyticsRequest.HOST));
        assertThat(
                Objects.requireNonNull(analyticsRequest.getParams())
                        .get(PaymentAnalyticsRequestFactory.FIELD_SOURCE_TYPE)
        ).isEqualTo("fpx");
    }

    @Test
    public void setAppInfo() {
        assertNull(Stripe.getAppInfo());

        final AppInfo appInfo = AppInfo.create("myapp");
        Stripe.setAppInfo(appInfo);
        assertEquals(appInfo, Stripe.getAppInfo());
    }

    @Test
    public void createFile_shouldCreateFile() {
        final File file = new FileFactory(context).create();
        final Stripe stripe = createStripe(testDispatcher);
        stripe.createFile(
                new StripeFileParams(
                        file,
                        StripeFilePurpose.BusinessIcon
                ),
                stripeFileCallback
        );
        idle();

        verify(stripeFileCallback).onSuccess(stripeFileArgumentCaptor.capture());

        final StripeFile stripeFile = stripeFileArgumentCaptor.getValue();

        assertNotNull(stripeFile.getId());
        assertEquals(StripeFilePurpose.BusinessIcon, stripeFile.getPurpose());
        assertEquals("png", stripeFile.getType());
        assertEquals("example.png", stripeFile.getFilename());
        assertEquals(976L, Objects.requireNonNull(stripeFile.getSize()).intValue());

        final String url = stripeFile.getUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("https://files.stripe.com/v1/files/file_"));
    }

    @Test
    public void createFileSynchronous_shouldCreateFile() {
        final File file = new FileFactory(context).create();
        final StripeFile stripeFile = createStripe().createFileSynchronous(
                new StripeFileParams(
                        file,
                        StripeFilePurpose.BusinessIcon
                )
        );
        assertNotNull(stripeFile.getId());
        assertEquals(StripeFilePurpose.BusinessIcon, stripeFile.getPurpose());
        assertEquals("png", stripeFile.getType());
        assertEquals("example.png", stripeFile.getFilename());
        assertEquals(976L, Objects.requireNonNull(stripeFile.getSize()).intValue());

        final String url = stripeFile.getUrl();
        assertNotNull(url);
        assertTrue(url.startsWith("https://files.stripe.com/v1/files/file_"));
    }

    @Test
    public void retrievePossibleBrands_shouldReturnPossibleBrands() {
        final Stripe stripe = createStripe(testDispatcher);

        stripe.retrievePossibleBrands(
                "4242 42",
                possibleBrands
        );

        idle();

        verify(possibleBrands).onSuccess(possibleBrandsArgumentCaptor.capture());

        final PossibleBrands result = possibleBrandsArgumentCaptor.getValue();

        assertFalse(result.getBrands().isEmpty());
        assertEquals(result.getBrands().size(), 1);
        assertEquals(result.getBrands().get(0), CardBrand.Visa);
    }

    @Test
    public void retrievePossibleBrands_shouldReturnMultipleCardBrands() {
        final Stripe stripe = createStripe(testDispatcher);

        stripe.retrievePossibleBrands(
                "5131 30",
                possibleBrands
        );

        idle();

        verify(possibleBrands).onSuccess(possibleBrandsArgumentCaptor.capture());

        final PossibleBrands result = possibleBrandsArgumentCaptor.getValue();

        assertFalse(result.getBrands().isEmpty());
        assertEquals(result.getBrands().size(), 2);
        assertTrue(result.getBrands().contains(CardBrand.MasterCard));
        assertTrue(result.getBrands().contains(CardBrand.CartesBancaires));
    }

    @Test
    public void retrievePossibleBrands_forVisaAndCartesBancaires_shouldReturnMultipleCardBrands() {
        final Stripe stripe = createStripe(testDispatcher);

        stripe.retrievePossibleBrands(
                "4000 0025 0000 1001",
                possibleBrands
        );

        idle();

        verify(possibleBrands).onSuccess(possibleBrandsArgumentCaptor.capture());

        final PossibleBrands result = possibleBrandsArgumentCaptor.getValue();

        assertFalse(result.getBrands().isEmpty());
        assertEquals(result.getBrands().size(), 2);
        assertTrue(result.getBrands().contains(CardBrand.Visa));
        assertTrue(result.getBrands().contains(CardBrand.CartesBancaires));
    }

    @Test
    public void retrievePossibleBrands_forMastercardAndCartesBancaires_shouldReturnMultipleCardBrands() {
        final Stripe stripe = createStripe(testDispatcher);

        stripe.retrievePossibleBrands(
                "5555 5525 0000 1001",
                possibleBrands
        );

        idle();

        verify(possibleBrands).onSuccess(possibleBrandsArgumentCaptor.capture());

        final PossibleBrands result = possibleBrandsArgumentCaptor.getValue();

        assertFalse(result.getBrands().isEmpty());
        assertEquals(result.getBrands().size(), 2);
        assertTrue(result.getBrands().contains(CardBrand.MasterCard));
        assertTrue(result.getBrands().contains(CardBrand.CartesBancaires));
    }

    @Test
    public void retrievePossibleBrands_shouldReturnError() {
        final Stripe stripe = createStripe(testDispatcher);

        stripe.retrievePossibleBrands(
                "4242",
                possibleBrands
        );

        idle();

        verify(possibleBrands).onError(errorArgumentCaptor.capture());

        final Exception result = errorArgumentCaptor.getValue();

        assertEquals(result.getMessage(), "cardNumber cannot be less than 6 characters");
    }

    @NonNull
    private Source createSource() throws StripeException {
        final Stripe stripe = defaultStripe;
        final SourceParams params = SourceParams.createCardParams(CARD_PARAMS);

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
        threeDParams.setMetadata(metamap);

        final Source source = stripe.createSourceSynchronous(threeDParams);
        assertNotNull(source);
        return source;
    }

    @NonNull
    private Stripe createStripe() {
        return createStripe(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY);
    }

    @NonNull
    private Stripe createStripe(@NonNull String publishableKey) {
        return createStripe(
                publishableKey,
                mock(AnalyticsRequestExecutor.class),
                defaultFraudDetectionDataRepository
        );
    }

    @NonNull
    private Stripe createStripe(
            @NonNull String publishableKey,
            @NonNull AnalyticsRequestExecutor analyticsRequestExecutor
    ) {
        return createStripe(
                publishableKey,
                analyticsRequestExecutor,
                defaultFraudDetectionDataRepository
        );
    }

    @NonNull
    private Stripe createStripe(
            @NonNull String publishableKey,
            @NonNull AnalyticsRequestExecutor analyticsRequestExecutor,
            @NonNull FraudDetectionDataRepository fraudDetectionDataRepository
    ) {
        final StripeRepository stripeRepository = createStripeRepository(
                publishableKey,
                (CoroutineContext) testDispatcher,
                analyticsRequestExecutor,
                fraudDetectionDataRepository
        );
        return new Stripe(
                stripeRepository,
                StripePaymentController.create(context, publishableKey, stripeRepository),
                publishableKey,
                null
        );
    }

    @NonNull
    private Stripe createStripe(@NonNull CoroutineDispatcher workDispatcher) {
        final StripeRepository stripeRepository = createStripeRepository(
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                (CoroutineContext) workDispatcher,
                mock(AnalyticsRequestExecutor.class),
                defaultFraudDetectionDataRepository
        );
        return new Stripe(
                stripeRepository,
                StripePaymentController.create(
                        context,
                        ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                        stripeRepository
                ),
                ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                null,
                (CoroutineContext) workDispatcher
        );
    }

    @NonNull
    private StripeRepository createStripeRepository(
            @NonNull final String publishableKey,
            @NonNull CoroutineContext workDispatcher,
            @NonNull AnalyticsRequestExecutor analyticsRequestExecutor,
            @NonNull FraudDetectionDataRepository fraudDetectionDataRepository
    ) {
        return new StripeApiRepository(
                context,
                () -> publishableKey,
                RequestSurface.PaymentElement,
                null,
                new FakeLogger(),
                workDispatcher,
                emptySet(),
                new DefaultStripeNetworkClient(workDispatcher),
                analyticsRequestExecutor,
                fraudDetectionDataRepository,
                new DefaultCardAccountRangeRepositoryFactory(context)
        );
    }

    private void idle() {
        idleLooper();
    }
}
