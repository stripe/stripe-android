package com.stripe;


import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

/*
 *  You may need to set ANDROID_HOME environment variable due to a hard-coded dependency in Robolectric.
 */
@RunWith(StripeRobolectricTestRunner.class)
public class CreateRequestTokenTest extends StripeFunctionalTest {

    @Test
    public void create_then_request_token() throws Exception {
        Robolectric.getUiThreadScheduler().pause();
        final Stripe stripe = new Stripe(publishableKey);

        final Token[] createTokenResponse = new Token[1];

        // Create new Token
        Executor executor = new MockExecutor();
        stripe.createToken(card, executor,
                new StripeSuccessHandler() {
                    @Override
                    public void onSuccess(Token token) {
                        createTokenResponse[0] = token;
                    }
                }, null);

        waitForExecutor();

        // Did we get a token?
        assertSuccessCalled(createTokenResponse);

        String requestTokenID = createTokenResponse[0].tokenId;

        final Token[] getTokenResponse = new Token[1];


        // Request Token we just created by id
        executor = new MockExecutor();
        stripe.requestToken(requestTokenID, executor,
                new StripeSuccessHandler() {
                    @Override
                    public void onSuccess(Token token) {
                        getTokenResponse[0] = token;
                    }
                }, null);

        waitForExecutor();

        // Did we get the same token back?
        assertSuccessCalledWithTokenID(getTokenResponse, requestTokenID);
    }
}


