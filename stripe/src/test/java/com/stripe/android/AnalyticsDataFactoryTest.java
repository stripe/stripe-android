package com.stripe.android;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.model.Source;
import com.stripe.android.model.Token;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link AnalyticsDataFactory}.
 */
@RunWith(RobolectricTestRunner.class)
public class AnalyticsDataFactoryTest {

    private static final String API_KEY = "pk_abc123";
    private static final List<String> EXPECTED_SINGLE_TOKEN_LIST =
            Collections.singletonList("CardInputView");

    @NonNull private final AnalyticsDataFactory mAnalyticsDataFactory =
            new AnalyticsDataFactory(ApplicationProvider.getApplicationContext());

    @Test
    public void getTokenCreationParams_withValidInput_createsCorrectMap() {
        List<String> tokensList = new ArrayList<>();
        tokensList.add("CardInputView");
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION);

        final Map<String, Object> params = mAnalyticsDataFactory.getTokenCreationParams(
                tokensList,
                API_KEY,
                Token.TokenType.PII);
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(expectedTokenName, params.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(Token.TokenType.PII, params.get(AnalyticsDataFactory.FIELD_TOKEN_TYPE));
    }

    @Test
    public void getCvcUpdateTokenCreationParams_withValidInput_createsCorrectMap() {
        List<String> tokensList = new ArrayList<>();
        tokensList.add("CardInputView");
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION);

        final Map<String, Object> params = mAnalyticsDataFactory.getTokenCreationParams(
                tokensList,
                API_KEY,
                Token.TokenType.CVC_UPDATE);
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(expectedTokenName, params.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(Token.TokenType.CVC_UPDATE, params.get(AnalyticsDataFactory.FIELD_TOKEN_TYPE));
    }

