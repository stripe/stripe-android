package com.stripe;


import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;

/*
 *  You may need to set ANDROID_HOME environment variable due to a hard-coded dependency in Robolectric.
 */
@RunWith(StripeRobolectricTestRunner.class)
public class InvalidCardNumberTest extends StripeFunctionalTest {

    @Test
    public void invalid_card_number() throws Exception {
        Robolectric.getUiThreadScheduler().pause();
        final Stripe stripe = new Stripe(publishableKey);

        HashMap<String, String> cardMap = new HashMap<String, String>();
        cardMap.put("number", "0000000000000000");
        cardMap.put("cvc", "700");
        cardMap.put("exp_year", "2013");
        cardMap.put("exp_month", "12");

        final StripeError[] errorResponse = new StripeError[1];

        // Create new Token
        Executor executor = new MockExecutor();
        stripe.createToken(new Card(cardMap), executor, null,
                new StripeErrorHandler() {
                    @Override
                    public void onError(StripeError error) {
                        errorResponse[0] = error;
                    }
                });

        waitForExecutor();

        // Did we get a token?
        assertErrorCalled(errorResponse);

        assertEquals(StripeError.StripeErrorCode.CardError, errorResponse[0].errorCode);
        assertEquals(StripeError.CardErrorCode.InvalidNumber, errorResponse[0].cardErrorCode);
        assertEquals("number", errorResponse[0].parameter);
    }
}


