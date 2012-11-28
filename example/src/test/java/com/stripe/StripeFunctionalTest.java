package com.stripe;


import com.stripe.activity.PaymentActivity;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.util.Scheduler;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 *  You may need to set ANDROID_HOME environment variable due to a hard-coded dependency in Robolectric.
 */
@RunWith(StripeRobolectricTestRunner.class)
public abstract class StripeFunctionalTest {

    /*
     * You need to set this to your stripe test publishable key.
     *
     * For more info, see https://stripe.com/docs/stripe.js
     *
     * E.g.
     *
     *   private static final String publishableKey = "pk_something123456789";
     *
     */
    protected static final String publishableKey = PaymentActivity.PUBLISHABLE_KEY;

    protected Card card;

    @Before
    public void setup() {
        HashMap<String, String> cardMap = new HashMap<String, String>();
        cardMap.put("number", "4242424242424242");
        cardMap.put("cvc", "123");
        cardMap.put("exp_year", "2013");
        cardMap.put("exp_month", "10");
        card = new Card(cardMap);
    }

    protected void assertSuccessCalled(Token[] token) {
        assertNotNull("Expected successHandler to have set reponseToken.", token[0]);
    }

    protected void assertErrorCalled(StripeError[] errors) {
        assertNotNull("Expected errorHandler to have generated errors.", errors[0]);
    }

    protected void assertSuccessCalledWithTokenID(Token[] token, String requestTokenID) {
        assertSuccessCalled(token);
        assertEquals("Same token id to be returned.", requestTokenID, token[0].tokenId);
    }

    protected void waitForExecutor() throws InterruptedException {
        Scheduler scheduler = Robolectric.getUiThreadScheduler();
        assertEquals("Tasks in queue", 1, scheduler.enqueuedTaskCount());
        scheduler.runOneTask();
    }

    class MockExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}


