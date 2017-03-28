package com.stripe.android.net;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.OngoingStubbing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link PollingSyncNetworkHandler}.
 */
public class PollingSyncNetworkHandlerTest {

    private static final String DUMMY_SOURCE_ID = "sourceId";
    private static final String DUMMY_CLIENT_SECRET = "clientSecret";
    private static final String DUMMY_PUBLISHABLE_KEY = "pubKey";

    @Mock Source mCancelledSource;
    @Mock Source mChargeableSource;
    @Mock Source mConsumedSource;
    @Mock Source mFailedSource;
    @Mock Source mPendingSource;

    @Mock SourceRetriever mSourceRetriever;

    private PollingParameters mPollingParameters;
    private PollingSyncNetworkHandler.TimeRetriever mTimeRetriever;
    private PollingSyncNetworkHandler mPollingSyncNetworkHandler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mTimeRetriever = new PollingSyncNetworkHandler.TimeRetriever() {
            @Override
            public long getCurrentTimeInMillis() {
                return System.currentTimeMillis();
            }
        };
        when(mConsumedSource.getStatus()).thenReturn(Source.CONSUMED);
        when(mChargeableSource.getStatus()).thenReturn(Source.CHARGEABLE);
        when(mPendingSource.getStatus()).thenReturn(Source.PENDING);
        when(mCancelledSource.getStatus()).thenReturn(Source.CANCELED);
        when(mFailedSource.getStatus()).thenReturn(Source.FAILED);

