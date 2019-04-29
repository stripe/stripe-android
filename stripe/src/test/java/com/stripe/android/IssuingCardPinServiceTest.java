package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private RequestExecutor mRequestExecutor;
    @Mock
    IssuingCardPinService.IssuingCardPinRetrievalListener mockRetrievalListener;
    @Mock
    IssuingCardPinService.IssuingCardPinUpdateListener mockUpdateListener;

    private IssuingCardPinService service;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        TestEphemeralKeyProvider ephemeralKeyProvider = new TestEphemeralKeyProvider();
        ephemeralKeyProvider.setNextRawEphemeralKey(EPHEMERAL_KEY);

        final StripeApiHandler apiHandler = new StripeApiHandler(
                ApplicationProvider.getApplicationContext(),
                mRequestExecutor,
                false
        );

        service = new IssuingCardPinService(ephemeralKeyProvider, apiHandler);
    }

    @Test
    public void testRetrieval()
            throws InvalidRequestException, APIConnectionException {

        StripeResponse response =
                new StripeResponse(200,
                        "{\"card\":\"ic_abcdef\",\"pin\":\"1234\"}",
                        null);

        when(mRequestExecutor.execute(
                eq(RequestExecutor.RestMethod.GET),
                eq("https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin"),
                ArgumentMatchers.<String, Object>anyMap(),
                any(RequestOptions.class)
        )).thenReturn(response);

        mockRetrievalListener =
                mock(IssuingCardPinService.IssuingCardPinRetrievalListener.class);

        service.retrievePin(
                "ic_abcdef",
                "iv_abcd",
                "123-456",
                mockRetrievalListener);

        verify(mockRetrievalListener).onIssuingCardPinRetrieved("1234");
    }

    @Test
    public void testUpdate()
            throws InvalidRequestException, APIConnectionException {

        StripeResponse response =
                new StripeResponse(200,
                        "{\"card\":\"ic_abcdef\",\"pin\":\"\"}",
                        null);

        when(mRequestExecutor.execute(
                eq(RequestExecutor.RestMethod.POST),
                eq("https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin"),
                ArgumentMatchers.<String, Object>anyMap(),
                any(RequestOptions.class)
        )).thenReturn(response);

        mockUpdateListener =
                mock(IssuingCardPinService.IssuingCardPinUpdateListener.class);

        service.updatePin(
                "ic_abcdef",
                "1234",
                "iv_abcd",
                "123-456",
                mockUpdateListener);

        verify(mockUpdateListener).onIssuingCardPinUpdated();
    }

    @Test
    public void testRetrievalFailsWithReason()
            throws InvalidRequestException, APIConnectionException {

        StripeResponse response =
                new StripeResponse(400,
                        "{\"error\": {\"code\": \"incorrect_code\", \"message\": " +
                                "\"Verification failed\", \"type\": \"invalid_request_error\"}}",
                        null);

        when(mRequestExecutor.execute(
                eq(RequestExecutor.RestMethod.GET),
                eq("https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin"),
                ArgumentMatchers.<String, Object>anyMap(),
                any(RequestOptions.class)
        )).thenReturn(response);

        mockRetrievalListener =
                mock(IssuingCardPinService.IssuingCardPinRetrievalListener.class);

        service.retrievePin(
                "ic_abcdef",
                "iv_abcd",
                "123-456",
                mockRetrievalListener);

        verify(mockRetrievalListener).onError(
                IssuingCardPinService.CardPinActionError.ONE_TIME_CODE_INCORRECT,
                "The one-time code was incorrect",
                null);
    }
}