    @Test
    public void getSourceCreationParams_withValidInput_createsCorrectMap() {
        // Size is SIZE-1 because tokens don't have a token_type field
        final int expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 1;
        List<String> tokenList = new ArrayList<>();
        tokenList.add("CardInputView");
        final Map<String, Object> loggingParams = mAnalyticsDataFactory.getSourceCreationParams(
                tokenList,
                API_KEY,
                Source.SourceType.SEPA_DEBIT);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(Source.SourceType.SEPA_DEBIT,
                loggingParams.get(AnalyticsDataFactory.FIELD_SOURCE_TYPE));
        assertEquals(API_KEY, loggingParams.get(AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY));
        assertEquals(AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.SOURCE_CREATION),
                loggingParams.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(AnalyticsDataFactory.getAnalyticsUa(),
                loggingParams.get(AnalyticsDataFactory.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getPaymentMethodCreationParams() {
        final Map<String, Object> loggingParams = mAnalyticsDataFactory
                .getPaymentMethodCreationParams(API_KEY);
        assertNotNull(loggingParams);
        assertEquals(API_KEY, loggingParams.get(AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY));
        assertEquals(AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.ADD_PAYMENT_METHOD),
                loggingParams.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(AnalyticsDataFactory.getAnalyticsUa(),
                loggingParams.get(AnalyticsDataFactory.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getPaymentIntentConfirmationParams_withValidInput_createsCorrectMap() {
        final int expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 1;
        List<String> tokenList = new ArrayList<>();
        tokenList.add("CardInputView");
        Map<String, Object> loggingParams = mAnalyticsDataFactory.getPaymentIntentConfirmationParams(
                tokenList,
                API_KEY,
                null);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(API_KEY, loggingParams.get(AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY));
        assertEquals(AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.CONFIRM_PAYMENT_INTENT),
                loggingParams.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(AnalyticsDataFactory.getAnalyticsUa(),
                loggingParams.get(AnalyticsDataFactory.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        final int expectedSize = AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 1;
        List<String> tokenList = new ArrayList<>();
        tokenList.add("CardInputView");
        Map<String, Object> loggingParams = mAnalyticsDataFactory.getPaymentIntentRetrieveParams(
                tokenList,
                API_KEY);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(API_KEY, loggingParams.get(AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY));
        assertEquals(AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.RETRIEVE_PAYMENT_INTENT),
                loggingParams.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(AnalyticsDataFactory.getAnalyticsUa(),
                loggingParams.get(AnalyticsDataFactory.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getEventLoggingParams_withProductUsage_createsAllFields()
            throws PackageManager.NameNotFoundException {
        List<String> tokensList = new ArrayList<>();
        tokensList.add("CardInputView");
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION);
        final String expectedUaName = AnalyticsDataFactory.getAnalyticsUa();

        final int versionCode = 20;
        final String packageName = BuildConfig.APPLICATION_ID;
        final PackageManager packageManager = mock(PackageManager.class);
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = versionCode;
        packageInfo.packageName = BuildConfig.APPLICATION_ID;
        when(packageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        final Map<String, Object> params = new AnalyticsDataFactory(packageManager, packageName)
                .getTokenCreationParams(tokensList, API_KEY, Token.TokenType.CARD);
        assertEquals(AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(API_KEY, params.get(AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY));
        assertEquals(EXPECTED_SINGLE_TOKEN_LIST, params.get(AnalyticsDataFactory.FIELD_PRODUCT_USAGE));
        assertEquals(Token.TokenType.CARD, params.get(AnalyticsDataFactory.FIELD_TOKEN_TYPE));
        assertEquals(Build.VERSION.SDK_INT, params.get(AnalyticsDataFactory.FIELD_OS_VERSION));
        assertNotNull(params.get(AnalyticsDataFactory.FIELD_OS_RELEASE));
        assertNotNull(params.get(AnalyticsDataFactory.FIELD_OS_NAME));
        assertEquals(versionCode, params.get(AnalyticsDataFactory.FIELD_APP_VERSION));
        assertEquals(BuildConfig.APPLICATION_ID, params.get(AnalyticsDataFactory.FIELD_APP_NAME));

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params.get(AnalyticsDataFactory.FIELD_BINDINGS_VERSION));
        assertEquals(expectedTokenName, params.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(expectedUaName, params.get(AnalyticsDataFactory.FIELD_ANALYTICS_UA));

        assertEquals("unknown_Android_robolectric",
                params.get(AnalyticsDataFactory.FIELD_DEVICE_TYPE));
    }

    @Test
    public void getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.SOURCE_CREATION);
        final String expectedUaName = AnalyticsDataFactory.getAnalyticsUa();

        final Map<String, Object> params = mAnalyticsDataFactory.getSourceCreationParams(
                null, API_KEY, Token.TokenType.BANK_ACCOUNT);
        assertEquals(AnalyticsDataFactory.VALID_PARAM_FIELDS.size() - 2, params.size());
        assertEquals(API_KEY, params.get(AnalyticsDataFactory.FIELD_PUBLISHABLE_KEY));
        assertEquals(Token.TokenType.BANK_ACCOUNT, params.get(AnalyticsDataFactory.FIELD_SOURCE_TYPE));

        assertEquals(Build.VERSION.SDK_INT, params.get(AnalyticsDataFactory.FIELD_OS_VERSION));
        assertNotNull(params.get(AnalyticsDataFactory.FIELD_OS_RELEASE));
        assertNotNull(params.get(AnalyticsDataFactory.FIELD_OS_NAME));

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params.get(AnalyticsDataFactory.FIELD_BINDINGS_VERSION));
        assertEquals(expectedTokenName, params.get(AnalyticsDataFactory.FIELD_EVENT));
        assertEquals(expectedUaName, params.get(AnalyticsDataFactory.FIELD_ANALYTICS_UA));

        assertEquals("unknown_Android_robolectric", params.get(AnalyticsDataFactory.FIELD_DEVICE_TYPE));
    }

    @Test
    public void addNameAndVersion_whenApplicationContextIsNull_addsNoContextValues() {
        final Map<String, Object> paramsMap = new AnalyticsDataFactory(null, null)
                .createNameAndVersionParams();
        assertEquals(AnalyticsDataFactory.NO_CONTEXT, paramsMap.get(AnalyticsDataFactory.FIELD_APP_NAME));
        assertEquals(AnalyticsDataFactory.NO_CONTEXT, paramsMap.get(AnalyticsDataFactory.FIELD_APP_VERSION));
    }

    @Test
    public void addNameAndVersion_whenPackageInfoNotFound_addsUnknownValues()
            throws PackageManager.NameNotFoundException {
        final String packageName = "dummy_name";
        PackageManager manager = mock(PackageManager.class);

        when(manager.getPackageInfo(packageName, 0))
                .thenThrow(new PackageManager.NameNotFoundException());

        final Map<String, Object> paramsMap = new AnalyticsDataFactory(manager, packageName)
                .createNameAndVersionParams();
        assertEquals(AnalyticsDataFactory.UNKNOWN, paramsMap.get(AnalyticsDataFactory.FIELD_APP_NAME));
        assertEquals(AnalyticsDataFactory.UNKNOWN, paramsMap.get(AnalyticsDataFactory.FIELD_APP_VERSION));
    }

    @Test
    public void getEventParamName_withTokenCreation_createsExpectedParameter() {
        final String expectedEventParam = "stripe_android.token_creation";
        assertEquals(expectedEventParam,
                AnalyticsDataFactory.getEventParamName(AnalyticsDataFactory.EventName.TOKEN_CREATION));
    }

    @Test
    public void getAnalyticsUa_returnsExpectedValue() {
        final String androidAnalyticsUserAgent = "analytics.stripe_android-1.0";
        assertEquals(androidAnalyticsUserAgent, AnalyticsDataFactory.getAnalyticsUa());
    }
}
