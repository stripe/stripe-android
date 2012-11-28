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
public class InvalidExpiryMonthTest extends StripeFunctionalTest {

    @Test
    public void invalid_expiry_month() throws Exception {
        Robolectric.getUiThreadScheduler().pause();
        final Stripe stripe = new Stripe(publishableKey);

        HashMap<String, String> cardMap = new HashMap<String, String>();
        cardMap.put("number", "4242424242424242");
        cardMap.put("cvc", "700");
        cardMap.put("exp_year", "2013");
        cardMap.put("exp_month", "13");

        final StripeError[] errorResponse = new StripeError[1];

        // Create new Token
        Executor executor = new MockExecutor();
        stripe.createToken(new Card(cardMap), executor,
                new StripeSuccessHandler() {
                    @Override
                    public void onSuccess(Token token) {
                    }
                },
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
        assertEquals(StripeError.CardErrorCode.InvalidExpMonth, errorResponse[0].cardErrorCode);
        assertEquals("expMonth", errorResponse[0].parameter);
    }


}


