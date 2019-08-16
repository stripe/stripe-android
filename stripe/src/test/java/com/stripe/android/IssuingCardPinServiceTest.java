package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link IssuingCardPinService}.
 */
@RunWith(RobolectricTestRunner.class)
public class IssuingCardPinServiceTest {

    private static final String EPHEMERAL_KEY = "{\n" +
            "  \"id\": \"ephkey_123\",\n" +
            "  \"object\": \"ephemeral_key\",\n" +
            "  \"secret\": \"ek_test_123\",\n" +
            "  \"created\": 1501179335,\n" +
            "  \"livemode\": false,\n" +
            "  \"expires\": 1501199335,\n" +
            "  \"associated_objects\": [{\n" +
            "            \"type\": \"issuing.card\",\n" +
            "            \"id\": \"ic_abcd\"\n" +
            "            }]\n" +
            "}";

    @Mock private ApiRequestExecutor mStripeApiRequestExecutor;
    @Mock private IssuingCardPinService.IssuingCardPinRetrievalListener mMockRetrievalListener;
    @Mock private IssuingCardPinService.IssuingCardPinUpdateListener mMockUpdateListener;

    private IssuingCardPinService mService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        TestEphemeralKeyProvider ephemeralKeyProvider = new TestEphemeralKeyProvider();
        ephemeralKeyProvider.setNextRawEphemeralKey(EPHEMERAL_KEY);

        final StripeRepository stripeRepository = new StripeApiRepository(
                ApplicationProvider.getApplicationContext(),
                mStripeApiRequestExecutor,
                new FakeFireAndForgetRequestExecutor(),
                null);

        mService = new IssuingCardPinService(ephemeralKeyProvider, stripeRepository,
                new OperationIdFactory());
    }

    @Test
    public void testRetrieval()
            throws InvalidRequestException, APIConnectionException {

        final StripeResponse response =
                new StripeResponse(200,
                        "{\"card\":\"ic_abcdef\",\"pin\":\"1234\"}",
                        null);

        when(mStripeApiRequestExecutor.execute(
                argThat(new ApiRequestMatcher(
                        StripeRequest.Method.GET,
                        "https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin?verification%5Bone_time_code%5D=123-456&verification%5Bid%5D=iv_abcd",
                        ApiRequest.Options.create("ek_test_123")
                ))))
                .thenReturn(response);

        mMockRetrievalListener =
                mock(IssuingCardPinService.IssuingCardPinRetrievalListener.class);

        mService.retrievePin(
                "ic_abcdef",
                "iv_abcd",
                "123-456",
                mMockRetrievalListener);

        verify(mMockRetrievalListener).onIssuingCardPinRetrieved("1234");
    }

    @Test
    public void testUpdate()
            throws InvalidRequestException, APIConnectionException {

        StripeResponse response =
                new StripeResponse(200,
                        "{\"card\":\"ic_abcdef\",\"pin\":\"\"}",
                        null);

        when(mStripeApiRequestExecutor.execute(
                argThat(new ApiRequestMatcher(
                        StripeRequest.Method.POST,
                        "https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin",
                        ApiRequest.Options.create("ek_test_123")
                ))))
                .thenReturn(response);

        mMockUpdateListener =
                mock(IssuingCardPinService.IssuingCardPinUpdateListener.class);

        mService.updatePin(
                "ic_abcdef",
                "1234",
                "iv_abcd",
                "123-456",
                mMockUpdateListener);

        verify(mMockUpdateListener).onIssuingCardPinUpdated();
    }

    @Test
    public void testRetrievalFailsWithReason()
            throws InvalidRequestException, APIConnectionException {

        final StripeResponse response =
                new StripeResponse(400,
                        "{\"error\": {\"code\": \"incorrect_code\", \"message\": " +
                                "\"Verification failed\", \"type\": \"invalid_request_error\"}}",
                        null);

        when(mStripeApiRequestExecutor.execute(
                argThat(new ApiRequestMatcher(
                        StripeRequest.Method.GET,
                        "https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin?verification%5Bone_time_code%5D=123-456&verification%5Bid%5D=iv_abcd",
                        ApiRequest.Options.create("ek_test_123")
                ))))
                .thenReturn(response);

        mMockRetrievalListener =
                mock(IssuingCardPinService.IssuingCardPinRetrievalListener.class);

        mService.retrievePin(
                "ic_abcdef",
                "iv_abcd",
                "123-456",
                mMockRetrievalListener);

        verify(mMockRetrievalListener).onError(
                IssuingCardPinService.CardPinActionError.ONE_TIME_CODE_INCORRECT,
                "The one-time code was incorrect",
                null);
    }
}
