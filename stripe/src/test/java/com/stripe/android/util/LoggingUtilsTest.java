package com.stripe.android.util;

import com.stripe.android.BuildConfig;
import com.stripe.android.model.Source;
import com.stripe.android.model.Token;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for {@link LoggingUtils}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class LoggingUtilsTest {

    private static final String DUMMY_API_KEY = "pk_abc123";
    private static final List<String> EXPECTED_SINGLE_TOKEN_LIST = new ArrayList<>();
    static {
        EXPECTED_SINGLE_TOKEN_LIST.add(LoggingUtils.CARD_WIDGET_TOKEN);
    }

    @Test
    public void getTokenCreationParams_withValidInput_createsCorrectMap() {
        List<String> tokensList = new ArrayList<>();
        tokensList.add(LoggingUtils.CARD_WIDGET_TOKEN);
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_TOKEN_CREATION);

        Map<String, Object> params = LoggingUtils.getTokenCreationParams(
                tokensList,
                DUMMY_API_KEY,
                Token.TYPE_PII);
        // Size is SIZE-1 because tokens don't have a source_type field
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(Token.TYPE_PII, params.get(LoggingUtils.FIELD_TOKEN_TYPE));
    }

    @Test
    public void getSourceCreationParams_withValidInput_createsCorrectMap() {
        // We don't use any product usage fields for sources yet, and we are also missing
        // the TOKEN_TYPE logging field.
        final int expectedSize = LoggingUtils.VALID_PARAM_FIELDS.size() - 2;
        Map<String, Object> loggingParams =
                LoggingUtils.getSourceCreationParams(DUMMY_API_KEY, Source.SEPA_DEBIT);
        assertEquals(expectedSize, loggingParams.size());
        assertEquals(Source.SEPA_DEBIT, loggingParams.get(LoggingUtils.FIELD_SOURCE_TYPE));
        assertEquals(DUMMY_API_KEY, loggingParams.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(LoggingUtils.getEventParamName(LoggingUtils.EVENT_SOURCE_CREATION),
                loggingParams.get(LoggingUtils.FIELD_EVENT));
        assertEquals(LoggingUtils.getAnalyticsUa(),
                loggingParams.get(LoggingUtils.FIELD_ANALYTICS_UA));
    }

    @Test
    public void getEventLoggingParams_withProductUsage_createsAllFields() {
        List<String> tokensList = new ArrayList<>();
        tokensList.add(LoggingUtils.CARD_WIDGET_TOKEN);
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_TOKEN_CREATION);
        final String expectedUaName = LoggingUtils.getAnalyticsUa();

        Map<String, Object> params =
                LoggingUtils.getEventLoggingParams(
                        tokensList,
                        null,
                        Token.TYPE_CARD,
                        DUMMY_API_KEY,
                        LoggingUtils.EVENT_TOKEN_CREATION);
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 1, params.size());
        assertEquals(DUMMY_API_KEY, params.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(EXPECTED_SINGLE_TOKEN_LIST, params.get(LoggingUtils.FIELD_PRODUCT_USAGE));
        assertEquals(Token.TYPE_CARD, params.get(LoggingUtils.FIELD_TOKEN_TYPE));
        // Expected value is 23 because that's the number in the @Config for this test class.
        assertEquals(23, params.get(LoggingUtils.FIELD_OS_VERSION));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_RELEASE));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_NAME));

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params.get(LoggingUtils.FIELD_BINDINGS_VERSION));
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(expectedUaName, params.get(LoggingUtils.FIELD_ANALYTICS_UA));

        // Not yet PowerMocking android.os.Build -- just check that this is logged.
        assertNotNull(params.get(LoggingUtils.FIELD_DEVICE_TYPE));
    }

    @Test
    public void getEventLoggingParams_withoutProductUsage_createsOnlyNeededFields() {
        // Correctness of these methods will be tested elsewhere. Assume validity for this test.
        final String expectedTokenName =
                LoggingUtils.getEventParamName(LoggingUtils.EVENT_SOURCE_CREATION);
        final String expectedUaName = LoggingUtils.getAnalyticsUa();

        Map<String, Object> params =
                LoggingUtils.getEventLoggingParams(
                        null,
                        null,
                        Token.TYPE_BANK_ACCOUNT,
                        DUMMY_API_KEY,
                        LoggingUtils.EVENT_SOURCE_CREATION);
        assertEquals(LoggingUtils.VALID_PARAM_FIELDS.size() - 2, params.size());
        assertEquals(DUMMY_API_KEY, params.get(LoggingUtils.FIELD_PUBLISHABLE_KEY));
        assertEquals(Token.TYPE_BANK_ACCOUNT, params.get(LoggingUtils.FIELD_TOKEN_TYPE));

        // Expected value is 23 because that's the number in the @Config for this test class.
        assertEquals(23, params.get(LoggingUtils.FIELD_OS_VERSION));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_RELEASE));
        assertNotNull(params.get(LoggingUtils.FIELD_OS_NAME));

        // The @Config constants param means BuildConfig constants are the same in prod as in test.
        assertEquals(BuildConfig.VERSION_NAME, params.get(LoggingUtils.FIELD_BINDINGS_VERSION));
        assertEquals(expectedTokenName, params.get(LoggingUtils.FIELD_EVENT));
        assertEquals(expectedUaName, params.get(LoggingUtils.FIELD_ANALYTICS_UA));

        // Not yet PowerMocking android.os.Build -- just check that this is logged.
        assertNotNull(params.get(LoggingUtils.FIELD_DEVICE_TYPE));
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
