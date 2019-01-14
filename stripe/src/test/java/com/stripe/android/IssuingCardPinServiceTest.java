package com.stripe.android;

import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRetrieval() {
        TestEphemeralKeyProvider ephemeralKeyProvider = new TestEphemeralKeyProvider();
        ephemeralKeyProvider.setNextRawEphemeralKey(EPHEMERAL_KEY);
        IssuingCardPinService service = IssuingCardPinService.create(ephemeralKeyProvider);

        StripeApiHandler.setMockGetStripeResponse(new StripeApiHandler.MockGetStripeResponse() {
            @Override
            public StripeResponse getStripeResponse(String method, String url, Map<String, Object> params, RequestOptions options) {
                if (!"GET".equals(method)) {
                    throw new RuntimeException("Invalid method " + method);
                }
                if (!url.equals("https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin")) {
                    throw new RuntimeException("Invalid url " + url);
                }
                if (params.get("verification") == null) {
                    throw new RuntimeException("Missing params");
                }
                Map verification = (Map) params.get("verification");
                if (!verification.get("id").equals("iv_abcd") || !verification.get("one_time_code").equals("123-456")) {
                    throw new RuntimeException("Invalid verification");
                }
                if (!options.getPublishableApiKey().equals("ek_test_123")) {
                    throw new RuntimeException("Invalid ephemeral key " + options.getPublishableApiKey());
                }
                return new StripeResponse(200, "{\"card\":\"ic_abcdef\",\"pin\":\"1234\"}", null);
            }
        });

        IssuingCardPinService.IssuingCardPinRetrievalListener mockListener =
                mock(IssuingCardPinService.IssuingCardPinRetrievalListener.class);

        service.retrievePin(
                "ic_abcdef",
                "iv_abcd",
                "123-456",
                mockListener);

        verify(mockListener).onIssuingCardPinRetrieved("1234");
    }

    @Test
    public void testUpdate() {
        TestEphemeralKeyProvider ephemeralKeyProvider = new TestEphemeralKeyProvider();
        ephemeralKeyProvider.setNextRawEphemeralKey(EPHEMERAL_KEY);
        IssuingCardPinService service = IssuingCardPinService.create(ephemeralKeyProvider);

        StripeApiHandler.setMockGetStripeResponse(new StripeApiHandler.MockGetStripeResponse() {
            @Override
            public StripeResponse getStripeResponse(String method, String url, Map<String, Object> params, RequestOptions options) {
                if (!"POST".equals(method)) {
                    throw new RuntimeException("Invalid method " + method);
                }
                if (!url.equals("https://api.stripe.com/v1/issuing/cards/ic_abcdef/pin")) {
                    throw new RuntimeException("Invalid url " + url);
                }
                if (params.get("pin") != "1234") {
                    throw new RuntimeException("Incorrect params pin: " + params.get("pin"));
                }
                if (params.get("verification") == null) {
                    throw new RuntimeException("Missing params");
                }
                Map verification = (Map) params.get("verification");
                if (!verification.get("id").equals("iv_abcd") || !verification.get("one_time_code").equals("123-456")) {
                    throw new RuntimeException("Invalid verification");
                }
                if (!options.getPublishableApiKey().equals("ek_test_123")) {
                    throw new RuntimeException("Invalid ephemeral key " + options.getPublishableApiKey());
                }
                return new StripeResponse(200, "{\"card\":\"ic_abcdef\",\"pin\":\"\"}", null);
            }
        });

        IssuingCardPinService.IssuingCardPinUpdateListener mockListener =
                mock(IssuingCardPinService.IssuingCardPinUpdateListener.class);

        service.updatePin(
                "ic_abcdef",
                "1234",
                "iv_abcd",
                "123-456",
                mockListener);

        verify(mockListener).onIssuingCardPinUpdated();
    }

    @Test
    public void testRetrievalFailsWithReason() {
        TestEphemeralKeyProvider ephemeralKeyProvider = new TestEphemeralKeyProvider();
        ephemeralKeyProvider.setNextRawEphemeralKey(EPHEMERAL_KEY);
        IssuingCardPinService service = IssuingCardPinService.create(ephemeralKeyProvider);

        StripeApiHandler.setMockGetStripeResponse(new StripeApiHandler.MockGetStripeResponse() {
            @Override
            public StripeResponse getStripeResponse(String method, String url, Map<String, Object> params, RequestOptions options) {
                return new StripeResponse(400, "{\"error\": {\"code\": \"incorrect_code\", \"message\": \"Verification failed\", \"type\": \"invalid_request_error\"}}", null);
            }
        });

        IssuingCardPinService.IssuingCardPinRetrievalListener mockListener =
                mock(IssuingCardPinService.IssuingCardPinRetrievalListener.class);

        service.retrievePin(
                "ic_abcdef",
                "iv_abcd",
                "123-456",
                mockListener);

        verify(mockListener).onError(IssuingCardPinService.CardPinActionError.ONE_TIME_CODE_INCORRECT, "The one-time code was incorrect", null);
    }

}
