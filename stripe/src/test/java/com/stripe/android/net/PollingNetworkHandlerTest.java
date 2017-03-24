package com.stripe.android.net;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PollingNetworkHandler}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class PollingNetworkHandlerTest {

    private static final String DUMMY_SOURCE_ID = "sourceId";
    private static final String DUMMY_CLIENT_SECRET = "clientSecret";
    private static final String DUMMY_PUBLISHABLE_KEY = "pubKey";

    @Mock Source mCancelledSource;
    @Mock Source mChargeableSource;
    @Mock Source mConsumedSource;
    @Mock Source mFailedSource;
    @Mock Source mPendingSource;

    @Mock SourceRetriever mSourceRetriever;
    @Mock PollingResponseHandler mPollingResponseHandler;

    private PollingNetworkHandler mPollingNetworkHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mPollingNetworkHandler = initializeHandler(
                mPollingResponseHandler,
                3000,
                mSourceRetriever);

        when(mConsumedSource.getStatus()).thenReturn(Source.CONSUMED);
        when(mChargeableSource.getStatus()).thenReturn(Source.CHARGEABLE);
        when(mPendingSource.getStatus()).thenReturn(Source.PENDING);
        when(mCancelledSource.getStatus()).thenReturn(Source.CANCELED);
        when(mFailedSource.getStatus()).thenReturn(Source.FAILED);
    }

    @Test
    public void createHandler_withNullTimeout_usesDefault() {
        PollingNetworkHandler handler = initializeHandler(
                mPollingResponseHandler,
                null,
                mSourceRetriever);
        assertEquals(10000L, handler.getTimeoutMs());
    }

    @Test
    public void createHandler_withNonNullTimeoutBelowMax_usesSetValue() {
        PollingNetworkHandler handler = initializeHandler(
                mPollingResponseHandler,
                12345,
                mSourceRetriever);
        assertEquals(12345L, handler.getTimeoutMs());
    }

    @Test
    public void createHandler_withTimeoutGreaterThanMax_usesMax() {
        long fiveMinutesInMillis = 5L * 60L * 1000L;
        Integer tenMinutesInMillis = 10 * 60 *1000;
        PollingNetworkHandler handler = initializeHandler(
                mPollingResponseHandler,
                tenMinutesInMillis,
                mSourceRetriever);
        assertEquals(fiveMinutesInMillis, handler.getTimeoutMs());
    }

    @Test
    public void startPolling_whenNeverUpdates_expires() {

        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();


        advanceMainLooperBy(2999);
        // This is simulating normal 200-type responses
        assertEquals(0, mPollingNetworkHandler.getRetryCount());
        advanceMainLooperBy(1);

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());
        PollingResponse response = pollingResponseCaptor.getValue();
        assertTrue(response.isExpired());
        assertFalse(response.isSuccess());
        assertEquals(mPendingSource, response.getSource());
    }

    @Test
    public void startPolling_whenChargeable_sendsSuccess() {

        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();

        setSourceResponse(mSourceRetriever, mChargeableSource);
        advanceMainLooperBy(1000);

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());
        PollingResponse response = pollingResponseCaptor.getValue();
        assertTrue(response.isSuccess());
        assertFalse(response.isExpired());
        // They're mocks, but they should theoretically be equal.
        assertEquals(mChargeableSource, response.getSource());

        // Get to the timeout
        advanceMainLooperBy(2000);
        verifyNoMoreInteractions(mPollingResponseHandler);
    }

    @Test
    public void startPolling_whenConsumed_sendsSuccess() {

        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();

        setSourceResponse(mSourceRetriever, mConsumedSource);
        advanceMainLooperBy(1000);

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());
        PollingResponse response = pollingResponseCaptor.getValue();
        assertTrue(response.isSuccess());
        assertFalse(response.isExpired());
        // They're mocks, but they should theoretically be equal.
        assertEquals(mConsumedSource, response.getSource());
    }

    @Test
    public void startPolling_whenCancelled_sendsFailureResponse() {
        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();

        setSourceResponse(mSourceRetriever, mCancelledSource);
        advanceMainLooperBy(1000);

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());
        PollingResponse response = pollingResponseCaptor.getValue();
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
        // They're mocks, but they should theoretically be equal.
        assertEquals(mCancelledSource, response.getSource());
    }

    @Test
    public void startPolling_whenFailed_sendsFailureResponse() {
        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();

        setSourceResponse(mSourceRetriever, mFailedSource);
        advanceMainLooperBy(1000);

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());
        PollingResponse response = pollingResponseCaptor.getValue();
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
        // They're mocks, but they should theoretically be equal.
        assertEquals(mFailedSource, response.getSource());
    }

    @Test
    public void startPolling_whenExceptionThrownInApi_retriesAfterDecayingDelay() {
        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();

        setSourceException(mSourceRetriever, new APIConnectionException("expected error"));

        advanceMainLooperBy(1000);
        assertEquals(1, mPollingNetworkHandler.getRetryCount());
    }

    @Test
    public void startPolling_whenExceptionThrownInApiFollowedByPending_setsRetryCountToZero() {
        PollingNetworkHandler pollingNetworkHandler = initializeHandler(
                mPollingResponseHandler,
                30000,
                mSourceRetriever);
        setSourceResponse(mSourceRetriever, mPendingSource);

        pollingNetworkHandler.start();

        SourceRetriever exceptionRetriever = mock(SourceRetriever.class);
        setSourceException(exceptionRetriever, new APIConnectionException("expected error"));
        pollingNetworkHandler.setSourceRetriever(exceptionRetriever);

        advanceMainLooperBy(1000);
        assertEquals(1, pollingNetworkHandler.getRetryCount());

        pollingNetworkHandler.setSourceRetriever(mSourceRetriever);
        advanceMainLooperBy(2000);
        assertEquals(0, pollingNetworkHandler.getRetryCount());
    }

    @Test
    public void startPolling_whenExceptionThrownTooManyTimes_sendsErrorResponse() {
        PollingNetworkHandler pollingNetworkHandler = initializeHandler(
                mPollingResponseHandler,
                50000,
                mSourceRetriever);
        setSourceException(mSourceRetriever, new APIConnectionException("expected error"));

        pollingNetworkHandler.start();
        assertEquals(1, pollingNetworkHandler.getRetryCount());

        advanceMainLooperBy(2000);
        assertEquals(2, pollingNetworkHandler.getRetryCount());

        advanceMainLooperBy(4000);
        assertEquals(3, pollingNetworkHandler.getRetryCount());

        advanceMainLooperBy(8000);
        assertEquals(4, pollingNetworkHandler.getRetryCount());

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);

        advanceMainLooperBy(15000);
        assertEquals(5, pollingNetworkHandler.getRetryCount());
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());

        PollingResponse response = pollingResponseCaptor.getValue();
        assertNotNull(response.getStripeException());
        assertEquals("expected error", response.getStripeException().getMessage());
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
        // This value is null because we never got a valid source back.
        assertNull(response.getSource());
    }

    @Test
    public void startPolling_whenTooManyErrorsAfterPendingOnce_sendsErrorResponseWithSource() {
        PollingNetworkHandler pollingNetworkHandler = initializeHandler(
                mPollingResponseHandler,
                50000,
                mSourceRetriever);
        setSourceResponse(mSourceRetriever, mPendingSource);

        pollingNetworkHandler.start();

        SourceRetriever exceptionRetriever = mock(SourceRetriever.class);
        setSourceException(exceptionRetriever, new APIConnectionException("expected error"));
        pollingNetworkHandler.setSourceRetriever(exceptionRetriever);

        advanceMainLooperBy(1000);

        advanceMainLooperBy(2000);

        advanceMainLooperBy(4000);

        advanceMainLooperBy(8000);

        SourceRetriever otherExceptionRetriever = mock(SourceRetriever.class);
        setSourceException(otherExceptionRetriever, new APIConnectionException("next error"));
        pollingNetworkHandler.setSourceRetriever(otherExceptionRetriever);

        ArgumentCaptor<PollingResponse> pollingResponseCaptor =
                ArgumentCaptor.forClass(PollingResponse.class);

        advanceMainLooperBy(15000);
        verify(mPollingResponseHandler).onPollingResponse(pollingResponseCaptor.capture());

        PollingResponse response = pollingResponseCaptor.getValue();
        assertNotNull(response.getStripeException());
        assertEquals("next error", response.getStripeException().getMessage());
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
        assertEquals(mPendingSource, response.getSource());
    }

    private static PollingNetworkHandler initializeHandler(
            PollingResponseHandler pollingResponseHandler,
            Integer timeout,
            SourceRetriever sourceRetriever) {
        return new PollingNetworkHandler(
                DUMMY_SOURCE_ID,
                DUMMY_CLIENT_SECRET,
                DUMMY_PUBLISHABLE_KEY,
                pollingResponseHandler,
                timeout,
                sourceRetriever,
                PollingParameters.generateDefaultParameters());
    }

    private static void advanceMainLooperBy(int millis) {
        ShadowLooper.pauseMainLooper();
        Robolectric.getForegroundThreadScheduler().advanceBy(millis, TimeUnit.MILLISECONDS);
        // Let the time advance
        ShadowLooper.unPauseMainLooper();
        // Now pause again
        ShadowLooper.pauseMainLooper();
    }

    private static void setSourceResponse(SourceRetriever sourceRetriever, Source source) {
        try {
            when(sourceRetriever.retrieveSource(
                    DUMMY_SOURCE_ID,
                    DUMMY_CLIENT_SECRET,
                    DUMMY_PUBLISHABLE_KEY)).thenReturn(source);
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx);
        }
    }

    private static void setSourceException(SourceRetriever sourceRetriever, StripeException ex) {
        try {
            when(sourceRetriever.retrieveSource(
                    DUMMY_SOURCE_ID,
                    DUMMY_CLIENT_SECRET,
                    DUMMY_PUBLISHABLE_KEY)).thenThrow(ex);
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx);
        }
    }
}
