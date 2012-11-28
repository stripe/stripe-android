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
public class InvalidKeyTest extends StripeFunctionalTest {

    @Test
    public void invalid_key() throws Exception {
        Robolectric.getUiThreadScheduler().pause();
        final Stripe stripe = new Stripe("THIS_KEY_IS_INVALID");

        final StripeError[] errorResponse = new StripeError[1];

        // Create new Token
        Executor executor = new MockExecutor();
        stripe.createToken(card, executor, null,
                new StripeErrorHandler() {
                    @Override
                    public void onError(StripeError error) {
                        errorResponse[0] = error;
                    }
                });

        waitForExecutor();

        // Did we get a token?
        assertErrorCalled(errorResponse);

        assertEquals(StripeError.UNAUTHORIZED, errorResponse[0]);

    }
}


