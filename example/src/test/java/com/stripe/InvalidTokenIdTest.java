package com.stripe;


import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;

/*
 *  You may need to set ANDROID_HOME environment variable due to a hard-coded dependency in Robolectric.
 */
@RunWith(StripeRobolectricTestRunner.class)
public class InvalidTokenIdTest extends StripeFunctionalTest {

    @Test
    public void invalid_token_id() throws Exception {
        Robolectric.getUiThreadScheduler().pause();
        final Stripe stripe = new Stripe(publishableKey);

        final StripeError[] errorResponse = new StripeError[1];

        // Create new Token
        Executor executor = new MockExecutor();
        stripe.requestToken("INVALID_TOKEN", executor, null,
                new StripeErrorHandler() {
                    @Override
                    public void onError(StripeError error) {
                        errorResponse[0] = error;
                    }
                });

        waitForExecutor();

        // Did we get a token?
        assertErrorCalled(errorResponse);

        assertEquals(StripeError.StripeErrorCode.InvalidRequestError, errorResponse[0].errorCode);
        assertEquals("token", errorResponse[0].parameter);
    }
}


