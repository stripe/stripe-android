package com.stripe.android.net;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.PollingFailedException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceRedirect;

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

import static com.stripe.android.net.PollingNetworkHandler.SourceRetriever;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Mock Source mFailedSource;
    @Mock Source mPendingSource;
    @Mock Source mSucceedSource;

    @Mock SourceRetriever mSourceRetriever;
    @Mock PollingResponseHandler mPollingResponseHandler;

    private PollingNetworkHandler mPollingNetworkHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mPollingNetworkHandler = initializeHandler(
                mPollingResponseHandler,
                700,
                mSourceRetriever);
        SourceRedirect failedSourceRedirect = mock(SourceRedirect.class);
        SourceRedirect pendingSourceRedirect = mock(SourceRedirect.class);
        SourceRedirect succeedSourceRedirect = mock(SourceRedirect.class);
        when(failedSourceRedirect.getStatus()).thenReturn(SourceRedirect.FAILED);
        when(pendingSourceRedirect.getStatus()).thenReturn(SourceRedirect.PENDING);
        when(succeedSourceRedirect.getStatus()).thenReturn(SourceRedirect.SUCCEEDED);

        when(mFailedSource.getRedirect()).thenReturn(failedSourceRedirect);
        when(mPendingSource.getRedirect()).thenReturn(pendingSourceRedirect);
        when(mSucceedSource.getRedirect()).thenReturn(succeedSourceRedirect);
    }

    @Test
    public void startPolling_whenNeverUpdates_expires() {

        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();
        ArgumentCaptor<StripeException> exceptionArgumentCaptor =
                ArgumentCaptor.forClass(StripeException.class);

        verify(mPollingResponseHandler).onRetry(200);

        advanceMainLooperBy(200);

        verify(mPollingResponseHandler).onRetry(400);

        advanceMainLooperBy(500);

        verify(mPollingResponseHandler).onError(exceptionArgumentCaptor.capture());
        StripeException expectedArgument = exceptionArgumentCaptor.getValue();
        assertTrue(expectedArgument instanceof PollingFailedException);
        assertTrue(((PollingFailedException) expectedArgument).isExpired());
    }

    @Test
    public void startPolling_whenSuccess_sendsSuccess() {

        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();
        verify(mPollingResponseHandler).onRetry(200);

        setSourceResponse(mSourceRetriever, mSucceedSource);
        advanceMainLooperBy(200);

        verify(mPollingResponseHandler).onSuccess();
    }

    @Test
    public void startPolling_whenFailure_sendsFailure() {
        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();
        verify(mPollingResponseHandler).onRetry(200);

        setSourceResponse(mSourceRetriever, mFailedSource);
        advanceMainLooperBy(200);

        ArgumentCaptor<StripeException> exceptionArgumentCaptor =
                ArgumentCaptor.forClass(StripeException.class);
        verify(mPollingResponseHandler).onError(exceptionArgumentCaptor.capture());
        StripeException expectedArgument = exceptionArgumentCaptor.getValue();
        assertTrue(expectedArgument instanceof PollingFailedException);
        // This one just failed -- it didn't expire.
        assertFalse(((PollingFailedException) expectedArgument).isExpired());
    }

    @Test
    public void startPolling_whenExceptionThrownInApi_passesExceptionToHandler() {
        setSourceResponse(mSourceRetriever, mPendingSource);

        mPollingNetworkHandler.start();
        verify(mPollingResponseHandler).onRetry(200);

        setSourceException(mSourceRetriever, new APIConnectionException("expected error"));

        advanceMainLooperBy(200);

        ArgumentCaptor<StripeException> exceptionArgumentCaptor =
                ArgumentCaptor.forClass(StripeException.class);
        verify(mPollingResponseHandler).onError(exceptionArgumentCaptor.capture());
        StripeException expectedArgument = exceptionArgumentCaptor.getValue();
        assertTrue(expectedArgument instanceof APIConnectionException);
        assertEquals("expected error", expectedArgument.getMessage());
    }

    private static PollingNetworkHandler initializeHandler(
            PollingResponseHandler pollingResponseHandler,
            int timeout,
            SourceRetriever sourceRetriever) {
        return new PollingNetworkHandler(
                DUMMY_SOURCE_ID,
                DUMMY_CLIENT_SECRET,
                DUMMY_PUBLISHABLE_KEY,
                pollingResponseHandler,
                timeout,
                sourceRetriever);
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
