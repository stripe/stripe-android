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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LoggingUtils}.
 */
@RunWith(RobolectricTestRunner.class)
public class LoggingUtilsTest {

    private static final String API_KEY = "pk_abc123";
    private static final List<String> EXPECTED_SINGLE_TOKEN_LIST = new ArrayList<>();
    static {
        EXPECTED_SINGLE_TOKEN_LIST.add("CardInputView");
    }

    @NonNull private final LoggingUtils mLoggingUtils =
            new LoggingUtils(ApplicationProvider.getApplicationContext());

    @Test
    public void getTokenCreationParams_withValidInput_createsCorrectMap() {
        List<String> tokensList = new ArrayList<>();
        tokensList.add("CardInputView");
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_TOKEN_CREATION);

        final Map<String, Object> params = mLoggingUtils.getTokenCreationParams(
                tokensList,
                API_KEY,
                Token.TYPE_PII);
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(Token.TYPE_PII, params.get(LoggingUtils.FIELD_TOKEN_TYPE));
    }

    @Test
    public void getCvcUpdateTokenCreationParams_withValidInput_createsCorrectMap() {
        List<String> tokensList = new ArrayList<>();
        tokensList.add("CardInputView");
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_TOKEN_CREATION);

        final Map<String, Object> params = mLoggingUtils.getTokenCreationParams(
                tokensList,
                API_KEY,
                Token.TYPE_CVC_UPDATE);
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(Token.TYPE_CVC_UPDATE, params.get(LoggingUtils.FIELD_TOKEN_TYPE));
    }

    @Test
    public void getSourceCreationParams_withValidInput_createsCorrectMap() {
        // Size is SIZE-1 because tokens don't have a token_type field
        final int expectedSize = LoggingUtils.VALID_PARAM_FIELDS.size() - 1;
        List<String> tokenList = new ArrayList<>();
        tokenList.add("CardInputView");
        final Map<String, Object> loggingParams = mLoggingUtils.getSourceCreationParams(
                tokenList,
                API_KEY,
                Source.SEPA_DEBIT);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(Source.SEPA_DEBIT, loggingParams.get(LoggingUtils.FIELD_SOURCE_TYPE));
        assertEquals(API_KEY, loggingParams.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(LoggingUtils.getEventParamName(LoggingUtils.EVENT_SOURCE_CREATION),
                loggingParams.get(LoggingUtils.FIELD_EVENT));
        assertEquals(LoggingUtils.getAnalyticsUa(),
                loggingParams.get(LoggingUtils.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getPaymentMethodCreationParams() {
        final Map<String, Object> loggingParams = mLoggingUtils
                .getPaymentMethodCreationParams(null, API_KEY);
        assertNotNull(loggingParams);
        assertEquals(API_KEY, loggingParams.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(LoggingUtils.getEventParamName(LoggingUtils.EVENT_ADD_PAYMENT_METHOD),
                loggingParams.get(LoggingUtils.FIELD_EVENT));
        assertEquals(LoggingUtils.getAnalyticsUa(),
                loggingParams.get(LoggingUtils.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getPaymentIntentConfirmationParams_withValidInput_createsCorrectMap() {
        final int expectedSize = LoggingUtils.VALID_PARAM_FIELDS.size() - 1;
        List<String> tokenList = new ArrayList<>();
        tokenList.add("CardInputView");
        Map<String, Object> loggingParams = mLoggingUtils.getPaymentIntentConfirmationParams(
                tokenList,
                API_KEY,
                null);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(API_KEY, loggingParams.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(LoggingUtils.getEventParamName(LoggingUtils.EVENT_CONFIRM_PAYMENT_INTENT),
                loggingParams.get(LoggingUtils.FIELD_EVENT));
        assertEquals(LoggingUtils.getAnalyticsUa(),
                loggingParams.get(LoggingUtils.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getPaymentIntentRetrieveParams_withValidInput_createsCorrectMap() {
        final int expectedSize = LoggingUtils.VALID_PARAM_FIELDS.size() - 1;
        List<String> tokenList = new ArrayList<>();
        tokenList.add("CardInputView");
        Map<String, Object> loggingParams = mLoggingUtils.getPaymentIntentRetrieveParams(
                tokenList,
                API_KEY);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(API_KEY, loggingParams.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(LoggingUtils.getEventParamName(LoggingUtils.EVENT_RETRIEVE_PAYMENT_INTENT),
                loggingParams.get(LoggingUtils.FIELD_EVENT));
        assertEquals(LoggingUtils.getAnalyticsUa(),
                loggingParams.get(LoggingUtils.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getEventLoggingParams_withProductUsage_createsAllFields()
            throws PackageManager.NameNotFoundException {
        List<String> tokensList = new ArrayList<>();
        tokensList.add("CardInputView");
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_TOKEN_CREATION);
        final String expectedUaName = LoggingUtils.getAnalyticsUa();

        final int versionCode = 20;
        final String packageName = BuildConfig.APPLICATION_ID;
        final PackageManager packageManager = mock(PackageManager.class);
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = versionCode;
        packageInfo.packageName = BuildConfig.APPLICATION_ID;
        when(packageManager.getPackageInfo(packageName, 0))
                .thenReturn(packageInfo);

        final Map<String, Object> params = new LoggingUtils(packageManager, packageName)
                .getEventLoggingParams(
                        tokensList,
                        null,
                        Token.TYPE_CARD,
                        API_KEY, LoggingUtils.EVENT_TOKEN_CREATION);
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(API_KEY, params.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(EXPECTED_SINGLE_TOKEN_LIST, params.get(LoggingUtils.FIELD_PRODUCT_USAGE));
        assertEquals(Token.TYPE_CARD, params.get(LoggingUtils.FIELD_TOKEN_TYPE));
        assertEquals(Build.VERSION.SDK_INT, params.get(LoggingUtils.FIELD_OS_VERSION));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_RELEASE));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_NAME));
        assertEquals(versionCode, params.get(LoggingUtils.FIELD_APP_VERSION));
        assertEquals(BuildConfig.APPLICATION_ID, params.get(LoggingUtils.FIELD_APP_NAME));

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params.get(LoggingUtils.FIELD_BINDINGS_VERSION));
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(expectedUaName, params.get(LoggingUtils.FIELD_ANALYTICS_UA));

        assertEquals("unknown_Android_robolectric", params.get(LoggingUtils.FIELD_DEVICE_TYPE));
    }

    @Test
    public void getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_SOURCE_CREATION);
        final String expectedUaName = LoggingUtils.getAnalyticsUa();

        final Map<String, Object> params = mLoggingUtils.getEventLoggingParams(
                null,
                null,
                Token.TYPE_BANK_ACCOUNT,
                API_KEY, LoggingUtils.EVENT_SOURCE_CREATION);
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 2, params.size());
        assertEquals(API_KEY, params.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(Token.TYPE_BANK_ACCOUNT, params.get(LoggingUtils.FIELD_TOKEN_TYPE));

        assertEquals(Build.VERSION.SDK_INT, params.get(LoggingUtils.FIELD_OS_VERSION));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_RELEASE));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_NAME));

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params.get(LoggingUtils.FIELD_BINDINGS_VERSION));
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(expectedUaName, params.get(LoggingUtils.FIELD_ANALYTICS_UA));

        assertEquals("unknown_Android_robolectric", params.get(LoggingUtils.FIELD_DEVICE_TYPE));
    }

    @Test
    public void addNameAndVersion_whenApplicationContextIsNull_addsNoContextValues() {
        Map<String, Object> paramsMap = new HashMap<>();
        new LoggingUtils(null, null).addNameAndVersion(paramsMap);
        assertEquals(LoggingUtils.NO_CONTEXT, paramsMap.get(LoggingUtils.FIELD_APP_NAME));
        assertEquals(LoggingUtils.NO_CONTEXT, paramsMap.get(LoggingUtils.FIELD_APP_VERSION));
    }

    @Test
    public void addNameAndVersion_whenPackageInfoNotFound_addsUnknownValues() {
        final String dummyName = "dummy_name";
        PackageManager manager = mock(PackageManager.class);

        try {
            when(manager.getPackageInfo(dummyName, 0))
                    .thenThrow(new PackageManager.NameNotFoundException());
        } catch (PackageManager.NameNotFoundException namex) {
            fail("Unexpected exception thrown.");
        }

        Map<String, Object> paramsMap = new HashMap<>();
        new LoggingUtils(manager, dummyName).addNameAndVersion(paramsMap);
        assertEquals(LoggingUtils.UNKNOWN, paramsMap.get(LoggingUtils.FIELD_APP_NAME));
        assertEquals(LoggingUtils.UNKNOWN, paramsMap.get(LoggingUtils.FIELD_APP_VERSION));
    }

    @Test
    public void getEventParamName_withTokenCreation_createsExpectedParameter() {
        final String expectedEventParam = "stripe_android.token_creation";
        assertEquals(expectedEventParam,
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_TOKEN_CREATION));
    }

    @Test
    public void getAnalyticsUa_returnsExpectedValue() {
        final String androidAnalyticsUserAgent = "analytics.stripe_android-1.0";
        assertEquals(androidAnalyticsUserAgent, LoggingUtils.getAnalyticsUa());
    }
}