        mPollingParameters = new PollingParameters(
                10000L,
                100L,
                1500,
                5,
                5L * 60L * 1000L,
                2);
        mPollingSyncNetworkHandler = new PollingSyncNetworkHandler(
                DUMMY_SOURCE_ID,
                DUMMY_CLIENT_SECRET,
                DUMMY_PUBLISHABLE_KEY,
                1000,
                mSourceRetriever,
                mTimeRetriever,
                mPollingParameters);
    }

    @Test
    public void createHandler_withNullTimeout_usesDefault() {
        PollingSyncNetworkHandler handler = new PollingSyncNetworkHandler(
                DUMMY_SOURCE_ID,
                DUMMY_CLIENT_SECRET,
                DUMMY_PUBLISHABLE_KEY,
                null,
                mSourceRetriever,
                mTimeRetriever,
                PollingParameters.generateDefaultParameters());
        assertEquals(10000L, handler.getTimeOutMs());
    }

    @Test
    public void createHandler_withNonNullTimeoutBelowMax_usesSetValue() {
        PollingSyncNetworkHandler handler = new PollingSyncNetworkHandler(
                DUMMY_SOURCE_ID,
                DUMMY_CLIENT_SECRET,
                DUMMY_PUBLISHABLE_KEY,
                12345,
                mSourceRetriever,
                mTimeRetriever,
                PollingParameters.generateDefaultParameters());
        assertEquals(12345L, handler.getTimeOutMs());
    }

    @Test
    public void createHandler_withTimeoutGreaterThanMax_usesMax() {
        long fiveMinutesInMillis = 5L * 60L * 1000L;
        Integer tenMinutesInMillis = 10 * 60 *1000;
        PollingSyncNetworkHandler handler = new PollingSyncNetworkHandler(
                DUMMY_SOURCE_ID,
                DUMMY_CLIENT_SECRET,
                DUMMY_PUBLISHABLE_KEY,
                tenMinutesInMillis,
                mSourceRetriever,
                mTimeRetriever,
                PollingParameters.generateDefaultParameters());
        assertEquals(fiveMinutesInMillis, handler.getTimeOutMs());
    }

    @Test
    public void pollForSourceUpdate_whenChargeable_returnsSuccess() {
        setSourceResponses(mSourceRetriever, mChargeableSource);

        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        assertNotNull(response.getSource());
        assertEquals(Source.CHARGEABLE, response.getSource().getStatus());
        assertTrue(response.isSuccess());
        assertFalse(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenConsumed_returnsSuccess() {
        setSourceResponses(mSourceRetriever, mConsumedSource);

        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        assertNotNull(response.getSource());
        assertEquals(Source.CONSUMED, response.getSource().getStatus());
        assertTrue(response.isSuccess());
        assertFalse(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenChangesToChargeable_returnsSuccess() {
        setSourceResponses(mSourceRetriever, mPendingSource, mChargeableSource);

        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        verifyRetrieveCallCount(mSourceRetriever, 2);

        assertNotNull(response.getSource());
        assertEquals(Source.CHARGEABLE, response.getSource().getStatus());
        assertTrue(response.isSuccess());
        assertFalse(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenChangesToFailed_returnsFailure() {
        setSourceResponses(mSourceRetriever, mPendingSource, mPendingSource, mFailedSource);

        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        verifyRetrieveCallCount(mSourceRetriever, 3);

        assertNotNull(response.getSource());
        assertEquals(Source.FAILED, response.getSource().getStatus());
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenChangesToCanceled_returnsFailure() {
        setSourceResponses(mSourceRetriever, mPendingSource, mPendingSource, mCancelledSource);

        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        verifyRetrieveCallCount(mSourceRetriever, 3);

        assertNotNull(response.getSource());
        assertEquals(Source.CANCELED, response.getSource().getStatus());
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenAlwaysPending_expires() {
        setSourceResponses(mSourceRetriever, mPendingSource);
        long startTime = System.currentTimeMillis();
        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        long endTime = System.currentTimeMillis();

        assertTrue(endTime - startTime > 1000L);
        // Default timeout is 10 seconds (set to 1 for test),
        // and we poll ten times per second (in test), so we should be slightly
        // over the timeout limit after 10 tries.
        verifyRetrieveCallCount(mSourceRetriever, 10);
        assertNotNull(response.getSource());
        assertEquals(Source.PENDING, response.getSource().getStatus());
        assertFalse(response.isSuccess());
        assertTrue(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenSingleExceptionThenChargeable_returnsSuccess() {
        setSourceResponses(mSourceRetriever, mPendingSource, mPendingSource, mCancelledSource);

        try {
            when(mSourceRetriever.retrieveSource(
                    DUMMY_SOURCE_ID,
                    DUMMY_CLIENT_SECRET,
                    DUMMY_PUBLISHABLE_KEY))
                    .thenReturn(mPendingSource)
                    .thenThrow(new APIConnectionException("Stripe is down"))
                    .thenThrow(new APIConnectionException("Still down"))
                    .thenReturn(mChargeableSource);
        } catch (StripeException stripeEx) {
            fail("Unexpected exception: " + stripeEx.getLocalizedMessage());
        }

        long startTime = System.currentTimeMillis();
        PollingResponse response = mPollingSyncNetworkHandler.pollForSourceUpdate();
        long endTime = System.currentTimeMillis();

        // If the exception instances were just pending responses, this would only take
        // approximately 4000ms
        assertTrue("Exponential backing off should delay calls", endTime - startTime >= 700L);
        verifyRetrieveCallCount(mSourceRetriever, 4);

        assertNotNull(response.getSource());
        assertEquals(Source.CHARGEABLE, response.getSource().getStatus());
        assertTrue(response.isSuccess());
        assertFalse(response.isExpired());
    }

    @Test
    public void pollForSourceUpdate_whenFiveExceptions_returnsFailureWithLastException() {

        PollingSyncNetworkHandler pollingSyncNetworkHandler
                = new PollingSyncNetworkHandler(
                        DUMMY_SOURCE_ID,
                        DUMMY_CLIENT_SECRET,
                        DUMMY_PUBLISHABLE_KEY,
                        5000, // need a longer timeout because of the exponential backoff
                        mSourceRetriever,
                        mTimeRetriever,
                        mPollingParameters);
        APIConnectionException connectEx = new APIConnectionException("Can't reach server");
        APIException apiException = new APIException("Something different", "abc", 123, null);
        setSourceExceptions(mSourceRetriever,
                connectEx,
                connectEx,
                connectEx,
                connectEx,
                apiException);

        long startTime = System.currentTimeMillis();
        PollingResponse response = pollingSyncNetworkHandler.pollForSourceUpdate();
        long endTime = System.currentTimeMillis();

        // Delays are 100 + 200 + 400 + 800 = 1500
        assertTrue("Exponential backing off should delay calls", endTime - startTime >= 1500L);
        verifyRetrieveCallCount(mSourceRetriever, 5);

        assertNull(response.getSource());
        assertNotNull(response.getStripeException());
        assertTrue(response.getStripeException() instanceof APIException);
        assertFalse(response.isSuccess());
        assertFalse(response.isExpired());
    }

    private static void verifyRetrieveCallCount(SourceRetriever sourceRetriever, int count) {
        try {
            verify(sourceRetriever, times(count)).retrieveSource(
                    DUMMY_SOURCE_ID,
                    DUMMY_CLIENT_SECRET,
                    DUMMY_PUBLISHABLE_KEY);
        } catch (StripeException stripeEx) {
            fail("Unexpected exception: " + stripeEx.getLocalizedMessage());
        }
    }

    private static void setSourceResponses(SourceRetriever sourceRetriever, Source... sources) {
        try {
            OngoingStubbing<Source> stubbing = null;
            for(Source source : sources) {
                if (stubbing == null) {
                    stubbing = when(sourceRetriever.retrieveSource(
                            DUMMY_SOURCE_ID,
                            DUMMY_CLIENT_SECRET,
                            DUMMY_PUBLISHABLE_KEY)).thenReturn(source);
                } else {
                    stubbing = stubbing.thenReturn(source);
                }
            }

        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }

    private static void setSourceExceptions(SourceRetriever sourceRetriever,
                                            StripeException... exes) {
        try {
            OngoingStubbing<Source> stubbing = null;

            for (StripeException ex : exes) {
                if (stubbing == null) {
                    stubbing = when(sourceRetriever.retrieveSource(
                            DUMMY_SOURCE_ID,
                            DUMMY_CLIENT_SECRET,
                            DUMMY_PUBLISHABLE_KEY)).thenThrow(ex);
                } else {
                    stubbing = stubbing.thenThrow(ex);
                }
            }
        } catch (StripeException stripeEx) {
            fail("Unexpected error: " + stripeEx.getLocalizedMessage());
        }
    }
}
