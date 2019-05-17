package com.stripe.android;

import com.stripe.android.model.Token;

/**
 * An interface representing a callback to be notified about the results of
 * {@link Token} creation or requests
 */
public interface TokenCallback extends ApiResultCallback<Token> {
}
